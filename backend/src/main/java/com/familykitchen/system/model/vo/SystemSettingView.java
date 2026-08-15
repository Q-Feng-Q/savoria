package com.familykitchen.system.model.vo;

import java.time.LocalDateTime;

/**
 * 平台管理员读取的系统配置视图；SMTP 密码仅以掩码和已配置标志呈现。
 * @param siteName 站点名称
 * @param siteLogoUrl 站点 Logo 地址
 * @param dishReviewEnabled 是否启用菜品审核
 * @param maintenanceEnabled 是否处于维护模式
 * @param maintenanceMessage 维护提示
 * @param mobileBindingEnabled 是否开放手机号绑定
 * @param emailBindingEnabled 是否开放邮箱绑定
 * @param wechatBindingEnabled 是否开放微信绑定
 * @param smtpHost SMTP 服务器地址
 * @param smtpPort SMTP 端口
 * @param smtpUsername SMTP 用户名
 * @param smtpPassword 密码掩码，未配置时为空
 * @param smtpPasswordConfigured 是否已保存 SMTP 密码
 * @param smtpTlsEnabled 是否启用 STARTTLS
 * @param smtpFrom 发件人地址
 * @param updatedAt 最近更新时间
 */
public record SystemSettingView(
    String siteName,String siteLogoUrl,boolean dishReviewEnabled,
    boolean maintenanceEnabled,String maintenanceMessage,
    boolean mobileBindingEnabled,boolean emailBindingEnabled,boolean wechatBindingEnabled,
    String smtpHost,Integer smtpPort,String smtpUsername,String smtpPassword,
    boolean smtpPasswordConfigured,boolean smtpTlsEnabled,String smtpFrom,
    LocalDateTime updatedAt
) {}
