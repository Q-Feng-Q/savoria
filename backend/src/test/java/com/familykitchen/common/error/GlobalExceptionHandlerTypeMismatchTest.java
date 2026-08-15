package com.familykitchen.common.error;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 验证参数类型转换错误不会落入未处理的系统异常分支。 */
class GlobalExceptionHandlerTypeMismatchTest {
  @Test
  void typeMismatchIsHandledAsValidationErrorInsteadOfUnhandledSystemError() {
    boolean registered = Arrays.stream(GlobalExceptionHandler.class.getDeclaredMethods())
        .map(method -> method.getAnnotation(ExceptionHandler.class))
        .filter(annotation -> annotation != null)
        .flatMap(annotation -> Arrays.stream(annotation.value()))
        .anyMatch(MethodArgumentTypeMismatchException.class::equals);

    assertTrue(registered, "Long 等参数转换失败应由参数校验处理器返回 400");
  }
}
