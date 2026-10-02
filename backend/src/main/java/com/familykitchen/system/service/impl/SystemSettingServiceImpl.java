package com.familykitchen.system.service.impl;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.system.mapper.SystemSettingMapper;
import com.familykitchen.system.mapper.SystemAuditMapper;
import com.familykitchen.system.model.dto.SystemSettingRequest;
import com.familykitchen.system.model.dto.BrandSettingRequest;
import com.familykitchen.system.model.entity.SystemSettingDO;
import com.familykitchen.system.model.vo.SystemSettingView;
import com.familykitchen.system.model.vo.PublicSystemSettingView;
import com.familykitchen.system.service.SystemSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.familykitchen.system.security.PlatformSecretCipher;
/**
 * 系统配置服务实现，以五秒缓存降低高频开关查询的数据库压力。
 *
 * <p>缓存到期后优先刷新数据库；若刷新发生瞬时运行时故障且已有历史缓存，
 * 则退回已过期值维持关键读路径，无历史值时仍向上抛出故障。</p>
 */
@Service
public class SystemSettingServiceImpl implements SystemSettingService {
  private static final String DEFAULT_BRAND_TAGLINE = "好好吃饭，就是幸福";
  private static final String DEFAULT_HOME_HERO_TAGLINE = "让家常菜 · 温暖每一餐\n就是最好的时光";
  private static final String DEFAULT_HOME_FOOTER_MESSAGE = "好好吃饭\n就是一家人在一起";
  private static final String DEFAULT_CART_HERO_TAGLINE = "让家常菜 · 温暖每一餐";
  private static final String DEFAULT_DELIVERY_MESSAGE = "美味正在路上，用食物，把温暖送到家";
  private static final String DEFAULT_PICKUP_MESSAGE = "先在一起，好好吃饭，期待您的到来";
  private static final String DEFAULT_CART_FOOTER_MESSAGE = "把平凡的日子，过成温暖的诗";
  private static final String DEFAULT_PROFILE_WELCOME_MESSAGE = "好好吃饭，就是幸福";
  private final SystemSettingMapper mapper;
  private final SystemAuditMapper auditMapper;
  private final PlatformSecretCipher secretCipher;
  private volatile SystemSettingDO cached;
  private volatile long cacheExpiresAt;
  /**
   * 创建系统配置服务。
   * @param mapper 系统配置持久化接口
   * @param auditMapper 安全审计持久化接口
   * @param secretCipher 平台敏感配置加密器
   */
  public SystemSettingServiceImpl(SystemSettingMapper mapper,SystemAuditMapper auditMapper,PlatformSecretCipher secretCipher){this.mapper=mapper;this.auditMapper=auditMapper;this.secretCipher=secretCipher;}
  /** {@inheritDoc} */
  @Override public SystemSettingView current(){return view(require());}
  /** {@inheritDoc} */
  @Override public PublicSystemSettingView publicCurrent(){SystemSettingDO e=require();return new PublicSystemSettingView(
      siteName(e),e.getSiteLogoUrl(),Boolean.TRUE.equals(e.getMaintenanceEnabled()),e.getMaintenanceMessage(),
      Boolean.TRUE.equals(e.getMobileBindingEnabled()),Boolean.TRUE.equals(e.getEmailBindingEnabled()),Boolean.TRUE.equals(e.getWechatBindingEnabled()),
      e.getSiteLogoSmallUrl(),e.getSiteLogoLargeUrl(),e.getSiteFaviconUrl(),
      size(e.getSiteLogoSmallSize(),32),size(e.getSiteLogoSize(),56),size(e.getSiteLogoLargeSize(),96),
      effective(e.getBrandTagline(),DEFAULT_BRAND_TAGLINE),effective(e.getHomeHeroTagline(),DEFAULT_HOME_HERO_TAGLINE),
      effective(e.getHomeFooterMessage(),DEFAULT_HOME_FOOTER_MESSAGE),effective(e.getCartHeroTagline(),DEFAULT_CART_HERO_TAGLINE),
      effective(e.getDeliveryMessage(),DEFAULT_DELIVERY_MESSAGE),effective(e.getPickupMessage(),DEFAULT_PICKUP_MESSAGE),
      effective(e.getCartFooterMessage(),DEFAULT_CART_FOOTER_MESSAGE),
      effective(e.getProfileWelcomeMessage(),DEFAULT_PROFILE_WELCOME_MESSAGE));}
  /** {@inheritDoc} */
  @Override public boolean dishReviewEnabled(){return Boolean.TRUE.equals(require().getDishReviewEnabled());}
  /** {@inheritDoc} */
  @Override public boolean mobileBindingEnabled(){return Boolean.TRUE.equals(require().getMobileBindingEnabled());}
  /** {@inheritDoc} */
  @Override public boolean emailBindingEnabled(){return Boolean.TRUE.equals(require().getEmailBindingEnabled());}
  /** {@inheritDoc} */
  @Override public boolean wechatBindingEnabled(){return Boolean.TRUE.equals(require().getWechatBindingEnabled());}
  /**
   * {@inheritDoc}
   * <p>密码为空或为展示掩码时保留原密文，只有提交新明文时才重新加密，避免读取后原样保存造成密码丢失。</p>
   */
  @Override @Transactional public SystemSettingView update(Long operatorId,SystemSettingRequest r){
    SystemSettingDO e=new SystemSettingDO(); e.setId(1L); e.setSiteName(r.siteName().trim());
    e.setSiteLogoUrl(r.siteLogoUrl()); e.setDishReviewEnabled(r.dishReviewEnabled());
    e.setSiteLogoSmallUrl(r.siteLogoSmallUrl());e.setSiteLogoLargeUrl(r.siteLogoLargeUrl());e.setSiteFaviconUrl(r.siteFaviconUrl());
    e.setSiteLogoSmallSize(r.siteLogoSmallSize());e.setSiteLogoSize(r.siteLogoSize());e.setSiteLogoLargeSize(r.siteLogoLargeSize());
    e.setBrandTagline(optional(r.brandTagline()));e.setHomeHeroTagline(optional(r.homeHeroTagline()));
    e.setHomeFooterMessage(optional(r.homeFooterMessage()));e.setCartHeroTagline(optional(r.cartHeroTagline()));
    e.setDeliveryMessage(optional(r.deliveryMessage()));e.setPickupMessage(optional(r.pickupMessage()));
    e.setCartFooterMessage(optional(r.cartFooterMessage()));e.setProfileWelcomeMessage(optional(r.profileWelcomeMessage()));
    e.setMaintenanceEnabled(r.maintenanceEnabled()); e.setMaintenanceMessage(r.maintenanceMessage().trim());
    e.setMobileBindingEnabled(r.mobileBindingEnabled());e.setEmailBindingEnabled(r.emailBindingEnabled());
    e.setWechatBindingEnabled(r.wechatBindingEnabled());e.setSmtpHost(r.smtpHost());e.setSmtpPort(r.smtpPort());
    e.setSmtpUsername(r.smtpUsername());e.setSmtpTlsEnabled(r.smtpTlsEnabled());e.setSmtpFrom(r.smtpFrom());
    String password=r.smtpPassword();e.setSmtpPasswordCiphertext(password==null||password.isBlank()||"******".equals(password)
        ? require().getSmtpPasswordCiphertext():secretCipher.encrypt(password));
    e.setUpdatedBy(operatorId); mapper.update(e);
    cached=null;cacheExpiresAt=0;
    auditMapper.insert(operatorId,"SYSTEM_SETTINGS_UPDATE","维护模式="+r.maintenanceEnabled()+"，菜品审核="+r.dishReviewEnabled());
    return current();
  }
  private SystemSettingDO require(){long now=System.currentTimeMillis();SystemSettingDO value=cached;
    if(value!=null&&now<cacheExpiresAt)return value;
    try{SystemSettingDO loaded=mapper.selectCurrent();
      if(loaded==null)throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统配置未初始化");
      cached=loaded;cacheExpiresAt=now+5000;return loaded;
    // 短暂数据库故障时允许使用已过期缓存维持维护开关等关键读路径；无任何历史值则必须暴露故障。
    }catch(RuntimeException exception){if(value!=null)return value;throw exception;}}
  @Override @Transactional public SystemSettingView updateBranding(Long operatorId,BrandSettingRequest r){
    SystemSettingDO e=new SystemSettingDO();e.setId(1L);
    e.setSiteName(r.siteName()==null?null:r.siteName().trim());e.setSiteLogoUrl(r.siteLogoUrl());
    e.setSiteLogoSmallUrl(r.siteLogoSmallUrl());e.setSiteLogoLargeUrl(r.siteLogoLargeUrl());e.setSiteFaviconUrl(r.siteFaviconUrl());
    e.setSiteLogoSmallSize(r.siteLogoSmallSize());e.setSiteLogoSize(r.siteLogoSize());e.setSiteLogoLargeSize(r.siteLogoLargeSize());
    e.setUpdatedBy(operatorId);mapper.updateBranding(e);
    cached=null;cacheExpiresAt=0;
    auditMapper.insert(operatorId,"SYSTEM_SETTINGS_UPDATE","品牌标识更新");
    return current();
  }
  private static String siteName(SystemSettingDO e){return e.getSiteName()==null||e.getSiteName().isBlank()?"食光栀味":e.getSiteName();}
  private static String effective(String value,String fallback){return value==null||value.isBlank()?fallback:value.trim();}
  private static String optional(String value){return value==null||value.isBlank()?null:value.trim();}
  private static Integer size(Integer value,int fallback){return value==null?fallback:value;}
  private static SystemSettingView view(SystemSettingDO e){return new SystemSettingView(siteName(e),
      e.getSiteLogoUrl(),Boolean.TRUE.equals(e.getDishReviewEnabled()),Boolean.TRUE.equals(e.getMaintenanceEnabled()),
      e.getMaintenanceMessage(),Boolean.TRUE.equals(e.getMobileBindingEnabled()),Boolean.TRUE.equals(e.getEmailBindingEnabled()),
      Boolean.TRUE.equals(e.getWechatBindingEnabled()),e.getSmtpHost(),e.getSmtpPort(),e.getSmtpUsername(),
      e.getSmtpPasswordCiphertext()==null||e.getSmtpPasswordCiphertext().isBlank()?"":"******",
      e.getSmtpPasswordCiphertext()!=null&&!e.getSmtpPasswordCiphertext().isBlank(),Boolean.TRUE.equals(e.getSmtpTlsEnabled()),
      e.getSmtpFrom(),e.getUpdatedAt(),e.getSiteLogoSmallUrl(),e.getSiteLogoLargeUrl(),e.getSiteFaviconUrl(),
      size(e.getSiteLogoSmallSize(),32),size(e.getSiteLogoSize(),56),size(e.getSiteLogoLargeSize(),96),
      effective(e.getBrandTagline(),DEFAULT_BRAND_TAGLINE),effective(e.getHomeHeroTagline(),DEFAULT_HOME_HERO_TAGLINE),
      effective(e.getHomeFooterMessage(),DEFAULT_HOME_FOOTER_MESSAGE),effective(e.getCartHeroTagline(),DEFAULT_CART_HERO_TAGLINE),
      effective(e.getDeliveryMessage(),DEFAULT_DELIVERY_MESSAGE),effective(e.getPickupMessage(),DEFAULT_PICKUP_MESSAGE),
      effective(e.getCartFooterMessage(),DEFAULT_CART_FOOTER_MESSAGE),
      effective(e.getProfileWelcomeMessage(),DEFAULT_PROFILE_WELCOME_MESSAGE));}
}
