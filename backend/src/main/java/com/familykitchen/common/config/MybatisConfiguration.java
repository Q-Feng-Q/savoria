package com.familykitchen.common.config;

import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 基础配置。
 *
 * <p>统一启用 Mapper 扫描和常用映射选项，保证 XML Mapper 与实体注解可以共存。</p>
 */
@Configuration
@MapperScan(basePackages = {
    "com.familykitchen.admin.mapper",
    "com.familykitchen.auth.mapper",
    "com.familykitchen.cart.mapper",
    "com.familykitchen.common.security",
    "com.familykitchen.dish.mapper",
    "com.familykitchen.family.mapper",
    "com.familykitchen.file.mapper",
    "com.familykitchen.merchant.mapper",
    "com.familykitchen.notification.mapper",
    "com.familykitchen.order.mapper",
    "com.familykitchen.purchase.mapper",
    "com.familykitchen.system.mapper",
    "com.familykitchen.user.mapper",
    "com.familykitchen.wallet.mapper"
})
public class MybatisConfiguration {

  /**
   * 配置 MyBatis 通用行为。
   *
   * <p>启用下划线转驼峰、空值 setter 调用和空 JDBC 类型回填。</p>
   * @return 应用于 MyBatis 全局配置的定制器
   */
  @Bean
  public org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer configurationCustomizer() {
    return configuration -> {
      configuration.setMapUnderscoreToCamelCase(true);
      configuration.setCallSettersOnNulls(true);
      configuration.setJdbcTypeForNull(JdbcType.NULL);
    };
  }
}

