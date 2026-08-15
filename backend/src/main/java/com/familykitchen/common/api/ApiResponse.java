package com.familykitchen.common.api;

/**
 * API 的统一响应包装，向调用方提供稳定的业务码、提示信息与结果数据结构。
 * @param code 业务结果码，成功时为 {@code 0}
 * @param message 面向调用方的结果说明
 * @param data 成功响应携带的数据，错误或无返回值时可为空
 * @param <T> 响应数据类型
 */
public record ApiResponse<T>(int code, String message, T data) {

  /**
   * 创建携带数据的成功响应。
   * @param data 响应数据
   * @param <T> 响应数据类型
   * @return 业务码为零的成功响应
   */
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "ok", data);
  }

  /**
   * 创建不携带数据的成功响应。
   * @return 业务码为零且不携带数据的成功响应
   */
  public static ApiResponse<Void> ok() {
    return new ApiResponse<>(0, "ok", null);
  }

  /**
   * 创建不携带数据的错误响应。
   * @param code 业务错误码
   * @param message 错误说明
   * @return 对应错误码和说明的响应
   */
  public static ApiResponse<Void> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}

