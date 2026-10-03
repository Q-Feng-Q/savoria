package com.familykitchen.notebook.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.mapper.NotebookImageMapper;
import com.familykitchen.notebook.mapper.NotebookRecordMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** Private image storage guarded by full notebook record authorization. */
@Service
public class NotebookImageService {
  private static final int MAX_BYTES = 4 * 1024 * 1024;
  /** Authorized image bytes with no storage path exposed.
   * @param contentType validated media type
   * @param bytes image bytes */
  public record Data(String contentType, byte[] bytes) {}
  /** Opaque image ID for API access and private value key for IMAGE fields.
   * @param imageId image ID
   * @param valueKey private record field key */
  public record Uploaded(long imageId, String valueKey) {}

  private final NotebookImageMapper images;
  private final NotebookRecordMapper records;
  private final NotebookAccessPolicy access;
  private final NotebookAuditService audit;
  private final Path root;
  private final Path publicRoot;

  /** Creates private image operations from application storage configuration.
   *
   * @param images metadata persistence
   * @param records record persistence
   *
   * @param access notebook access policy
   * @param audit action audit
   *
   * @param privateRoot private directory
   * @param publicRoot public upload directory */
  @Autowired
  public NotebookImageService(NotebookImageMapper images, NotebookRecordMapper records,
      NotebookAccessPolicy access, NotebookAuditService audit,
      @Value("${family-kitchen.notebook.private-root:./data/notebook-private}") String privateRoot,
      @Value("${family-kitchen.file-storage.local-root:./uploads}") String publicRoot) {
    this(images, records, access, audit, Path.of(privateRoot), Path.of(publicRoot));
  }

  /** Creates private image operations with explicit roots for isolated tests.
   *
   * @param images metadata persistence
   * @param records record persistence
   *
   * @param access notebook access policy
   * @param audit action audit
   *
   * @param root private directory
   * @param publicRoot public directory */
  public NotebookImageService(NotebookImageMapper images, NotebookRecordMapper records,
      NotebookAccessPolicy access, NotebookAuditService audit, Path root, Path publicRoot) {
    this.images = images; this.records = records; this.access = access; this.audit = audit;
    this.root = root.toAbsolutePath().normalize(); this.publicRoot = publicRoot.toAbsolutePath().normalize();
  }

