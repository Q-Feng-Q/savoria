package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.familykitchen.testsupport.TestDatabaseEnvironmentPostProcessor;
import com.familykitchen.testsupport.TestDatabaseOwnership;
import com.familykitchen.testsupport.TestDatabaseUrlGuard;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    TestDatabaseOwnership.Registration ownership = TestDatabaseOwnership.register(container);

    assertDoesNotThrow(() -> TestDatabaseUrlGuard.requireOwnedContainer(
        ownership, "jdbc:mysql://127.0.0.1:49152/kitchen_test", "tester", "secret",
        "test-container"));
    assertThrows(IllegalStateException.class, () -> TestDatabaseUrlGuard.requireOwnedContainer(
        ownership, BUSINESS_URL, "tester", "secret", "test-container"));
    assertThrows(IllegalStateException.class, () -> TestDatabaseUrlGuard.requireOwnedContainer(
        ownership, "jdbc:mysql://127.0.0.1:49152/kitchen_test", "tester", "secret",
        "test-h2"));
  }

  @Test
  void containerNamedLikeBusinessDatabaseIsRejectedEvenWhenCoordinatesMatch() {
    MySQLContainer<?> container = mock(MySQLContainer.class);
    when(container.isRunning()).thenReturn(true);
    when(container.getJdbcUrl()).thenReturn("jdbc:mysql://127.0.0.1:49152/family_kitchen");
    when(container.getUsername()).thenReturn("tester");
    when(container.getPassword()).thenReturn("secret");
    when(container.getHost()).thenReturn("127.0.0.1");
    when(container.getMappedPort(3306)).thenReturn(49152);
    TestDatabaseOwnership.Registration ownership = TestDatabaseOwnership.register(container);

    assertThrows(IllegalStateException.class, () -> TestDatabaseUrlGuard.requireOwnedContainer(
        ownership, "jdbc:mysql://127.0.0.1:49152/family_kitchen", "tester", "secret",
        "test-container"));
  }

  @Test
  void runningButUnregisteredContainerCannotClaimCurrentJvmOwnership() {
    MySQLContainer<?> container = mock(MySQLContainer.class);
    when(container.isRunning()).thenReturn(true);
    when(container.getJdbcUrl()).thenReturn("jdbc:mysql://127.0.0.1:49152/kitchen_test");
    when(container.getUsername()).thenReturn("tester");
    when(container.getPassword()).thenReturn("secret");
    when(container.getHost()).thenReturn("127.0.0.1");
    when(container.getMappedPort(3306)).thenReturn(49152);

    assertThrows(IllegalStateException.class, () -> TestDatabaseUrlGuard.requireOwnedContainer(
        null, "jdbc:mysql://127.0.0.1:49152/kitchen_test", "tester", "secret",
        "test-container"));
  }

  @Test
  void mvcExclusionsMustMatchBothAutoConfigurationClassesExactly() {
    assertThrows(IllegalStateException.class, () -> TestDatabaseEnvironmentPostProcessor.validate(
        "test-mvc", "", TestDatabaseEnvironmentPostProcessor.MVC_EXCLUSIONS.replace(
            "DataSourceAutoConfiguration", "DataSourceAutoConfigurationSuffix")));
  }

  @Test
  void everyMysqlIntegrationTestUsesTheOwnedContainerAdapters() throws Exception {
    List<Path> mysqlTests = new ArrayList<>();
    try (var files = Files.walk(testJavaRoot())) {
      for (Path file : files.filter(path -> path.toString().endsWith("MySqlTest.java")).toList()) {
        String source = Files.readString(file);
        mysqlTests.add(file);
        String relative = testJavaRoot().relativize(file).toString();
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("new MySQLContainer"),
            relative + ": every MySQL test must use an owned Testcontainer");
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("MYSQL_OWNER"), relative);
        org.junit.jupiter.api.Assertions.assertTrue(
            source.contains("TestDatabaseOwnership.register(MYSQL)"), relative);
        org.junit.jupiter.api.Assertions.assertFalse(source.contains("jdbc:mysql:"), relative);
        org.junit.jupiter.api.Assertions.assertFalse(
            source.contains("registry.add(\"spring.datasource.url\""), relative);
        org.junit.jupiter.api.Assertions.assertFalse(source.contains("Flyway.configure()"), relative);

        if (source.contains("@SpringBootTest")) {
          org.junit.jupiter.api.Assertions.assertTrue(
              source.contains("@ActiveProfiles(\"test-container\")"), relative);
          org.junit.jupiter.api.Assertions.assertTrue(
              source.contains("SafeTestDatabaseProperties.register"), relative);
        }
        if (source.contains(".migrate()")) {
          org.junit.jupiter.api.Assertions.assertTrue(
              source.contains("SafeTestFlyway.configure(MYSQL_OWNER)"), relative);
        }
        if (source.contains("DriverManager." + "getConnection")) {
          org.junit.jupiter.api.Assertions.assertTrue(source.contains(
              "DriverManager." + "getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())"),
              relative);
        }
      }
    }
    org.junit.jupiter.api.Assertions.assertFalse(mysqlTests.isEmpty(),
        "MySQL test discovery unexpectedly found no integration tests");
  }

  @Test
  void rawJdbcEntrypointsAcrossTheTestTreeCannotBypassOwnedContainers() throws Exception {
    String rawJdbcCall = "DriverManager." + "getConnection";
    String ownedJdbcCall = "DriverManager." + "getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), "
        + "MYSQL.getPassword())";
    List<String> mysqlUrlAllowlist = List.of(
        "com/familykitchen/database/TestContainerFinalPropertyGuardTest.java",
        "com/familykitchen/database/TestDatabaseIsolationContractTest.java",
        "com/familykitchen/database/TestMvcProfileBootstrapTest.java");
    try (var files = Files.walk(testJavaRoot())) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file);
        String relative = testJavaRoot().relativize(file).toString().replace('\\', '/');
        if (source.contains("jdbc:mysql:")) {
          org.junit.jupiter.api.Assertions.assertTrue(mysqlUrlAllowlist.contains(relative),
              relative + ": literal MySQL URLs are allowed only in negative guard tests");
        }
        if (source.contains(rawJdbcCall)) {
          org.junit.jupiter.api.Assertions.assertTrue(relative.endsWith("MySqlTest.java"),
              relative + ": raw JDBC is allowed only in owned MySQL integration tests");
          org.junit.jupiter.api.Assertions.assertTrue(source.contains("MYSQL_OWNER"), relative);
          org.junit.jupiter.api.Assertions.assertTrue(source.contains(ownedJdbcCall), relative);
        }
      }
    }
  }

  @Test
  void finalPropertyGuardIsLoadedAsTestAutoConfiguration() throws Exception {
    Path imports = testResourcesRoot().resolve(
        "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
    org.junit.jupiter.api.Assertions.assertTrue(Files.exists(imports));
    org.junit.jupiter.api.Assertions.assertTrue(Files.readString(imports).contains(
        "com.familykitchen.testsupport.TestDatabaseGuardAutoConfiguration"));
  }

  private static Path backendRoot() {
    Path cwd = Path.of(System.getProperty("user.dir"));
    return Files.isDirectory(cwd.resolve("src/test/java")) ? cwd : cwd.resolve("backend");
  }

  private static Path testJavaRoot() {
    return backendRoot().resolve("src/test/java");
  }

  private static Path testResourcesRoot() {
    return backendRoot().resolve("src/test/resources");
  }
}
