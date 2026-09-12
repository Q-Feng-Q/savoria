package com.familykitchen.dish.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.dish.config.DishTemplateAssetProperties;
import com.familykitchen.dish.model.entity.DishTemplateImageAssetEntity;
import com.familykitchen.dish.service.DishTemplateImageStorageService;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

/** 基于本地目录的模板图片受控存储实现。 */
@Service
public class DishTemplateImageStorageServiceImpl implements DishTemplateImageStorageService {
  private static final Map<String, String> EXTENSIONS = Map.of(
      "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp", "image/gif", ".gif");
  private final Path privateRoot;
  private final Path publicRoot;
  private final String publicUrlPrefix;

  /**
   * 创建文件存储服务并规范化配置根目录。
   * @param properties 模板图片目录配置
   */
  public DishTemplateImageStorageServiceImpl(DishTemplateAssetProperties properties) {
    privateRoot = properties.getPrivateRoot().toAbsolutePath().normalize();
    publicRoot = properties.getPublicRoot().toAbsolutePath().normalize();
    publicUrlPrefix = properties.getPublicUrlPrefix().replaceAll("/+$", "");
  }

  /** {@inheritDoc} */
  @Override
  public PreviewResource preview(DishTemplateImageAssetEntity asset) {
    Path file = controlledPath(privateRoot, asset.getInternalStorageKey());
    try {
      if (!Files.isRegularFile(file)) throw notFound();
      long actualSize = Files.size(file);
      if (asset.getFileSize() != null && asset.getFileSize() != actualSize) {
        throw new BusinessException(ErrorCode.STATE_CONFLICT, "内部图片文件大小与审核记录不一致");
      }
      if (!asset.getContentSha256().equalsIgnoreCase(sha256(file))) {
        throw new BusinessException(ErrorCode.STATE_CONFLICT, "内部图片内容摘要与审核记录不一致");
      }
      return new PreviewResource(new FileSystemResource(file), asset.getMimeType(), actualSize);
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取内部图片失败");
    }
  }

  /** {@inheritDoc} */
  @Override
  public PublishedResource publish(DishTemplateImageAssetEntity asset) {
    PreviewResource preview = preview(asset);
    String extension = EXTENSIONS.get(asset.getMimeType());
    if (extension == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持发布该图片格式");
    String fileName = asset.getContentSha256().toLowerCase() + extension;
    Path target = controlledPath(publicRoot, fileName);
    try {
      Files.createDirectories(publicRoot);
      if (Files.isRegularFile(target)) {
        if (!asset.getContentSha256().equalsIgnoreCase(sha256(target))) {
          throw new BusinessException(ErrorCode.STATE_CONFLICT, "公共图片文件内容与文件名摘要不一致");
        }
        return new PublishedResource(publicUrlPrefix + "/" + fileName, target, false);
      }
      Path temporary = Files.createTempFile(publicRoot, ".publishing-", ".tmp");
      try {
        Files.copy(preview.resource().getInputStream(), temporary, StandardCopyOption.REPLACE_EXISTING);
        try {
          Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
          Files.move(temporary, target);
        }
      } finally {
        Files.deleteIfExists(temporary);
      }
      return new PublishedResource(publicUrlPrefix + "/" + fileName, target, true);
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发布模板图片失败");
    }
  }

  /** {@inheritDoc} */
  @Override
  public void deleteNewFile(PublishedResource resource) {
    if (!resource.newlyCreated()) return;
    try {
      Files.deleteIfExists(controlledPath(publicRoot, resource.path().getFileName().toString()));
    } catch (IOException ignored) {
      // 回滚清理失败由运维文件巡检处理，不能覆盖原始数据库异常。
    }
  }

  private static Path controlledPath(Path root, String key) {
    if (key == null || key.isBlank() || Path.of(key).isAbsolute()) throw notFound();
    Path candidate = root.resolve(key).normalize();
    if (!candidate.startsWith(root)) throw notFound();
    return candidate;
  }

  private static BusinessException notFound() {
    return new BusinessException(ErrorCode.NOT_FOUND, "内部图片不存在");
  }

  private static String sha256(Path file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (var input = Files.newInputStream(file)) {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("JVM不支持SHA-256", exception);
    }
  }
}
