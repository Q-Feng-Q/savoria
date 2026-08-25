package com.familykitchen.merchant;

import static org.assertj.core.api.Assertions.assertThat;

import com.familykitchen.merchant.controller.MerchantProfileController;
import com.familykitchen.merchant.model.dto.UpdateMerchantProfileRequest;
import com.familykitchen.merchant.model.vo.MerchantProfileView;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** 商户资料自助接口路由与字段白名单契约。 */
class MerchantProfileControllerContractTest {
  @Test
  void routesAndEditableFieldsAreExact() throws Exception {
    RequestMapping root = MerchantProfileController.class.getAnnotation(RequestMapping.class);
    Method get = MerchantProfileController.class.getMethod(
        "profile", jakarta.servlet.http.HttpServletRequest.class);
    Method put = MerchantProfileController.class.getMethod(
        "updateProfile", jakarta.servlet.http.HttpServletRequest.class, UpdateMerchantProfileRequest.class);

    assertThat(root.value()).containsExactly("/merchant/profile");
    assertThat(get.getAnnotation(GetMapping.class)).isNotNull();
    assertThat(put.getAnnotation(PutMapping.class)).isNotNull();
    assertThat(Arrays.stream(UpdateMerchantProfileRequest.class.getRecordComponents())
        .map(component -> component.getName()).toList())
        .containsExactly("name", "contactName", "contactPhone");
    assertThat(Arrays.stream(MerchantProfileView.class.getRecordComponents())
        .map(component -> component.getName()).toList())
        .containsExactly("name", "contactName", "contactPhone");
  }
}
