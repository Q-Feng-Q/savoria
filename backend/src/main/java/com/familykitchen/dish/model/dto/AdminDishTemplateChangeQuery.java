package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 平台管理员模板菜品修改申请分页筛选。
 * @param status 审核状态
 * @param merchantId 商户 ID
 * @param templateId 模板菜品 ID
 * @param keyword 模板菜名关键词
 * @param page 页码
 * @param pageSize 每页数量
 */
@Schema(description = "平台模板菜品修改申请查询参数")
public record AdminDishTemplateChangeQuery(String status, Long merchantId, Long templateId, String keyword,
    Integer page, Integer pageSize) {
  /**
   * 返回规范化页码。
   * @return 从 1 开始的规范化页码
   */
  public int normalizedPage() { return page == null || page < 1 ? 1 : page; }
  /**
   * 返回规范化每页数量。
   * @return 限制在 1 至 100 的每页数量
   */
  public int normalizedPageSize() { return pageSize == null ? 20 : Math.max(1, Math.min(100, pageSize)); }
  /**
   * 返回分页偏移量。
   * @return MySQL 分页偏移量
   */
  public int offset() { return (normalizedPage() - 1) * normalizedPageSize(); }
  /**
   * 返回规范化关键词。
   * @return 去除首尾空白的关键词，无有效内容时为空
   */
  public String normalizedKeyword() { return text(keyword); }
  /**
   * 返回规范化审核状态。
   * @return 转为大写的审核状态，无筛选时为空
   */
  public String normalizedStatus() { return text(status) == null ? null : text(status).toUpperCase(); }
  private static String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
