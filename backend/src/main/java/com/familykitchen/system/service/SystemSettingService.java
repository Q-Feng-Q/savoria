package com.familykitchen.system.service;
import com.familykitchen.system.model.dto.SystemSettingRequest;
import com.familykitchen.system.model.vo.SystemSettingView;
import com.familykitchen.system.model.vo.PublicSystemSettingView;
/**
 * 管理平台级单例配置，并按调用方暴露不同的数据边界。
 *
 * <p>公开查询只返回客户端可见的功能开关，管理员查询与更新返回脱敏后的完整配置；
 * SMTP 密码等敏感值不会通过视图对象明文返回。</p>
 */
public interface SystemSettingService {
  /** 查询管理员配置视图。
   * @return 平台管理员可见且密码已脱敏的系统配置 */
  SystemSettingView current();
  /** 查询公开配置视图。
   * @return 不含 SMTP 凭据的公开系统配置 */
  PublicSystemSettingView publicCurrent();
  /**
   * 更新平台系统配置并记录审计日志。
   * @param operatorId 操作账号标识
   * @param request 新配置
   * @return 更新后的脱敏配置
   */
  SystemSettingView update(Long operatorId,SystemSettingRequest request);
  /** 查询菜品审核开关。
   * @return 是否启用菜品审核 */
  boolean dishReviewEnabled();
  /** 查询手机号绑定开关。
   * @return 是否开放手机号绑定 */
  boolean mobileBindingEnabled();
  /** 查询邮箱绑定开关。
   * @return 是否开放邮箱绑定 */
  boolean emailBindingEnabled();
  /** 查询微信绑定开关。
   * @return 是否开放微信绑定 */
  boolean wechatBindingEnabled();
}
