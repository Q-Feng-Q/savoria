package com.familykitchen.system.service.impl;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.system.mapper.SystemSettingMapper;
import com.familykitchen.system.mapper.SystemAuditMapper;
import com.familykitchen.system.model.dto.SystemSettingRequest;
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
      e.getSiteName(),e.getSiteLogoUrl(),Boolean.TRUE.equals(e.getMaintenanceEnabled()),e.getMaintenanceMessage(),
      Boolean.TRUE.equals(e.getMobileBindingEnabled()),Boolean.TRUE.equals(e.getEmailBindingEnabled()),Boolean.TRUE.equals(e.getWechatBindingEnabled()));}
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
  private static SystemSettingView view(SystemSettingDO e){return new SystemSettingView(e.getSiteName(),
      e.getSiteLogoUrl(),Boolean.TRUE.equals(e.getDishReviewEnabled()),Boolean.TRUE.equals(e.getMaintenanceEnabled()),
      e.getMaintenanceMessage(),Boolean.TRUE.equals(e.getMobileBindingEnabled()),Boolean.TRUE.equals(e.getEmailBindingEnabled()),
      Boolean.TRUE.equals(e.getWechatBindingEnabled()),e.getSmtpHost(),e.getSmtpPort(),e.getSmtpUsername(),
      e.getSmtpPasswordCiphertext()==null||e.getSmtpPasswordCiphertext().isBlank()?"":"******",
      e.getSmtpPasswordCiphertext()!=null&&!e.getSmtpPasswordCiphertext().isBlank(),Boolean.TRUE.equals(e.getSmtpTlsEnabled()),
      e.getSmtpFrom(),e.getUpdatedAt());}
}
