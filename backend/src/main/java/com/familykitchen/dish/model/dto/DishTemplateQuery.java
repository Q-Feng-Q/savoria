package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商户查询平台菜品模板时使用的筛选和分页参数。
 * @param categoryId 模板分类 ID
 * @param keyword 菜名关键词
 * @param imported 当前商户导入状态筛选
 * @param page 页码
 * @param pageSize 每页数量
 */
@Schema(description = "平台菜品模板查询参数")
public record DishTemplateQuery(
    @Schema(description = "模板分类ID") Long categoryId,
    @Schema(description = "菜名关键词") String keyword,
    @Schema(description = "是否已被当前商户导入") Boolean imported,
    @Schema(description = "页码，从1开始", defaultValue = "1") Integer page,
    @Schema(description = "每页数量，最大100", defaultValue = "20") Integer pageSize) {
  /**
   * 规范化页码。
   * @return 规范化后从 1 开始的页码
   */
  public int normalizedPage() { return page == null || page < 1 ? 1 : page; }
  /**
   * 规范化每页数量。
   * @return 规范化后 1 至 100 的每页数量
   */
  public int normalizedPageSize() { return pageSize == null ? 20 : Math.max(1, Math.min(100, pageSize)); }
  /**
   * 计算分页偏移量。
   * @return MySQL 分页起始偏移量
   */
  public int offset() { return (normalizedPage() - 1) * normalizedPageSize(); }
  /**
   * 规范化搜索关键词。
   * @return 去除首尾空白后的关键词，无有效内容时为空
   */
  public String normalizedKeyword() { return keyword == null || keyword.isBlank() ? null : keyword.trim(); }
}
