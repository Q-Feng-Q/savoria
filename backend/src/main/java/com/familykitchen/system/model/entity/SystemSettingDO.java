package com.familykitchen.system.model.entity;
import java.time.LocalDateTime;
/** 平台唯一系统配置的持久化实体，包含绑定开关和加密后的 SMTP 凭据。 */
public class SystemSettingDO {
  private String siteLogoSmallUrl, siteLogoLargeUrl, siteFaviconUrl;
  /** 通用品牌标语。 */
  private String brandTagline;
  /** 家庭首页顶部标语。 */
  private String homeHeroTagline;
  /** 家庭首页与相关页面底部寄语。 */
  private String homeFooterMessage;
  /** 餐篮顶部标语。 */
  private String cartHeroTagline;
  /** 配送氛围提示。 */
  private String deliveryMessage;
  /** 自取氛围提示。 */
  private String pickupMessage;
  /** 餐篮底部寄语。 */
  private String cartFooterMessage;
  /** 个人中心欢迎语。 */
  private String profileWelcomeMessage;
  private Integer siteLogoSmallSize, siteLogoSize, siteLogoLargeSize;
  public String getSiteLogoSmallUrl(){return siteLogoSmallUrl;}
  public void setSiteLogoSmallUrl(String v){siteLogoSmallUrl=v;}
  public String getSiteLogoLargeUrl(){return siteLogoLargeUrl;}
  public void setSiteLogoLargeUrl(String v){siteLogoLargeUrl=v;}
  public String getSiteFaviconUrl(){return siteFaviconUrl;}
  public void setSiteFaviconUrl(String v){siteFaviconUrl=v;}
  public Integer getSiteLogoSmallSize(){return siteLogoSmallSize;}
  public void setSiteLogoSmallSize(Integer v){siteLogoSmallSize=v;}
  public Integer getSiteLogoSize(){return siteLogoSize;}
  public void setSiteLogoSize(Integer v){siteLogoSize=v;}
  public Integer getSiteLogoLargeSize(){return siteLogoLargeSize;}
  public void setSiteLogoLargeSize(Integer v){siteLogoLargeSize=v;}
  /** 返回通用品牌标语。
   * @return 通用品牌标语 */
  public String getBrandTagline(){return brandTagline;}
  /** 设置通用品牌标语。
   * @param v 通用品牌标语 */
  public void setBrandTagline(String v){brandTagline=v;}
  /** 返回家庭首页顶部标语。
   * @return 家庭首页顶部标语 */
  public String getHomeHeroTagline(){return homeHeroTagline;}
  /** 设置家庭首页顶部标语。
   * @param v 家庭首页顶部标语 */
  public void setHomeHeroTagline(String v){homeHeroTagline=v;}
  /** 返回家庭首页与相关页面底部寄语。
   * @return 家庭首页与相关页面底部寄语 */
  public String getHomeFooterMessage(){return homeFooterMessage;}
  /** 设置家庭首页与相关页面底部寄语。
   * @param v 家庭首页与相关页面底部寄语 */
  public void setHomeFooterMessage(String v){homeFooterMessage=v;}
  /** 返回餐篮顶部标语。
   * @return 餐篮顶部标语 */
  public String getCartHeroTagline(){return cartHeroTagline;}
  /** 设置餐篮顶部标语。
   * @param v 餐篮顶部标语 */
  public void setCartHeroTagline(String v){cartHeroTagline=v;}
  /** 返回配送氛围提示。
   * @return 配送氛围提示 */
  public String getDeliveryMessage(){return deliveryMessage;}
  /** 设置配送氛围提示。
   * @param v 配送氛围提示 */
  public void setDeliveryMessage(String v){deliveryMessage=v;}
  /** 返回自取氛围提示。
   * @return 自取氛围提示 */
  public String getPickupMessage(){return pickupMessage;}
  /** 设置自取氛围提示。
   * @param v 自取氛围提示 */
  public void setPickupMessage(String v){pickupMessage=v;}
  /** 返回餐篮底部寄语。
   * @return 餐篮底部寄语 */
  public String getCartFooterMessage(){return cartFooterMessage;}
  /** 设置餐篮底部寄语。
   * @param v 餐篮底部寄语 */
  public void setCartFooterMessage(String v){cartFooterMessage=v;}
  /** 返回个人中心欢迎语。
   * @return 个人中心欢迎语 */
  public String getProfileWelcomeMessage(){return profileWelcomeMessage;}
  /** 设置个人中心欢迎语。
   * @param v 个人中心欢迎语 */
  public void setProfileWelcomeMessage(String v){profileWelcomeMessage=v;}
  /**
   * 固定配置记录主键。
   */
  private Long id;
  /**
   * 站点名称。
   */
  private String siteName;
  /**
   * 站点 Logo 地址。
   */
  private String siteLogoUrl;
  /**
   * 菜品审核开关。
   */
  private Boolean dishReviewEnabled;
  /**
   * 维护模式开关。
   */
  private Boolean maintenanceEnabled;
  /**
   * 维护提示。
   */
  private String maintenanceMessage;
  /**
   * 最近更新账号标识。
   */
  private Long updatedBy;
  /**
   * 最近更新时间。
   */
  private LocalDateTime updatedAt;
  /**
   * 手机号绑定开关。
   */
  private Boolean mobileBindingEnabled,
      /**
       * 邮箱绑定开关。
       */
      emailBindingEnabled,
      /**
       * 微信绑定开关。
       */
      wechatBindingEnabled,
      /**
       * SMTP STARTTLS 开关。
       */
      smtpTlsEnabled;
  /**
   * SMTP 服务器地址。
   */
  private String smtpHost,
      /**
       * SMTP 用户名。
       */
      smtpUsername,
      /**
       * 使用平台密钥加密后的 SMTP 密码密文。
       */
      smtpPasswordCiphertext,
      /**
       * SMTP 发件人地址。
       */
      smtpFrom;
  /**
   * SMTP 端口。
   */
  private Integer smtpPort;
  /**
   * 返回配置主键。
   *
   * @return 配置主键
   */
  public Long getId(){return id;}
  /**
   * 设置配置主键。
   *
   * @param v 配置主键
   */
  public void setId(Long v){id=v;}
  /**
   * 返回站点名称。
   *
   * @return 站点名称
   */
  public String getSiteName(){return siteName;}
  /**
   * 设置站点名称。
   *
   * @param v 站点名称
   */
  public void setSiteName(String v){siteName=v;}
  /**
   * 返回站点 Logo 地址。
   *
   * @return 站点 Logo 地址
   */
  public String getSiteLogoUrl(){return siteLogoUrl;}
  /**
   * 设置站点 Logo 地址。
   *
   * @param v 站点 Logo 地址
   */
  public void setSiteLogoUrl(String v){siteLogoUrl=v;}
  /**
   * 返回菜品审核开关。
   *
   * @return 菜品审核开关
   */
  public Boolean getDishReviewEnabled(){return dishReviewEnabled;}
  /**
   * 设置菜品审核开关。
   *
   * @param v 菜品审核开关
   */
  public void setDishReviewEnabled(Boolean v){dishReviewEnabled=v;}
  /**
   * 返回维护模式开关。
   *
   * @return 维护模式开关
   */
  public Boolean getMaintenanceEnabled(){return maintenanceEnabled;}
  /**
   * 设置维护模式开关。
   *
   * @param v 维护模式开关
   */
  public void setMaintenanceEnabled(Boolean v){maintenanceEnabled=v;}
  /**
   * 返回维护提示。
   *
   * @return 维护提示
   */
  public String getMaintenanceMessage(){return maintenanceMessage;}
  /**
   * 设置维护提示。
   *
   * @param v 维护提示
   */
  public void setMaintenanceMessage(String v){maintenanceMessage=v;}
  /**
   * 返回更新账号标识。
   *
   * @return 最近更新账号标识
   */
  public Long getUpdatedBy(){return updatedBy;}
  /**
   * 设置更新账号标识。
   *
   * @param v 最近更新账号标识
   */
  public void setUpdatedBy(Long v){updatedBy=v;}
  /**
   * 返回最近更新时间。
   *
   * @return 最近更新时间
   */
  public LocalDateTime getUpdatedAt(){return updatedAt;}
  /**
   * 设置最近更新时间。
   *
   * @param v 最近更新时间
   */
  public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
  /**
   * 返回手机号绑定开关。
   *
   * @return 手机号绑定开关
   */
  public Boolean getMobileBindingEnabled(){return mobileBindingEnabled;}
  /**
   * 设置手机号绑定开关。
   *
   * @param v 手机号绑定开关
   */
  public void setMobileBindingEnabled(Boolean v){mobileBindingEnabled=v;}
  /**
   * 返回邮箱绑定开关。
   *
   * @return 邮箱绑定开关
   */
  public Boolean getEmailBindingEnabled(){return emailBindingEnabled;}
  /**
   * 设置邮箱绑定开关。
   *
   * @param v 邮箱绑定开关
   */
  public void setEmailBindingEnabled(Boolean v){emailBindingEnabled=v;}
  /**
   * 返回微信绑定开关。
   *
   * @return 微信绑定开关
   */
  public Boolean getWechatBindingEnabled(){return wechatBindingEnabled;}
  /**
   * 设置微信绑定开关。
   *
   * @param v 微信绑定开关
   */
  public void setWechatBindingEnabled(Boolean v){wechatBindingEnabled=v;}
  /**
   * 返回 SMTP 服务器地址。
   *
   * @return SMTP 服务器地址
   */
  public String getSmtpHost(){return smtpHost;}
  /**
   * 设置 SMTP 服务器地址。
   *
   * @param v SMTP 服务器地址
   */
  public void setSmtpHost(String v){smtpHost=v;}
  /**
   * 返回 SMTP 端口。
   *
   * @return SMTP 端口
   */
  public Integer getSmtpPort(){return smtpPort;}
  /**
   * 设置 SMTP 端口。
   *
   * @param v SMTP 端口
   */
  public void setSmtpPort(Integer v){smtpPort=v;}
  /**
   * 返回 SMTP 用户名。
   *
   * @return SMTP 用户名
   */
  public String getSmtpUsername(){return smtpUsername;}
  /**
   * 设置 SMTP 用户名。
   *
   * @param v SMTP 用户名
   */
  public void setSmtpUsername(String v){smtpUsername=v;}
  /**
   * 返回 SMTP 密码密文。
   *
   * @return SMTP 密码密文
   */
  public String getSmtpPasswordCiphertext(){return smtpPasswordCiphertext;}
  /**
   * 设置 SMTP 密码密文。
   *
   * @param v SMTP 密码密文
   */
  public void setSmtpPasswordCiphertext(String v){smtpPasswordCiphertext=v;}
  /**
   * 返回 STARTTLS 开关。
   *
   * @return STARTTLS 开关
   */
  public Boolean getSmtpTlsEnabled(){return smtpTlsEnabled;}
  /**
   * 设置 STARTTLS 开关。
   *
   * @param v STARTTLS 开关
   */
  public void setSmtpTlsEnabled(Boolean v){smtpTlsEnabled=v;}
  /**
   * 返回 SMTP 发件人地址。
   *
   * @return SMTP 发件人地址
   */
  public String getSmtpFrom(){return smtpFrom;}
  /**
   * 设置 SMTP 发件人地址。
   *
   * @param v SMTP 发件人地址
   */
  public void setSmtpFrom(String v){smtpFrom=v;}
}
