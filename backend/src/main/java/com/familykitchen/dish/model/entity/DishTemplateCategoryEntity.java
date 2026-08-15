package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

/** 平台菜品模板分类数据库实体。 */
@TableName("dish_template_categories")
@Schema(description = "平台菜品模板分类实体")
public class DishTemplateCategoryEntity {
  /** 数据库主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  @Schema(description = "模板分类ID", example = "1")
  private Long id;
  /** 稳定分类编码。 */
  @Schema(description = "稳定分类编码", example = "HOME_HOT")
  private String code;
  /** 名称。 */
  @Schema(description = "分类名称", example = "家常热菜")
  private String name;
  /** 排序值。 */
  @Schema(description = "排序值", example = "10")
  private Integer sortOrder;
  /** 是否启用。 */
  @Schema(description = "是否启用")
  private Boolean enabled;
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
   * 获取稳定分类编码。
   * @return 稳定分类编码
   */
  public String getCode() { return code; }
  /**
   * 设置稳定分类编码。
   * @param code 稳定分类编码
   */
  public void setCode(String code) { this.code = code; }
  /**
   * 获取名称。
   * @return 名称
   */
  public String getName() { return name; }
  /**
   * 设置名称。
   * @param name 名称
   */
  public void setName(String name) { this.name = name; }
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
  /**
   * 获取是否启用。
   * @return 是否启用
   */
  public Boolean getEnabled() { return enabled; }
  /**
   * 设置是否启用。
   * @param enabled 是否启用
   */
  public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
