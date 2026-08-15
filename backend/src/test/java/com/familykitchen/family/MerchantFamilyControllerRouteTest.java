package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.familykitchen.family.controller.MerchantFamilyController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 验证商户家庭ControllerRoute相关业务契约与回归场景。
 */
class MerchantFamilyControllerRouteTest {

  @Test
  void merchantFamilyRouteDoesNotDuplicateTheGatewayApiPrefix() {
    RequestMapping mapping = MerchantFamilyController.class.getAnnotation(RequestMapping.class);

    assertThat(mapping.value()).containsExactly("/merchant/families");
  }
}
