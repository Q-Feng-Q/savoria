package com.familykitchen.dish.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishTemplateMapper;
import com.familykitchen.dish.model.dto.AdminDishTemplateQuery;
import com.familykitchen.dish.model.dto.AdminDishTemplateUpdateRequest;
import com.familykitchen.dish.model.dto.DishTemplateImagePromotionRequest;
import com.familykitchen.dish.model.dto.DishTemplateImageRejectionRequest;
import com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity;
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateImageAssetEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.entity.DishTemplateSourceRecordEntity;
import com.familykitchen.dish.model.vo.AdminDishTemplateDetailView;
import com.familykitchen.dish.model.vo.AdminDishTemplatePageView;
import com.familykitchen.dish.model.vo.AdminDishTemplateView;
import com.familykitchen.dish.model.vo.DishTemplateImageAssetStatusView;
import com.familykitchen.dish.model.vo.DishTemplateMutationView;
import com.familykitchen.dish.service.AdminDishTemplateService;
import com.familykitchen.dish.service.NourishmentFields;
import com.familykitchen.dish.service.DishTemplateImageStorageService;
import com.familykitchen.dish.service.DishTemplateProcurementReadinessEvaluator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 平台管理员模板维护服务，直接通过 MyBatis Mapper 完成事务写入。 */
@Service
public class AdminDishTemplateServiceImpl implements AdminDishTemplateService {
  private static final Set<String> MEALS = Set.of("BREAKFAST", "LUNCH", "DINNER");
  private static final Set<String> QUANTITY_STATUSES = Set.of(
      "VERIFIED", "SOURCE_BATCH", "MISSING", "NOT_APPLICABLE");
  private static final Set<String> CALC_TYPES = Set.of("FIXED", "PER_PERSON");
  private final DishTemplateMapper mapper;
  private final DishTemplateProcurementReadinessEvaluator evaluator;
  private final DishTemplateImageStorageService storage;
  private final ObjectMapper objectMapper;

  /**
   * 创建平台模板管理服务。
   * @param mapper 模板MyBatis Mapper
   * @param evaluator 采购就绪判定器
   * @param storage 图片受控存储服务
   * @param objectMapper JSON标签转换器
   */
  public AdminDishTemplateServiceImpl(DishTemplateMapper mapper,
      DishTemplateProcurementReadinessEvaluator evaluator,
      DishTemplateImageStorageService storage, ObjectMapper objectMapper) {
    this.mapper = mapper;
    this.evaluator = evaluator;
    this.storage = storage;
    this.objectMapper = objectMapper;
  }

  /** {@inheritDoc} */
  @Override
  public AdminDishTemplatePageView page(CurrentUserContext user, AdminDishTemplateQuery rawQuery) {
    requireAdmin(user);
    AdminDishTemplateQuery query = rawQuery.validated();
    long total = query.productType() == null ? mapper.countAdminTemplates(query.normalizedKeyword(), query.sourceType(),
        query.templateType(), query.dataStatus(), query.normalizedSourceCategory(), query.missingImage(),
        query.missingSteps()) : mapper.countAdminTemplatesByProductType(query.normalizedKeyword(), query.sourceType(),
        query.templateType(), query.dataStatus(), query.normalizedSourceCategory(), query.missingImage(), query.missingSteps(), query.productType());
    List<AdminDishTemplateView> items = (query.productType() == null ? mapper.selectAdminTemplatesPage(query.normalizedKeyword(),
        query.sourceType(), query.templateType(), query.dataStatus(), query.normalizedSourceCategory(),
        query.missingImage(), query.missingSteps(), query.offset(), query.normalizedPageSize())
        : mapper.selectAdminTemplatesPageByProductType(query.normalizedKeyword(), query.sourceType(), query.templateType(),
        query.dataStatus(), query.normalizedSourceCategory(), query.missingImage(), query.missingSteps(), query.offset(), query.normalizedPageSize(), query.productType())).stream()
        .map(this::toListView).toList();
    return new AdminDishTemplatePageView(items, total, query.normalizedPage(), query.normalizedPageSize());
  }

