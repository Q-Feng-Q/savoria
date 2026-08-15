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
