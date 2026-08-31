package com.familykitchen.testsupport;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.testcontainers.containers.MySQLContainer;

/** Issues unforgeable-in-process registrations for containers created by this test JVM. */
public final class TestDatabaseOwnership {

  private static final UUID JVM_ID = UUID.randomUUID();
  private static final Set<Registration> REGISTRATIONS = ConcurrentHashMap.newKeySet();

  private TestDatabaseOwnership() { }

  /**
   * Registers a container object as owned by the current test JVM.
   *
   * @param container container created by the current test JVM
   * @return ownership registration bound to the exact container object
   */
  public static Registration register(MySQLContainer<?> container) {
    Registration registration = new Registration(
        JVM_ID, Objects.requireNonNull(container, "container"));
    REGISTRATIONS.add(registration);
    return registration;
  }

  static MySQLContainer<?> require(Registration registration) {
    if (registration == null || !JVM_ID.equals(registration.jvmId)
        || !REGISTRATIONS.contains(registration)) {
      throw new IllegalStateException(
          "Unsafe test database configuration: container was not registered by this test JVM");
    }
    return registration.container;
  }

  static Registration requireMatching(String url, String username, String password) {
    for (Registration registration : REGISTRATIONS) {
      MySQLContainer<?> container = registration.container;
      if (container.isRunning()
          && Objects.equals(container.getJdbcUrl(), url)
          && Objects.equals(container.getUsername(), username)
          && Objects.equals(container.getPassword(), password)) {
        return registration;
      }
    }
    throw new IllegalStateException(
        "Unsafe test database configuration: final datasource is not owned by this test JVM");
  }

  /** Opaque proof that one exact container object was registered by this test JVM. */
  public static final class Registration {

    private final UUID jvmId;
    private final MySQLContainer<?> container;

    private Registration(UUID jvmId, MySQLContainer<?> container) {
      this.jvmId = jvmId;
      this.container = container;
    }
  }
}
