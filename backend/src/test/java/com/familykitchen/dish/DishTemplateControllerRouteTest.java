package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.controller.DishTemplateController;
import com.familykitchen.dish.model.vo.DishTemplateImportResultView;
import com.familykitchen.dish.service.DishTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** 验证系统模板全量导入路由只使用实时商户上下文。 */
class DishTemplateControllerRouteTest {

  @Test
  void importAllRouteHasNoBodyOrExternalMerchantParameter() throws Exception {
    Method method = routeMethod();
    PostMapping mapping = method.getAnnotation(PostMapping.class);
    assertTrue(List.of(mapping.value()).contains("/dish-templates/import-all"));
    assertEquals(1, method.getParameterCount());
    assertEquals(HttpServletRequest.class, method.getParameterTypes()[0]);
    assertFalse(method.getParameters()[0].isAnnotationPresent(RequestBody.class));
  }

  @Test
  void importAllPassesTheExactAuthorizedMerchantContext() throws Exception {
    CurrentUserProvider provider = mock(CurrentUserProvider.class);
    DishTemplateService service = mock(DishTemplateService.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    CurrentUserContext user = user(11L, Set.of("MERCHANT_ADMIN"));
    DishTemplateImportResultView result = new DishTemplateImportResultView(List.of(2L), List.of());
    when(provider.require(request)).thenReturn(user);
    when(service.importAllTemplates(user)).thenReturn(result);

    @SuppressWarnings("unchecked")
    ApiResponse<DishTemplateImportResultView> response =
        (ApiResponse<DishTemplateImportResultView>) routeMethod().invoke(
            new DishTemplateController(provider, service), request);

    assertEquals(result, response.data());
    verify(service).importAllTemplates(user);
  }

  @Test
  void importAllRejectsMissingPermissionAndMissingMerchantId() throws Exception {
    for (CurrentUserContext user : List.of(user(11L, Set.of()), user(null, Set.of("MERCHANT_ADMIN")))) {
      CurrentUserProvider provider = mock(CurrentUserProvider.class);
      DishTemplateService service = mock(DishTemplateService.class);
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(provider.require(request)).thenReturn(user);

      InvocationTargetException wrapped = assertThrows(InvocationTargetException.class,
          () -> routeMethod().invoke(new DishTemplateController(provider, service), request));

      BusinessException error = (BusinessException) wrapped.getCause();
      assertEquals(ErrorCode.FORBIDDEN, error.errorCode());
      verify(service, never()).importAllTemplates(user);
    }
  }

  private static Method routeMethod() throws NoSuchMethodException {
    return DishTemplateController.class.getMethod("importAllTemplates", HttpServletRequest.class);
  }

  private static CurrentUserContext user(Long merchantId, Set<String> roles) {
    return new CurrentUserContext(7L, merchantId, null, null, null, roles, Set.of());
  }
}
