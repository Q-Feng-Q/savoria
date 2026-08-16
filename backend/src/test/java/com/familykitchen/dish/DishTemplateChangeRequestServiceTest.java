package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.mapper.DishTemplateChangeRequestMapper;
import com.familykitchen.dish.mapper.DishTemplateMapper;
import com.familykitchen.dish.model.dto.ImportedDishTemplateSyncRequest;
import com.familykitchen.dish.model.dto.DishTemplateChangeSubmitRequest;
import com.familykitchen.dish.model.dto.DishTemplateApproveRequest;
import com.familykitchen.dish.model.dto.DishTemplateRejectRequest;
import com.familykitchen.dish.model.entity.DishTemplateCategoryEntity;
import com.familykitchen.dish.model.entity.DishTemplateChangeRequestDO;
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishIngredientEntity;
import com.familykitchen.dish.model.entity.IngredientDictionaryEntity;
import com.familykitchen.dish.model.vo.DishTemplateChangeSubmitView;
import com.familykitchen.dish.service.DishTemplateSnapshotValidator;
import com.familykitchen.dish.service.impl.DishTemplateChangeRequestServiceImpl;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.notification.model.entity.NotificationDO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

/** 验证模板菜品修改申请的权限、锁顺序、租户隔离和状态流转。 */
@ExtendWith(MockitoExtension.class)
class DishTemplateChangeRequestServiceTest {

  @Mock private DishTemplateChangeRequestMapper requestMapper;
  @Mock private DishTemplateMapper templateMapper;
  @Mock private DishMapper dishMapper;
  @Mock private NotificationPersistenceMapper notificationMapper;

