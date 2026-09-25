package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishIngredientEntity;
import com.familykitchen.dish.model.entity.IngredientDictionaryEntity;
import com.familykitchen.dish.model.vo.DishTemplateChangeSubmitView;
import com.familykitchen.dish.service.DishTemplateSnapshotValidator;
import com.familykitchen.dish.service.DishTemplateProcurementReadinessEvaluator;
import com.familykitchen.dish.service.impl.DishTemplateChangeRequestServiceImpl;
import com.familykitchen.file.mapper.FileAssetMapper;
import com.familykitchen.file.model.entity.FileAssetDO;
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
  @Test void approvalAppliesNewFieldsButLegacySnapshotPreservesCurrentNourishment() throws Exception {
    for (boolean legacy : new boolean[]{true, false}) {
      org.mockito.Mockito.reset(requestMapper, templateMapper, notificationMapper);
      DishTemplateChangeRequestDO application = pendingApplication();
      if (!legacy) application.setSnapshotJson(targetSnapshot().put("productType", "NORMAL")
          .put("nourishmentDescription", "  新介绍  ").put("servingAdvice", " ").toString());
      String originalBase = application.getBaseSnapshotJson();
      DishTemplateEntity current = template(5L, 3L, "旧汤", 4L);
      current.setProductType("NOURISHMENT"); current.setNourishmentDescription("原介绍"); current.setServingAdvice("原建议");
      when(requestMapper.selectForUpdate(88L)).thenReturn(application);
      when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(current);
      var picturedStep = new com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity();
      picturedStep.setItemKey(targetSnapshot().at("/cookingSteps/0/itemId").asText());
      picturedStep.setImageUrls(List.of("/uploads/images/template.jpg"));
      when(templateMapper.selectTemplateCookingSteps(5L)).thenReturn(List.of(picturedStep));
      when(templateMapper.selectCategoryForUpdate(2L)).thenReturn(category(2L, true));
      when(templateMapper.updateAdminTemplate(any(), any())).thenReturn(1);
      when(requestMapper.markApproved(88L, 1L, null)).thenReturn(1);
      when(notificationMapper.insertNotificationEntity(any())).thenAnswer(call -> {
        call.<NotificationDO>getArgument(0).setId(501L); return 1;
      });
      service.approve(platformAdmin(), 88L, new DishTemplateApproveRequest(null));
      assertEquals(legacy ? "NOURISHMENT" : "NORMAL", current.getProductType());
      assertEquals(legacy ? "原介绍" : "新介绍", current.getNourishmentDescription());
      assertEquals(legacy ? "原建议" : null, current.getServingAdvice());
      assertEquals(originalBase, application.getBaseSnapshotJson());
      var savedStep = ArgumentCaptor.forClass(com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity.class);
      verify(templateMapper).insertTemplateCookingStep(savedStep.capture());
      assertEquals(picturedStep.getImageUrls(), savedStep.getValue().getImageUrls());
    }
  }

  @Test void componentSubmissionCannotAddNourishment() {
    DishTemplateEntity component = template(5L, 3L, "配料", 4L); component.setTemplateType("COMPONENT");
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(component);
    assertThrows(BusinessException.class, () -> service.submit(merchantAdmin, 5L,
        new DishTemplateChangeSubmitRequest(null, targetSnapshot().put("productType", "NOURISHMENT"))));
    verify(requestMapper, never()).insert(any());
  }
  @Test void newLegacySubmissionFillsNourishmentFromPersistedBaseline() throws Exception {
    DishTemplateEntity source = template(5L, 3L, "旧汤", 4L);
    source.setProductType("NOURISHMENT"); source.setNourishmentDescription("原介绍"); source.setServingAdvice("原建议");
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(source);
    when(templateMapper.selectCategoryForUpdate(2L)).thenReturn(category(2L, true));
    when(templateMapper.selectTemplateIngredients(5L)).thenReturn(List.of(ingredient("豆角")));
    service.submit(merchantAdmin, 5L, new DishTemplateChangeSubmitRequest(null, targetSnapshot().put("servingAdvice", "   ")));
    var saved = ArgumentCaptor.forClass(DishTemplateChangeRequestDO.class);
    verify(requestMapper).insert(saved.capture());
    var target = objectMapper.readTree(saved.getValue().getSnapshotJson());
    var base = objectMapper.readTree(saved.getValue().getBaseSnapshotJson());
    assertEquals("NOURISHMENT", target.path("productType").asText());
    assertEquals("原介绍", target.path("nourishmentDescription").asText());
    assertEquals("", target.path("servingAdvice").asText());
    assertEquals("原建议", base.path("servingAdvice").asText());
  }

  @Mock private DishTemplateChangeRequestMapper requestMapper;
  @Mock private DishTemplateMapper templateMapper;
  @Mock private DishMapper dishMapper;
  @Mock private FileAssetMapper fileAssetMapper;
  @Mock private NotificationPersistenceMapper notificationMapper;
  @Mock private DishTemplateProcurementReadinessEvaluator procurementEvaluator;

  private ObjectMapper objectMapper;
  private DishTemplateChangeRequestServiceImpl service;
  private CurrentUserContext merchantAdmin;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new DishTemplateChangeRequestServiceImpl(requestMapper, templateMapper, dishMapper, fileAssetMapper,
        notificationMapper,
        objectMapper, new DishTemplateSnapshotValidator(objectMapper), procurementEvaluator);
    lenient().when(procurementEvaluator.evaluate(any(), any(), any())).thenReturn(
        new DishTemplateProcurementReadinessEvaluator.EvaluationResult(true, List.of(), List.of()));
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
  void submitRejectsImageAssetOwnedByAnotherUser() {
    ObjectNode snapshot = targetSnapshot();
    snapshot.put("imageUrl", "/uploads/images/new.jpg");
    snapshot.put("imageAssetId", 31L);
    snapshot.put("imageRightsConfirmed", true);
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(template(5L, 3L, "原菜名", 0L));
    when(fileAssetMapper.selectOwnedImage(31L, 7L)).thenReturn(null);

    BusinessException error = assertThrows(BusinessException.class, () -> service.submit(merchantAdmin, 5L,
        new DishTemplateChangeSubmitRequest(null, snapshot)));

    assertEquals("所选图片不存在或不属于当前账号，请重新上传", error.getMessage());
    verify(requestMapper, never()).insert(any());
  }

  @Test
  void submitStoresOwnedUploadedImageInTargetSnapshot() throws Exception {
    ObjectNode snapshot = targetSnapshot();
    snapshot.put("imageUrl", "/uploads/images/client-value.jpg");
    snapshot.put("imageAssetId", 31L);
    snapshot.put("imageRightsConfirmed", true);
    DishTemplateEntity template = template(5L, 3L, "原菜名", 0L);
    template.setImageUrl("/uploads/images/old.jpg");
    FileAssetDO asset = new FileAssetDO();
    asset.setId(31L); asset.setUrl("/uploads/images/server-value.jpg");
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(template);
    when(fileAssetMapper.selectOwnedImage(31L, 7L)).thenReturn(asset);
    when(templateMapper.selectCategoryForUpdate(2L)).thenReturn(category(2L, true));
    when(templateMapper.selectTemplateIngredients(5L)).thenReturn(List.of(ingredient("豆角")));
    when(requestMapper.insert(any())).thenAnswer(invocation -> {
      DishTemplateChangeRequestDO value = invocation.getArgument(0); value.setId(89L); return 1;
    });

    service.submit(merchantAdmin, 5L, new DishTemplateChangeSubmitRequest(null, snapshot));

    ArgumentCaptor<DishTemplateChangeRequestDO> stored = ArgumentCaptor.forClass(DishTemplateChangeRequestDO.class);
    verify(requestMapper).insert(stored.capture());
    assertEquals("/uploads/images/server-value.jpg",
        objectMapper.readTree(stored.getValue().getSnapshotJson()).get("imageUrl").asText());
  }

  @Test
  void submitRejectsUnknownComponentBeforeCreatingReviewRequest() {
    ObjectNode snapshot = targetSnapshot();
    ((ObjectNode) snapshot.withArray("ingredients").get(0)).put("componentTemplateId", 999L)
        .put("componentMultiplier", "1.00");
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(template(5L, 3L, "原菜名", 4L));
    when(templateMapper.selectCategoryForUpdate(2L)).thenReturn(category(2L, true));
    when(templateMapper.selectTemplateIngredients(5L)).thenReturn(List.of(ingredient("豆角")));
    when(templateMapper.selectTemplateCookingSteps(5L)).thenReturn(List.of());
    when(templateMapper.selectTemplateForUpdate(999L)).thenReturn(null);

    BusinessException error = assertThrows(BusinessException.class, () -> service.submit(merchantAdmin, 5L,
        new DishTemplateChangeSubmitRequest(null, snapshot)));

    assertEquals("审核快照引用的配料组件不存在：999", error.getMessage());
    verify(requestMapper, never()).insert(any());
  }

  @Test
  void submitFromImportedDishUsesEditableMerchantFieldsAndOmitsTemplateOwnedFields() throws Exception {
    DishEntity dish = importedDish(8L, 5L);
    dish.setProductType("NOURISHMENT"); dish.setPrecautions("注意食材过敏");
    when(dishMapper.selectDish(21L, 8L)).thenReturn(dish);
    when(dishMapper.selectDishIngredients(8L)).thenReturn(List.of(
        dishIngredient("豆角", "120.00"), dishIngredient("面条", "300.00"),
        dishIngredient("蒜", "10.00")));
    when(dishMapper.selectCookingSteps(8L)).thenReturn(List.of(dishStep(1, "焖制", "加水焖熟")));
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
    assertEquals(2, snapshot.get("schemaVersion").asInt());
    assertEquals("商户豆角焖面", snapshot.get("name").asText());
    assertEquals("按家庭反馈调整", snapshot.get("description").asText());
    assertEquals("NOURISHMENT", snapshot.path("productType").asText());
    assertEquals("注意食材过敏", snapshot.path("precautions").asText());
    assertEquals("/images/dish-templates/original.jpg", snapshot.get("imageUrl").asText());
    assertEquals(0, new BigDecimal("22.00").compareTo(snapshot.get("referencePrice").decimalValue()));
    assertEquals(3L, snapshot.get("categoryId").asLong());
    assertTrue(!snapshot.has("imageSourceUrl"));
    assertTrue(!snapshot.has("imageAuthor"));
    assertTrue(!snapshot.has("imageLicense"));
    assertEquals("家常", snapshot.get("tasteTags").get(0).asText());
    assertEquals("DINNER", snapshot.get("mealTags").get(0).asText());
    assertEquals("时令蔬菜", snapshot.get("ingredients").get(0).get("ingredientCategory").asText());
    assertEquals("米面粮油", snapshot.get("ingredients").get(1).get("ingredientCategory").asText());
    assertEquals("其他", snapshot.get("ingredients").get(2).get("ingredientCategory").asText());
    assertEquals("焖制", snapshot.get("cookingSteps").get(0).get("title").asText());
    assertEquals("加水焖熟", snapshot.get("cookingSteps").get(0).get("content").asText());
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
  void approveLocksInOrderUpdatesEditableFieldsAndCreatesMerchantNotification() throws Exception {
    CurrentUserContext platformAdmin = platformAdmin();
    DishTemplateChangeRequestDO application = pendingApplication();
    when(requestMapper.selectForUpdate(88L)).thenReturn(application);
    DishTemplateEntity current = template(5L, 3L, "原菜名", 4L);
    current.setImageUrl("/images/reviewed.jpg");
    current.setSourceUrl("https://cooklikehoc.soilzhu.su/炒菜/豆角焖面");
    when(templateMapper.selectTemplateForUpdate(5L)).thenReturn(current);
    when(templateMapper.selectCategoryForUpdate(2L)).thenReturn(category(2L, true));
    when(templateMapper.updateAdminTemplate(any(), org.mockito.ArgumentMatchers.eq(4L))).thenReturn(1);
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
    ArgumentCaptor<DishTemplateEntity> updatedTemplate = ArgumentCaptor.forClass(DishTemplateEntity.class);
    verify(templateMapper).updateAdminTemplate(updatedTemplate.capture(), org.mockito.ArgumentMatchers.eq(4L));
    assertEquals("豆角焖面", updatedTemplate.getValue().getName());
    assertEquals("/images/reviewed.jpg", updatedTemplate.getValue().getImageUrl());
    assertEquals("https://cooklikehoc.soilzhu.su/炒菜/豆角焖面", updatedTemplate.getValue().getSourceUrl());
    verify(templateMapper).deleteTemplateIngredients(5L);
    verify(templateMapper).insertTemplateIngredient(any());
    verify(templateMapper).deleteTemplateCookingSteps(5L);
    verify(templateMapper).insertTemplateCookingStep(any());
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
    node.put("schemaVersion", 2);
    node.put("categoryId", 2L);
    node.put("name", "豆角焖面");
    node.put("description", "北方家常焖面");
    node.put("referencePrice", "18.00");
    node.putArray("tasteTags").add("家常");
    node.putArray("mealTags").add("LUNCH").add("DINNER");
    node.put("sortOrder", 10);
    node.put("enabled", true);
    ObjectNode ingredient = objectMapper.createObjectNode();
    ingredient.put("itemId", "line-1");
    ingredient.put("ingredientName", "豆角");
    ingredient.put("ingredientCategory", "蔬菜");
    ingredient.put("quantityStatus", "VERIFIED");
    ingredient.put("quantity", "100.00");
    ingredient.put("unit", "克");
    ingredient.put("calcType", "FIXED");
    ingredient.put("sortOrder", 1);
    node.putArray("ingredients").add(ingredient);
    ObjectNode step = objectMapper.createObjectNode();
    step.put("itemId", "step-1"); step.put("stepNo", 1); step.put("title", "焖制");
    step.put("content", "加入豆角焖熟");
    node.putArray("cookingSteps").add(step);
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
    entity.setTemplateType("DISH");
    entity.setDataStatus("READY");
    entity.setProcurementReady(true);
    entity.setImageRightsStatus("DECLARED");
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
    entity.setQuantityStatus("VERIFIED");
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

  private static DishCookingStepEntity dishStep(int stepNo, String title, String content) {
    DishCookingStepEntity entity = new DishCookingStepEntity();
    entity.setId((long) stepNo);
    entity.setStepNo(stepNo);
    entity.setTitle(title);
    entity.setContent(content);
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
