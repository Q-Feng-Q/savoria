package com.familykitchen.system.model.vo;

/**
 * 无需登录即可读取的系统配置视图，明确排除全部 SMTP 凭据。
 * @param siteName 站点名称
 * @param siteLogoUrl 站点 Logo 地址
 * @param maintenanceEnabled 是否处于维护模式
 * @param maintenanceMessage 维护提示
 * @param mobileBindingEnabled 是否开放手机号绑定
 * @param emailBindingEnabled 是否开放邮箱绑定
 * @param wechatBindingEnabled 是否开放微信绑定
 */
public record PublicSystemSettingView(
    String siteName,
    String siteLogoUrl,
    boolean maintenanceEnabled,
    String maintenanceMessage,
    boolean mobileBindingEnabled,
    boolean emailBindingEnabled,
    boolean wechatBindingEnabled
) {}
