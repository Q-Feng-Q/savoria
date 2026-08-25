package com.familykitchen.dish.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
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
 * 模板菜品完整快照校验器。
 *
 * <p>该组件使用独立的严格 Jackson Reader 拒绝未知字段，并在进入业务事务前完成
 * UTF-8 大小、字段边界、标签集合和食材计量规则校验。它不会修改应用全局 ObjectMapper。</p>
 */
@Component
public class DishTemplateSnapshotValidator {
  private static final int MAX_SNAPSHOT_BYTES = 1024 * 1024;
  private static final Set<String> MEAL_TAGS = Set.of("BREAKFAST", "LUNCH", "DINNER");
  private static final Set<String> CALC_TYPES = Set.of("FIXED", "PER_PERSON", "NO_PURCHASE");

  private final ObjectMapper objectMapper;
  private final ObjectReader strictReader;

  /**
   * 创建快照校验器。
   * @param objectMapper 应用 Jackson 对象映射器，仅用于派生本组件的严格 Reader
   */
  public DishTemplateSnapshotValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.strictReader = objectMapper.readerFor(DishTemplateSnapshotRequest.class)
        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  /**
   * 严格解析、规范化并校验目标快照。
   *
   * @param targetSnapshot 客户端提交的目标快照 JSON
   * @return 可安全持久化和审核应用的规范化完整快照
   */
  public DishTemplateSnapshotRequest parseAndValidate(JsonNode targetSnapshot) {
    if (targetSnapshot == null || targetSnapshot.isNull() || !targetSnapshot.isObject()) {
      throw badRequest("模板菜品修改内容不完整");
    }
    try {
      if (objectMapper.writeValueAsBytes(targetSnapshot).length > MAX_SNAPSHOT_BYTES) {
        throw badRequest("模板菜品目标快照不能超过1MB");
      }
      DishTemplateSnapshotRequest source = strictReader.readValue(targetSnapshot);
      return normalizeAndValidate(source);
    } catch (BusinessException exception) {
      throw exception;
    } catch (com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException exception) {
      throw badRequest("模板菜品修改内容包含未知字段：" + exception.getPropertyName());
    } catch (Exception exception) {
      throw badRequest("模板菜品修改内容不完整或格式错误");
    }
  }

  private DishTemplateSnapshotRequest normalizeAndValidate(DishTemplateSnapshotRequest source) {
    if (source == null) throw badRequest("模板菜品修改内容不完整");
    if (!Integer.valueOf(1).equals(source.schemaVersion())) {
      throw badRequest("模板菜品快照版本不支持");
    }
    Long categoryId = requirePositive(source.categoryId(), "模板分类ID必须为正整数");
    String name = requireText(source.name(), 100, "菜品名称");
    String description = requireText(source.description(), 255, "菜品简介");
    String imageUrl = requireText(source.imageUrl(), 500, "菜品图片");
    if (!imageUrl.startsWith("/uploads/") && !imageUrl.startsWith("/images/")) {
      throw badRequest("菜品图片必须使用本地上传地址");
    }
    String imageSourceUrl = requireText(source.imageSourceUrl(), 1000, "图片来源地址");
    String imageAuthor = requireText(source.imageAuthor(), 255, "图片作者");
    String imageLicense = requireText(source.imageLicense(), 255, "图片授权说明");
    BigDecimal referencePrice = requireAmount(source.referencePrice(), true, "参考价格");
    List<String> tasteTags = normalizeTags(source.tasteTags(), 10, null, "口味标签");
    List<String> mealTags = normalizeTags(source.mealTags(), 3, MEAL_TAGS, "推荐餐次");
    Integer sortOrder = requireSortOrder(source.sortOrder(), "模板排序值");
    if (source.enabled() == null) throw badRequest("启用状态不能为空");
    List<DishTemplateIngredientSnapshotRequest> ingredients = normalizeIngredients(source.ingredients());
    return new DishTemplateSnapshotRequest(1, categoryId, name, description, imageUrl, imageSourceUrl,
        imageAuthor, imageLicense, referencePrice, tasteTags, mealTags, sortOrder, source.enabled(), ingredients);
  }

  private List<DishTemplateIngredientSnapshotRequest> normalizeIngredients(
      List<DishTemplateIngredientSnapshotRequest> source) {
    if (source == null || source.isEmpty()) throw badRequest("模板菜品至少需要1项食材");
    if (source.size() > 100) throw badRequest("模板菜品最多包含100项食材");
    Set<String> names = new LinkedHashSet<>();
    List<DishTemplateIngredientSnapshotRequest> result = new ArrayList<>(source.size());
    for (DishTemplateIngredientSnapshotRequest item : source) {
      if (item == null) throw badRequest("食材信息不能为空");
      String name = requireText(item.ingredientName(), 100, "食材名称");
      if (!names.add(name.toLowerCase(Locale.ROOT))) throw badRequest("食材名称不能重复");
      String category = requireText(item.ingredientCategory(), 50, "食材分类");
      String unit = requireText(item.unit(), 20, "食材单位");
      String calcType = requireText(item.calcType(), 20, "食材计算方式").toUpperCase(Locale.ROOT);
      if (!CALC_TYPES.contains(calcType)) throw badRequest("食材计算方式不支持");
      BigDecimal quantity = requireAmount(item.quantity(), false, "食材用量");
      if ("NO_PURCHASE".equals(calcType) && quantity.compareTo(BigDecimal.ZERO) != 0) {
        throw badRequest("无需采购的食材用量必须为0");
      }
      if (!"NO_PURCHASE".equals(calcType) && quantity.compareTo(BigDecimal.ZERO) <= 0) {
        throw badRequest("固定或按人数计算的食材用量必须大于0");
      }
      result.add(new DishTemplateIngredientSnapshotRequest(name, category, quantity, unit, calcType,
          requireSortOrder(item.sortOrder(), "食材排序值")));
    }
    return List.copyOf(result);
  }

  private List<String> normalizeTags(List<String> source, int maxSize, Set<String> allowed, String label) {
    if (source == null) throw badRequest(label + "不能为空");
    if (source.size() > maxSize) throw badRequest(label + "最多" + maxSize + "项");
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (String value : source) {
      String normalized = requireText(value, 20, label);
      if (allowed != null) {
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw badRequest("推荐餐次只支持早餐、午餐和晚餐");
      }
      result.add(normalized);
    }
    return List.copyOf(result);
  }

  private static String requireText(String value, int maxLength, String label) {
    if (value == null || value.trim().isEmpty()) throw badRequest(label + "不能为空");
    String normalized = value.trim();
    if (normalized.length() > maxLength) throw badRequest(label + "最多" + maxLength + "个字符");
    return normalized;
  }

  private static Long requirePositive(Long value, String message) {
    if (value == null || value <= 0) throw badRequest(message);
    return value;
  }

  private static Integer requireSortOrder(Integer value, String label) {
    if (value == null) throw badRequest(label + "不能为空");
    if (value < -1_000_000 || value > 1_000_000) throw badRequest(label + "超出范围");
    return value;
  }

  private static BigDecimal requireAmount(BigDecimal value, boolean allowZero, String label) {
    if (value == null) throw badRequest(label + "不能为空");
    if (value.scale() > 2) throw badRequest(label + "最多保留2位小数");
    if (value.compareTo(BigDecimal.ZERO) < 0
        || value.compareTo(new BigDecimal("99999999.99")) > 0) {
      throw badRequest(label + "超出范围");
    }
    return value;
  }

  private static BusinessException badRequest(String message) {
    return new BusinessException(ErrorCode.BAD_REQUEST, message);
  }
}
