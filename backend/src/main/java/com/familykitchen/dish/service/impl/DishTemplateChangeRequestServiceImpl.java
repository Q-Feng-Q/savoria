package com.familykitchen.dish.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishTemplateChangeRequestMapper;
import com.familykitchen.dish.mapper.DishTemplateMapper;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.AdminDishTemplateChangeQuery;
import com.familykitchen.dish.model.dto.DishTemplateApproveRequest;
import com.familykitchen.dish.model.dto.DishTemplateChangeSubmitRequest;
import com.familykitchen.dish.model.dto.DishTemplateCookingStepSnapshotRequest;
import com.familykitchen.dish.model.dto.DishTemplateIngredientSnapshotRequest;
import com.familykitchen.dish.model.dto.DishTemplateRejectRequest;
import com.familykitchen.dish.model.dto.DishTemplateSnapshotRequest;
import com.familykitchen.dish.model.dto.MerchantDishTemplateChangeQuery;
import com.familykitchen.dish.model.dto.ImportedDishTemplateSyncRequest;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishIngredientEntity;
import com.familykitchen.dish.model.entity.DishTemplateCategoryEntity;
import com.familykitchen.dish.model.entity.DishTemplateChangeRequestDO;
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.entity.IngredientDictionaryEntity;
import com.familykitchen.dish.model.vo.DishTemplateChangeDetailView;
import com.familykitchen.dish.model.vo.DishTemplateChangeItemView;
import com.familykitchen.dish.model.vo.DishTemplateChangePageView;
import com.familykitchen.dish.model.vo.DishTemplateChangeSubmitView;
import com.familykitchen.dish.service.DishTemplateChangeRequestService;
import com.familykitchen.dish.service.DishTemplateSnapshotValidator;
import com.familykitchen.dish.service.DishTemplateProcurementReadinessEvaluator;
import com.familykitchen.file.mapper.FileAssetMapper;
import com.familykitchen.file.model.entity.FileAssetDO;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.notification.model.entity.NotificationDO;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模板菜品修改申请服务实现。
 *
 * <p>商户提交时持久化不可变的原快照与目标快照；平台通过时以行锁和模板版本号保证
 * 主信息、食材、审核状态和结果通知原子提交。已导入商户菜品不参与任何更新。</p>
 */