  /** Uploads a validated image for an editable record.
   *
   * @param actor authenticated user
   * @param recordId record ID
   * @param file multipart image
   *
   * @return opaque image ID and record value key */
  @Transactional
  public Uploaded upload(long actor, long recordId, MultipartFile file) {
    var record = record(recordId);
    access.requireEdit(actor, record.eventId(), record.from().atOffset(ZoneOffset.UTC),
        record.to().atOffset(ZoneOffset.UTC));
    if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES
        || !Set.of("image/png", "image/jpeg").contains(file.getContentType())) throw bad();
    Path path = null;
    try {
      byte[] bytes = file.getInputStream().readNBytes(MAX_BYTES + 1);
      if (bytes.length == 0 || bytes.length > MAX_BYTES) throw bad();
      boolean png = bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8),
          new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
      boolean jpeg = bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216
          && (bytes[2] & 255) == 255;
      if (!(file.getContentType().equals("image/png") ? png : jpeg)) throw bad();
      try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
        var readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) throw bad();
        ImageReader reader = readers.next();
        try {
          reader.setInput(input, true, true);
          if (!(png ? reader.getFormatName().equalsIgnoreCase("png")
              : reader.getFormatName().equalsIgnoreCase("jpeg"))) throw bad();
          int width = reader.getWidth(0), height = reader.getHeight(0);
          if (width < 1 || height < 1 || (long) width * height > 20_000_000L
              || reader.read(0) == null) throw bad();
        } finally { reader.dispose(); }
      }
      Path privateDirectory = checkedRoot(true);
      path = privateDirectory.resolve(UUID.randomUUID() + (png ? ".png" : ".jpg"));
      Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
      String name = file.getOriginalFilename();
      if (name == null || name.isBlank()) name = "image";
      name = Path.of(name).getFileName().toString();
      if (name.length() > 255) name = name.substring(0, 255);
      long id = images.insert(recordId, record.ownerUserId(), path.getFileName().toString(),
          name, file.getContentType(), bytes.length);
      Path saved = path;
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) try { Files.deleteIfExists(saved); }
            catch (IOException ignored) { /* orphan cleanup can retry */ }
          }
        });
      }
      audit.record(actor, record.ownerUserId(), record.eventId(), recordId, "IMAGE_UPLOAD");
      return new Uploaded(id, path.getFileName().toString());
    } catch (IOException failure) {
      if (path != null) try { Files.deleteIfExists(path); } catch (IOException ignored) { /* retry */ }
      throw bad();
    } catch (RuntimeException failure) {
      if (path != null) try { Files.deleteIfExists(path); } catch (IOException ignored) { /* retry via orphan scan */ }
      throw failure;
    }
  }

  /** Reads authorized private bytes.
   * @param actor authenticated user
   * @param imageId image ID
   *
   * @return authorized image bytes */
  public Data read(long actor, long imageId) {
    var image = images.find(imageId);
    if (image == null) throw missing();
    var record = record(image.recordId());
    if (record.ownerUserId() != image.ownerUserId()) throw missing();
    access.requireRecordRead(actor, record.eventId(), record.from(), record.to());
    try {
      Path root = checkedRoot(false);
      Path path = safeImage(root, image.storageKey());
      var data = new Data(image.contentType(), Files.readAllBytes(path));
      audit.record(actor, record.ownerUserId(), record.eventId(), record.id(), "IMAGE_VIEW");
      return data;
    } catch (IOException failure) { throw missing(); }
  }

  /** Deletes metadata and queues physical cleanup for an editable record.
   *
   * @param actor authenticated user
   * @param imageId image ID */
  @Transactional
  public void delete(long actor, long imageId) {
    var image = images.find(imageId);
    if (image == null) throw missing();
    var record = record(image.recordId());
    if (record.ownerUserId() != image.ownerUserId()) throw missing();
    access.requireEdit(actor, record.eventId(), record.from().atOffset(ZoneOffset.UTC),
        record.to().atOffset(ZoneOffset.UTC));
    images.enqueueCleanup(image, record.eventId());
    if (images.delete(imageId) != 1) throw missing();
    audit.record(actor, record.ownerUserId(), record.eventId(), record.id(), "IMAGE_DELETE");
  }

  /** Lists image display metadata for an already-authorized record.
   *
   * @param recordId record ID
   * @return metadata without key or URL */
  public java.util.List<java.util.Map<String, Object>> metadata(long recordId) {
    return images.forRecord(recordId).stream().map(image -> java.util.Map.<String, Object>of(
        "id", image.id(), "originalName", image.originalName(),
        "contentType", image.contentType(), "byteSize", image.byteSize())).toList();
  }

  /** Resolves only referenced private image keys to export-safe metadata.
   *
   * @param recordId authorized record ID
   * @param keys IMAGE field values
   *
   * @return metadata without storage keys or bytes */
  public java.util.List<java.util.Map<String, Object>> metadataForKeys(long recordId,
      java.util.List<?> keys) {
    if (keys == null) return java.util.List.of();
    return images.forRecord(recordId).stream().filter(image -> keys.contains(image.storageKey()))
        .map(image -> java.util.Map.<String, Object>of("id", image.id(),
            "originalName", image.originalName(), "contentType", image.contentType(),
            "byteSize", image.byteSize())).toList();
  }

  private NotebookRecordMapper.Row record(long id) {
    var record = records.findAny(id);
    if (record == null) throw missing();
    return record;
  }

  private Path checkedRoot(boolean create) throws IOException {
    if (create) Files.createDirectories(root);
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Private root missing");
    Path real = root.toRealPath();
    Path publicReal = Files.exists(publicRoot) ? publicRoot.toRealPath() : publicRoot;
    if (!real.equals(root) || real.startsWith(publicReal) || publicReal.startsWith(real))
      throw new IOException("Private/public storage overlap");
    return real;
  }

  private Path safeImage(Path directory, String key) throws IOException {
    if (key == null || !key.matches("[a-f0-9-]{36}\\.(png|jpg)")) throw new IOException("Invalid key");
    Path path = directory.resolve(key).normalize();
    if (!path.getParent().equals(directory) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        || !path.toRealPath().equals(path)) throw new IOException("Unsafe image path");
    return path;
  }

  private static BusinessException bad() { return new BusinessException(ErrorCode.BAD_REQUEST, "Invalid notebook image"); }
  private static BusinessException missing() { return new BusinessException(ErrorCode.NOT_FOUND, "Notebook image not found"); }
}
