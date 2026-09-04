package com.familykitchen.dish.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.dish.model.dto.DishTemplateCookingStepSnapshotRequest;
import com.familykitchen.dish.model.dto.DishTemplateIngredientSnapshotRequest;
import com.familykitchen.dish.model.dto.DishTemplateSnapshotRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 模板审核快照的严格解析与第二版业务校验器。
 *
 * <p>独立严格Reader会拒绝图片、来源身份、模板类型、派生状态和其他未知字段，
 * 避免商户通过审核快照修改服务端所有数据。</p>
 */
@Component
public class DishTemplateSnapshotValidator {
  /** 快照JSON最大UTF-8字节数。 */ private static final int MAX_SNAPSHOT_BYTES = 1024 * 1024;
  /** 支持的餐次。 */ private static final Set<String> MEAL_TAGS = Set.of("BREAKFAST", "LUNCH", "DINNER");
  /** 支持的采购计算方式。 */ private static final Set<String> CALC_TYPES = Set.of("FIXED", "PER_PERSON");
  /** 支持的数量状态。 */ private static final Set<String> QUANTITY_STATUSES = Set.of(
      "VERIFIED", "SOURCE_BATCH", "MISSING", "NOT_APPLICABLE");

  private final ObjectMapper objectMapper;
  private final ObjectReader strictReader;

  /**
   * 创建快照校验器。
   * @param objectMapper 应用JSON映射器
   */
  public DishTemplateSnapshotValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    strictReader = objectMapper.readerFor(DishTemplateSnapshotRequest.class)
        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  /**
   * 严格解析、规范化并校验第二版目标快照。
   * @param targetSnapshot 客户端提交的目标快照JSON
   * @return 可安全持久化的规范化快照
   */
  public DishTemplateSnapshotRequest parseAndValidate(JsonNode targetSnapshot) {
    if (targetSnapshot == null || targetSnapshot.isNull() || !targetSnapshot.isObject()) {
      throw bad("模板菜品修改内容不完整");
    }
    try {
      if (objectMapper.writeValueAsBytes(targetSnapshot).length > MAX_SNAPSHOT_BYTES) {
        throw bad("模板菜品目标快照不能超过1MB");
      }
      return normalize(strictReader.readValue(targetSnapshot));
    } catch (BusinessException exception) {
      throw exception;
    } catch (com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException exception) {
      throw bad("模板菜品修改内容包含未知字段：" + exception.getPropertyName());
    } catch (Exception exception) {
      throw bad("模板菜品修改内容不完整或格式错误");
    }
  }

  private DishTemplateSnapshotRequest normalize(DishTemplateSnapshotRequest source) {
    if (source == null) throw bad("模板菜品修改内容不完整");
    if (!Integer.valueOf(2).equals(source.schemaVersion())) throw bad("模板菜品快照版本不支持");
    Long categoryId = positive(source.categoryId(), "模板分类ID必须为正整数");
    String name = text(source.name(), 100, "菜品名称");
    String description = optionalText(source.description(), 255, "菜品简介");
    BigDecimal price = amount(source.referencePrice(), true, "参考价格");
    List<String> tastes = tags(source.tasteTags(), 10, null, "口味标签");
    List<String> meals = tags(source.mealTags(), 3, MEAL_TAGS, "推荐餐次");
    Integer sortOrder = sortOrder(source.sortOrder(), "模板排序值");
    if (source.enabled() == null) throw bad("启用状态不能为空");
    return new DishTemplateSnapshotRequest(2, categoryId, name, description, price, tastes, meals,
        sortOrder, source.enabled(), ingredients(source.ingredients()), steps(source.cookingSteps()));
  }

