package com.familykitchen.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Modifier;
import java.lang.annotation.Annotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

/** 验证核心业务新增操作均使用显式 MyBatis SQL，避免运行期出现绑定语句缺失。 */
class MapperInsertStatementContractTest {

  @Test
  void coreAggregateInsertsHaveExplicitMapperStatements() throws Exception {
    assertInsert("dish/DishMapper.xml", "insertDish");
    assertInsert("cart/CartMapper.xml", "insertCart");
    assertInsert("order/OrderPersistenceMapper.xml", "insertOrder");
  }

  @Test
  void everyDeclaredMapperMethodHasAnExecutableStatement() throws Exception {
    Path sourceRoot = Path.of("src/main/java");
    try (var files = Files.walk(sourceRoot.resolve("com/familykitchen"))) {
      for (Path file : files.filter(path -> path.getFileName().toString().endsWith("Mapper.java")).toList()) {
        String className = sourceRoot.relativize(file).toString()
            .replace('\\', '.').replace('/', '.').replaceFirst("\\.java$", "");
        Class<?> mapperType = Class.forName(className);
        if (!mapperType.isInterface() || !mapperType.isAnnotationPresent(Mapper.class)) continue;
        for (var method : mapperType.getDeclaredMethods()) {
          if (method.isDefault() || Modifier.isStatic(method.getModifiers())) continue;
          String statementId = mapperType.getName() + "." + method.getName();
          if (hasSqlAnnotation(method.getAnnotations())) continue;
          String module = mapperType.getPackageName().split("\\.")[2];
          Path xmlPath = Path.of("src/main/resources/mapper", module, mapperType.getSimpleName() + ".xml");
          assertThat(xmlPath).as("Mapper 方法缺少 XML 文件：%s", statementId).exists();
          String xml = Files.readString(xmlPath, StandardCharsets.UTF_8);
          assertThat(xml).as("Mapper 方法缺少 SQL 映射：%s", statementId)
              .containsPattern("<(select|insert|update|delete)\\s+id=\\\"" + method.getName() + "\\\"");
        }
      }
    }
  }

  private static boolean hasSqlAnnotation(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      Class<? extends Annotation> type = annotation.annotationType();
      if (type == Select.class || type == Insert.class || type == Update.class || type == Delete.class) return true;
    }
    return false;
  }

  private static void assertInsert(String mapperPath, String statementId) throws Exception {
    String xml = Files.readString(
        Path.of("src/main/resources/mapper").resolve(mapperPath), StandardCharsets.UTF_8);
    assertThat(xml).contains("<insert id=\"" + statementId + "\"");
  }
}
