package com.familykitchen.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class MiniProgramEndpointContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void everyMiniProgramServiceOperationMapsToTheDeclaredControllerHandler() throws Exception {
    try (InputStream input = getClass().getResourceAsStream(
        "/contracts/mini-program-api-contract.json")) {
      assertNotNull(input, "missing mini-program-api-contract.json");
      List<EndpointContract> contracts = objectMapper.readValue(
          input, new TypeReference<List<EndpointContract>>() { });
      assertEquals(118, contracts.size());
      Set<String> operationKeys = new HashSet<>();
      for (EndpointContract contract : contracts) {
        assertTrue(operationKeys.add(contract.operation()), contract.operation());
        Class<?> controller = Class.forName(contract.controllerFqcn());
        Method handler = Arrays.stream(controller.getDeclaredMethods())
            .filter(method -> method.getName().equals(contract.controllerMethod()))
            .findFirst()
            .orElseThrow(() -> new AssertionError(contract.operation() + ": missing handler"));
        assertEquals(contract.method(), httpMethod(handler), contract.operation());
        assertEquals(contract.backendRoute(), route(controller, handler), contract.operation());
        assertFalse(contract.pages().isEmpty(), contract.operation() + ": no page or shared runtime owner");
      }
    }
  }

  @Test
  void everyBackendHandlerInsideMiniProgramBoundaryIsDeclared() throws Exception {
    Set<String> declared = new HashSet<>();
    for (EndpointContract contract : readContracts()) {
      declared.add(contract.method() + " " + contract.backendRoute());
    }
    Set<String> backendHandlers = new HashSet<>();
    try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
      for (Path file : files.filter(path -> path.getFileName().toString().endsWith("Controller.java")).toList()) {
        String source = Files.readString(file);
        String packageName = source.replaceAll("(?s).*?package\\s+([\\w.]+);.*", "$1");
        String className = source.replaceAll("(?s).*?public\\s+class\\s+(\\w+).*", "$1");
        Class<?> controller = Class.forName(packageName + "." + className);
        RequestMapping root = controller.getAnnotation(RequestMapping.class);
        if (root == null || root.value().length == 0 || !insideBoundary(root.value()[0])) continue;
        for (Method method : controller.getDeclaredMethods()) {
          if (!hasHttpMapping(method)) continue;
          backendHandlers.add(httpMethod(method) + " " + route(controller, method));
        }
      }
    }
    assertEquals(backendHandlers, declared);
  }

  private List<EndpointContract> readContracts() throws Exception {
    try (InputStream input = getClass().getResourceAsStream("/contracts/mini-program-api-contract.json")) {
      assertNotNull(input, "missing mini-program-api-contract.json");
      return objectMapper.readValue(input, new TypeReference<List<EndpointContract>>() { });
    }
  }

  private boolean insideBoundary(String route) {
    return Stream.of("/auth", "/users/me", "/family", "/merchant",
        "/notifications", "/files", "/public/system-settings")
        .anyMatch(route::startsWith)
        && !route.startsWith("/admin")
        && !route.startsWith("/merchant/members");
  }

  private boolean hasHttpMapping(Method method) {
    return method.isAnnotationPresent(GetMapping.class)
        || method.isAnnotationPresent(PostMapping.class)
        || method.isAnnotationPresent(PutMapping.class)
        || method.isAnnotationPresent(DeleteMapping.class);
  }

  private String route(Class<?> controller, Method handler) {
    RequestMapping root = controller.getAnnotation(RequestMapping.class);
    String prefix = root == null || root.value().length == 0 ? "" : root.value()[0];
    return (prefix + mappingPath(handler)).replaceAll("/{2,}", "/");
  }

  private String httpMethod(Method method) {
    if (method.isAnnotationPresent(GetMapping.class)) return "GET";
    if (method.isAnnotationPresent(PostMapping.class)) return "POST";
    if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
    if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
    throw new AssertionError(method + ": missing HTTP mapping");
  }

  private String mappingPath(Method method) {
    if (method.isAnnotationPresent(GetMapping.class)) {
      GetMapping mapping = method.getAnnotation(GetMapping.class);
      return first(mapping.value(), mapping.path());
    }
    if (method.isAnnotationPresent(PostMapping.class)) {
      PostMapping mapping = method.getAnnotation(PostMapping.class);
      return first(mapping.value(), mapping.path());
    }
    if (method.isAnnotationPresent(PutMapping.class)) {
      PutMapping mapping = method.getAnnotation(PutMapping.class);
      return first(mapping.value(), mapping.path());
    }
    if (method.isAnnotationPresent(DeleteMapping.class)) {
      DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
      return first(mapping.value(), mapping.path());
    }
    throw new AssertionError(method + ": missing HTTP mapping");
  }

  private String first(String[] values, String[] paths) {
    if (values.length > 0) return values[0];
    return paths.length == 0 ? "" : paths[0];
  }

  record EndpointContract(
      String operation,
      List<String> pages,
      String method,
      String backendRoute,
      String controllerFqcn,
      String controllerMethod
  ) { }
}
