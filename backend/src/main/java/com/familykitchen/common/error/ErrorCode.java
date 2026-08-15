package com.familykitchen.common.error;

/** API 业务错误码集合；前三位用于映射 HTTP 状态，后两位区分具体业务场景。 */
public enum ErrorCode {
  /** 请求参数或输入内容不合法。 */
  BAD_REQUEST(40001),
  /** 请求未携带有效身份或会话已失效。 */
  UNAUTHORIZED(40101),
  /** 身份有效但无权执行当前操作。 */
  FORBIDDEN(40301),
  /** 请求的业务资源不存在。 */
  NOT_FOUND(40401),
  /** 资源当前状态不允许执行操作。 */
  STATE_CONFLICT(40901),
  /** 用户已加入家庭，不能重复加入。 */
  USER_ALREADY_IN_FAMILY(40911),
  /** 用户尚未加入家庭，无法执行家庭内操作。 */
  FAMILY_NOT_JOINED(42211),
  /** 邮箱已绑定到其他账号或重复绑定。 */
  EMAIL_ALREADY_BOUND(40912),
  /** 邀请的当前状态与请求操作冲突。 */
  INVITATION_STATE_CONFLICT(40913),
  /** 系统处于维护模式，暂不处理常规请求。 */
  SYSTEM_MAINTENANCE(50301),
  /** 菜品审核状态不允许当前流转。 */
  DISH_REVIEW_STATE_CONFLICT(40921),
  /** 输入合法但不满足业务规则。 */
  BUSINESS_INVALID(42201),
  /** 未归类的服务端内部错误。 */
  SYSTEM_ERROR(50001);

  private final int code;

  ErrorCode(int code) {
    this.code = code;
  }

  /**
   * 返回对外使用的业务错误码。
   * @return 五位业务错误码
   */
  public int code() {
    return code;
  }
}

