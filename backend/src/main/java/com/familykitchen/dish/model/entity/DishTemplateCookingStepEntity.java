package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

/** 平台菜品模板的结构化制作步骤实体。 */
@TableName("dish_template_cooking_steps")
@Schema(description = "平台菜品模板制作步骤实体")
public class DishTemplateCookingStepEntity {
  /** 数据库主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 跨快照保持稳定的步骤项键。 */
  private String itemKey;
  /** 所属模板ID。 */
  private Long templateId;
  /** 连续步骤序号。 */
  private Integer stepNo;
  /** 步骤标题。 */
  private String title;
  /** 归纳后的完整操作。 */
  private String content;
  /** 简短来源定位和事实摘要。 */
  private String sourceText;
  /** 持续秒数。 */
  private Integer durationSeconds;
  /** 温度说明。 */
  private String temperatureText;
  /** 火候说明。 */
  private String heatLevel;
  /** 当前步骤使用的组件模板ID。 */
  private Long componentTemplateId;

  /**

   * 获取数据库主键。

   *

   * @return 数据库主键

   */

  public Long getId() { return id; }
  /**
   * 设置数据库主键。
   *
   * @param id 数据库主键
   */
  public void setId(Long id) { this.id = id; }
  /**
   * 获取稳定步骤项键。
   * @return 稳定步骤项键
   */
  public String getItemKey() { return itemKey; }
  /**
   * 设置稳定步骤项键。
   * @param itemKey 稳定步骤项键
   */
  public void setItemKey(String itemKey) { this.itemKey = itemKey; }
  /**
   * 获取所属模板ID。
   *
   * @return 所属模板ID
   */
  public Long getTemplateId() { return templateId; }
  /**
   * 设置所属模板ID。
   *
   * @param templateId 所属模板ID
   */
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  /**
   * 获取步骤序号。
   *
   * @return 步骤序号
   */
  public Integer getStepNo() { return stepNo; }
  /**
   * 设置步骤序号。
   *
   * @param stepNo 步骤序号
   */
  public void setStepNo(Integer stepNo) { this.stepNo = stepNo; }
  /**
   * 获取步骤标题。
   *
   * @return 步骤标题
   */
  public String getTitle() { return title; }
  /**
   * 设置步骤标题。
   *
   * @param title 步骤标题
   */
  public void setTitle(String title) { this.title = title; }
  /**
   * 获取完整操作。
   *
   * @return 完整操作
   */
  public String getContent() { return content; }
  /**
   * 设置完整操作。
   *
   * @param content 完整操作
   */
  public void setContent(String content) { this.content = content; }
  /**
   * 获取来源摘要。
   *
   * @return 来源摘要
   */
  public String getSourceText() { return sourceText; }
  /**
   * 设置来源摘要。
   *
   * @param sourceText 来源摘要
   */
  public void setSourceText(String sourceText) { this.sourceText = sourceText; }
  /**
   * 获取持续秒数。
   *
   * @return 持续秒数
   */
  public Integer getDurationSeconds() { return durationSeconds; }
  /**
   * 设置持续秒数。
   *
   * @param durationSeconds 持续秒数
   */
  public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
  /**
   * 获取温度说明。
   *
   * @return 温度说明
   */
  public String getTemperatureText() { return temperatureText; }
  /**
   * 设置温度说明。
   *
   * @param temperatureText 温度说明
   */
  public void setTemperatureText(String temperatureText) { this.temperatureText = temperatureText; }
  /**
   * 获取火候说明。
   *
   * @return 火候说明
   */
  public String getHeatLevel() { return heatLevel; }
  /**
   * 设置火候说明。
   *
   * @param heatLevel 火候说明
   */
  public void setHeatLevel(String heatLevel) { this.heatLevel = heatLevel; }
  /**
   * 获取组件模板ID。
   *
   * @return 组件模板ID
   */
  public Long getComponentTemplateId() { return componentTemplateId; }
  /**
   * 设置组件模板ID。
   *
   * @param componentTemplateId 组件模板ID
   */
  public void setComponentTemplateId(Long componentTemplateId) { this.componentTemplateId = componentTemplateId; }
}
