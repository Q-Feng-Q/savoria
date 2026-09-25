package com.familykitchen.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.familykitchen.system.mapper.SystemSettingMapper;
import com.familykitchen.system.model.entity.SystemSettingDO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/** Runs only on a uniquely named in-memory H2 database; never loads Spring or Flyway. */
class BrandSettingMapperTest {
  @Test void savesAndRereadsBrandingWithoutTouchingUnrelatedColumns() throws Exception {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:brand_" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
      sql.execute("CREATE TABLE system_settings (id BIGINT PRIMARY KEY, site_name VARCHAR(100), site_logo_url VARCHAR(500), "
          + "site_logo_small_url VARCHAR(500), site_logo_large_url VARCHAR(500), site_favicon_url VARCHAR(500), "
          + "site_logo_small_size INT, site_logo_size INT, site_logo_large_size INT, dish_review_enabled BOOLEAN, "
          + "maintenance_enabled BOOLEAN, maintenance_message VARCHAR(500), mobile_binding_enabled BOOLEAN, "
          + "email_binding_enabled BOOLEAN, wechat_binding_enabled BOOLEAN, smtp_host VARCHAR(255), smtp_port INT, "
          + "smtp_username VARCHAR(255), smtp_password_ciphertext VARCHAR(500), smtp_tls_enabled BOOLEAN, "
          + "smtp_from VARCHAR(255), updated_by BIGINT, updated_at TIMESTAMP)");
      sql.execute("INSERT INTO system_settings (id,site_name,site_logo_url,site_logo_small_url,site_logo_small_size, "
          + "smtp_host,smtp_password_ciphertext,maintenance_enabled) VALUES (1,'Custom','/standard.png','/small.png',40,'smtp.private','secret',TRUE)");
    }
    var config = new Configuration(new Environment("isolated-brand-test", new JdbcTransactionFactory(), ds));
    try (var stream = getClass().getResourceAsStream("/mapper/system/SystemSettingMapper.xml")) {
      new XMLMapperBuilder(stream, config, "brand-mapper", config.getSqlFragments()).parse();
    }
    try (var session = new SqlSessionFactoryBuilder().build(config).openSession(true)) {
      var mapper = session.getMapper(SystemSettingMapper.class);
      var patch = new SystemSettingDO();patch.setSiteLogoSmallUrl("");patch.setSiteLogoLargeSize(160);patch.setUpdatedBy(9L);
      mapper.updateBranding(patch);
      var result = mapper.selectCurrent();
      assertThat(result.getSiteName()).isEqualTo("Custom");
      assertThat(result.getSiteLogoUrl()).isEqualTo("/standard.png");
      assertThat(result.getSiteLogoSmallUrl()).isEmpty();
      assertThat(result.getSiteLogoSmallSize()).isEqualTo(40);
      assertThat(result.getSiteLogoLargeSize()).isEqualTo(160);
      assertThat(result.getSmtpHost()).isEqualTo("smtp.private");
      assertThat(result.getSmtpPasswordCiphertext()).isEqualTo("secret");
      assertThat(result.getMaintenanceEnabled()).isTrue();
      assertThat(result.getUpdatedBy()).isEqualTo(9L);
      // Old full PUT leaves new fields null: SQL must preserve their stored values.
      var oldFullPut = new SystemSettingDO();oldFullPut.setSiteName("Changed");oldFullPut.setUpdatedBy(10L);
      mapper.update(oldFullPut);
      var reread = mapper.selectCurrent();
      assertThat(reread.getSiteLogoSmallUrl()).isEmpty();
      assertThat(reread.getSiteLogoSmallSize()).isEqualTo(40);
      assertThat(reread.getSiteLogoLargeSize()).isEqualTo(160);
      var allFields = new SystemSettingDO();
      allFields.setSiteName("Brand");allFields.setSiteLogoUrl("https://cdn.example.com/standard.png");
      allFields.setSiteLogoSmallUrl("/small.png");allFields.setSiteLogoLargeUrl("/large.png");allFields.setSiteFaviconUrl("/favicon.ico");
      allFields.setSiteLogoSmallSize(64);allFields.setSiteLogoSize(120);allFields.setSiteLogoLargeSize(160);allFields.setUpdatedBy(11L);
      mapper.updateBranding(allFields);
      var allRead = mapper.selectCurrent();
      assertThat(allRead.getSiteName()).isEqualTo("Brand");
      assertThat(allRead.getSiteLogoUrl()).isEqualTo("https://cdn.example.com/standard.png");
      assertThat(allRead.getSiteLogoSmallUrl()).isEqualTo("/small.png");
      assertThat(allRead.getSiteLogoLargeUrl()).isEqualTo("/large.png");
      assertThat(allRead.getSiteFaviconUrl()).isEqualTo("/favicon.ico");
      assertThat(allRead.getSiteLogoSmallSize()).isEqualTo(64);
      assertThat(allRead.getSiteLogoSize()).isEqualTo(120);
      assertThat(allRead.getSiteLogoLargeSize()).isEqualTo(160);
    }
  }
}
