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
 * @param brandTagline 通用品牌标语
 * @param homeHeroTagline 家庭首页顶部标语
 * @param homeFooterMessage 家庭首页与相关页面底部寄语
 * @param cartHeroTagline 餐篮顶部标语
 * @param deliveryMessage 配送氛围提示
 * @param pickupMessage 自取氛围提示
 * @param cartFooterMessage 餐篮底部寄语
 * @param profileWelcomeMessage 个人中心欢迎语
 */
public record PublicSystemSettingView(
    String siteName,
    String siteLogoUrl,
    boolean maintenanceEnabled,
    String maintenanceMessage,
    boolean mobileBindingEnabled,
    boolean emailBindingEnabled,
    boolean wechatBindingEnabled,
    String siteLogoSmallUrl, String siteLogoLargeUrl, String siteFaviconUrl,
    Integer siteLogoSmallSize, Integer siteLogoSize, Integer siteLogoLargeSize,
    String brandTagline, String homeHeroTagline, String homeFooterMessage, String cartHeroTagline,
    String deliveryMessage, String pickupMessage, String cartFooterMessage, String profileWelcomeMessage
) {}
