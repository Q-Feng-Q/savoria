package com.familykitchen.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.system.mapper.SystemAuditMapper;
import com.familykitchen.system.mapper.SystemSettingMapper;
import com.familykitchen.system.model.dto.SystemSettingRequest;
import com.familykitchen.system.model.dto.BrandSettingRequest;
import com.familykitchen.system.model.entity.SystemSettingDO;
import com.familykitchen.system.security.PlatformSecretCipher;
import com.familykitchen.system.service.impl.SystemSettingServiceImpl;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class BrandSettingTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  @Test void publicBrandHasDefaultSizesAndNoSecrets() {
    var mapper = mock(SystemSettingMapper.class);
    var row = new SystemSettingDO();
    row.setSmtpPasswordCiphertext("secret");
    when(mapper.selectCurrent()).thenReturn(row);
    var service = new SystemSettingServiceImpl(mapper, mock(SystemAuditMapper.class), new PlatformSecretCipher("test"));
    var tree = json.valueToTree(service.publicCurrent());
    assertThat(tree.path("siteLogoSmallSize").asInt()).isEqualTo(32);
    assertThat(tree.path("siteLogoSize").asInt()).isEqualTo(56);
    assertThat(tree.path("siteLogoLargeSize").asInt()).isEqualTo(96);
    assertThat(tree.path("siteName").asText()).isEqualTo("食光栀味");
    assertThat(tree.size()).isEqualTo(13);
    assertThat(tree.toString()).doesNotContain("smtp", "updatedBy", "secret");
  }

  @Test void fullUpdateRejectsUnsafeLogoUrls() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      for (var url : new String[]{"javascript:alert(1)", "data:image/png;base64,abc", "file:///x", "//evil.test/x", "http://example.com/x", "/\\evil", "https://user:pass@example.com/x", "https://", "/x\n"}) {
        var request = new SystemSettingRequest("品牌", url, true, false, "维护中", true, true, true,
            null, null, null, null, false, null);
        assertThat(factory.getValidator().validate(request)).as(url).isNotEmpty();
      }
    }
  }

  @Test void validatesEveryUrlAndSizeBoundary() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      for (var url : new String[]{null, "", "/uploads/brand.png", "https://cdn.example.com/logo.png"}) {
        assertThat(validator.validate(new BrandSettingRequest(null, url, url, url, url, 16, 24, 48))).isEmpty();
        assertThat(validator.validate(new BrandSettingRequest("品牌", url, url, url, url, 64, 120, 160))).isEmpty();
      }
      assertThat(validator.validate(new BrandSettingRequest(" ", null, null, null, null, 15, 23, 47))).hasSize(4);
      assertThat(validator.validate(new BrandSettingRequest(null, null, null, null, null, 65, 121, 161))).hasSize(3);
      assertThat(validator.validate(new BrandSettingRequest(null, "//x", "data:x", "file:x", "javascript:x", null, null, null))).hasSize(4);
      assertThat(validator.validate(new BrandSettingRequest(null, null, "/" + "x".repeat(500), null, null, null, null, null))).isNotEmpty();
    }
  }

  @Test void patchWritesWithoutReadingStaleSettingsAndInvalidatesCache() {
    var mapper = mock(SystemSettingMapper.class);
    var audits = mock(SystemAuditMapper.class);
    var before = new SystemSettingDO();before.setSiteName("旧名称");
    var after = new SystemSettingDO();after.setSiteName("新名称");after.setSiteLogoSmallSize(40);
    when(mapper.selectCurrent()).thenReturn(before, after);
    var service = new SystemSettingServiceImpl(mapper, audits, new PlatformSecretCipher("test"));
    service.current();
    clearInvocations(mapper);
    var result = service.updateBranding(7L, new BrandSettingRequest(" 新名称 ", null, "", null, null, 40, null, null));
    var captured = org.mockito.ArgumentCaptor.forClass(SystemSettingDO.class);
    var order = inOrder(mapper, audits);
    order.verify(mapper).updateBranding(captured.capture());
    order.verify(audits).insert(eq(7L), eq("SYSTEM_SETTINGS_UPDATE"), anyString());
    order.verify(mapper).selectCurrent();
    verify(mapper, never()).update(any());
    assertThat(captured.getValue().getSiteName()).isEqualTo("新名称");
    assertThat(captured.getValue().getSiteLogoSmallUrl()).isEmpty();
    assertThat(captured.getValue().getSiteLogoUrl()).isNull();
    assertThat(captured.getValue().getSmtpHost()).isNull();
    assertThat(captured.getValue().getMaintenanceEnabled()).isNull();
    assertThat(result.siteName()).isEqualTo("新名称");
    assertThat(result.siteLogoSmallSize()).isEqualTo(40);
  }

  @Test void omittedAndExplicitNullFieldsKeepTheSamePatchSemantics() throws Exception {
    var omitted = json.readValue("{}", BrandSettingRequest.class);
    var explicit = json.readValue("{\"siteLogoUrl\":null,\"siteLogoSmallUrl\":null,\"siteLogoSize\":null}", BrandSettingRequest.class);
    assertThat(explicit).isEqualTo(omitted);
    var legacy = json.readValue("{\"siteName\":\"Custom\",\"maintenanceMessage\":\"Maintenance\"}", SystemSettingRequest.class);
    assertThat(legacy.siteLogoSmallUrl()).isNull();
    assertThat(legacy.siteLogoLargeUrl()).isNull();
    assertThat(legacy.siteFaviconUrl()).isNull();
    assertThat(legacy.siteLogoSmallSize()).isNull();
    assertThat(legacy.siteLogoSize()).isNull();
    assertThat(legacy.siteLogoLargeSize()).isNull();
  }

  @Test void freshSchemaIncludesNullableBrandingColumns() throws Exception {
    try (var stream = getClass().getResourceAsStream("/db/migration/V1__init_schema.sql")) {
      assertThat(stream).isNotNull();
      var sql = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      for (var column : new String[]{"site_logo_small_url", "site_logo_large_url", "site_favicon_url"}) {
        assertThat(sql).contains(column + " varchar(500) DEFAULT NULL");
      }
      for (var column : new String[]{"site_logo_small_size", "site_logo_size", "site_logo_large_size"}) {
        assertThat(sql).contains(column + " int DEFAULT NULL");
      }
      assertThat(sql).doesNotContain("DROP ", "DELETE ");
    }
  }
}
