package com.familykitchen.file.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.file.model.vo.FileUploadResponse;
import com.familykitchen.file.model.entity.FileAssetDO;
import com.familykitchen.file.mapper.FileAssetMapper;
import com.familykitchen.file.service.FileUploadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传应用服务实现。
 *
 * <p>图片写入 {@code family-kitchen.file-storage.local-root} 所配置根目录下的
 * {@code images} 子目录；配置缺省时边界为 {@code ./uploads/images}。
 * 所有目标路径均规范化并校验不得越出该图片目录。</p>
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private static final long MAX_IMAGE_SIZE = 4L * 1024L * 1024L;
  private final Path imageDir;
  private final FileAssetMapper fileAssetMapper;

  /**
   * 创建本地图片上传服务。
   * @param localRoot 本地文件存储根目录
   * @param fileAssetMapper 文件资产持久化接口
   */
  public FileUploadServiceImpl(
      @Value("${family-kitchen.file-storage.local-root:./uploads}") String localRoot,
      FileAssetMapper fileAssetMapper) {
    this.imageDir = Path.of(localRoot).toAbsolutePath().normalize().resolve("images");
    this.fileAssetMapper = fileAssetMapper;
  }

  /**
   * 校验图片声明类型与文件签名后写入限定目录，并登记文件资产。
   * <p>同时检查 MIME 类型和魔数，避免仅伪造请求头便将非图片内容写入可公开访问目录。</p>
   * @param ownerUserId 文件归属的账号标识
   * @param file 待上传的图片
   * @return 已持久化的文件标识、访问路径和图片元数据
   */
  @Override
  public FileUploadResponse uploadImage(Long ownerUserId, MultipartFile file) {
    validateImage(file);

    String storedName = buildStoredFileName(file.getOriginalFilename());
    Integer width;
    Integer height;
    try {
      byte[] bytes=file.getBytes();
      validateMagic(bytes,file.getContentType());
      var image=ImageIO.read(new java.io.ByteArrayInputStream(bytes));
      width=image==null?null:image.getWidth(); height=image==null?null:image.getHeight();
      Files.createDirectories(imageDir);
      Path target=imageDir.resolve(storedName).normalize();
      if(!target.startsWith(imageDir))throw new BusinessException(ErrorCode.BAD_REQUEST,"非法文件名");
      Files.write(target,bytes);
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
    }
    String objectKey="images/"+storedName;String url="/uploads/"+objectKey;
    FileAssetDO asset=new FileAssetDO();asset.setOwnerType("user");asset.setOwnerId(ownerUserId);
    asset.setFileType("image");asset.setStorageType("local");asset.setUrl(url);asset.setObjectKey(objectKey);
    asset.setMimeType(file.getContentType());asset.setSizeBytes(file.getSize());asset.setWidth(width);asset.setHeight(height);
    fileAssetMapper.insert(asset);
    return new FileUploadResponse(asset.getId(),url,width,height,file.getSize());
  }

  /**
   * 校验上传文件是否满足图片上传规则。
   */
  private static void validateImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
    }
    if (file.getSize() > MAX_IMAGE_SIZE) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "图片大小不能超过 4MB");
    }
    if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 jpeg/png/webp 图片格式");
    }
  }

  /**
   * 生成本地存储文件名，避免直接使用原始文件名造成冲突。
   */
  private static String buildStoredFileName(String originalFilename) {
    String source = originalFilename == null || originalFilename.isBlank() ? "image" : originalFilename;
    int dotIndex = source.lastIndexOf('.');
    String extension = "";
    if (dotIndex >= 0 && dotIndex < source.length() - 1) {
      extension = "." + source.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
    return UUID.randomUUID().toString().replace("-", "") + extension;
  }

  private static void validateMagic(byte[] bytes,String mimeType){boolean valid=switch(mimeType){
    case "image/jpeg" -> bytes.length>2&&(bytes[0]&255)==0xff&&(bytes[1]&255)==0xd8&&(bytes[2]&255)==0xff;
    case "image/png" -> bytes.length>7&&(bytes[0]&255)==0x89&&bytes[1]==0x50&&bytes[2]==0x4e&&bytes[3]==0x47;
    case "image/webp" -> bytes.length>11&&bytes[0]=='R'&&bytes[1]=='I'&&bytes[2]=='F'&&bytes[3]=='F'
        &&bytes[8]=='W'&&bytes[9]=='E'&&bytes[10]=='B'&&bytes[11]=='P';
    default -> false;};
    if(!valid)throw new BusinessException(ErrorCode.BAD_REQUEST,"文件内容与图片格式不匹配");
  }
}