  /** {@inheritDoc} */
  @Override
  public AdminDishTemplateDetailView detail(CurrentUserContext user, Long templateId) {
    requireAdmin(user);
    DishTemplateEntity template = requireTemplate(mapper.selectAdminTemplate(templateId));
    var assets = mapper.selectTemplateImageAssets(templateId).stream()
        .map(asset -> new AdminDishTemplateDetailView.InternalAssetReview(asset.getId(), asset.getAssetStatus(),
            !"REJECTED".equals(asset.getAssetStatus()), asset.getMimeType(), asset.getFileSize(),
            asset.getRejectionReason())).toList();
    return new AdminDishTemplateDetailView(template.getId(), template.getTemplateCode(), template.getCategoryId(),
        template.getCategoryName(), template.getName(), template.getDescription(), template.getImageUrl(),
        template.getReferencePrice(), parseTags(template.getTasteTags()), parseTags(template.getMealTags()),
        template.getSourceType(), template.getSourceCategory(), template.getTemplateType(),
        template.getDataStatus(), Boolean.TRUE.equals(template.getProcurementReady()),
        template.getImageRightsStatus(), template.getSortOrder(), Boolean.TRUE.equals(template.getEnabled()),
        template.getVersion(), mapper.selectTemplateSourceRecords(templateId),
        mapper.selectTemplateNameAliases(templateId), mapper.selectTemplateIngredients(templateId),
        mapper.selectTemplateCookingSteps(templateId), assets, template.getProductType(),
        template.getNourishmentDescription(), template.getServingAdvice(), template.getPrecautions());
  }

