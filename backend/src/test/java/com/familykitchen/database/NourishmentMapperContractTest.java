package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NourishmentMapperContractTest {
  @Test void templatePagingAndCountUseDatabaseTypePredicate() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/mapper/dish/DishTemplateMapper.xml"));
    for (String fragment : new String[]{"TemplateFilters", "AdminTemplateFilters"}) {
      String block = sql.substring(sql.indexOf("<sql id=\"" + fragment + "\""));
      block = block.substring(0, block.indexOf("</sql>"));
      assertTrue(block.contains("#{productType}"), fragment);
    }
  }
  @Test void familyProjectionContainsAllFields() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/mapper/family/FamilyMapper.xml"));
    String block = sql.substring(sql.indexOf("<select id=\"selectFamilyMenuItems\""));
    block = block.substring(0, block.indexOf("</select>"));
    for (String field : new String[]{"productType", "nourishmentDescription", "servingAdvice", "precautions"}) assertTrue(block.contains(field), field);
  }
  @Test void initialSchemaContainsFieldsWithoutNewAlterMigration() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/db/migration/V1__init_schema.sql"));
    for (String table : new String[]{"dishes", "dish_templates"}) {
      String block = sql.substring(sql.indexOf("CREATE TABLE " + table + " ("));
      block = block.substring(0, block.indexOf("ENGINE=InnoDB"));
      for (String field : new String[]{"product_type", "nourishment_description", "serving_advice", "precautions"}) assertTrue(block.contains(field), field);
    }
    assertFalse(Files.exists(Path.of("src/main/resources/db/migration/V5__dish_nourishment.sql")));
  }
}
