package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.family.controller.MerchantFamilyController;
import com.familykitchen.family.controller.MerchantFamilyMenuController;
import com.familykitchen.family.model.dto.SetFeaturedDishRequest;
import com.familykitchen.family.service.MerchantFamilyMenuApplicationService;
import com.familykitchen.dish.controller.DishController;
import com.familykitchen.dish.model.dto.DishFeaturedRequest;
import com.familykitchen.dish.service.DishApplicationService;
import com.familykitchen.dish.service.DishReviewService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Validation;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 验证商户家庭ControllerRoute相关业务契约与回归场景。
 */
class MerchantFamilyControllerRouteTest {

  @Test
  void merchantFamilyRouteDoesNotDuplicateTheGatewayApiPrefix() {
    RequestMapping mapping = MerchantFamilyController.class.getAnnotation(RequestMapping.class);

    assertThat(mapping.value()).containsExactly("/merchant/families");
  }

  @Test
  void featuredRoutePassesExactRealtimeMerchantContextAndDish() {
    CurrentUserProvider users = mock(CurrentUserProvider.class);
    MerchantFamilyMenuApplicationService service = mock(MerchantFamilyMenuApplicationService.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    CurrentUserContext context = new CurrentUserContext(2L, 2L, 1L, 2L, "owner", Set.of(), Set.of("MERCHANT_ADMIN"));
    when(users.require(request)).thenReturn(context);

    new MerchantFamilyMenuController(users, service).setFeaturedDish(request, 1L, new SetFeaturedDishRequest(8L));

    verify(service).setFeaturedDish(context, 1L, 8L);
  }

  @Test
  void featuredRouteRejectsMissingPermissionOrMerchantIdentity() {
    for (CurrentUserContext context : new CurrentUserContext[] {
        new CurrentUserContext(2L, 2L, 1L, 2L, "owner", Set.of(), Set.of()),
        new CurrentUserContext(2L, null, 1L, 2L, "owner", Set.of(), Set.of("MERCHANT_ADMIN")) }) {
      CurrentUserProvider users = mock(CurrentUserProvider.class);
      MerchantFamilyMenuApplicationService service = mock(MerchantFamilyMenuApplicationService.class);
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(users.require(request)).thenReturn(context);
      MerchantFamilyMenuController controller = new MerchantFamilyMenuController(users, service);

      assertThrows(BusinessException.class,
          () -> controller.setFeaturedDish(request, 1L, new SetFeaturedDishRequest(8L)));
      verify(service, never()).setFeaturedDish(context, 1L, 8L);
    }
  }

  @Test
  void merchantDishFeaturedRoutePassesExactRealtimeContext() {
    CurrentUserProvider users = mock(CurrentUserProvider.class);
    DishApplicationService service = mock(DishApplicationService.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    CurrentUserContext context = new CurrentUserContext(
        2L, 2L, 1L, 2L, "owner", Set.of(), Set.of("MERCHANT_ADMIN"));
    when(users.require(request)).thenReturn(context);

    new DishController(users, service, mock(DishReviewService.class))
        .setFeaturedDish(request, 8L, new DishFeaturedRequest(Boolean.TRUE));

    verify(service).setFeaturedDish(context, 8L, true);
  }

  @Test
  void merchantDishFeaturedRouteRejectsMissingPermissionOrMerchantIdentityBeforeService() {
    for (CurrentUserContext context : new CurrentUserContext[] {
        new CurrentUserContext(2L, 2L, 1L, 2L, "owner", Set.of(), Set.of()),
        new CurrentUserContext(2L, null, 1L, 2L, "owner", Set.of(), Set.of("MERCHANT_ADMIN")) }) {
      CurrentUserProvider users = mock(CurrentUserProvider.class);
      DishApplicationService service = mock(DishApplicationService.class);
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(users.require(request)).thenReturn(context);

      DishController controller = new DishController(users, service, mock(DishReviewService.class));
      assertThrows(BusinessException.class,
          () -> controller.setFeaturedDish(request, 8L, new DishFeaturedRequest(Boolean.TRUE)));
      verify(service, never()).setFeaturedDish(context, 8L, true);
    }
  }

  @Test
  void merchantDishFeaturedRequestRejectsMissingOrNullBoolean() throws Exception {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    ObjectMapper json = new ObjectMapper();

    assertThat(validator.validate(json.readValue("{}", DishFeaturedRequest.class))).isNotEmpty();
    assertThat(validator.validate(new DishFeaturedRequest(null))).isNotEmpty();
  }

  @Test
  void oldFamilyFeaturedRouteIsDeprecated() throws Exception {
    Operation operation = MerchantFamilyMenuController.class
        .getMethod("setFeaturedDish", HttpServletRequest.class, Long.class, SetFeaturedDishRequest.class)
        .getAnnotation(Operation.class);

    assertThat(operation.deprecated()).isTrue();
  }
}
