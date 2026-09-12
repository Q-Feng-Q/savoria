package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 平台菜品模板列表项。
 * @param templateId 模板 ID
 * @param templateCode 稳定模板编码
 * @param categoryId 模板分类 ID
 * @param categoryName 模板分类名称
 * @param name 菜品名称
 * @param description 菜品简介
 * @param imageUrl 本地图片访问地址
 * @param referencePrice 参考价格
 * @param tasteTags 口味标签
 * @param mealTags 推荐餐次
 * @param sourceCategory 来源原始分类
 * @param templateType 模板类型
 * @param dataStatus 数据完整状态
 * @param procurementReady 采购用量是否就绪
 * @param imageRightsStatus 图片权利状态
 * @param missingSteps 是否缺少制作步骤
 * @param version 模板并发版本号
 * @param ingredientCount 食材数量
 * @param imported 当前商户是否已导入
 */
@Schema(description = "平台菜品模板列表项")
public record DishTemplateView(Long templateId, String templateCode, Long categoryId, String categoryName,
    String name, String description, String imageUrl, BigDecimal referencePrice, List<String> tasteTags,
    List<String> mealTags, String sourceCategory, String templateType, String dataStatus,
    boolean procurementReady, String imageRightsStatus, boolean missingSteps, Long version,
    Integer ingredientCount, boolean imported) { }
