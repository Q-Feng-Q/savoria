package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies new family, home, purchase, and notification flows do not require meal slots. */
class FamilyHomeExpectedTimeContractTest {
  @Test void newFlowsUseExpectedMealTimeWithoutCreatingOrFetchingMealSlots() throws Exception {
    String controller = read("src/main/java/com/familykitchen/family/controller/FamilyController.java");
    String service = read(
        "src/main/java/com/familykitchen/family/service/impl/FamilyApplicationServiceImpl.java");
    String workflow = read(
        "src/main/java/com/familykitchen/family/mapper/FamilyWorkflowMapper.java");
    String approval = read(
        "src/main/java/com/familykitchen/admin/service/impl/AdminFamilyApplicationServiceImpl.java");
    String purchase = read("src/main/resources/mapper/purchase/PurchaseMapper.xml");
    assertThat(controller).doesNotContain("/meal-slots", "mealSlots(");
    assertThat(service).doesNotContain("selectMealSlots", "mealSlots(");
    assertThat(workflow).doesNotContain("insertDefaultMealSlots", "INSERT INTO meal_slots");
    assertThat(approval).doesNotContain("insertDefaultMealSlots");
    assertThat(purchase).contains("o.expected_meal_time", "date(o.expected_meal_time)");
    assertThat(purchase).doesNotContain("order by o.family_id asc, o.meal_slot_id asc");
  }

  private static String read(String relative) throws Exception {
    return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
  }
}
