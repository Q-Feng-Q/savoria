package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 菜品制作步骤持久化实体。
 */
@TableName("dish_cooking_steps")
@Schema(description = "商户菜品制作步骤实体")
public class DishCookingStepEntity {

  /**
   * 标识。
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /**
   * 菜品标识。
   */
  private Long dishId;
  /**
   * stepNo。
   */
  private Integer stepNo;
  /**
   * title。
   */
  private String title;
  /**
   * content。
   */
  private String content;
  /** 制作持续秒数。 */
  private Integer durationSeconds;
  /** 制作温度说明。 */
  private String temperatureText;
  /** 制作火候说明。 */
  private String heatLevel;
  /** 来源平台模板步骤ID。 */
  private Long sourceTemplateStepId;
  /** 来源配料组件模板ID。 */
  private Long componentTemplateId;
  /** 来源步骤简短追踪说明。 */
  private String sourceNote;

  /**
   * 获取标识。
   *
   * @return 获取标识的结果
   */
  public Long getId() { return id; }
  /**
   * 标识。
   */
  /**
   * 设置标识。
   *
   * @param id 标识
   */
  public void setId(Long id) { this.id = id; }
  /**
   * 获取菜品标识。
   *
   * @return 获取菜品标识的结果
   */
  public Long getDishId() { return dishId; }
  /**
   * 菜品标识。
   */
  /**
   * 设置菜品标识。
   *
   * @param dishId 菜品标识
   */
  public void setDishId(Long dishId) { this.dishId = dishId; }
  /**
   * 获取StepNo。
   *
   * @return 获取StepNo的结果
   */
  public Integer getStepNo() { return stepNo; }
  /**
   * stepNo。
   */
  /**
   * 设置StepNo。
   *
   * @param stepNo stepNo
   */
  public void setStepNo(Integer stepNo) { this.stepNo = stepNo; }
  /**
   * 获取Title。
   *
   * @return 获取Title的结果
   */
  public String getTitle() { return title; }
  /**
   * title。
   */
  /**
   * 设置Title。
   *
   * @param title title
   */
  public void setTitle(String title) { this.title = title; }
  /**
   * 获取Content。
   *
   * @return 获取Content的结果
   */
  public String getContent() { return content; }
  /**
   * content。
   */
  /**
   * 设置Content。
   *
   * @param content content
   */
  public void setContent(String content) { this.content = content; }
  /**
   * 获取制作持续秒数。
   *
   * @return 制作持续秒数
   */
  public Integer getDurationSeconds() { return durationSeconds; }
  /**
   * 设置制作持续秒数。
   *
   * @param durationSeconds 制作持续秒数
   */
  public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
  /**
   * 获取制作温度说明。
   *
   * @return 制作温度说明
   */
  public String getTemperatureText() { return temperatureText; }
  /**
   * 设置制作温度说明。
   *
   * @param temperatureText 制作温度说明
   */
  public void setTemperatureText(String temperatureText) { this.temperatureText = temperatureText; }
  /**
   * 获取制作火候说明。
   *
   * @return 制作火候说明
   */
  public String getHeatLevel() { return heatLevel; }
  /**
   * 设置制作火候说明。
   *
   * @param heatLevel 制作火候说明
   */
  public void setHeatLevel(String heatLevel) { this.heatLevel = heatLevel; }
  /**
   * 获取来源平台模板步骤ID。
   *
   * @return 来源平台模板步骤ID
   */
  public Long getSourceTemplateStepId() { return sourceTemplateStepId; }
  /**
   * 设置来源平台模板步骤ID。
   *
   * @param sourceTemplateStepId 来源平台模板步骤ID
   */
  public void setSourceTemplateStepId(Long sourceTemplateStepId) { this.sourceTemplateStepId = sourceTemplateStepId; }
  /**
   * 获取来源配料组件模板ID。
   *
   * @return 来源配料组件模板ID
   */
  public Long getComponentTemplateId() { return componentTemplateId; }
  /**
   * 设置来源配料组件模板ID。
   *
   * @param componentTemplateId 来源配料组件模板ID
   */
  public void setComponentTemplateId(Long componentTemplateId) { this.componentTemplateId = componentTemplateId; }
  /**
   * 获取来源步骤简短追踪说明。
   *
   * @return 来源步骤简短追踪说明
   */
  public String getSourceNote() { return sourceNote; }
  /**
   * 设置来源步骤简短追踪说明。
   *
   * @param sourceNote 来源步骤简短追踪说明
   */
  public void setSourceNote(String sourceNote) { this.sourceNote = sourceNote; }
}
