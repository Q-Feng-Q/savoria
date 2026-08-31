package com.familykitchen.testsupport;

import java.net.URI;
import java.util.Objects;
import org.testcontainers.containers.MySQLContainer;

/** Fails test startup before a datasource can target a developer or business database. */
public final class TestDatabaseUrlGuard {

  private TestDatabaseUrlGuard() { }

  /**
   * Requires an in-memory H2 URL under an explicit safe profile.
   *
   * @param url resolved datasource URL
   * @param profile active test profile
   */
  public static void requireSafeH2(String url, String profile) {
    if (!"test-h2".equals(profile) && !"test-container".equals(profile)) {
      throw unsafe("H2 bootstrap requires test-h2 or test-container", profile, url);
    }
    String value = normalized(url);
    if (!value.toLowerCase().startsWith("jdbc:h2:mem:")) {
      throw unsafe("only an in-memory H2 datasource is allowed before test startup", profile, url);
    }
  }

  /**
   * Requires datasource coordinates to match a running test-owned container.
   *
   * @param container container owned by the current test JVM
   * @param url resolved datasource URL
   * @param username resolved datasource user
   * @param password resolved datasource password
   * @param profile active test profile
   */
  public static void requireOwnedContainer(MySQLContainer<?> container, String url,
      String username, String password, String profile) {
    if (!"test-container".equals(profile)) {
      throw unsafe("container datasource requires test-container", profile, url);
    }
    if (container == null || !container.isRunning()) {
      throw unsafe("the MySQL container is not owned and running in this test JVM", profile, url);
    }
    if (!Objects.equals(container.getJdbcUrl(), url)
        || !Objects.equals(container.getUsername(), username)
        || !Objects.equals(container.getPassword(), password)) {
      throw unsafe("datasource credentials do not match the running container", profile, url);
    }
    URI endpoint;
    try {
      endpoint = URI.create(normalized(url).substring("jdbc:".length()));
    } catch (RuntimeException error) {
      throw unsafe("invalid container JDBC URL", profile, url);
    }
    if (!Objects.equals(endpoint.getHost(), container.getHost())
        || endpoint.getPort() != container.getMappedPort(3306)) {
      throw unsafe("datasource host or port does not match the running container", profile, url);
    }
  }

  private static String normalized(String value) {
    return value == null ? "" : value.trim();
  }

  private static IllegalStateException unsafe(String reason, String profile, String url) {
    return new IllegalStateException("Unsafe test database configuration: " + reason
        + " [profile=" + normalized(profile) + ", url=" + normalized(url) + "]");
  }
}
