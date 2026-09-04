package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.dish.config.DishTemplateAssetProperties;
import com.familykitchen.dish.model.entity.DishTemplateImageAssetEntity;
import com.familykitchen.dish.service.impl.DishTemplateImageStorageServiceImpl;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证私有图片路径边界和按内容摘要发布行为。 */
class DishTemplateImageStorageServiceTest {
  @TempDir Path root;

  @Test
  void rejectsTraversalOutsidePrivateRoot() {
    var service = service();
    DishTemplateImageAssetEntity asset = asset("../secret.jpg", "a".repeat(64));
    assertThrows(BusinessException.class, () -> service.preview(asset));
  }

  @Test
  void publishesContentNamedFileWithoutExposingPrivateKey() throws Exception {
    Path privateFile = root.resolve("private/cooklikehoc/source.jpg");
    Files.createDirectories(privateFile.getParent());
    Files.write(privateFile, new byte[] {1, 2, 3});
    String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(new byte[] {1, 2, 3}));
    DishTemplateImageAssetEntity asset = asset("cooklikehoc/source.jpg", sha);
    asset.setFileSize(3L);

    var result = service().publish(asset);

    assertEquals("/uploads/dish-template-assets/" + sha + ".jpg", result.publicUrl());
    assertTrue(Files.isRegularFile(result.path()));
    assertTrue(result.newlyCreated());
  }

  private DishTemplateImageStorageServiceImpl service() {
    DishTemplateAssetProperties properties = new DishTemplateAssetProperties();
    properties.setPrivateRoot(root.resolve("private"));
    properties.setPublicRoot(root.resolve("public"));
    properties.setPublicUrlPrefix("/uploads/dish-template-assets");
    return new DishTemplateImageStorageServiceImpl(properties);
  }

  private static DishTemplateImageAssetEntity asset(String key, String sha) {
    DishTemplateImageAssetEntity asset = new DishTemplateImageAssetEntity();
    asset.setInternalStorageKey(key); asset.setContentSha256(sha); asset.setMimeType("image/jpeg");
    return asset;
  }
}
