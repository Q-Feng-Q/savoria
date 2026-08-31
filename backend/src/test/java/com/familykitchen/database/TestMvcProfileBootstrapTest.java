package com.familykitchen.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Exercises the datasource-free MVC profile through a real Spring bootstrap. */
class TestMvcProfileBootstrapTest {

  private static final String BUSINESS_URL =
      "jdbc:mysql://192.168.5.100:3308/family_kitchen?useUnicode=true";
  private static final AtomicBoolean DATA_SOURCE_CONSTRUCTED = new AtomicBoolean();

  @AfterEach
  void resetProbe() {
    DATA_SOURCE_CONSTRUCTED.set(false);
  }

  @Test
  void businessUrlFailsBeforeAnyDatasourceCanBeConstructed() {
    SpringApplication application = application();

    assertThatThrownBy(() -> application.run(
        "--spring.profiles.active=test-mvc",
        "--spring.datasource.url=" + BUSINESS_URL,
        "--probe.datasource=true",
        "--spring.main.banner-mode=off"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Unsafe test database configuration");
    assertFalse(DATA_SOURCE_CONSTRUCTED.get());
  }

  @Test
  void normalMvcProfileContainsNeitherDatasourceNorFlyway() {
    SpringApplication application = application();
    try (ConfigurableApplicationContext context = application.run(
        "--spring.profiles.active=test-mvc",
        "--spring.main.banner-mode=off")) {
      assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
      assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
      Set<String> exclusions = Arrays.stream(context.getEnvironment()
              .getRequiredProperty("spring.autoconfigure.exclude").split(","))
          .map(String::trim)
          .collect(Collectors.toSet());
      assertThat(exclusions).containsExactlyInAnyOrder(
          "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
          "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration");
    }
  }

  private static SpringApplication application() {
    SpringApplication application = new SpringApplication(MvcProbeConfiguration.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    return application;
  }

  /** Minimal configuration used to prove database auto-configuration stays disabled. */
  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  static class MvcProbeConfiguration {

    /**
     * Creates an observable datasource probe only when explicitly requested.
     *
     * @return datasource that must never be created for an unsafe bootstrap
     */
    @Bean
    @ConditionalOnProperty(name = "probe.datasource", havingValue = "true")
    DataSource probeDataSource() {
      DATA_SOURCE_CONSTRUCTED.set(true);
      return new DriverManagerDataSource("jdbc:h2:mem:must_not_construct");
    }
  }
}
