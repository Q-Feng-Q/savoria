package com.familykitchen.notification;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * 验证家庭通知EventContract相关业务契约与回归场景。
 */
class FamilyNotificationEventContractTest {
 @Test void familyWorkflowsWriteUserScopedNotifications() throws Exception {
  String member=Files.readString(Path.of("src/main/java/com/familykitchen/family/service/impl/FamilyMemberApplicationServiceImpl.java"));
  String admin=Files.readString(Path.of("src/main/java/com/familykitchen/admin/service/impl/AdminFamilyApplicationServiceImpl.java"));
  assertTrue(member.contains("notificationMapper.insertNotification"));
  assertTrue(admin.contains("notificationMapper.insertNotification"));
  assertTrue(member.contains("家庭邀请"));
  assertTrue(admin.contains("家庭创建申请已通过"));
 }
 @Test void newOrderNotificationIncludesExpectedMealTime() throws Exception {
  String order=Files.readString(Path.of(
      "src/main/java/com/familykitchen/order/service/impl/FamilyOrderApplicationServiceImpl.java"));
  assertTrue(order.contains("预计 "));
  assertTrue(order.contains("用餐"));
  assertTrue(order.contains("cart.getExpectedMealTime().format(MEAL_TIME)"));
 }
}