  private ObjectMapper objectMapper;
  private DishTemplateChangeRequestServiceImpl service;
  private CurrentUserContext merchantAdmin;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new DishTemplateChangeRequestServiceImpl(requestMapper, templateMapper, dishMapper, notificationMapper,
        objectMapper, new DishTemplateSnapshotValidator(objectMapper));
    merchantAdmin = new CurrentUserContext(7L, 21L, null, 7L, "user",
        Set.of("MERCHANT_ADMIN"), Set.of("MERCHANT_ADMIN"), "session");
  }

  @Test
  void submitLocksTemplateAndCategoryAndCapturesBothSnapshots() {
    DishTemplateEntity template = template(5L, 3L, "原豆角焖面", 4L);
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(template);
    when(templateMapper.selectCategoryForUpdate(2L)).thenReturn(category(2L, true));
    when(templateMapper.selectTemplateIngredients(5L)).thenReturn(List.of(ingredient("豆角")));
    when(requestMapper.countPending(21L, 5L)).thenReturn(0);
    when(requestMapper.insert(any())).thenAnswer(invocation -> {
      DishTemplateChangeRequestDO entity = invocation.getArgument(0);
      entity.setId(88L);
      entity.setSubmittedAt(LocalDateTime.of(2026, 8, 15, 20, 0));
      return 1;
    });

    DishTemplateChangeSubmitView result = service.submit(merchantAdmin, 5L,
        new DishTemplateChangeSubmitRequest("调整为家庭版本", targetSnapshot()));

    assertEquals(88L, result.requestId());
    assertEquals("PENDING", result.status());
    verify(templateMapper).selectTemplateForUpdate(5L);
    verify(templateMapper).selectCategoryForUpdate(2L);
    verify(requestMapper).insert(any(DishTemplateChangeRequestDO.class));
  }

  @Test
  void submitRejectsDuplicatePendingRequestWithConflict() {
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(template(5L, 3L, "原菜名", 0L));
    when(templateMapper.selectCategoryForUpdate(2L)).thenReturn(category(2L, true));
    when(templateMapper.selectTemplateIngredients(5L)).thenReturn(List.of(ingredient("豆角")));
    when(requestMapper.countPending(21L, 5L)).thenReturn(1);

    BusinessException error = assertThrows(BusinessException.class, () -> service.submit(merchantAdmin, 5L,
        new DishTemplateChangeSubmitRequest(null, targetSnapshot())));

    assertEquals(ErrorCode.STATE_CONFLICT, error.errorCode());
    assertEquals("本商户对该模板已有待审核修改申请", error.getMessage());
    verify(requestMapper, never()).insert(any());
  }

  @Test
  void submitFromImportedDishUsesMerchantFieldsAndPreservesTemplateOwnedFields() throws Exception {
    DishEntity dish = importedDish(8L, 5L);
    when(dishMapper.selectDish(21L, 8L)).thenReturn(dish);
    when(dishMapper.selectDishIngredients(8L)).thenReturn(List.of(
        dishIngredient("豆角", "120.00"), dishIngredient("面条", "300.00"),
        dishIngredient("蒜", "10.00")));
    when(dishMapper.selectIngredientDictionary(21L)).thenReturn(List.of(dictionary("豆角", "时令蔬菜")));
    DishTemplateEntity template = template(5L, 3L, "原豆角焖面", 4L);
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(template);
    when(templateMapper.selectCategoryForUpdate(3L)).thenReturn(category(3L, true));
    when(templateMapper.selectTemplateIngredients(5L)).thenReturn(List.of(
        templateIngredient("面条", "米面粮油"), templateIngredient("豆角", "蔬菜")));
    when(requestMapper.countPending(21L, 5L)).thenReturn(0);
    when(requestMapper.insert(any())).thenAnswer(invocation -> {
      DishTemplateChangeRequestDO entity = invocation.getArgument(0);
      entity.setId(89L);
      entity.setSubmittedAt(LocalDateTime.of(2026, 8, 15, 21, 0));
      return 1;
    });

    DishTemplateChangeSubmitView result = service.submitFromImportedDish(merchantAdmin, 8L,
        new ImportedDishTemplateSyncRequest("采用商户实测用量"));

    assertEquals(89L, result.requestId());
    ArgumentCaptor<DishTemplateChangeRequestDO> request =
        ArgumentCaptor.forClass(DishTemplateChangeRequestDO.class);
    verify(requestMapper).insert(request.capture());
    var snapshot = objectMapper.readTree(request.getValue().getSnapshotJson());
    assertEquals("商户豆角焖面", snapshot.get("name").asText());
    assertEquals("按家庭反馈调整", snapshot.get("description").asText());
    assertEquals("/uploads/images/merchant-dish.jpg", snapshot.get("imageUrl").asText());
    assertEquals(0, new BigDecimal("22.00").compareTo(snapshot.get("referencePrice").decimalValue()));
    assertEquals(3L, snapshot.get("categoryId").asLong());
    assertEquals("https://example.com/original", snapshot.get("imageSourceUrl").asText());
    assertEquals("原作者", snapshot.get("imageAuthor").asText());
    assertEquals("原授权", snapshot.get("imageLicense").asText());
    assertEquals("家常", snapshot.get("tasteTags").get(0).asText());
    assertEquals("DINNER", snapshot.get("mealTags").get(0).asText());
    assertEquals("时令蔬菜", snapshot.get("ingredients").get(0).get("ingredientCategory").asText());
    assertEquals("米面粮油", snapshot.get("ingredients").get(1).get("ingredientCategory").asText());
    assertEquals("其他", snapshot.get("ingredients").get(2).get("ingredientCategory").asText());
    assertEquals("采用商户实测用量", request.getValue().getSubmitNote());
  }

  @Test
  void submitFromImportedDishRejectsManualDishBeforeReadingTemplate() {
    DishEntity dish = importedDish(8L, null);
    when(dishMapper.selectDish(21L, 8L)).thenReturn(dish);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.submitFromImportedDish(merchantAdmin, 8L,
            new ImportedDishTemplateSyncRequest(null)));

    assertEquals(ErrorCode.BAD_REQUEST, error.errorCode());
    assertEquals("该菜品不是从平台模板导入，不能申请同步", error.getMessage());
    verify(templateMapper, never()).selectTemplateForUpdate(any());
  }

  @Test
  void submitFromImportedDishRejectsMissingOwnedDishWithClearMessage() {
    when(dishMapper.selectDish(21L, 404L)).thenReturn(null);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.submitFromImportedDish(merchantAdmin, 404L,
            new ImportedDishTemplateSyncRequest(null)));

    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    assertEquals("菜品不存在或无权操作", error.getMessage());
    verify(templateMapper, never()).selectTemplateForUpdate(any());
  }

  @Test
  void submitFromImportedDishRejectsEmptyIngredientsBeforeReadingTemplate() {
    when(dishMapper.selectDish(21L, 8L)).thenReturn(importedDish(8L, 5L));
    when(dishMapper.selectDishIngredients(8L)).thenReturn(List.of());

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.submitFromImportedDish(merchantAdmin, 8L,
            new ImportedDishTemplateSyncRequest(null)));

    assertEquals(ErrorCode.BUSINESS_INVALID, error.errorCode());
    assertEquals("当前菜品至少需要1项食材后才能申请同步", error.getMessage());
    verify(templateMapper, never()).selectTemplateForUpdate(any());
  }

  @Test
  void submitFromImportedDishRejectsUnavailableSourceTemplate() {
    when(dishMapper.selectDish(21L, 8L)).thenReturn(importedDish(8L, 5L));
    when(dishMapper.selectDishIngredients(8L)).thenReturn(List.of(dishIngredient("豆角", "120.00")));
    when(dishMapper.selectIngredientDictionary(21L)).thenReturn(List.of());
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(null);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.submitFromImportedDish(merchantAdmin, 8L,
            new ImportedDishTemplateSyncRequest(null)));

    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    assertEquals("来源模板菜品不存在或已停用", error.getMessage());
    verify(requestMapper, never()).insert(any());
  }

  @Test
  void withdrawUsesMerchantScopedLockAndOnlyAcceptsPending() {
    DishTemplateChangeRequestDO request = new DishTemplateChangeRequestDO();
    request.setId(88L);
    request.setMerchantId(21L);
    request.setStatus("PENDING");
    when(requestMapper.selectMerchantForUpdate(88L, 21L)).thenReturn(request);
    when(requestMapper.markWithdrawn(88L, 7L)).thenReturn(1);

    service.withdraw(merchantAdmin, 88L);

    verify(requestMapper).selectMerchantForUpdate(88L, 21L);
    verify(requestMapper).markWithdrawn(88L, 7L);
  }

  @Test
  void merchantWithoutBackendAccessCannotSubmit() {
    CurrentUserContext member = new CurrentUserContext(9L, 21L, null, 9L, "user",
        Set.of(), Set.of(), "session");

    BusinessException error = assertThrows(BusinessException.class, () -> service.submit(member, 5L,
        new DishTemplateChangeSubmitRequest(null, targetSnapshot())));

    assertEquals(ErrorCode.FORBIDDEN, error.errorCode());
    verify(templateMapper, never()).selectTemplateForUpdate(any());
  }

  @Test
  void approveLocksInOrderReplacesTemplateAndCreatesMerchantNotification() throws Exception {
    CurrentUserContext platformAdmin = platformAdmin();
    DishTemplateChangeRequestDO application = pendingApplication();
    when(requestMapper.selectForUpdate(88L)).thenReturn(application);
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(template(5L, 3L, "原菜名", 4L));
    when(templateMapper.selectCategoryForUpdate(2L)).thenReturn(category(2L, true));
    when(templateMapper.replaceTemplate(any(), org.mockito.ArgumentMatchers.eq(4L))).thenReturn(1);
    when(requestMapper.markApproved(88L, 1L, "资料完整")).thenReturn(1);
    when(notificationMapper.insertNotificationEntity(any())).thenAnswer(invocation -> {
      NotificationDO notification = invocation.getArgument(0);
      notification.setId(501L);
      return 1;
    });
    when(requestMapper.setResultNotificationId(88L, 501L)).thenReturn(1);

    service.approve(platformAdmin, 88L, new DishTemplateApproveRequest("资料完整"));

    InOrder locks = inOrder(requestMapper, templateMapper);
    locks.verify(requestMapper).selectForUpdate(88L);
    locks.verify(templateMapper).selectTemplateForUpdate(5L);
    locks.verify(templateMapper).selectCategoryForUpdate(2L);
    verify(templateMapper).deleteTemplateIngredients(5L);
    verify(templateMapper).insertTemplateIngredient(any());
    ArgumentCaptor<NotificationDO> notification = ArgumentCaptor.forClass(NotificationDO.class);
    verify(notificationMapper).insertNotificationEntity(notification.capture());
    assertEquals("merchant", notification.getValue().getReceiverType());
    assertEquals(21L, notification.getValue().getReceiverId());
    assertTrue(notification.getValue().getContent().contains("申请ID：88"));
    assertTrue(notification.getValue().getContent().contains("豆角焖面"));
    assertTrue(notification.getValue().getContent().contains("已通过"));
    assertTrue(notification.getValue().getContent().contains("资料完整"));
    assertTrue(notification.getValue().getContent().contains("详情入口"));
  }

  @Test
  void staleApprovalKeepsPendingAndCreatesNoNotification() throws Exception {
    DishTemplateChangeRequestDO application = pendingApplication();
    when(requestMapper.selectForUpdate(88L)).thenReturn(application);
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(template(5L, 3L, "已变化", 5L));

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.approve(platformAdmin(), 88L, new DishTemplateApproveRequest(null)));

    assertEquals("模板菜品已发生变化，请重新提交", error.getMessage());
    verify(requestMapper, never()).markApproved(any(), any(), any());
    verify(notificationMapper, never()).insertNotificationEntity(any());
  }

  @Test
  void rejectLocksOnlyApplicationAndCreatesOneResultNotification() throws Exception {
    DishTemplateChangeRequestDO application = pendingApplication();
    application.setTemplateName("豆角焖面");
    when(requestMapper.selectForUpdate(88L)).thenReturn(application);
    when(requestMapper.markRejected(88L, 1L, "来源信息不完整")).thenReturn(1);
    when(notificationMapper.insertNotificationEntity(any())).thenAnswer(invocation -> {
      NotificationDO notification = invocation.getArgument(0);
      notification.setId(502L);
      return 1;
    });

    service.reject(platformAdmin(), 88L, new DishTemplateRejectRequest("来源信息不完整"));

    verify(templateMapper, never()).selectTemplateForUpdate(any());
    verify(templateMapper, never()).selectCategoryForUpdate(any());
    verify(requestMapper).setResultNotificationId(88L, 502L);
    verify(notificationMapper).insertNotificationEntity(any());
  }

  private ObjectNode targetSnapshot() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("schemaVersion", 1);
    node.put("categoryId", 2L);
    node.put("name", "豆角焖面");
    node.put("description", "北方家常焖面");
    node.put("imageUrl", "/images/dish-templates/dou-jiao-men-mian.jpg");
    node.put("imageSourceUrl", "https://example.com/source");
    node.put("imageAuthor", "家庭厨房");
    node.put("imageLicense", "授权使用");
    node.put("referencePrice", "18.00");
    node.putArray("tasteTags").add("家常");
    node.putArray("mealTags").add("LUNCH").add("DINNER");
    node.put("sortOrder", 10);
    node.put("enabled", true);
    ObjectNode ingredient = objectMapper.createObjectNode();
    ingredient.put("ingredientName", "豆角");
    ingredient.put("ingredientCategory", "蔬菜");
    ingredient.put("quantity", "100.00");
    ingredient.put("unit", "克");
    ingredient.put("calcType", "FIXED");
    ingredient.put("sortOrder", 1);
    node.putArray("ingredients").add(ingredient);
    return node;
  }

  private DishTemplateChangeRequestDO pendingApplication() throws Exception {
    DishTemplateChangeRequestDO application = new DishTemplateChangeRequestDO();
    application.setId(88L);
    application.setMerchantId(21L);
    application.setTemplateId(5L);
    application.setBaseTemplateVersion(4L);
    application.setSnapshotJson(objectMapper.writeValueAsString(targetSnapshot()));
    application.setStatus("PENDING");
    return application;
  }

  private static CurrentUserContext platformAdmin() {
    return new CurrentUserContext(1L, null, null, 1L, "user", Set.of("PLATFORM_ADMIN"), Set.of(), "admin");
  }

  private static DishTemplateEntity template(Long id, Long categoryId, String name, Long version) {
    DishTemplateEntity entity = new DishTemplateEntity();
    entity.setId(id);
    entity.setTemplateCode("TPL-005");
    entity.setCategoryId(categoryId);
    entity.setName(name);
    entity.setDescription("原简介");
    entity.setImageUrl("/images/dish-templates/original.jpg");
    entity.setImageSourceUrl("https://example.com/original");
    entity.setImageAuthor("原作者");
    entity.setImageLicense("原授权");
    entity.setReferencePrice(new BigDecimal("16.00"));
    entity.setTasteTags("[\"家常\"]");
    entity.setMealTags("[\"DINNER\"]");
    entity.setSortOrder(5);
    entity.setEnabled(true);
    entity.setVersion(version);
    return entity;
  }

  private static DishTemplateCategoryEntity category(Long id, boolean enabled) {
    DishTemplateCategoryEntity entity = new DishTemplateCategoryEntity();
    entity.setId(id);
    entity.setEnabled(enabled);
    return entity;
  }

  private static DishTemplateIngredientEntity ingredient(String name) {
    DishTemplateIngredientEntity entity = new DishTemplateIngredientEntity();
    entity.setIngredientName(name);
    entity.setIngredientCategory("蔬菜");
    entity.setQuantity(new BigDecimal("80.00"));
    entity.setUnit("克");
    entity.setCalcType("FIXED");
    entity.setSortOrder(1);
    return entity;
  }

  private static DishTemplateIngredientEntity templateIngredient(String name, String category) {
    DishTemplateIngredientEntity entity = ingredient(name);
    entity.setIngredientCategory(category);
    return entity;
  }

  private static DishEntity importedDish(Long id, Long sourceTemplateId) {
    DishEntity entity = new DishEntity();
    entity.setId(id);
    entity.setMerchantId(21L);
    entity.setCategoryId(9L);
    entity.setName("商户豆角焖面");
    entity.setDescription("按家庭反馈调整");
    entity.setImageUrl("/uploads/images/merchant-dish.jpg");
    entity.setBasePrice(new BigDecimal("22.00"));
    entity.setSourceTemplateId(sourceTemplateId);
    entity.setStatus("active");
    return entity;
  }

  private static DishIngredientEntity dishIngredient(String name, String quantity) {
    DishIngredientEntity entity = new DishIngredientEntity();
    entity.setIngredientName(name);
    entity.setQuantity(new BigDecimal(quantity));
    entity.setUnit("克");
    entity.setCalcType("FIXED");
    return entity;
  }

  private static IngredientDictionaryEntity dictionary(String name, String category) {
    IngredientDictionaryEntity entity = new IngredientDictionaryEntity();
    entity.setName(name);
    entity.setCategory(category);
    entity.setUnit("克");
    return entity;
  }
}