@Service
public class DishTemplateChangeRequestServiceImpl implements DishTemplateChangeRequestService {
  private static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "WITHDRAWN");
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

  private final DishTemplateChangeRequestMapper requestMapper;
  private final DishTemplateMapper templateMapper;
  private final DishMapper dishMapper;
  private final FileAssetMapper fileAssetMapper;
  private final NotificationPersistenceMapper notificationMapper;
  private final ObjectMapper objectMapper;
  private final DishTemplateSnapshotValidator snapshotValidator;
  private final DishTemplateProcurementReadinessEvaluator procurementEvaluator;

  /**
   * 创建模板菜品修改申请服务。
   * @param requestMapper 审核申请持久化 Mapper
   * @param templateMapper 模板菜品与食材 Mapper
   * @param dishMapper 商户菜品、配方与食材字典 Mapper
   * @param fileAssetMapper 上传文件资产 Mapper
   * @param notificationMapper 站内通知 Mapper
   * @param objectMapper JSON 序列化组件
   * @param snapshotValidator 完整快照校验器
   * @param procurementEvaluator 采购就绪状态判定器
   */
  public DishTemplateChangeRequestServiceImpl(DishTemplateChangeRequestMapper requestMapper,
      DishTemplateMapper templateMapper, DishMapper dishMapper, FileAssetMapper fileAssetMapper,
      NotificationPersistenceMapper notificationMapper,
      ObjectMapper objectMapper, DishTemplateSnapshotValidator snapshotValidator,
      DishTemplateProcurementReadinessEvaluator procurementEvaluator) {
    this.requestMapper = requestMapper;
    this.templateMapper = templateMapper;
    this.dishMapper = dishMapper;
    this.fileAssetMapper = fileAssetMapper;
    this.notificationMapper = notificationMapper;
    this.objectMapper = objectMapper;
    this.snapshotValidator = snapshotValidator;
    this.procurementEvaluator = procurementEvaluator;
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public DishTemplateChangeSubmitView submit(CurrentUserContext user, Long templateId,
      DishTemplateChangeSubmitRequest request) {
    requireMerchantAdmin(user);
    if (templateId == null || templateId <= 0) throw badRequest("模板菜品ID必须为正整数");
    if (request == null) throw badRequest("模板菜品修改申请不能为空");
    DishTemplateSnapshotRequest target = snapshotValidator.parseAndValidate(request.targetSnapshot());
    String submitNote = request.normalizedSubmitNote();
    if (submitNote != null && submitNote.length() > 500) throw badRequest("提交说明最多500个字符");

    DishTemplateEntity template = templateMapper.selectTemplateForUpdate(templateId);
    if (template == null || !Boolean.TRUE.equals(template.getEnabled())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模板菜品不存在或已停用");
    }
    target = resolveSubmittedImage(user, template, target);
    List<DishTemplateIngredientEntity> ingredients = templateMapper.selectTemplateIngredients(templateId);
    List<DishTemplateCookingStepEntity> steps = templateMapper.selectTemplateCookingSteps(templateId);
    return persistSubmission(user, template, target, submitNote, ingredients, steps);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public DishTemplateChangeSubmitView submitFromImportedDish(CurrentUserContext user, Long dishId,
      ImportedDishTemplateSyncRequest request) {
    requireMerchantAdmin(user);
    if (dishId == null || dishId <= 0) throw badRequest("菜品ID必须为正整数");
    if (request == null) throw badRequest("同步模板申请不能为空");
    String submitNote = request.normalizedSubmitNote();
    if (submitNote != null && submitNote.length() > 500) throw badRequest("提交说明最多500个字符");

    DishEntity dish = dishMapper.selectDish(user.merchantId(), dishId);
    if (dish == null) throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在或无权操作");
    if ("deleted".equalsIgnoreCase(dish.getStatus())) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "菜品已删除");
    }
    if (dish.getSourceTemplateId() == null) {
      throw badRequest("该菜品不是从平台模板导入，不能申请同步");
    }
    List<DishIngredientEntity> dishIngredients = dishMapper.selectDishIngredients(dishId);
    if (dishIngredients == null || dishIngredients.isEmpty()) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "当前菜品至少需要1项食材后才能申请同步");
    }
    List<IngredientDictionaryEntity> dictionary = dishMapper.selectIngredientDictionary(user.merchantId());
    List<DishCookingStepEntity> dishSteps = dishMapper.selectCookingSteps(dishId);

    DishTemplateEntity template = templateMapper.selectTemplateForUpdate(dish.getSourceTemplateId());
    if (template == null || !Boolean.TRUE.equals(template.getEnabled())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "来源模板菜品不存在或已停用");
    }
    List<DishTemplateIngredientEntity> templateIngredients =
        templateMapper.selectTemplateIngredients(dish.getSourceTemplateId());
    List<DishTemplateCookingStepEntity> templateSteps =
        templateMapper.selectTemplateCookingSteps(dish.getSourceTemplateId());
    DishTemplateSnapshotRequest target = snapshotValidator.parseAndValidate(objectMapper.valueToTree(
        toImportedDishSnapshot(dish, template, dishIngredients, dishSteps, dictionary, templateIngredients)));
    return persistSubmission(user, template, target, submitNote, templateIngredients, templateSteps);
  }

  private DishTemplateChangeSubmitView persistSubmission(CurrentUserContext user, DishTemplateEntity template,
      DishTemplateSnapshotRequest target, String submitNote,
      List<DishTemplateIngredientEntity> templateIngredients,
      List<DishTemplateCookingStepEntity> templateSteps) {
    Long templateId = template.getId();
    target = fillNourishmentForSubmission(template, target);
    target = resolveStepImages(templateId, target, templateSteps);
    requireEnabledCategory(target.categoryId());
    List<DishTemplateIngredientEntity> targetIngredients = target.ingredients().stream()
        .map(item -> toIngredient(templateId, item)).toList();
    List<DishTemplateCookingStepEntity> targetSteps = target.cookingSteps().stream()
        .map(item -> toStep(templateId, item)).toList();
    loadGraph(template, targetIngredients, targetSteps);
    DishTemplateSnapshotRequest base = toBaseSnapshot(template, templateIngredients, templateSteps);
    if (requestMapper.countPending(user.merchantId(), templateId) > 0) {
      throw conflict("本商户对该模板已有待审核修改申请");
    }

    DishTemplateChangeRequestDO entity = new DishTemplateChangeRequestDO();
    entity.setMerchantId(user.merchantId());
    entity.setTemplateId(templateId);
    entity.setBaseTemplateVersion(template.getVersion() == null ? 0L : template.getVersion());
    entity.setBaseSnapshotJson(writeSnapshot(base));
    entity.setSnapshotJson(writeSnapshot(target));
    entity.setSubmitNote(submitNote);
    entity.setStatus("PENDING");
    entity.setSubmittedBy(user.userId());
    try {
      requestMapper.insert(entity);
    } catch (DuplicateKeyException exception) {
      throw conflict("本商户对该模板已有待审核修改申请");
    }
    return new DishTemplateChangeSubmitView(entity.getId(), "PENDING", entity.getSubmittedAt());
  }

  private DishTemplateSnapshotRequest toImportedDishSnapshot(DishEntity dish, DishTemplateEntity template,
      List<DishIngredientEntity> dishIngredients, List<DishCookingStepEntity> dishSteps,
      List<IngredientDictionaryEntity> dictionary,
      List<DishTemplateIngredientEntity> templateIngredients) {
    Map<String, String> dictionaryCategories = new LinkedHashMap<>();
    if (dictionary != null) {
      for (IngredientDictionaryEntity item : dictionary) {
        putCategory(dictionaryCategories, item.getName(), item.getCategory());
      }
    }
    Map<String, String> templateCategories = new LinkedHashMap<>();
    if (templateIngredients != null) {
      for (DishTemplateIngredientEntity item : templateIngredients) {
        putCategory(templateCategories, item.getIngredientName(), item.getIngredientCategory());
      }
    }
    List<DishTemplateIngredientSnapshotRequest> ingredients = new java.util.ArrayList<>();
    for (int index = 0; index < dishIngredients.size(); index++) {
      DishIngredientEntity item = dishIngredients.get(index);
      String key = ingredientKey(item.getIngredientName());
      String category = dictionaryCategories.get(key);
      if (category == null) category = templateCategories.get(key);
      if (category == null) category = "其他";
      boolean noPurchase = "NO_PURCHASE".equals(item.getCalcType());
      ingredients.add(new DishTemplateIngredientSnapshotRequest("dish:" + dish.getId() + ":ingredient:" + index,
          item.getIngredientName(), category, noPurchase ? "NOT_APPLICABLE" : "VERIFIED",
          noPurchase ? null : item.getQuantity(), noPurchase ? null : item.getUnit(),
          noPurchase ? null : item.getCalcType(), null, null, null, null, index + 1));
    }
    List<DishTemplateCookingStepSnapshotRequest> steps = new ArrayList<>();
    if (dishSteps != null) {
      for (int index = 0; index < dishSteps.size(); index++) {
        DishCookingStepEntity item = dishSteps.get(index);
        String itemId = item.getSourceTemplateStepId() == null
            ? "dish:" + dish.getId() + ":step:" + index : "template-step:" + item.getSourceTemplateStepId();
        steps.add(new DishTemplateCookingStepSnapshotRequest(itemId, index + 1, item.getTitle(),
            item.getContent(), item.getDurationSeconds(), item.getTemperatureText(), item.getHeatLevel(),
            item.getComponentTemplateId(), item.getImageUrls()));
      }
    }
    return new DishTemplateSnapshotRequest(2, template.getCategoryId(), dish.getName(), dish.getDescription(),
        template.getImageUrl(), null, false, false, dish.getBasePrice(),
        readTags(template.getTasteTags()), readTags(template.getMealTags()),
        template.getSortOrder(), template.getEnabled(), List.copyOf(ingredients), List.copyOf(steps),
        dish.getProductType(), snapshotText(dish.getNourishmentDescription()), snapshotText(dish.getServingAdvice()), snapshotText(dish.getPrecautions()));
  }

  private static void putCategory(Map<String, String> target, String name, String category) {
    if (name == null || name.isBlank() || category == null || category.isBlank()) return;
    target.putIfAbsent(ingredientKey(name), category.trim());
  }

  private static String ingredientKey(String name) {
    return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public DishTemplateChangePageView merchantPage(CurrentUserContext user, MerchantDishTemplateChangeQuery query) {
    requireMerchantAdmin(user);
    MerchantDishTemplateChangeQuery normalized = query == null
        ? new MerchantDishTemplateChangeQuery(null, null, 1, 20) : query;
    String status = requireStatus(normalized.normalizedStatus());
    long total = requestMapper.countMerchant(user.merchantId(), status, normalized.normalizedKeyword());
    List<DishTemplateChangeItemView> items = requestMapper.selectMerchantPage(user.merchantId(), status,
        normalized.normalizedKeyword(), normalized.offset(), normalized.normalizedPageSize()).stream()
        .map(DishTemplateChangeRequestServiceImpl::toItem).toList();
    return new DishTemplateChangePageView(items, total, normalized.normalizedPage(), normalized.normalizedPageSize());
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public DishTemplateChangeDetailView merchantDetail(CurrentUserContext user, Long requestId) {
    requireMerchantAdmin(user);
    DishTemplateChangeRequestDO entity = requestMapper.selectMerchantDetail(requestId, user.merchantId());
    return toDetail(requireRequest(entity));
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public void withdraw(CurrentUserContext user, Long requestId) {
    requireMerchantAdmin(user);
    DishTemplateChangeRequestDO entity = requestMapper.selectMerchantForUpdate(requestId, user.merchantId());
    requirePending(requireRequest(entity));
    if (requestMapper.markWithdrawn(requestId, user.userId()) != 1) {
      throw conflict("模板菜品修改申请已处理");
    }
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public DishTemplateChangePageView adminPage(CurrentUserContext user, AdminDishTemplateChangeQuery query) {
    requirePlatformAdmin(user);
    AdminDishTemplateChangeQuery normalized = query == null
        ? new AdminDishTemplateChangeQuery(null, null, null, null, 1, 20) : query;
    String status = requireStatus(normalized.normalizedStatus());
    long total = requestMapper.countAdmin(status, normalized.merchantId(), normalized.templateId(),
        normalized.normalizedKeyword());
    List<DishTemplateChangeItemView> items = requestMapper.selectAdminPage(status, normalized.merchantId(),
        normalized.templateId(), normalized.normalizedKeyword(), normalized.offset(), normalized.normalizedPageSize())
        .stream().map(DishTemplateChangeRequestServiceImpl::toItem).toList();
    return new DishTemplateChangePageView(items, total, normalized.normalizedPage(), normalized.normalizedPageSize());
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public DishTemplateChangeDetailView adminDetail(CurrentUserContext user, Long requestId) {
    requirePlatformAdmin(user);
    return toDetail(requireRequest(requestMapper.selectDetail(requestId)));
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public void approve(CurrentUserContext user, Long requestId, DishTemplateApproveRequest request) {
    requirePlatformAdmin(user);
    DishTemplateChangeRequestDO application = requireRequest(requestMapper.selectForUpdate(requestId));
    requirePending(application);
    DishTemplateEntity current = templateMapper.selectTemplateForUpdate(application.getTemplateId());
    if (current == null || !Boolean.TRUE.equals(current.getEnabled())) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "模板菜品不存在或已停用，无法通过申请");
    }
    long currentVersion = current.getVersion() == null ? 0L : current.getVersion();
    if (currentVersion != application.getBaseTemplateVersion()) {
      throw conflict("模板菜品已发生变化，请重新提交");
    }
    DishTemplateSnapshotRequest target = preserveCurrentImageForLegacySnapshot(
        current, readSnapshot(application.getSnapshotJson()));
    target = resolveStepImages(application.getTemplateId(), target,
        templateMapper.selectTemplateCookingSteps(application.getTemplateId()));
    requireEnabledCategory(target.categoryId());
    DishTemplateEntity replacement = applyEditableFields(current, target);
    List<DishTemplateIngredientEntity> targetIngredients = target.ingredients().stream()
        .map(item -> toIngredient(application.getTemplateId(), item)).toList();
    List<DishTemplateCookingStepEntity> targetSteps = target.cookingSteps().stream()
        .map(item -> toStep(application.getTemplateId(), item)).toList();
    RecipeGraph graph = loadGraph(replacement, targetIngredients, targetSteps);
    var readiness = procurementEvaluator.evaluate(replacement.getId(),
        List.copyOf(graph.templates().values()), List.copyOf(graph.ingredients()));
    replacement.setProcurementReady(readiness.ready());
    replacement.setDataStatus(deriveStatus(replacement, readiness.ready()));
    if (templateMapper.updateAdminTemplate(replacement, application.getBaseTemplateVersion()) != 1) {
      throw conflict("模板菜品已发生变化，请重新提交");
    }
    templateMapper.deleteTemplateIngredients(application.getTemplateId());
    targetIngredients.forEach(templateMapper::insertTemplateIngredient);
    templateMapper.deleteTemplateCookingSteps(application.getTemplateId());
    targetSteps.forEach(templateMapper::insertTemplateCookingStep);
    String reason = request == null ? null : request.normalizedReason();
    if (reason != null && reason.length() > 500) throw badRequest("审核意见最多500个字符");
    if (requestMapper.markApproved(requestId, user.userId(), reason) != 1) {
      throw conflict("模板菜品修改申请已处理");
    }
    createResultNotification(application, target.name(), "已通过", reason);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public void reject(CurrentUserContext user, Long requestId, DishTemplateRejectRequest request) {
    requirePlatformAdmin(user);
    DishTemplateChangeRequestDO application = requireRequest(requestMapper.selectForUpdate(requestId));
    requirePending(application);
    String reason = request == null ? null : request.normalizedReason();
    if (reason == null || reason.isBlank()) throw badRequest("驳回原因不能为空");
    if (reason.length() > 500) throw badRequest("驳回原因最多500个字符");
    if (requestMapper.markRejected(requestId, user.userId(), reason) != 1) {
      throw conflict("模板菜品修改申请已处理");
    }
    String templateName = application.getTemplateName();
    if (templateName == null || templateName.isBlank()) {
      templateName = readSnapshot(application.getSnapshotJson()).name();
    }
    createResultNotification(application, templateName, "已驳回", reason);
  }

  private void createResultNotification(DishTemplateChangeRequestDO application, String templateName,
      String result, String reason) {
    NotificationDO notification = new NotificationDO();
    notification.setReceiverType("merchant");
    notification.setReceiverId(application.getMerchantId());
    notification.setReceiverScope("merchant");
    notification.setCategory("dish_template_review");
    notification.setTitle("模板菜品修改申请" + result);
    notification.setContent("申请ID：" + application.getId() + "；模板菜品：" + templateName + "；结果：" + result
        + "；审核原因：" + (reason == null ? "无" : reason)
        + "；详情入口：/merchant/dish-template-change-requests/" + application.getId());
    if (notificationMapper.insertNotificationEntity(notification) != 1 || notification.getId() == null) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "审核结果通知创建失败");
    }
    requestMapper.setResultNotificationId(application.getId(), notification.getId());
  }

  private DishTemplateSnapshotRequest toBaseSnapshot(DishTemplateEntity template,
      List<DishTemplateIngredientEntity> ingredients, List<DishTemplateCookingStepEntity> steps) {
    List<DishTemplateIngredientSnapshotRequest> items = new ArrayList<>();
    for (int index = 0; index < ingredients.size(); index++) {
      DishTemplateIngredientEntity item = ingredients.get(index);
      String itemId = item.getSourceLineKey() == null ? "template:" + template.getId() + ":ingredient:" + index
          : item.getSourceLineKey();
      items.add(new DishTemplateIngredientSnapshotRequest(itemId, item.getIngredientName(),
          item.getIngredientCategory(), item.getQuantityStatus() == null ? "VERIFIED" : item.getQuantityStatus(),
          item.getQuantity(), item.getUnit(), item.getCalcType(), item.getSourceText(),
          item.getSourceQuantityText(), item.getComponentTemplateId(), item.getComponentMultiplier(),
          item.getSortOrder()));
    }
    List<DishTemplateCookingStepSnapshotRequest> cookingSteps = new ArrayList<>();
    for (int index = 0; index < steps.size(); index++) {
      DishTemplateCookingStepEntity item = steps.get(index);
      String itemId = item.getItemKey() == null ? "template:" + template.getId() + ":step:" + index
          : item.getItemKey();
      cookingSteps.add(new DishTemplateCookingStepSnapshotRequest(itemId, index + 1, item.getTitle(),
          item.getContent(), item.getDurationSeconds(), item.getTemperatureText(), item.getHeatLevel(),
          item.getComponentTemplateId(), item.getImageUrls()));
    }
    return new DishTemplateSnapshotRequest(2, template.getCategoryId(), template.getName(),
        template.getDescription(), template.getImageUrl(), null, false, false,
        template.getReferencePrice(), readTags(template.getTasteTags()),
        readTags(template.getMealTags()), template.getSortOrder(), template.getEnabled(),
        List.copyOf(items), List.copyOf(cookingSteps), template.getProductType(),
        snapshotText(template.getNourishmentDescription()), snapshotText(template.getServingAdvice()), snapshotText(template.getPrecautions()));
  }

  private DishTemplateChangeDetailView toDetail(DishTemplateChangeRequestDO entity) {
    DishTemplateSnapshotRequest base = readSnapshot(entity.getBaseSnapshotJson());
    DishTemplateSnapshotRequest target = readSnapshot(entity.getSnapshotJson());
    Long currentVersion = entity.getCurrentTemplateVersion();
    boolean stale = currentVersion == null || !currentVersion.equals(entity.getBaseTemplateVersion());
    return new DishTemplateChangeDetailView(entity.getId(), entity.getMerchantId(), entity.getMerchantName(),
        entity.getTemplateId(), entity.getTemplateName(), entity.getBaseTemplateVersion(), currentVersion, stale,
        entity.getStatus(), entity.getSubmitNote(), base, target, entity.getSubmittedBy(), entity.getSubmittedAt(),
        entity.getReviewedBy(), entity.getReviewReason(), entity.getReviewedAt(), entity.getWithdrawnBy(),
        entity.getWithdrawnAt(), entity.getResultNotificationId());
  }

  private DishTemplateSnapshotRequest readSnapshot(String json) {
    try {
      JsonNode node = objectMapper.readTree(json);
      return snapshotValidator.parseAndValidate(node);
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模板菜品审核快照无法读取");
    }
  }

  private List<String> readTags(String json) {
    if (json == null || json.isBlank()) return List.of();
    try {
      return objectMapper.readValue(json, STRING_LIST);
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模板菜品标签数据格式错误");
    }
  }

  private String writeSnapshot(DishTemplateSnapshotRequest snapshot) {
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模板菜品审核快照生成失败");
    }
  }

  private DishTemplateCategoryEntity requireEnabledCategory(Long categoryId) {
    DishTemplateCategoryEntity category = templateMapper.selectCategoryForUpdate(categoryId);
    if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "模板菜品分类不存在或已停用");
    }
    return category;
  }

  private DishTemplateEntity applyEditableFields(DishTemplateEntity entity, DishTemplateSnapshotRequest source) {
    com.familykitchen.dish.service.NourishmentFields.apply(entity, source.productType(), source.nourishmentDescription(), source.servingAdvice(), source.precautions());
    entity.setCategoryId(source.categoryId()); entity.setName(source.name());
    entity.setDescription(source.description()); entity.setReferencePrice(source.referencePrice());
    entity.setTasteTags(writeTags(source.tasteTags())); entity.setMealTags(writeTags(source.mealTags()));
    entity.setSortOrder(source.sortOrder()); entity.setEnabled(source.enabled());
    if (!java.util.Objects.equals(entity.getImageUrl(), source.imageUrl())) {
      entity.setImageUrl(source.imageUrl());
      if (source.imageUrl() == null) {
        entity.setImageSourceUrl(null); entity.setImageAuthor(null); entity.setImageLicense(null);
        entity.setImageRightsStatus("NONE");
      } else {
        entity.setImageSourceUrl(source.imageUrl()); entity.setImageAuthor("商户上传");
        entity.setImageLicense("提交者确认拥有合法使用权并授权平台使用");
        entity.setImageRightsStatus("DECLARED");
      }
    }
    return entity;
  }

  /**
   * 将客户端图片变更规范化为可审核的确定目标值。
   * <p>旧客户端未提交图片字段时保留模板原图；新图必须引用当前账号刚上传并登记的图片资产。</p>
   */
  private DishTemplateSnapshotRequest resolveSubmittedImage(CurrentUserContext user,
      DishTemplateEntity template, DishTemplateSnapshotRequest source) {
    String imageUrl = source.imageUrl();
    Long imageAssetId = source.imageAssetId();
    boolean removeImage = Boolean.TRUE.equals(source.removeImage());
    boolean rightsConfirmed = Boolean.TRUE.equals(source.imageRightsConfirmed());
    if (removeImage) {
      imageUrl = null;
      imageAssetId = null;
      rightsConfirmed = false;
    } else if (imageAssetId != null) {
      FileAssetDO asset = fileAssetMapper.selectOwnedImage(imageAssetId, user.userId());
      if (asset == null) throw badRequest("所选图片不存在或不属于当前账号，请重新上传");
      if (!rightsConfirmed) throw badRequest("更换模板图片前必须确认拥有合法使用权");
      imageUrl = asset.getUrl();
    } else {
      if (imageUrl != null && !java.util.Objects.equals(imageUrl, template.getImageUrl())) {
        throw badRequest("更换模板图片请先通过图片上传接口上传");
      }
      imageUrl = template.getImageUrl();
      rightsConfirmed = false;
    }
    return new DishTemplateSnapshotRequest(source.schemaVersion(), source.categoryId(), source.name(),
        source.description(), imageUrl, imageAssetId, removeImage, rightsConfirmed,
        source.referencePrice(), source.tasteTags(), source.mealTags(), source.sortOrder(), source.enabled(),
        source.ingredients(), source.cookingSteps(), source.productType(), source.nourishmentDescription(), source.servingAdvice(), source.precautions());
  }

  /** 兼容图片字段上线前创建的待审核快照，未显式删除或替换时始终保留当前模板图片。 */
  private DishTemplateSnapshotRequest preserveCurrentImageForLegacySnapshot(DishTemplateEntity template,
      DishTemplateSnapshotRequest source) {
    if (source.imageUrl() != null || source.imageAssetId() != null || Boolean.TRUE.equals(source.removeImage())) {
      return source;
    }
    return new DishTemplateSnapshotRequest(source.schemaVersion(), source.categoryId(), source.name(),
        source.description(), template.getImageUrl(), null, false, false,
        source.referencePrice(), source.tasteTags(), source.mealTags(), source.sortOrder(), source.enabled(),
        source.ingredients(), source.cookingSteps(), source.productType(), source.nourishmentDescription(), source.servingAdvice(), source.precautions());
  }

  private static String snapshotText(String value) { return value == null ? "" : value; }

  /** Fill only new submissions, never rewrite a stored legacy snapshot or its baseline. */
  private DishTemplateSnapshotRequest fillNourishmentForSubmission(DishTemplateEntity template, DishTemplateSnapshotRequest source) {
    DishTemplateEntity merged = new DishTemplateEntity();
    merged.setTemplateType(template.getTemplateType());
    merged.setProductType(template.getProductType());
    merged.setNourishmentDescription(template.getNourishmentDescription());
    merged.setServingAdvice(template.getServingAdvice()); merged.setPrecautions(template.getPrecautions());
    com.familykitchen.dish.service.NourishmentFields.apply(merged, source.productType(), source.nourishmentDescription(), source.servingAdvice(), source.precautions());
    return new DishTemplateSnapshotRequest(source.schemaVersion(), source.categoryId(), source.name(), source.description(),
        source.imageUrl(), source.imageAssetId(), source.removeImage(), source.imageRightsConfirmed(), source.referencePrice(),
        source.tasteTags(), source.mealTags(), source.sortOrder(), source.enabled(), source.ingredients(), source.cookingSteps(),
        merged.getProductType(), snapshotText(merged.getNourishmentDescription()), snapshotText(merged.getServingAdvice()), snapshotText(merged.getPrecautions()));
  }

  private static DishTemplateIngredientEntity toIngredient(Long templateId,
      DishTemplateIngredientSnapshotRequest source) {
    DishTemplateIngredientEntity entity = new DishTemplateIngredientEntity();
    entity.setTemplateId(templateId); entity.setSourceLineKey(stableKey(templateId, source.itemId()));
    entity.setIngredientName(source.ingredientName()); entity.setIngredientCategory(source.ingredientCategory());
    entity.setQuantityStatus(source.quantityStatus()); entity.setQuantity(source.quantity());
    entity.setUnit(source.unit()); entity.setCalcType(source.calcType()); entity.setSourceText(source.sourceText());
    entity.setSourceQuantityText(source.sourceQuantityText());
    entity.setComponentTemplateId(source.componentTemplateId());
    entity.setComponentMultiplier(source.componentMultiplier()); entity.setSortOrder(source.sortOrder());
    return entity;
  }

  private static DishTemplateCookingStepEntity toStep(Long templateId,
      DishTemplateCookingStepSnapshotRequest source) {
    DishTemplateCookingStepEntity entity = new DishTemplateCookingStepEntity();
    entity.setTemplateId(templateId); entity.setItemKey(stableKey(templateId, source.itemId()));
    entity.setStepNo(source.stepNo()); entity.setTitle(source.title()); entity.setContent(source.content());
    entity.setDurationSeconds(source.durationSeconds()); entity.setTemperatureText(source.temperatureText());
    entity.setHeatLevel(source.heatLevel()); entity.setComponentTemplateId(source.componentTemplateId());
    entity.setImageUrls(source.imageUrls());
    return entity;
  }

  private static DishTemplateSnapshotRequest resolveStepImages(Long templateId, DishTemplateSnapshotRequest source,
      List<DishTemplateCookingStepEntity> current) {
    var steps = source.cookingSteps().stream().map(s -> new DishTemplateCookingStepSnapshotRequest(
        s.itemId(), s.stepNo(), s.title(), s.content(), s.durationSeconds(), s.temperatureText(), s.heatLevel(),
        s.componentTemplateId(), com.familykitchen.dish.service.CookingStepImages.resolveTemplate(
            templateId, s.itemId(), s.imageUrls(), current))).toList();
    return new DishTemplateSnapshotRequest(source.schemaVersion(), source.categoryId(), source.name(), source.description(),
        source.imageUrl(), source.imageAssetId(), source.removeImage(), source.imageRightsConfirmed(), source.referencePrice(),
        source.tasteTags(), source.mealTags(), source.sortOrder(), source.enabled(), source.ingredients(), steps,
        source.productType(), source.nourishmentDescription(), source.servingAdvice(), source.precautions());
  }

  private String writeTags(List<String> values) {
    try {
      return objectMapper.writeValueAsString(values);
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模板菜品标签数据生成失败");
    }
  }

  private RecipeGraph loadGraph(DishTemplateEntity root,
      List<DishTemplateIngredientEntity> rootIngredients,
      List<DishTemplateCookingStepEntity> rootSteps) {
    Map<Long, DishTemplateEntity> templates = new LinkedHashMap<>();
    List<DishTemplateIngredientEntity> ingredients = new ArrayList<>(rootIngredients);
    templates.put(root.getId(), root);
    rootIngredients.forEach(item -> loadComponent(item.getComponentTemplateId(), templates, ingredients));
    rootSteps.forEach(item -> loadComponent(item.getComponentTemplateId(), templates, ingredients));
    return new RecipeGraph(templates, ingredients);
  }

  private void loadComponent(Long componentId, Map<Long, DishTemplateEntity> templates,
      List<DishTemplateIngredientEntity> ingredients) {
    if (componentId == null || templates.containsKey(componentId)) return;
    DishTemplateEntity component = templateMapper.selectTemplateForUpdate(componentId);
    if (component == null || !"COMPONENT".equals(component.getTemplateType())) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "审核快照引用的配料组件不存在：" + componentId);
    }
    templates.put(componentId, component);
    List<DishTemplateIngredientEntity> rows = templateMapper.selectTemplateIngredients(componentId);
    ingredients.addAll(rows);
    rows.forEach(item -> loadComponent(item.getComponentTemplateId(), templates, ingredients));
  }

  private static String deriveStatus(DishTemplateEntity template, boolean procurementReady) {
    boolean missingPrice = "DISH".equals(template.getTemplateType()) && template.getReferencePrice() == null;
    if (missingPrice && !procurementReady) return "NEEDS_BOTH";
    if (missingPrice) return "NEEDS_PRICE";
    return procurementReady ? "READY" : "NEEDS_PURCHASE_DATA";
  }

  private static String stableKey(Long templateId, String itemId) {
    String prefix = "review:" + templateId + ":";
    return itemId.startsWith(prefix) ? itemId : prefix + itemId;
  }

  private static DishTemplateChangeItemView toItem(DishTemplateChangeRequestDO item) {
    return new DishTemplateChangeItemView(item.getId(), item.getMerchantId(), item.getMerchantName(),
        item.getTemplateId(), item.getTemplateName(), item.getBaseTemplateVersion(), item.getStatus(),
        item.getSubmitNote(), item.getSubmittedBy(), item.getSubmittedAt(), item.getReviewedBy(),
        item.getReviewReason(), item.getReviewedAt());
  }

  private static DishTemplateChangeRequestDO requireRequest(DishTemplateChangeRequestDO entity) {
    if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "模板菜品修改申请不存在");
    return entity;
  }

  private static void requirePending(DishTemplateChangeRequestDO entity) {
    if (!"PENDING".equals(entity.getStatus())) throw conflict("模板菜品修改申请已处理");
  }

  private static String requireStatus(String status) {
    if (status != null && !STATUSES.contains(status)) throw badRequest("模板菜品修改申请状态不支持");
    return status;
  }

  private static void requireMerchantAdmin(CurrentUserContext user) {
    if (user == null || user.merchantId() == null || !user.hasMerchantBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "需要商户后台管理权限");
    }
  }

  private static void requirePlatformAdmin(CurrentUserContext user) {
    if (user == null || !user.hasPlatformBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "需要平台后台管理权限");
    }
  }

  private static BusinessException badRequest(String message) {
    return new BusinessException(ErrorCode.BAD_REQUEST, message);
  }

  private static BusinessException conflict(String message) {
    return new BusinessException(ErrorCode.STATE_CONFLICT, message);
  }

  /**
   * 审核目标模板及其可达组件组成的采购计算图。
   * @param templates 根模板和组件模板
   * @param ingredients 图内全部食材和组件引用行
   */
  private record RecipeGraph(Map<Long, DishTemplateEntity> templates,
                             List<DishTemplateIngredientEntity> ingredients) { }
}
