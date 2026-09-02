package com.familykitchen.dish.model.vo;

import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 平台菜品模板完整详情。
 * @param templateId 模板 ID
 * @param templateCode 稳定模板编码
 * @param categoryId 模板分类 ID
 * @param categoryName 模板分类名称
 * @param name 菜品名称
 * @param description 菜品简介
 * @param imageUrl 本地图片访问地址
 * @param imageSourceUrl 图片原始来源页面
 * @param imageAuthor 图片作者或来源平台
 * @param imageLicense 图片许可证
 * @param referencePrice 参考价格
 * @param tasteTags 口味标签
 * @param mealTags 推荐餐次
 * @param sourceCategory 来源原始分类
 * @param templateType 模板类型
 * @param dataStatus 数据完整状态
 * @param procurementReady 采购用量是否就绪
 * @param imageRightsStatus 图片权利状态
 * @param sortOrder 模板排序值
 * @param enabled 模板是否启用
 * @param version 模板并发版本号
 * @param imported 当前商户是否已导入
 * @param ingredients 模板食材明细
 * @param cookingSteps 按序排列的制作步骤
 */
@Schema(description = "平台菜品模板详情")
public record DishTemplateDetailView(Long templateId, String templateCode, Long categoryId, String categoryName,
    String name, String description, String imageUrl, String imageSourceUrl, String imageAuthor,
    String imageLicense, BigDecimal referencePrice, List<String> tasteTags, List<String> mealTags,
    String sourceCategory, String templateType, String dataStatus, boolean procurementReady,
    String imageRightsStatus, Integer sortOrder, boolean enabled, Long version, boolean imported,
    List<DishTemplateIngredientEntity> ingredients, List<DishTemplateCookingStepEntity> cookingSteps) { }
