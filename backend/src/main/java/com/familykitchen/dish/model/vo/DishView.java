package com.familykitchen.dish.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 封装返回给调用方的菜品数据。
 *
 * @param dishId 菜品标识
 * @param categoryId category标识
 * @param categoryName 分类名称
 * @param categorySortOrder 分类排序
 * @param name 名称
 * @param description description
 * @param imageUrl imageUrl
 * @param price price
 * @param status 状态
 * @param sourceTemplateId 来源平台模板 ID，手工菜品为空
 * @param templateImported 是否由平台模板导入
 * @param featuredAt 商户推荐时间
 * @param featured 是否为商户推荐菜
 * @param deletedAt 逻辑删除时间
 * @param deletedByName 删除操作人显示名
 */
public record DishView(
    Long dishId,
    Long categoryId,
    String categoryName,
    Integer categorySortOrder,
    String name,
    String description,
    String imageUrl,
    BigDecimal price,
    String status,
    Long sourceTemplateId,
    boolean templateImported,
    LocalDateTime featuredAt,
    boolean featured,
    LocalDateTime deletedAt,
    String deletedByName,
    String productType, String nourishmentDescription, String servingAdvice, String precautions
) {
  /** Backward-compatible constructor for clients without nourishment fields. */
  public DishView(Long dishId,
    Long categoryId,
    String categoryName,
    Integer categorySortOrder,
    String name,
    String description,
    String imageUrl,
    BigDecimal price,
    String status,
    Long sourceTemplateId,
    boolean templateImported,
    LocalDateTime featuredAt,
    boolean featured,
    LocalDateTime deletedAt,
    String deletedByName) {
    this(dishId, categoryId, categoryName, categorySortOrder, name, description, imageUrl, price, status, sourceTemplateId, templateImported, featuredAt, featured, deletedAt, deletedByName, "NORMAL", null, null, null);
  }

}

