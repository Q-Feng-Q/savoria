package com.familykitchen.common.error;

/** 可预期业务失败的统一异常，携带可映射为 API 响应的业务错误码。 */
public class BusinessException extends RuntimeException {

  /** 供异常处理器映射 API 错误码与 HTTP 状态的业务错误类别。 */
  private final ErrorCode errorCode;

  /**
   * 创建业务异常。
   * @param errorCode 业务错误类别
   * @param message 面向调用方的错误说明
   */
  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * 返回本次业务失败的错误类别。
   * @return 业务错误码枚举
   */
  public ErrorCode errorCode() {
    return errorCode;
  }
}

