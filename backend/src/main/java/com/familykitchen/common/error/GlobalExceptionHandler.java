package com.familykitchen.common.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.familykitchen.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局异常处理器。
 *
 * <p>统一将业务异常、参数校验异常和系统异常转换为标准 API 响应结构，
 * 便于前端稳定处理错误。业务码前三位映射 HTTP 状态，无法识别时回退为 400；
 * 未预期异常仅在服务端记录完整详情，对外固定返回通用 500 信息，避免泄露内部实现。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final Map<String, String> PARAMETER_LABELS = Map.ofEntries(
      Map.entry("name", "名称"), Map.entry("familyName", "家庭名称"),
      Map.entry("newMerchantName", "私厨名称"), Map.entry("merchantMode", "私厨关联方式"),
      Map.entry("merchantInvitationCode", "私厨邀请码"), Map.entry("code", "邀请码"),
      Map.entry("username", "用户名"), Map.entry("password", "密码"),
      Map.entry("currentPassword", "当前密码"), Map.entry("newPassword", "新密码"),
      Map.entry("nickname", "昵称"), Map.entry("email", "邮箱"),
      Map.entry("verificationCode", "验证码"), Map.entry("authorizationCode", "微信授权码"),
      Map.entry("categoryId", "菜品分类"), Map.entry("dishId", "菜品ID"),
      Map.entry("basePrice", "菜品价格"), Map.entry("ingredients", "食材列表"),
      Map.entry("cookingSteps", "制作步骤"), Map.entry("ingredientName", "食材名称"),
      Map.entry("quantity", "数量"), Map.entry("unit", "单位"),
      Map.entry("date", "日期"), Map.entry("serviceDate", "用餐日期"),
      Map.entry("mealSlotId", "餐次ID"), Map.entry("deliveryMode", "配送方式"),
      Map.entry("deliveryFee", "配送费"), Map.entry("addressId", "地址ID"),
      Map.entry("contactName", "联系人"), Map.entry("contactPhone", "联系电话"),
      Map.entry("addressText", "详细地址"), Map.entry("familyId", "家庭ID"),
      Map.entry("merchantId", "商户ID"), Map.entry("orderId", "订单ID"),
      Map.entry("itemId", "项目ID"), Map.entry("notificationId", "通知ID"),
      Map.entry("targetUserId", "目标用户ID"), Map.entry("ownerUserId", "负责人用户ID"),
      Map.entry("status", "状态"), Map.entry("remark", "备注"), Map.entry("reason", "原因"),
      Map.entry("page", "页码"), Map.entry("pageSize", "每页条数"),
      Map.entry("receiverScope", "通知范围"), Map.entry("recipient", "收件邮箱")
  );

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    return ResponseEntity.status(statusOf(exception.errorCode()))
        .body(ApiResponse.error(exception.errorCode().code(), exception.getMessage()));
  }

  /** 将请求体字段校验错误整理为前端可直接展示的字段级提示。 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
    return badRequest(exception.getBindingResult().getFieldErrors().stream()
        .map(GlobalExceptionHandler::fieldErrorMessage)
        .collect(Collectors.joining("；")));
  }

  /** 处理表单或查询对象绑定校验失败。 */
  @ExceptionHandler(BindException.class)
  ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
    return badRequest(exception.getBindingResult().getFieldErrors().stream()
        .map(GlobalExceptionHandler::fieldErrorMessage)
        .collect(Collectors.joining("；")));
  }

  /** 处理方法参数约束校验失败。 */
  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
    Set<String> messages = exception.getConstraintViolations().stream()
        .map(violation -> {
          String path = violation.getPropertyPath().toString();
          String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
          return label(field) + normalizeValidationMessage(violation.getMessage());
        })
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return badRequest(String.join("；", messages));
  }

  /** 处理缺少必填查询参数。 */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ApiResponse<Void>> handleMissingRequestParameter(MissingServletRequestParameterException exception) {
    return badRequest("缺少必填参数：" + labelWithCode(exception.getParameterName()));
  }

  /** 处理缺少必填请求头。 */
  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(MissingRequestHeaderException exception) {
    return badRequest("缺少必填请求头：" + exception.getHeaderName());
  }

  /** 处理文件上传时缺少必填文件部分。 */
  @ExceptionHandler(MissingServletRequestPartException.class)
  ResponseEntity<ApiResponse<Void>> handleMissingRequestPart(MissingServletRequestPartException exception) {
    return badRequest("缺少必填文件：" + labelWithCode(exception.getRequestPartName()));
  }

  /** 处理查询参数或路径参数类型错误。 */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
    return badRequest("参数“" + labelWithCode(exception.getName()) + "”格式不正确，应为"
        + expectedType(exception.getRequiredType()));
  }

  /** 处理空请求体、非法 JSON、枚举值或日期格式错误。 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
    String rawMessage = exception.getMessage() == null ? "" : exception.getMessage();
    if (rawMessage.contains("Required request body is missing")) {
      return badRequest("请求内容不能为空");
    }
    InvalidFormatException invalidFormat = findCause(exception, InvalidFormatException.class);
    if (invalidFormat != null) {
      String field = jsonField(invalidFormat);
      Class<?> targetType = invalidFormat.getTargetType();
      if (targetType != null && targetType.isEnum()) {
        String values = java.util.Arrays.stream(targetType.getEnumConstants())
            .map(String::valueOf).collect(Collectors.joining("、"));
        return badRequest("参数“" + labelWithCode(field) + "”取值无效，可选值：" + values);
      }
      return badRequest("参数“" + labelWithCode(field) + "”格式不正确，应为" + expectedType(targetType));
    }
    return badRequest("请求内容格式错误，请检查 JSON 字段、类型和日期格式");
  }

  /** 处理 MyBatis 方法缺少 SQL 映射等服务端接口配置错误。 */
  @ExceptionHandler(org.apache.ibatis.binding.BindingException.class)
  ResponseEntity<ApiResponse<Void>> handleMapperBindingException(
      org.apache.ibatis.binding.BindingException exception) {
    log.error("MyBatis mapper statement is missing or invalid", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ErrorCode.SYSTEM_ERROR.code(), "服务接口配置异常，请联系管理员"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
    // 完整异常仅写入服务端日志；响应隐藏堆栈和底层消息，避免暴露实现与敏感数据。
    log.error("Unhandled API exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ErrorCode.SYSTEM_ERROR.code(), "服务暂时不可用，请稍后重试"));
  }

  private static ResponseEntity<ApiResponse<Void>> badRequest(String message) {
    String resolved = message == null || message.isBlank() ? "请求参数不正确" : message;
    return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST.code(), resolved));
  }

  private static String fieldErrorMessage(FieldError error) {
    String fieldLabel = nestedFieldLabel(error.getField());
    String objectName = error.getObjectName().toLowerCase(Locale.ROOT);
    if ("name".equals(error.getField())) {
      if (objectName.contains("dishcategory")) fieldLabel = "分类名称";
      else if (objectName.contains("dish")) fieldLabel = "菜品名称";
      else if (objectName.contains("ingredient")) fieldLabel = "食材名称";
      else if (objectName.contains("merchant")) fieldLabel = "商户名称";
    }
    return fieldLabel + normalizeValidationMessage(error.getDefaultMessage());
  }

  private static String nestedFieldLabel(String fieldPath) {
    String field = fieldPath.contains(".") ? fieldPath.substring(fieldPath.lastIndexOf('.') + 1) : fieldPath;
    String translated = label(field);
    int start = fieldPath.indexOf('[');
    int end = fieldPath.indexOf(']', start + 1);
    if (start >= 0 && end > start) {
      try {
        int index = Integer.parseInt(fieldPath.substring(start + 1, end)) + 1;
        if (fieldPath.startsWith("ingredients")) return "第" + index + "项食材的" + translated;
        if (fieldPath.startsWith("cookingSteps")) return "第" + index + "个制作步骤的" + translated;
        return "第" + index + "项的" + translated;
      } catch (NumberFormatException ignored) {
        // 非数字下标时回退到字段名称。
      }
    }
    return translated;
  }

  private static String normalizeValidationMessage(String message) {
    String value = message == null ? "" : message.trim();
    String lower = value.toLowerCase(Locale.ROOT);
    if (lower.contains("must not be blank") || lower.contains("must not be null")
        || lower.contains("must not be empty") || value.contains("不能为空")) return "不能为空";
    if (lower.contains("must be a well-formed email") || value.contains("邮箱")) return "格式不正确";
    if (lower.contains("size must be between") || lower.contains("length must be between")) return "长度不符合要求";
    if (lower.contains("must be greater than") || lower.contains("must be less than")) return "数值超出允许范围";
    if (lower.contains("must match") || value.contains("匹配")) return "格式不正确";
    return value.isBlank() ? "不符合要求" : "：" + value;
  }

  private static String label(String field) {
    return PARAMETER_LABELS.getOrDefault(field, "参数“" + field + "”");
  }

  private static String labelWithCode(String field) {
    String translated = PARAMETER_LABELS.get(field);
    return translated == null ? field : translated + "（" + field + "）";
  }

  private static String expectedType(Class<?> type) {
    if (type == null) return "正确的数据类型";
    if (type == LocalDate.class) return "日期（yyyy-MM-dd）";
    if (type == Boolean.class || type == boolean.class) return "布尔值（true 或 false）";
    if (Number.class.isAssignableFrom(type) || type.isPrimitive() && type != boolean.class && type != char.class) {
      return type == Float.class || type == Double.class || type == float.class || type == double.class
          ? "数字" : "整数";
    }
    return type.getSimpleName();
  }

  private static String jsonField(InvalidFormatException exception) {
    return exception.getPath().stream().map(JsonMappingException.Reference::getFieldName)
        .filter(java.util.Objects::nonNull).reduce((first, second) -> second).orElse("请求字段");
  }

  private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
    Throwable current = throwable;
    while (current != null) {
      if (type.isInstance(current)) return type.cast(current);
      current = current.getCause();
    }
    return null;
  }

  private static HttpStatus statusOf(ErrorCode code) {
    // 五位业务码的前三位约定为 HTTP 状态；未知前缀安全回退为客户端请求错误。
    int status = code.code() / 100;
    return HttpStatus.resolve(status) == null ? HttpStatus.BAD_REQUEST : HttpStatus.valueOf(status);
  }
}
