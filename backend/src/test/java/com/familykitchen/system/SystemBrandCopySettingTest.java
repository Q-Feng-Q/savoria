package com.familykitchen.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.system.mapper.SystemAuditMapper;
import com.familykitchen.system.mapper.SystemSettingMapper;
import com.familykitchen.system.model.dto.SystemSettingRequest;
import com.familykitchen.system.model.entity.SystemSettingDO;
import com.familykitchen.system.security.PlatformSecretCipher;
import com.familykitchen.system.service.impl.SystemSettingServiceImpl;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SystemBrandCopySettingTest {
  @Test void returnsEffectiveDefaultsWhenCopyIsMissing() {
    var mapper = mock(SystemSettingMapper.class);
    when(mapper.selectCurrent()).thenReturn(new SystemSettingDO());
    var service = service(mapper);

    assertThat(service.publicCurrent().pickupMessage()).isEqualTo("先在一起，好好吃饭，期待您的到来");
    assertThat(service.current().deliveryMessage()).isEqualTo("美味正在路上，用食物，把温暖送到家");
    assertThat(service.current().homeHeroTagline()).isEqualTo("让家常菜 · 温暖每一餐\n就是最好的时光");
  }

  @Test void trimsCustomCopyButPreservesInternalNewlines() {
    var mapper = mock(SystemSettingMapper.class);
    var row = new SystemSettingDO();
    row.setBrandTagline("  自定义幸福  ");
    row.setHomeHeroTagline("  第一行\n第二行  ");
    when(mapper.selectCurrent()).thenReturn(row);
    var view = service(mapper).publicCurrent();

    assertThat(view.brandTagline()).isEqualTo("自定义幸福");
    assertThat(view.homeHeroTagline()).isEqualTo("第一行\n第二行");
  }

  @Test void blankAdminValuesArePersistedAsNullToRestoreDefaults() throws Exception {
    var mapper = mock(SystemSettingMapper.class);
    when(mapper.selectCurrent()).thenReturn(new SystemSettingDO());
    var service = service(mapper);
    var request = new ObjectMapper().readValue("""
        {"siteName":"食光栀味","maintenanceMessage":"维护中",
         "brandTagline":"  ","homeHeroTagline":"\\n",
         "homeFooterMessage":"","cartHeroTagline":"  ",
         "deliveryMessage":"","pickupMessage":" ",
         "cartFooterMessage":"","profileWelcomeMessage":"  "}
        """, SystemSettingRequest.class);

    service.update(7L, request);

    var captured = ArgumentCaptor.forClass(SystemSettingDO.class);
    verify(mapper).update(captured.capture());
    assertThat(captured.getValue().getBrandTagline()).isNull();
    assertThat(captured.getValue().getHomeHeroTagline()).isNull();
    assertThat(captured.getValue().getHomeFooterMessage()).isNull();
    assertThat(captured.getValue().getCartHeroTagline()).isNull();
    assertThat(captured.getValue().getDeliveryMessage()).isNull();
    assertThat(captured.getValue().getPickupMessage()).isNull();
    assertThat(captured.getValue().getCartFooterMessage()).isNull();
    assertThat(captured.getValue().getProfileWelcomeMessage()).isNull();
  }

  @Test void validatesCopyLengthBoundaries() throws Exception {
    var json = new ObjectMapper();
    var request = json.readValue("""
        {"siteName":"食光栀味","maintenanceMessage":"维护中",
         "brandTagline":"%s","deliveryMessage":"%s"}
        """.formatted("标".repeat(121), "送".repeat(81)), SystemSettingRequest.class);
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(request))
          .extracting(violation -> violation.getPropertyPath().toString())
          .containsExactlyInAnyOrder("brandTagline", "deliveryMessage");
    }
  }

  private static SystemSettingServiceImpl service(SystemSettingMapper mapper) {
    return new SystemSettingServiceImpl(
        mapper, mock(SystemAuditMapper.class), new PlatformSecretCipher("test-secret"));
  }
}
