package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 模板审核快照中的稳定食材项。
 * @param itemId 跨快照稳定的食材项ID
 * @param ingredientName 食材名称
 * @param ingredientCategory 食材分类
 * @param quantityStatus 数量状态
 * @param quantity 可空家庭采购数量
 * @param unit 可空计量单位
 * @param calcType 可空计算方式
 * @param sourceText 来源原料说明
 * @param sourceQuantityText 来源批量用量
 * @param componentTemplateId 引用组件模板ID
 * @param componentMultiplier 组件展开倍数
 * @param sortOrder 排序值
 */
@Schema(description = "模板审核快照食材项")
public record DishTemplateIngredientSnapshotRequest(String itemId, String ingredientName,
    String ingredientCategory, String quantityStatus, BigDecimal quantity, String unit, String calcType,
    String sourceText, String sourceQuantityText, Long componentTemplateId,
    BigDecimal componentMultiplier, Integer sortOrder) { }
