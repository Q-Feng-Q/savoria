package com.familykitchen.testsupport;

import java.util.Objects;
import java.util.UUID;
import org.testcontainers.containers.MySQLContainer;

/** Issues unforgeable-in-process registrations for containers created by this test JVM. */
public final class TestDatabaseOwnership {

  private static final UUID JVM_ID = UUID.randomUUID();

  private TestDatabaseOwnership() { }

  /**
   * Registers a container object as owned by the current test JVM.
   *
   * @param container container created by the current test JVM
   * @return ownership registration bound to the exact container object
   */
  public static Registration register(MySQLContainer<?> container) {
    return new Registration(JVM_ID, Objects.requireNonNull(container, "container"));
  }

  static MySQLContainer<?> require(Registration registration) {
    if (registration == null || !JVM_ID.equals(registration.jvmId)) {
      throw new IllegalStateException(
          "Unsafe test database configuration: container was not registered by this test JVM");
    }
    return registration.container;
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
