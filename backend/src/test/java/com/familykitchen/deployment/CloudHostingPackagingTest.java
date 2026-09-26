package com.familykitchen.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies the standalone Cloud Hosting packaging variants and their port contracts. */
class CloudHostingPackagingTest {

  private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();

  @Test
  void cloudHostingImageRunsOnlyTheBackendJarOnThePlatformPort() throws IOException {
    String dockerfile = Files.readString(REPOSITORY_ROOT.resolve("Dockerfile"));
    String containerConfig = Files.readString(REPOSITORY_ROOT.resolve("container.config.json"));
    String applicationConfig = Files.readString(
        REPOSITORY_ROOT.resolve("backend/src/main/resources/application.yml"));

    assertTrue(dockerfile.contains("java\", \"-jar\", \"/app/app.jar"));
    assertFalse(dockerfile.contains("nginx"));
    assertFalse(dockerfile.contains("cloudrun-entrypoint.sh"));
    assertFalse(dockerfile.contains("SERVER_PORT"));
    assertFalse(containerConfig.contains("SERVER_PORT"));
    assertTrue(dockerfile.contains("PORT=8081"));
    assertTrue(dockerfile.contains("EXPOSE 8081"));
    assertTrue(containerConfig.contains("\"containerPort\": 8081"));
    assertTrue(containerConfig.contains("\"PORT\": \"8081\""));
    assertTrue(applicationConfig.contains("port: ${PORT:8081}"));
    assertTrue(applicationConfig.contains(
        "jdbc:" + "mysql://${MYSQL_ADDRESS}/${MYSQL_DATABASE}"));
    assertTrue(applicationConfig.contains("username: ${MYSQL_USERNAME}"));
    assertTrue(applicationConfig.contains("password: ${MYSQL_PASSWORD}"));
    assertTrue(applicationConfig.contains("secret: ${FAMILY_KITCHEN_JWT_SECRET}"));
    assertTrue(applicationConfig.contains(
        "username: ${FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_USERNAME:admin}"));
    assertTrue(applicationConfig.contains(
        "password: ${FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD}"));
    assertTrue(applicationConfig.contains("app-id: \"${FAMILY_KITCHEN_WECHAT_APP_ID}\""));
    assertTrue(applicationConfig.contains("app-secret: \"${FAMILY_KITCHEN_WECHAT_APP_SECRET}\""));
  }

  @Test
  void previousCompleteCloudHostingPackageIsKeptInItsOwnDirectory() throws IOException {
    Path packageDirectory = REPOSITORY_ROOT.resolve("docker/cloud-hosting-full");

    String dockerfile = Files.readString(packageDirectory.resolve("Dockerfile"));
    String readme = Files.readString(packageDirectory.resolve("README.md"));

    assertTrue(Files.isRegularFile(packageDirectory.resolve("cloudrun-entrypoint.sh")));
    assertTrue(Files.isRegularFile(packageDirectory.resolve("cloudrun-nginx.conf")));
    assertTrue(Files.isRegularFile(packageDirectory.resolve("container.config.json")));
    assertTrue(Files.isRegularFile(packageDirectory.resolve("Dockerfile.dockerignore")));
    assertTrue(dockerfile.contains("nginx"));
    assertTrue(dockerfile.contains("docker/cloud-hosting-full/cloudrun-entrypoint.sh"));
    assertTrue(dockerfile.contains("docker/cloud-hosting-full/cloudrun-nginx.conf"));
    assertTrue(readme.contains("docker build -f docker/cloud-hosting-full/Dockerfile"));
  }

  @Test
  void backendOnlyCloudHostingPackageIsKeptInItsOwnDirectory() throws IOException {
    Path packageDirectory = REPOSITORY_ROOT.resolve("docker/cloud-hosting-backend");

    String dockerfile = Files.readString(packageDirectory.resolve("Dockerfile"));
    String containerConfig = Files.readString(packageDirectory.resolve("container.config.json"));
    String readme = Files.readString(packageDirectory.resolve("README.md"));

    assertTrue(Files.isRegularFile(packageDirectory.resolve("Dockerfile.dockerignore")));
    assertTrue(dockerfile.contains("java\", \"-jar\", \"/app/app.jar"));
    assertTrue(dockerfile.contains("PORT=8081"));
    assertFalse(dockerfile.contains("nginx"));
    assertFalse(dockerfile.contains("SERVER_PORT"));
    assertTrue(containerConfig.contains("\"containerPort\": 8081"));
    assertTrue(containerConfig.contains("\"PORT\": \"8081\""));
    assertTrue(readme.contains("docker build -f docker/cloud-hosting-backend/Dockerfile"));
    assertTrue(normalizeLineEndings(dockerfile).equals(normalizeLineEndings(
        Files.readString(REPOSITORY_ROOT.resolve("Dockerfile")))));
    assertTrue(normalizeLineEndings(containerConfig).equals(normalizeLineEndings(
        Files.readString(REPOSITORY_ROOT.resolve("container.config.json")))));
  }

  private static String normalizeLineEndings(String value) {
    return value.replace("\r\n", "\n");
  }
}
