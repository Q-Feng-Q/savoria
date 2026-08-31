package com.familykitchen.testsupport;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/** Validates test datasource properties after config files load and before bean creation. */
public final class TestDatabaseEnvironmentPostProcessor
    implements EnvironmentPostProcessor, Ordered {

  /** Auto-configurations disabled by the datasource-free MVC test profile. */
  public static final String MVC_EXCLUSIONS = String.join(",",
      "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
      "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration");

  /** {@inheritDoc} */
  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment,
      SpringApplication application) {
    String profile = Arrays.stream(environment.getActiveProfiles())
        .filter(value -> value.startsWith("test-"))
        .findFirst()
        .orElse("");
    validate(profile, environment.getProperty("spring.datasource.url", ""),
        environment.getProperty("spring.autoconfigure.exclude", ""));
  }

  /**
   * Validates one resolved test database bootstrap.
   *
   * @param profile explicit test profile
   * @param url resolved datasource URL
   * @param exclusions disabled Spring auto-configurations
   */
  public static void validate(String profile, String url, String exclusions) {
    if ("test-h2".equals(profile) || "test-container".equals(profile)) {
      TestDatabaseUrlGuard.requireSafeH2(url, profile);
      return;
    }
    if ("test-mvc".equals(profile)) {
      String value = url == null ? "" : url.trim();
      String excluded = exclusions == null ? "" : exclusions;
      if (!value.isEmpty()
          || !excluded.contains("DataSourceAutoConfiguration")
          || !excluded.contains("FlywayAutoConfiguration")) {
        throw new IllegalStateException(
            "Unsafe test database configuration: test-mvc must disable datasource and Flyway");
      }
      return;
    }
    throw new IllegalStateException(
        "Unsafe test database configuration: an explicit test profile is required");
  }

  /** {@inheritDoc} */
  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
