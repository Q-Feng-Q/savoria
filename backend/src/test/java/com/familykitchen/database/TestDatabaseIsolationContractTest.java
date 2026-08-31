package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.familykitchen.testsupport.TestDatabaseEnvironmentPostProcessor;
import com.familykitchen.testsupport.TestDatabaseUrlGuard;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

/** Verifies that every database-backed test is isolated from business databases. */
class TestDatabaseIsolationContractTest {

  private static final String BUSINESS_URL =
      "jdbc:mysql://192.168.5.100:3308/family_kitchen?useUnicode=true";

  @Test
  void everyTestProfileRejectsTheBusinessDatabaseBeforeDatasourceCreation() {
    assertThrows(IllegalStateException.class,
        () -> TestDatabaseEnvironmentPostProcessor.validate("test-h2", BUSINESS_URL, ""));
    assertThrows(IllegalStateException.class,
        () -> TestDatabaseEnvironmentPostProcessor.validate("test-container", BUSINESS_URL, ""));
    assertThrows(IllegalStateException.class,
        () -> TestDatabaseEnvironmentPostProcessor.validate("test-mvc", BUSINESS_URL,
            TestDatabaseEnvironmentPostProcessor.MVC_EXCLUSIONS));
    assertThrows(IllegalStateException.class,
        () -> TestDatabaseEnvironmentPostProcessor.validate("", BUSINESS_URL, ""));
  }

  @Test
  void h2AndMvcProfilesAcceptOnlyTheirExplicitSafeBootstraps() {
    assertDoesNotThrow(() -> TestDatabaseUrlGuard.requireSafeH2(
        "jdbc:h2:mem:kitchen_test;MODE=MySQL", "test-h2"));
    assertDoesNotThrow(() -> TestDatabaseUrlGuard.requireSafeH2(
        "jdbc:h2:mem:kitchen_container_bootstrap;MODE=MySQL", "test-container"));
    assertDoesNotThrow(() -> TestDatabaseEnvironmentPostProcessor.validate(
        "test-mvc", "  ", TestDatabaseEnvironmentPostProcessor.MVC_EXCLUSIONS));
    assertThrows(IllegalStateException.class,
        () -> TestDatabaseUrlGuard.requireSafeH2("jdbc:h2:file:./kitchen", "test-h2"));
  }

  @Test
  void containerUrlMustBelongToTheRunningContainerObject() {
    MySQLContainer<?> container = mock(MySQLContainer.class);
    when(container.isRunning()).thenReturn(true);
    when(container.getJdbcUrl()).thenReturn("jdbc:mysql://127.0.0.1:49152/kitchen_test");
    when(container.getUsername()).thenReturn("tester");
    when(container.getPassword()).thenReturn("secret");
    when(container.getHost()).thenReturn("127.0.0.1");
    when(container.getMappedPort(3306)).thenReturn(49152);

    assertDoesNotThrow(() -> TestDatabaseUrlGuard.requireOwnedContainer(
        container, "jdbc:mysql://127.0.0.1:49152/kitchen_test", "tester", "secret",
        "test-container"));
    assertThrows(IllegalStateException.class, () -> TestDatabaseUrlGuard.requireOwnedContainer(
        container, BUSINESS_URL, "tester", "secret", "test-container"));
    assertThrows(IllegalStateException.class, () -> TestDatabaseUrlGuard.requireOwnedContainer(
        container, "jdbc:mysql://127.0.0.1:49152/kitchen_test", "tester", "secret",
        "test-h2"));
  }

  @Test
  void everyMysqlIntegrationTestUsesTheOwnedContainerAdapters() throws Exception {
    List<String> springTests = List.of(
        "cart/SharedCartConcurrencyMySqlTest.java",
        "database/FamilyCartWalletMigrationRecoveryMySqlTest.java",
        "dish/MerchantFeaturedDishConcurrencyMySqlTest.java",
        "family/FamilyMemberCartCleanupConcurrencyMySqlTest.java",
        "family/FamilyOwnerTransferConcurrencyMySqlTest.java",
        "merchant/MerchantProfileMySqlTest.java",
        "order/FamilyOrderSubmissionMySqlTest.java",
        "order/FamilyWalletOrderLifecycleMySqlTest.java",
        "wallet/FamilyWalletConcurrencyMySqlTest.java");
    for (String relative : springTests) {
      String source = Files.readString(testSource(relative));
      org.junit.jupiter.api.Assertions.assertTrue(source.contains("@ActiveProfiles(\"test-container\")"), relative);
      org.junit.jupiter.api.Assertions.assertTrue(source.contains("SafeTestDatabaseProperties.register"), relative);
      org.junit.jupiter.api.Assertions.assertFalse(source.contains("spring.datasource.url\""), relative);
    }
    for (String relative : List.of(
        "database/DishTemplateChangeMigrationMySqlTest.java",
        "database/FamilyCartWalletMigrationMySqlTest.java",
        "database/MerchantFeaturedDishMigrationMySqlTest.java")) {
      String source = Files.readString(testSource(relative));
      org.junit.jupiter.api.Assertions.assertTrue(source.contains("SafeTestFlyway.configure"), relative);
      org.junit.jupiter.api.Assertions.assertFalse(source.contains("Flyway.configure()"), relative);
    }
  }

  private static Path testSource(String relative) {
    Path cwd = Path.of(System.getProperty("user.dir"));
    Path backend = Files.isDirectory(cwd.resolve("src/test/java")) ? cwd : cwd.resolve("backend");
    return backend.resolve("src/test/java/com/familykitchen").resolve(relative);
  }
}
