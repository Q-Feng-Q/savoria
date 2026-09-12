package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 平台菜品模板的食材用量数据库实体。 */
@TableName("dish_template_ingredients")
@Schema(description = "平台菜品模板食材实体")
public class DishTemplateIngredientEntity {
  /** 数据库主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 模板ID。 */
  private Long templateId;
  /** 食材名称。 */
  private String ingredientName;
  /** 食材分类。 */
  private String ingredientCategory;
  /** 默认用量。 */
  private BigDecimal quantity;
  /** 计量单位。 */
  private String unit;
  /** 用量计算方式。 */
  private String calcType;
  /** 归一化来源原料行。 */
  private String sourceText;
  /** 来源批量用量原文。 */
  private String sourceQuantityText;
  /** 数量状态。 */
  private String quantityStatus;
  /** 引用组件模板ID。 */
  private Long componentTemplateId;
  /** 来源原料行稳定键。 */
  private String sourceLineKey;
  /** 组件引用出现键。 */
  private String componentOccurrenceKey;
  /** 经验证的组件引用倍数。 */
  private BigDecimal componentMultiplier;
  /** 排序值。 */
  private Integer sortOrder;
  /**
   * 获取数据库主键。
   * @return 数据库主键
   */
  public Long getId() { return id; }
  /**
   * 设置数据库主键。
   * @param id 数据库主键
   */
  public void setId(Long id) { this.id = id; }
  /**
   * 获取模板ID。
   * @return 模板ID
   */
  public Long getTemplateId() { return templateId; }
  /**
   * 设置模板ID。
   * @param templateId 模板ID
   */
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  /**
   * 获取食材名称。
   * @return 食材名称
   */
  public String getIngredientName() { return ingredientName; }
  /**
   * 设置食材名称。
   * @param ingredientName 食材名称
   */
  public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
  /**
   * 获取食材分类。
   * @return 食材分类
   */
  public String getIngredientCategory() { return ingredientCategory; }
  /**
   * 设置食材分类。
   * @param ingredientCategory 食材分类
   */
  public void setIngredientCategory(String ingredientCategory) { this.ingredientCategory = ingredientCategory; }
  /**
   * 获取默认用量。
   * @return 默认用量
   */
  public BigDecimal getQuantity() { return quantity; }
  /**
   * 设置默认用量。
   * @param quantity 默认用量
   */
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  /**
   * 获取计量单位。
   * @return 计量单位
   */
  public String getUnit() { return unit; }
  /**
   * 设置计量单位。
   * @param unit 计量单位
   */
  public void setUnit(String unit) { this.unit = unit; }
  /**
   * 获取用量计算方式。
   * @return 用量计算方式
   */
  public String getCalcType() { return calcType; }
  /**
   * 设置用量计算方式。
   * @param calcType 用量计算方式
   */
  public void setCalcType(String calcType) { this.calcType = calcType; }
  /**
   * 获取归一化来源原料行。
   *
   * @return 归一化来源原料行
   */
  public String getSourceText() { return sourceText; }
  /**
   * 设置归一化来源原料行。
   *
   * @param sourceText 归一化来源原料行
   */
  public void setSourceText(String sourceText) { this.sourceText = sourceText; }
  /**
   * 获取来源批量用量原文。
   *
   * @return 来源批量用量原文
   */
  public String getSourceQuantityText() { return sourceQuantityText; }
  /**
   * 设置来源批量用量原文。
   *
   * @param sourceQuantityText 来源批量用量原文
   */
  public void setSourceQuantityText(String sourceQuantityText) { this.sourceQuantityText = sourceQuantityText; }
  /**
   * 获取数量状态。
   *
   * @return 数量状态
   */
  public String getQuantityStatus() { return quantityStatus; }
  /**
   * 设置数量状态。
   *
   * @param quantityStatus 数量状态
   */
  public void setQuantityStatus(String quantityStatus) { this.quantityStatus = quantityStatus; }
  /**
   * 获取引用组件模板ID。
   *
   * @return 引用组件模板ID
   */
  public Long getComponentTemplateId() { return componentTemplateId; }
  /**
   * 设置引用组件模板ID。
   *
   * @param componentTemplateId 引用组件模板ID
   */
  public void setComponentTemplateId(Long componentTemplateId) { this.componentTemplateId = componentTemplateId; }
  /**
   * 获取来源原料行稳定键。
   *
   * @return 来源原料行稳定键
   */
  public String getSourceLineKey() { return sourceLineKey; }
  /**
   * 设置来源原料行稳定键。
   *
   * @param sourceLineKey 来源原料行稳定键
   */
  public void setSourceLineKey(String sourceLineKey) { this.sourceLineKey = sourceLineKey; }
  /**
   * 获取组件引用出现键。
   *
   * @return 组件引用出现键
   */
  public String getComponentOccurrenceKey() { return componentOccurrenceKey; }
  /**
   * 设置组件引用出现键。
   *
   * @param componentOccurrenceKey 组件引用出现键
   */
  public void setComponentOccurrenceKey(String componentOccurrenceKey) { this.componentOccurrenceKey = componentOccurrenceKey; }
  /**
   * 获取经验证的组件引用倍数。
   *
   * @return 经验证的组件引用倍数
   */
  public BigDecimal getComponentMultiplier() { return componentMultiplier; }
  /**
   * 设置经验证的组件引用倍数。
   *
   * @param componentMultiplier 经验证的组件引用倍数
   */
  public void setComponentMultiplier(BigDecimal componentMultiplier) { this.componentMultiplier = componentMultiplier; }
  /**
   * 获取排序值。
   * @return 排序值
   */
  public Integer getSortOrder() { return sortOrder; }
  /**
   * 设置排序值。
   * @param sortOrder 排序值
   */
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
