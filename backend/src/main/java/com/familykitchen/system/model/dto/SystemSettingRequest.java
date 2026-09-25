package com.familykitchen.system.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.familykitchen.system.validation.BrandUrl;
import com.familykitchen.system.validation.BrandSizeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * 平台管理员更新系统配置的请求。
 * @param siteName 站点名称
 * @param siteLogoUrl 站点 Logo 地址
 * @param dishReviewEnabled 是否启用菜品审核
 * @param maintenanceEnabled 是否启用维护模式
 * @param maintenanceMessage 维护期间展示的提示
 * @param mobileBindingEnabled 是否开放手机号绑定
 * @param emailBindingEnabled 是否开放邮箱绑定及邮件发送能力
 * @param wechatBindingEnabled 是否开放微信绑定
 * @param smtpHost SMTP 服务器地址
 * @param smtpPort SMTP 端口，范围为 1 至 65535
 * @param smtpUsername SMTP 登录用户名
 * @param smtpPassword SMTP 明文密码；空值或掩码表示保留原密码
 * @param smtpTlsEnabled 是否启用 STARTTLS
 * @param smtpFrom 邮件发件人地址
 */
public record SystemSettingRequest(
    @NotBlank @Size(max=100) String siteName,
    @BrandUrl @Size(max=500) String siteLogoUrl,
    boolean dishReviewEnabled,
    boolean maintenanceEnabled,
    @NotBlank @Size(max=500) String maintenanceMessage,
    boolean mobileBindingEnabled,
    boolean emailBindingEnabled,
    boolean wechatBindingEnabled,
    @Size(max=255) String smtpHost,
    @Min(1) @Max(65535) Integer smtpPort,
    @Size(max=255) String smtpUsername,
    @Size(max=500) String smtpPassword,
    boolean smtpTlsEnabled,
    @Size(max=255) String smtpFrom,
    @BrandUrl @Size(max=500) String siteLogoSmallUrl,
    @BrandUrl @Size(max=500) String siteLogoLargeUrl,
    @BrandUrl @Size(max=500) String siteFaviconUrl,
    @JsonDeserialize(using=BrandSizeDeserializer.class) @Min(16) @Max(64) Integer siteLogoSmallSize,
    @JsonDeserialize(using=BrandSizeDeserializer.class) @Min(24) @Max(120) Integer siteLogoSize,
    @JsonDeserialize(using=BrandSizeDeserializer.class) @Min(48) @Max(160) Integer siteLogoLargeSize
) {
  public SystemSettingRequest(String siteName, String siteLogoUrl, boolean dishReviewEnabled,
      boolean maintenanceEnabled, String maintenanceMessage, boolean mobileBindingEnabled,
      boolean emailBindingEnabled, boolean wechatBindingEnabled, String smtpHost, Integer smtpPort,
      String smtpUsername, String smtpPassword, boolean smtpTlsEnabled, String smtpFrom) {
    this(siteName, siteLogoUrl, dishReviewEnabled, maintenanceEnabled, maintenanceMessage,
        mobileBindingEnabled, emailBindingEnabled, wechatBindingEnabled, smtpHost, smtpPort,
        smtpUsername, smtpPassword, smtpTlsEnabled, smtpFrom, null, null, null, null, null, null);
  }
}
