package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 平台菜品模板可检索名称别名实体。 */
@TableName("dish_template_name_aliases")
@Schema(description = "平台菜品模板名称别名实体")
public class DishTemplateNameAliasEntity {
  /** 数据库主键。 */ @TableId(value = "id", type = IdType.AUTO) private Long id;
  /** 模板ID。 */ private Long templateId;
  /** 名称别名。 */ private String aliasName;
  /** 规范化全局唯一别名。 */ private String normalizedAliasName;
  /** 别名类型。 */ private String aliasType;
  /** 创建时间。 */ private LocalDateTime createdAt;

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
   * 获取模板ID。
   *
   * @return 模板ID
   */
  public Long getTemplateId() { return templateId; }
  /**
   * 设置模板ID。
   *
   * @param templateId 模板ID
   */
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  /**
   * 获取名称别名。
   *
   * @return 名称别名
   */
  public String getAliasName() { return aliasName; }
  /**
   * 设置名称别名。
   *
   * @param aliasName 名称别名
   */
  public void setAliasName(String aliasName) { this.aliasName = aliasName; }
  /**
   * 获取规范化别名。
   *
   * @return 规范化别名
   */
  public String getNormalizedAliasName() { return normalizedAliasName; }
  /**
   * 设置规范化别名。
   *
   * @param normalizedAliasName 规范化别名
   */
  public void setNormalizedAliasName(String normalizedAliasName) { this.normalizedAliasName = normalizedAliasName; }
  /**
   * 获取别名类型。
   *
   * @return 别名类型
   */
  public String getAliasType() { return aliasType; }
  /**
   * 设置别名类型。
   *
   * @param aliasType 别名类型
   */
  public void setAliasType(String aliasType) { this.aliasType = aliasType; }
  /**
   * 获取创建时间。
   *
   * @return 创建时间
   */
  public LocalDateTime getCreatedAt() { return createdAt; }
  /**
   * 设置创建时间。
   *
   * @param createdAt 创建时间
   */
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
