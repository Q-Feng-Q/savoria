package com.familykitchen.dish.model.dto;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/**
 * 平台模板管理列表筛选和分页参数。
 * @param page 页码
 * @param pageSize 每页数量
 * @param keyword 菜名关键词
 * @param sourceType 来源类型
 * @param templateType 模板类型
 * @param dataStatus 数据完整状态
 * @param sourceCategory 来源分类
 * @param missingImage 是否缺少公开图片
 * @param missingSteps 是否缺少制作步骤
 */
@Schema(description = "平台模板管理查询条件")
public record AdminDishTemplateQuery(Integer page, Integer pageSize, String keyword, String sourceType,
    String templateType, String dataStatus, String sourceCategory, Boolean missingImage,
    Boolean missingSteps, String productType) {
  public AdminDishTemplateQuery(Integer page, Integer pageSize, String keyword, String sourceType,
      String templateType, String dataStatus, String sourceCategory, Boolean missingImage, Boolean missingSteps) {
    this(page, pageSize, keyword, sourceType, templateType, dataStatus, sourceCategory, missingImage, missingSteps, null);
  }
  /** 支持的来源类型。 */ private static final Set<String> SOURCE_TYPES = Set.of("COOK_LIKE_HOC", "LOCAL_EXTENSION");
  /** 支持的模板类型。 */ private static final Set<String> TEMPLATE_TYPES = Set.of("DISH", "COMPONENT");
  /** 支持的数据完整状态。 */ private static final Set<String> DATA_STATUSES = Set.of(
      "READY", "NEEDS_PURCHASE_DATA", "NEEDS_PRICE", "NEEDS_BOTH");

  /**
   * 校验枚举和分页边界，非法筛选直接返回明确的400错误。
   * @return 已完成校验的当前查询对象
   */
  public AdminDishTemplateQuery validated() {
    if (productType != null) com.familykitchen.dish.service.NourishmentFields.type(productType, null);
    checkEnum("sourceType", sourceType, SOURCE_TYPES);
    checkEnum("templateType", templateType, TEMPLATE_TYPES);
    checkEnum("dataStatus", dataStatus, DATA_STATUSES);
    if (normalizedPage() < 1) bad("page必须大于0");
    if (normalizedPageSize() < 1 || normalizedPageSize() > 100) bad("pageSize必须在1到100之间");
    return this;
  }

  /**
   * 获取应用默认值后的页码。
   * @return 应用默认值后的页码
   */
  public int normalizedPage() { return page == null ? 1 : page; }
  /**
   * 获取应用默认值后的每页数量。
   * @return 应用默认值后的每页数量
   */
  public int normalizedPageSize() { return pageSize == null ? 20 : pageSize; }
  /**
   * 计算数据库分页偏移量。
   * @return 数据库分页偏移量
   */
  public int offset() { return (normalizedPage() - 1) * normalizedPageSize(); }
  /**
   * 规范化菜名关键词。
   * @return 去除首尾空白后的关键词，空值返回null
   */
  public String normalizedKeyword() { return trimToNull(keyword); }
  /**
   * 规范化来源分类。
   * @return 去除首尾空白后的来源分类，空值返回null
   */
  public String normalizedSourceCategory() { return trimToNull(sourceCategory); }

  private static void checkEnum(String field, String value, Set<String> allowed) {
    if (value != null && !value.isBlank() && !allowed.contains(value)) {
      bad(field + "取值不合法，可选值：" + allowed);
    }
  }

  private static String trimToNull(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }

  private static void bad(String message) { throw new BusinessException(ErrorCode.BAD_REQUEST, message); }
}
