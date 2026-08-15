package com.familykitchen.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.familykitchen.system.mapper.SystemAuditMapper;
import com.familykitchen.system.mapper.SystemSettingMapper;
import com.familykitchen.system.model.entity.SystemSettingDO;
import com.familykitchen.system.service.impl.SystemSettingServiceImpl;
import com.familykitchen.system.security.PlatformSecretCipher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 验证绑定开关的公开可见性与管理员 SMTP 密码脱敏边界。 */
@ExtendWith(MockitoExtension.class)
class SystemBindingSettingServiceTest {
  @Mock private SystemSettingMapper mapper;
  @Mock private SystemAuditMapper audits;

  @Test
  void exposesBindingSwitchesPubliclyWithoutSmtpCredentials() {
    var setting = configured();
    when(mapper.selectCurrent()).thenReturn(setting);
    var service = new SystemSettingServiceImpl(mapper, audits, new PlatformSecretCipher("test-secret"));

    var publicView = service.publicCurrent();

    assertThat(publicView.emailBindingEnabled()).isTrue();
    assertThat(publicView.mobileBindingEnabled()).isTrue();
    assertThat(publicView.wechatBindingEnabled()).isFalse();
    assertThat(publicView.toString()).doesNotContain("smtp.example.com", "smtp-user", "encrypted-secret");
  }

  @Test
  void masksConfiguredSmtpPasswordForPlatformAdministrator() {
    var setting = configured();
    when(mapper.selectCurrent()).thenReturn(setting);
    var service = new SystemSettingServiceImpl(mapper, audits, new PlatformSecretCipher("test-secret"));

    var adminView = service.current();

    assertThat(adminView.smtpPassword()).isEqualTo("******");
    assertThat(adminView.smtpPasswordConfigured()).isTrue();
  }

  private static SystemSettingDO configured() {
    var setting = new SystemSettingDO();
    setting.setId(1L); setting.setSiteName("食光知味"); setting.setSiteLogoUrl("");
    setting.setDishReviewEnabled(true); setting.setMaintenanceEnabled(false); setting.setMaintenanceMessage("维护中");
    setting.setEmailBindingEnabled(true); setting.setMobileBindingEnabled(true); setting.setWechatBindingEnabled(false);
    setting.setSmtpHost("smtp.example.com"); setting.setSmtpPort(587); setting.setSmtpUsername("smtp-user");
    setting.setSmtpPasswordCiphertext("encrypted-secret"); setting.setSmtpTlsEnabled(true); setting.setSmtpFrom("noreply@example.com");
    return setting;
  }
}
