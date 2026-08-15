package com.familykitchen.dish.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.mapper.DishTemplateMapper;
import com.familykitchen.dish.model.dto.DishTemplateQuery;
import com.familykitchen.dish.model.entity.DishCategoryEntity;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishIngredientEntity;
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.entity.IngredientDictionaryEntity;
import com.familykitchen.dish.model.vo.DishTemplateCategoryView;
import com.familykitchen.dish.model.vo.DishTemplateDetailView;
import com.familykitchen.dish.model.vo.DishTemplateImportResultView;
import com.familykitchen.dish.model.vo.DishTemplatePageView;
import com.familykitchen.dish.model.vo.DishTemplateView;
import com.familykitchen.dish.service.DishTemplateService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台菜品模板服务实现。
 *
 * <p>模板只读查询不依赖商户数据；导入操作在单一事务内直接调用 MyBatis Mapper，
 * 复制分类、菜品、食材用量和商户食材字典。导入生成的菜品归商户所有，后续修改不回写平台模板。</p>
 */
@Service
public class DishTemplateServiceImpl implements DishTemplateService {
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
  private final DishTemplateMapper templateMapper;
  private final DishMapper dishMapper;
  private final ObjectMapper objectMapper;

  /**
   * 创建平台菜品模板服务。
   * @param templateMapper 模板查询和导入 Mapper
   * @param dishMapper 商户菜品与食材 Mapper
   * @param objectMapper JSON 标签解析器
   */
  public DishTemplateServiceImpl(DishTemplateMapper templateMapper, DishMapper dishMapper,
      ObjectMapper objectMapper) {
    this.templateMapper = templateMapper;
    this.dishMapper = dishMapper;
    this.objectMapper = objectMapper;
  }

  /** {@inheritDoc} */
  @Override
  public List<DishTemplateCategoryView> categories() {
    return templateMapper.selectTemplateCategories().stream()
        .map(item -> new DishTemplateCategoryView(item.getId(), item.getCode(), item.getName(), item.getSortOrder()))
        .toList();
  }

  /** {@inheritDoc} */
  @Override
  public DishTemplatePageView page(CurrentUserContext user, DishTemplateQuery query) {
    long total = templateMapper.countTemplates(user.merchantId(), query.categoryId(),
        query.normalizedKeyword(), query.imported());
    List<DishTemplateView> items = templateMapper.selectTemplates(user.merchantId(), query.categoryId(),
        query.normalizedKeyword(), query.imported(), query.offset(), query.normalizedPageSize()).stream()
        .map(this::toView).toList();
    return new DishTemplatePageView(items, total, query.normalizedPage(), query.normalizedPageSize());
  }

