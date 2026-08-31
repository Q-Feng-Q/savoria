package com.familykitchen.database;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Configuration;

/** Proves late dynamic datasource overrides are rejected before bean creation. */
class TestContainerFinalPropertyGuardTest {

  private static final String BUSINESS_URL =
      "jdbc:mysql://192.168.5.100:3308/family_kitchen?useUnicode=true";

  @Test
  void lateBusinessUrlOverrideIsRejectedAfterEnvironmentPostProcessing() {
    SpringApplication application = new SpringApplication(ContainerProbeConfiguration.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    application.addInitializers(context -> TestPropertyValues.of(
        "spring.datasource.url=" + BUSINESS_URL,
        "spring.datasource.username=business",
        "spring.datasource.password=business").applyTo(context));

    assertThatThrownBy(() -> application.run(
        "--spring.profiles.active=test-container",
        "--spring.main.banner-mode=off"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("final datasource");
  }

  /** Minimal context that loads test auto-configuration without constructing a datasource. */
  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration(exclude = {
      DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class
  })
  static class ContainerProbeConfiguration { }
}