  /** {@inheritDoc} */
  @Override
  public List<DishTemplateSourceRecordEntity> sourceRecords(CurrentUserContext user, Long templateId) {
    requireAdmin(user);
    requireTemplate(mapper.selectAdminTemplate(templateId));
    return mapper.selectTemplateSourceRecords(templateId);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public DishTemplateMutationView update(CurrentUserContext user, Long templateId,
      AdminDishTemplateUpdateRequest request) {
    requireAdmin(user);
    validateUpdate(request);
    DishTemplateEntity locked = requireTemplate(mapper.selectTemplateForUpdate(templateId));
    if (!request.expectedVersion().equals(locked.getVersion())) conflict();
    var category = mapper.selectCategoryForUpdate(request.categoryId());
    if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "模板分类不存在或已停用");
    }
    List<DishTemplateIngredientEntity> ingredients = request.ingredients().stream()
        .map(item -> toIngredient(templateId, item)).toList();
    var currentSteps = mapper.selectTemplateCookingSteps(templateId);
    List<DishTemplateCookingStepEntity> steps = request.cookingSteps().stream()
        .map(item -> {
          var row = toStep(templateId, item);
          row.setImageUrls(com.familykitchen.dish.service.CookingStepImages.resolveTemplate(
              templateId, item.itemId(), item.imageUrls(), currentSteps));
          return row;
        }).toList();
    DishTemplateEntity edited = editableCopy(locked, request);
    RecipeGraph graph = loadGraph(edited, ingredients, steps);
    var readiness = evaluator.evaluate(templateId, List.copyOf(graph.templates.values()), graph.ingredients);
    edited.setProcurementReady(readiness.ready());
    edited.setDataStatus(deriveStatus(edited, readiness.ready()));
    if (mapper.updateAdminTemplate(edited, request.expectedVersion()) != 1) conflict();
    mapper.deleteTemplateIngredients(templateId);
    ingredients.forEach(mapper::insertTemplateIngredient);
    mapper.deleteTemplateCookingSteps(templateId);
    steps.forEach(mapper::insertTemplateCookingStep);
    return new DishTemplateMutationView(templateId, request.expectedVersion() + 1,
        edited.getDataStatus(), readiness.ready());
  }

  /** {@inheritDoc} */
  @Override
  public DishTemplateImageStorageService.PreviewResource preview(CurrentUserContext user, Long assetId) {
    requireAdmin(user);
    DishTemplateImageAssetEntity asset = mapper.selectTemplateImageAsset(assetId);
    if (asset == null || "REJECTED".equals(asset.getAssetStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "内部图片不存在");
    }
    return storage.preview(asset);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public DishTemplateMutationView promoteImage(CurrentUserContext user, Long templateId,
      DishTemplateImagePromotionRequest request) {
    requireAdmin(user);
    DishTemplateEntity template = requireTemplate(mapper.selectTemplateForUpdate(templateId));
    if (!request.expectedVersion().equals(template.getVersion())) conflict();
    DishTemplateImageAssetEntity asset = mapper.selectTemplateImageAssetForUpdate(request.internalAssetId());
    if (asset == null || !templateId.equals(asset.getTemplateId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "内部图片不存在");
    }
    if (!"INTERNAL_REVIEW".equals(asset.getAssetStatus())) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "该图片已完成审核，不能重复发布");
    }
    var published = storage.publish(asset);
    registerCleanup(published);
    if (mapper.markTemplateImageAssetPublished(asset.getId(), published.publicUrl(),
        request.author().trim(), request.license().trim(), user.userId()) != 1) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "图片审核状态已变化，请刷新后重试");
    }
    if (mapper.publishTemplateImage(templateId, published.publicUrl(), request.sourceUrl().trim(),
        request.author().trim(), request.license().trim(), request.expectedVersion()) != 1) conflict();
    return new DishTemplateMutationView(templateId, request.expectedVersion() + 1,
        template.getDataStatus(), Boolean.TRUE.equals(template.getProcurementReady()));
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public DishTemplateImageAssetStatusView rejectImage(CurrentUserContext user, Long templateId, Long assetId,
      DishTemplateImageRejectionRequest request) {
    requireAdmin(user);
    DishTemplateImageAssetEntity asset = mapper.selectTemplateImageAssetForUpdate(assetId);
    if (asset == null || !templateId.equals(asset.getTemplateId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "内部图片不存在");
    }
    if (!"INTERNAL_REVIEW".equals(asset.getAssetStatus())) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有待审核图片可以驳回");
    }
    if (mapper.rejectTemplateImageAsset(assetId, user.userId(), request.reason().trim()) != 1) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "图片审核状态已变化，请刷新后重试");
    }
    return new DishTemplateImageAssetStatusView(assetId, "REJECTED");
  }

  private RecipeGraph loadGraph(DishTemplateEntity root, List<DishTemplateIngredientEntity> rootIngredients,
      List<DishTemplateCookingStepEntity> rootSteps) {
    Map<Long, DishTemplateEntity> templates = new LinkedHashMap<>();
    List<DishTemplateIngredientEntity> ingredients = new ArrayList<>();
    templates.put(root.getId(), root);
    ingredients.addAll(rootIngredients);
    for (DishTemplateIngredientEntity row : rootIngredients) loadComponent(row.getComponentTemplateId(), templates, ingredients);
    for (DishTemplateCookingStepEntity step : rootSteps) {
      loadComponent(step.getComponentTemplateId(), templates, ingredients);
      if (step.getComponentTemplateId() != null && !templates.containsKey(step.getComponentTemplateId())) {
        bad("制作步骤引用的组件模板不存在：" + step.getComponentTemplateId());
      }
    }
    return new RecipeGraph(templates, ingredients);
  }

  private void loadComponent(Long id, Map<Long, DishTemplateEntity> templates,
      List<DishTemplateIngredientEntity> ingredients) {
    if (id == null || templates.containsKey(id)) return;
    DishTemplateEntity component = mapper.selectTemplateForUpdate(id);
    if (component == null || !"COMPONENT".equals(component.getTemplateType())) return;
    templates.put(id, component);
    List<DishTemplateIngredientEntity> rows = mapper.selectTemplateIngredients(id);
    ingredients.addAll(rows);
    rows.forEach(row -> loadComponent(row.getComponentTemplateId(), templates, ingredients));
  }

  private static void validateUpdate(AdminDishTemplateUpdateRequest request) {
    if (!Integer.valueOf(2).equals(request.schemaVersion())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "schemaVersion当前必须为2");
    }
    if (request.expectedVersion() < 0) bad("expectedVersion不能小于0");
    if (request.imageUrl() != null || request.imageSourceUrl() != null || request.imageAuthor() != null
        || request.imageLicense() != null || request.sourceType() != null || request.sourceKey() != null
        || request.sourceUrl() != null || request.sourceRevision() != null || request.sourceCategory() != null
        || request.sourceYieldText() != null || request.templateType() != null || request.dataStatus() != null
        || request.procurementReady() != null || request.imageRightsStatus() != null) {
      bad("请求包含来源、图片版权或派生状态等服务端维护字段");
    }
    if (!MEALS.containsAll(request.mealTags())) bad("mealTags包含不支持的餐次");
    Set<String> itemIds = new HashSet<>();
    for (var item : request.ingredients()) {
      if (!itemIds.add(item.itemId())) bad("食材itemId不能重复");
      if (!QUANTITY_STATUSES.contains(item.quantityStatus())) bad("食材quantityStatus取值不合法");
      boolean verified = "VERIFIED".equals(item.quantityStatus());
      if (verified && (item.quantity() == null || item.unit() == null || item.unit().isBlank()
          || !CALC_TYPES.contains(item.calcType()))) bad("VERIFIED食材必须填写有效数量、单位和计算方式");
      if (!verified && (item.quantity() != null || item.unit() != null || item.calcType() != null)) {
        bad("非VERIFIED食材不能填写采购数量、单位或计算方式");
      }
      if (item.componentTemplateId() != null && item.componentMultiplier() == null) {
        bad("组件食材必须填写componentMultiplier");
      }
    }
    itemIds.clear();
    for (int index = 0; index < request.cookingSteps().size(); index++) {
      var step = request.cookingSteps().get(index);
      if (!itemIds.add(step.itemId())) bad("步骤itemId不能重复");
      if (step.stepNo() == null || step.stepNo() != index + 1) bad("制作步骤序号必须从1开始连续排列");
      if (step.durationSeconds() != null && step.durationSeconds() < 0) bad("步骤时长不能小于0");
    }
  }

  private DishTemplateEntity editableCopy(DishTemplateEntity source, AdminDishTemplateUpdateRequest request) {
    NourishmentFields.apply(source, request.productType(), request.nourishmentDescription(), request.servingAdvice(), request.precautions());
    source.setCategoryId(request.categoryId()); source.setName(request.name().trim());
    source.setDescription(trimToNull(request.description())); source.setReferencePrice(request.referencePrice());
    source.setTasteTags(writeTags(request.tasteTags())); source.setMealTags(writeTags(request.mealTags()));
    source.setEnabled(request.enabled());
    return source;
  }

  private static DishTemplateIngredientEntity toIngredient(Long templateId,
      AdminDishTemplateUpdateRequest.IngredientItem item) {
    DishTemplateIngredientEntity row = new DishTemplateIngredientEntity();
    row.setTemplateId(templateId); row.setIngredientName(item.ingredientName().trim());
    row.setIngredientCategory(item.ingredientCategory().trim()); row.setQuantityStatus(item.quantityStatus());
    row.setQuantity(item.quantity()); row.setUnit(trimToNull(item.unit())); row.setCalcType(item.calcType());
    row.setSourceText(trimToNull(item.sourceText())); row.setSourceQuantityText(trimToNull(item.sourceQuantityText()));
    row.setComponentTemplateId(item.componentTemplateId()); row.setComponentMultiplier(item.componentMultiplier());
    row.setSourceLineKey(stableItemKey(templateId, item.itemId())); row.setSortOrder(item.sortOrder());
    return row;
  }

  private static DishTemplateCookingStepEntity toStep(Long templateId,
      AdminDishTemplateUpdateRequest.CookingStepItem item) {
    DishTemplateCookingStepEntity row = new DishTemplateCookingStepEntity();
    row.setItemKey(stableItemKey(templateId, item.itemId()));
    row.setTemplateId(templateId); row.setStepNo(item.stepNo()); row.setTitle(trimToNull(item.title()));
    row.setContent(item.content().trim()); row.setDurationSeconds(item.durationSeconds());
    row.setTemperatureText(trimToNull(item.temperatureText())); row.setHeatLevel(trimToNull(item.heatLevel()));
    row.setComponentTemplateId(item.componentTemplateId()); return row;
  }

  private AdminDishTemplateView toListView(DishTemplateEntity item) {
    return new AdminDishTemplateView(item.getId(), item.getTemplateCode(), item.getName(),
        item.getSourceCategory(), item.getSourceType(), item.getTemplateType(), item.getDataStatus(),
        Boolean.TRUE.equals(item.getProcurementReady()), item.getImageRightsStatus(), item.getImageUrl(),
        item.getReferencePrice(), Boolean.TRUE.equals(item.getMissingSteps()), item.getSourceRevision(),
        Boolean.TRUE.equals(item.getEnabled()), item.getVersion(), item.getProductType(),
        item.getNourishmentDescription(), item.getServingAdvice(), item.getPrecautions());
  }

  private String deriveStatus(DishTemplateEntity template, boolean procurementReady) {
    boolean needsPrice = "DISH".equals(template.getTemplateType()) && template.getReferencePrice() == null;
    if (needsPrice && !procurementReady) return "NEEDS_BOTH";
    if (needsPrice) return "NEEDS_PRICE";
    return procurementReady ? "READY" : "NEEDS_PURCHASE_DATA";
  }

  private List<String> parseTags(String json) {
    try { return json == null ? List.of() : objectMapper.readValue(json,
        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)); }
    catch (JsonProcessingException exception) { throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模板标签数据损坏"); }
  }

  private String writeTags(List<String> tags) {
    try { return objectMapper.writeValueAsString(tags); }
    catch (JsonProcessingException exception) { throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模板标签保存失败"); }
  }

  private void registerCleanup(DishTemplateImageStorageService.PublishedResource resource) {
    if (!resource.newlyCreated() || !TransactionSynchronizationManager.isSynchronizationActive()) return;
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) storage.deleteNewFile(resource);
      }
    });
  }

  private static DishTemplateEntity requireTemplate(DishTemplateEntity template) {
    if (template == null) throw new BusinessException(ErrorCode.NOT_FOUND, "模板菜品不存在");
    return template;
  }

  private static void requireAdmin(CurrentUserContext user) {
    if (user == null || !user.hasPlatformBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无平台管理员权限");
    }
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String stableItemKey(Long templateId, String itemId) {
    String prefix = "admin:" + templateId + ":";
    return itemId.startsWith(prefix) ? itemId : prefix + itemId;
  }

  private static void bad(String message) { throw new BusinessException(ErrorCode.BAD_REQUEST, message); }
  private static void conflict() { throw new BusinessException(ErrorCode.STATE_CONFLICT, "模板版本已变化，请刷新后重试"); }
  /**
   * 一棵模板及配料组件形成的采购计算图。
   * @param templates 根模板和可达组件
   * @param ingredients 图中全部食材与组件引用行
   */
  private record RecipeGraph(Map<Long, DishTemplateEntity> templates,
                             List<DishTemplateIngredientEntity> ingredients) { }
}