  private List<DishTemplateIngredientSnapshotRequest> ingredients(
      List<DishTemplateIngredientSnapshotRequest> source) {
    if (source == null || source.isEmpty()) throw bad("模板至少需要1项食材或组件引用");
    if (source.size() > 100) throw bad("模板最多包含100项食材");
    Set<String> itemIds = new LinkedHashSet<>();
    List<DishTemplateIngredientSnapshotRequest> result = new ArrayList<>();
    for (DishTemplateIngredientSnapshotRequest item : source) {
      if (item == null) throw bad("食材信息不能为空");
      String itemId = text(item.itemId(), 500, "食材itemId");
      if (!itemIds.add(itemId)) throw bad("食材itemId不能重复");
      String name = text(item.ingredientName(), 100, "食材名称");
      String category = text(item.ingredientCategory(), 50, "食材分类");
      String status = text(item.quantityStatus(), 30, "食材数量状态").toUpperCase(Locale.ROOT);
      if (!QUANTITY_STATUSES.contains(status)) throw bad("食材数量状态不支持");
      BigDecimal quantity = item.quantity();
      String unit = optionalText(item.unit(), 20, "食材单位");
      String calcType = optionalText(item.calcType(), 20, "食材计算方式");
      if (calcType != null) calcType = calcType.toUpperCase(Locale.ROOT);
      if ("VERIFIED".equals(status)) {
        quantity = amount(quantity, false, "食材用量");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0 || unit == null || !CALC_TYPES.contains(calcType)) {
          throw bad("VERIFIED食材必须填写正数用量、单位和支持的计算方式");
        }
      } else if (quantity != null || unit != null || calcType != null) {
        throw bad("非VERIFIED食材不能填写采购用量、单位或计算方式");
      }
      Long componentId = item.componentTemplateId();
      BigDecimal multiplier = item.componentMultiplier();
      if (componentId != null) {
        positive(componentId, "组件模板ID必须为正整数");
        if (multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0) throw bad("组件引用倍数必须大于0");
      } else if (multiplier != null) {
        throw bad("非组件食材不能填写组件倍数");
      }
      result.add(new DishTemplateIngredientSnapshotRequest(itemId, name, category, status, quantity, unit,
          calcType, optionalText(item.sourceText(), 1000, "来源原料说明"),
          optionalText(item.sourceQuantityText(), 255, "来源批量用量"), componentId, multiplier,
          sortOrder(item.sortOrder(), "食材排序值")));
    }
    return List.copyOf(result);
  }

  private List<DishTemplateCookingStepSnapshotRequest> steps(
      List<DishTemplateCookingStepSnapshotRequest> source) {
    if (source == null) throw bad("制作步骤不能为空，请使用空数组表示暂无步骤");
    if (source.size() > 100) throw bad("制作步骤最多100项");
    Set<String> itemIds = new LinkedHashSet<>();
    List<DishTemplateCookingStepSnapshotRequest> result = new ArrayList<>();
    for (int index = 0; index < source.size(); index++) {
      DishTemplateCookingStepSnapshotRequest item = source.get(index);
      if (item == null) throw bad("制作步骤不能为空");
      String itemId = text(item.itemId(), 500, "步骤itemId");
      if (!itemIds.add(itemId)) throw bad("步骤itemId不能重复");
      if (item.stepNo() == null || item.stepNo() != index + 1) throw bad("制作步骤序号必须从1开始连续排列");
      if (item.durationSeconds() != null && item.durationSeconds() < 0) throw bad("步骤时长不能小于0");
      Long componentId = item.componentTemplateId();
      if (componentId != null) positive(componentId, "步骤组件模板ID必须为正整数");
      result.add(new DishTemplateCookingStepSnapshotRequest(itemId, item.stepNo(),
          optionalText(item.title(), 100, "步骤标题"), text(item.content(), 20_000, "步骤内容"),
          item.durationSeconds(), optionalText(item.temperatureText(), 100, "温度说明"),
          optionalText(item.heatLevel(), 50, "火候说明"), componentId));
    }
    return List.copyOf(result);
  }

  private static List<String> tags(List<String> source, int max, Set<String> allowed, String label) {
    if (source == null) throw bad(label + "不能为空");
    if (source.size() > max) throw bad(label + "最多" + max + "项");
    Set<String> result = new LinkedHashSet<>();
    for (String value : source) {
      String normalized = text(value, 20, label);
      if (allowed != null) {
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw bad("推荐餐次只支持早餐、午餐和晚餐");
      }
      result.add(normalized);
    }
    return List.copyOf(result);
  }

  private static String text(String value, int max, String label) {
    String normalized = optionalText(value, max, label);
    if (normalized == null) throw bad(label + "不能为空");
    return normalized;
  }

  private static String optionalText(String value, int max, String label) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.trim();
    if (normalized.length() > max) throw bad(label + "最多" + max + "个字符");
    return normalized;
  }

  private static Long positive(Long value, String message) {
    if (value == null || value <= 0) throw bad(message);
    return value;
  }

  private static Integer sortOrder(Integer value, String label) {
    if (value == null) throw bad(label + "不能为空");
    if (value < -1_000_000 || value > 1_000_000) throw bad(label + "超出范围");
    return value;
  }

  private static BigDecimal amount(BigDecimal value, boolean nullable, String label) {
    if (value == null) {
      if (nullable) return null;
      throw bad(label + "不能为空");
    }
    if (value.scale() > 4) throw bad(label + "小数位数超出范围");
    if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("99999999.9999")) > 0) {
      throw bad(label + "超出范围");
    }
    return value;
  }

  private static BusinessException bad(String message) {
    return new BusinessException(ErrorCode.BAD_REQUEST, message);
  }
}
