package com.familykitchen.common.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.familykitchen.order.model.enums.DeliveryMode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 验证所有常见参数错误都返回可直接展示给前端的中文提示。 */
class GlobalExceptionHandlerValidationMessageTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void missingQueryParameterNamesTheRequiredParameter() {
    var response = handler.handleMissingRequestParameter(
        new MissingServletRequestParameterException("date", "LocalDate"));

    assertThat(response.getBody().message()).isEqualTo("缺少必填参数：日期（date）");
  }

  @Test
  void emptyRequestBodyReturnsAnActionableMessage() {
    var exception = new HttpMessageNotReadableException(
        "Required request body is missing", mock(HttpInputMessage.class));

    var response = handler.handleUnreadableMessage(exception);

    assertThat(response.getBody().message()).isEqualTo("请求内容不能为空");
  }

  @Test
  void beanValidationListsReadableFieldErrors() {
    var binding = new BeanPropertyBindingResult(new Object(), "dishRequest");
    binding.addError(new FieldError("dishRequest", "name", "must not be blank"));
    binding.addError(new FieldError("dishRequest", "categoryId", "must not be null"));
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(mock(MethodParameter.class), binding);

    var response = handler.handleMethodArgumentNotValid(exception);

    assertThat(response.getBody().message()).contains("菜品名称不能为空", "菜品分类不能为空");
  }

  @Test
  void typeMismatchNamesFieldAndExpectedType() {
    var exception = new MethodArgumentTypeMismatchException(
        "abc", Long.class, "dishId", mock(MethodParameter.class), new NumberFormatException());

    var response = handler.handleTypeMismatch(exception);

    assertThat(response.getBody().message()).isEqualTo("参数“菜品ID（dishId）”格式不正确，应为整数");
  }

  @Test
  void invalidEnumListsAcceptedValues() {
    InvalidFormatException cause = InvalidFormatException.from(
        null, "invalid delivery mode", "EXPRESS", DeliveryMode.class);
    cause.prependPath(new Object(), "deliveryMode");
    var exception = new HttpMessageNotReadableException(
        "JSON parse error", cause, mock(HttpInputMessage.class));

    var response = handler.handleUnreadableMessage(exception);

    assertThat(response.getBody().message())
        .isEqualTo("参数“配送方式（deliveryMode）”取值无效，可选值：PICKUP、DELIVERY");
  }

  @Test
  void mapperBindingFailureReturnsReadableFrontendMessage() {
    var response = handler.handleMapperBindingException(
        new org.apache.ibatis.binding.BindingException("Invalid bound statement"));

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody().message()).isEqualTo("服务接口配置异常，请联系管理员");
  }
}
