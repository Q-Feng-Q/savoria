package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 验证模板图片均已本地化并保留可追溯授权信息。 */
class DishTemplateImageAssetTest {

  private static final Path DATA_DIR = Path.of("src/main/resources/db/data");
  private static final Path IMAGE_DIR = Path.of("src/main/resources/static/images/dish-templates");
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void everyTemplateHasReadableLocalJpegAndAttribution() throws Exception {
    List<Map<String, Object>> manifest = objectMapper.readValue(
        DATA_DIR.resolve("dish-template-image-manifest.json").toFile(), new TypeReference<>() {});
    Map<String, Map<String, String>> sources = objectMapper.readValue(
        DATA_DIR.resolve("dish-template-image-sources.json").toFile(), new TypeReference<>() {});

    assertEquals(240, manifest.size());
    assertEquals(240, sources.size());
    for (Map<String, Object> item : manifest) {
      String code = item.get("templateCode").toString();
      String fileName = item.get("imageFileName").toString();
      Path image = IMAGE_DIR.resolve(fileName);
      assertTrue(Files.isRegularFile(image), code + " 缺少本地图片");
      assertTrue(Files.size(image) > 1024 && Files.size(image) <= 800_000, code + " 图片体积不合规");
      byte[] header = Files.readAllBytes(image);
      assertTrue((header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8, code + " 不是有效 JPEG");
      Map<String, String> source = sources.get(code);
      assertTrue(source.getOrDefault("sourcePage", "").startsWith("https://"));
      assertTrue(!source.getOrDefault("author", "").isBlank());
      assertTrue(!source.getOrDefault("license", "").isBlank());
    }
  }
}