  /** {@inheritDoc} */
  @Override
  public DishTemplateDetailView detail(CurrentUserContext user, Long templateId) {
    DishTemplateEntity template = templateMapper.selectTemplate(user.merchantId(), templateId);
    if (template == null) throw new BusinessException(ErrorCode.NOT_FOUND, "模板菜品不存在或已停用");
    return new DishTemplateDetailView(template.getId(), template.getTemplateCode(), template.getCategoryId(),
        template.getCategoryName(), template.getName(), template.getDescription(), template.getImageUrl(),
        template.getImageSourceUrl(), template.getImageAuthor(), template.getImageLicense(),
        template.getReferencePrice(), parseTags(template.getTasteTags()), parseTags(template.getMealTags()),
        Boolean.TRUE.equals(template.getImported()), templateMapper.selectTemplateIngredients(templateId));
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public DishTemplateImportResultView importTemplates(CurrentUserContext user, List<Long> templateIds) {
    List<Long> requested = new ArrayList<>(new LinkedHashSet<>(templateIds));
    if (requested.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要导入的模板菜品");
    if (requested.size() > 100) throw new BusinessException(ErrorCode.BAD_REQUEST, "单次最多导入100道模板菜品");
    List<DishTemplateEntity> templates = templateMapper.selectTemplatesByIds(requested);
    Set<Long> foundIds = templates.stream().map(DishTemplateEntity::getId).collect(Collectors.toSet());
    List<Long> unavailable = requested.stream().filter(id -> !foundIds.contains(id)).toList();
    if (!unavailable.isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模板菜品不存在或已停用：" + unavailable);
    }
    return importResolvedTemplates(user, templates);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public DishTemplateImportResultView importAllTemplates(CurrentUserContext user) {
    List<DishTemplateEntity> templates = templateMapper.selectAllEnabledTemplates();
    if (templates.isEmpty()) return new DishTemplateImportResultView(List.of(), List.of());
    return importResolvedTemplates(user, templates);
  }

  private DishTemplateImportResultView importResolvedTemplates(CurrentUserContext user,
      List<DishTemplateEntity> templates) {
    List<Long> requested = templates.stream().map(DishTemplateEntity::getId).toList();
    Set<Long> alreadyImported = new LinkedHashSet<>(
        templateMapper.selectImportedTemplateIds(user.merchantId(), requested));
    List<DishTemplateIngredientEntity> ingredients = templateMapper.selectTemplateIngredientsByIds(requested);
    Map<Long, List<DishTemplateIngredientEntity>> ingredientsByTemplate = ingredients.stream()
        .collect(Collectors.groupingBy(DishTemplateIngredientEntity::getTemplateId, LinkedHashMap::new,
            Collectors.toList()));
    Map<String, DishCategoryEntity> categoryCache = new LinkedHashMap<>();
    List<Long> importedIds = new ArrayList<>();
    List<Long> skippedIds = new ArrayList<>();
    for (DishTemplateEntity template : templates) {
      if (alreadyImported.contains(template.getId())) {
        skippedIds.add(template.getId());
        continue;
      }
      DishCategoryEntity category = categoryCache.computeIfAbsent(template.getCategoryName(),
          name -> requireMerchantCategory(user.merchantId(), name, template.getSortOrder()));
      DishEntity dish = toImportedDish(user.merchantId(), category.getId(), template);
      if (templateMapper.insertImportedDishIgnore(dish) == 0) {
        skippedIds.add(template.getId());
        continue;
      }
      Long dishId = dish.getId() == null
          ? templateMapper.selectImportedDishId(user.merchantId(), template.getId()) : dish.getId();
      for (DishTemplateIngredientEntity source : ingredientsByTemplate.getOrDefault(template.getId(), List.of())) {
        dishMapper.insertDishIngredient(toDishIngredient(dishId, source));
        templateMapper.insertMerchantIngredientIgnore(toDictionaryIngredient(user.merchantId(), source));
      }
      importedIds.add(template.getId());
    }
    return new DishTemplateImportResultView(List.copyOf(importedIds), List.copyOf(skippedIds));
  }

  private DishCategoryEntity requireMerchantCategory(Long merchantId, String name, Integer sortOrder) {
    DishCategoryEntity category = templateMapper.selectMerchantCategoryByName(merchantId, name);
    if (category != null) return category;
    DishCategoryEntity created = new DishCategoryEntity();
    created.setMerchantId(merchantId); created.setName(name);
    created.setSortOrder(sortOrder == null ? 0 : sortOrder); created.setEnabled(true);
    templateMapper.insertMerchantCategoryIgnore(created);
    category = templateMapper.selectMerchantCategoryByName(merchantId, name);
    if (category == null) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导入模板菜品时创建分类失败");
    return category;
  }

  private DishEntity toImportedDish(Long merchantId, Long categoryId, DishTemplateEntity template) {
    DishEntity dish = new DishEntity();
    dish.setMerchantId(merchantId); dish.setCategoryId(categoryId); dish.setName(template.getName());
    dish.setDescription(template.getDescription()); dish.setImageUrl(template.getImageUrl());
    dish.setBasePrice(template.getReferencePrice()); dish.setSourceTemplateId(template.getId()); dish.setStatus("active");
    return dish;
  }

  private static DishIngredientEntity toDishIngredient(Long dishId, DishTemplateIngredientEntity source) {
    DishIngredientEntity target = new DishIngredientEntity();
    target.setDishId(dishId); target.setIngredientName(source.getIngredientName());
    target.setQuantity(source.getQuantity()); target.setUnit(source.getUnit()); target.setCalcType(source.getCalcType());
    return target;
  }

  private static IngredientDictionaryEntity toDictionaryIngredient(Long merchantId,
      DishTemplateIngredientEntity source) {
    IngredientDictionaryEntity target = new IngredientDictionaryEntity();
    target.setMerchantId(merchantId); target.setName(source.getIngredientName());
    target.setCategory(source.getIngredientCategory()); target.setUnit(source.getUnit());
    return target;
  }

  private DishTemplateView toView(DishTemplateEntity item) {
    return new DishTemplateView(item.getId(), item.getTemplateCode(), item.getCategoryId(), item.getCategoryName(),
        item.getName(), item.getDescription(), item.getImageUrl(), item.getReferencePrice(),
        parseTags(item.getTasteTags()), parseTags(item.getMealTags()), item.getIngredientCount(),
        Boolean.TRUE.equals(item.getImported()));
  }

  private List<String> parseTags(String json) {
    if (json == null || json.isBlank()) return List.of();
    try {
      return objectMapper.readValue(json, STRING_LIST);
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模板菜品标签数据格式错误");
    }
  }
}
