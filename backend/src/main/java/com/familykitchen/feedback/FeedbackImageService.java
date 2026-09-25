package com.familykitchen.feedback;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** Authenticated private image storage; no URL or filesystem path escapes the service. */
@Service
public class FeedbackImageService {
  private static final Logger LOG=LoggerFactory.getLogger(FeedbackImageService.class);
  private static final int MAX_BYTES=4*1024*1024;
  private final FeedbackMapper mapper;
  private final TransactionTemplate transactions;
  private final Path root,publicRoot;
  /**
   * Resolve storage roots and reject overlapping public/private locations.
   * @param mapper persistence boundary
   * @param manager transaction manager
   * @param privateRoot private directory
   * @param publicRoot public uploads directory
   * @throws IOException when storage roots cannot be resolved
   */
  public FeedbackImageService(FeedbackMapper mapper,PlatformTransactionManager manager,
      @Value("${family-kitchen.feedback.private-root:./data/feedback-private}") String privateRoot,
      @Value("${family-kitchen.file-storage.local-root:./uploads}") String publicRoot) throws IOException {
    this.mapper=mapper;this.transactions=new TransactionTemplate(manager);
    Files.createDirectories(Path.of(publicRoot));this.publicRoot=Path.of(publicRoot).toRealPath();
    Files.createDirectories(Path.of(privateRoot));this.root=Path.of(privateRoot).toRealPath();checkRoots();
  }
  /**
   * Authorized image bytes without disk metadata.
   * @param mime validated media type
   * @param bytes image content
   */
  public record ImageData(String mime,byte[] bytes) {}
  private void checkRoots() throws IOException {
    Path real=root.toRealPath(),publicReal=publicRoot.toRealPath();
    if(!real.equals(root)||real.startsWith(publicReal)||publicReal.startsWith(real))throw new IllegalStateException("反馈私有目录不能位于公开图片目录内或包含公开目录");
  }
  /**
   * Validate and store one image under an account quota lock.
   * @param user authenticated uploader
   * @param file multipart image
   * @return opaque image ID
   */
  public Map<String,String> upload(CurrentUserContext user,MultipartFile file) {
    Path[] written={null};
    try {
      return transactions.execute(tx->{
        if(mapper.lockUser(user.userId())==null)throw new BusinessException(ErrorCode.UNAUTHORIZED,"账号不可用");
        LocalDateTime now=LocalDateTime.now();
        if(mapper.countRecentImages(user.userId(),now.minusHours(1))>=30)throw FeedbackService.conflict("每小时最多上传 30 张反馈图片，请稍后重试");
        try {
          checkRoots();
          if(file==null||file.isEmpty()||file.getSize()>MAX_BYTES)throw FeedbackService.bad("图片需大于 0 且不超过 4 MiB");
          String mime=file.getContentType();if(!Set.of("image/jpeg","image/png").contains(mime==null?"":mime))throw FeedbackService.bad("图片仅支持 JPEG/PNG 格式");
          byte[] bytes;try(InputStream input=file.getInputStream()){bytes=input.readNBytes(MAX_BYTES+1);}
          if(bytes.length==0||bytes.length>MAX_BYTES)throw FeedbackService.bad("图片需大于 0 且不超过 4 MiB");
          boolean png=bytes.length>=8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
          boolean jpeg=bytes.length>=3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255;
          if(!(mime.equals("image/png")?png:jpeg))throw FeedbackService.bad("图片格式与内容不符");
          int width,height;
          try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw FeedbackService.bad("图片无法解码");
            ImageReader reader=readers.next();try {
              reader.setInput(input,true,true);String format=reader.getFormatName();
              if(!(png?format.equalsIgnoreCase("png"):format.equalsIgnoreCase("jpeg")))throw FeedbackService.bad("图片格式无效");
              width=reader.getWidth(0);height=reader.getHeight(0);
              if(width<1||height<1||(long)width*height>20_000_000L)throw FeedbackService.bad("图片像素不能超过 2000 万");
              if(reader.read(0)==null)throw FeedbackService.bad("图片无法解码");
            } finally {reader.dispose();}
          }
          FeedbackImage image=new FeedbackImage();image.id=UUID.randomUUID().toString();image.objectKey=UUID.randomUUID()+(png?".png":".jpg");
          image.ownerUserId=user.userId();image.mime=mime;image.width=width;image.height=height;image.byteSize=bytes.length;image.createdAt=now;
          Path target=root.resolve(image.objectKey);written[0]=target;Files.write(target,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
          if(mapper.insertImage(image)!=1)throw new IllegalStateException("附件登记失败");
          return Map.of("imageId",image.id);
        } catch(IOException e) {throw FeedbackService.bad("图片读取或保存失败，请重新上传");}
      });
    } catch(RuntimeException failure) {
      if(written[0]!=null)try{Files.deleteIfExists(written[0]);}catch(IOException cleanup){LOG.error("Feedback upload compensation failed for {}; orphan cleanup will retry",written[0].getFileName(),cleanup);}
      throw failure;
    }
  }
  /**
   * Read an owner image or a submitted-feedback image for a platform administrator.
   * @param user authenticated caller
   * @param id opaque image ID
   * @return authorized bytes
   */
  public ImageData read(CurrentUserContext user,String id) {
    FeedbackImage image=mapper.image(id);
    if(image==null||(!user.userId().equals(image.ownerUserId)&&!(user.hasPlatformBackendAccess()&&image.feedbackId!=null))
        ||(image.feedbackId==null&&!image.createdAt.isAfter(LocalDateTime.now().minusHours(24))))throw new BusinessException(ErrorCode.NOT_FOUND,"图片不存在");
    try {return new ImageData(image.mime,Files.readAllBytes(existing(image.objectKey)));}
    catch(IOException e){throw new BusinessException(ErrorCode.NOT_FOUND,"图片不存在");}
  }
  private Path existing(String key) throws IOException {
    checkRoots();if(key==null||!key.matches("[A-Za-z0-9-]+\\.(png|jpg)"))throw new IOException("Invalid object key");
    Path path=root.resolve(key);Path real=path.toRealPath();
    if(!real.getParent().equals(root)||!real.equals(path)||Files.isSymbolicLink(path))throw new IOException("Private image boundary violation");
    return real;
  }
  @Scheduled(fixedDelayString="${family-kitchen.feedback.cleanup-delay-ms:3600000}",initialDelayString="${family-kitchen.feedback.cleanup-delay-ms:3600000}")
  /**
   * Remove only expired unbound attachments and old unregistered private objects.
   */
  public void cleanupExpired() {
    LocalDateTime cutoff=LocalDateTime.now().minusHours(24);
    for(String id:mapper.expired(cutoff)) {
      try {transactions.executeWithoutResult(tx->{
        FeedbackImage image=mapper.lockImage(id);
        if(image==null||image.feedbackId!=null||image.createdAt.isAfter(cutoff))return;
        try {
          checkRoots();Path path=root.resolve(image.objectKey);
          if(Files.exists(path,LinkOption.NOFOLLOW_LINKS))Files.delete(existing(image.objectKey));
          mapper.deleteImage(id);
        }catch(IOException e){throw new IllegalStateException("Private feedback cleanup failed",e);}
      });}catch(RuntimeException failure){LOG.warn("Feedback image cleanup will retry imageId={}",id,failure);}
    }
    cleanupOrphans();
  }
  private void cleanupOrphans() {
    // Only our random object names in this dedicated private root are eligible. A grace
    // period excludes in-flight uploads; failures leave files present for the next run.
    try {
      checkRoots();java.time.Instant cutoff=java.time.Instant.now().minusSeconds(24*3600);
      try(var files=Files.list(root)) {
        for(Path file:files.filter(p->p.getFileName().toString().matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(png|jpg)")).toList()) {
          try {
            if(Files.getLastModifiedTime(file,LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(cutoff)
                &&mapper.imageByObject(file.getFileName().toString())==null)Files.delete(existing(file.getFileName().toString()));
          }catch(IOException|RuntimeException e){LOG.warn("Feedback orphan cleanup will retry object={}",file.getFileName(),e);}
        }
      }
    }catch(IOException e){LOG.warn("Feedback orphan scan will retry",e);}
  }
}
