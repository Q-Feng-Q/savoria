package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

/**
 * 菜品持久化实体。
 */
@TableName("dishes")
public class DishEntity {

  /** 菜品 ID。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 商户 ID。 */
  @TableField("merchant_id")
  private Long merchantId;

  /** 分类 ID。 */
  @TableField("category_id")
  private Long categoryId;

  /** 菜品名称。 */
  @TableField("name")
  private String name;

  /** 菜品简介。 */
  @TableField("description")
  private String description;

  /** 菜品图片地址。 */
  @TableField("image_url")
  private String imageUrl;

  /** 基础价格。 */
  @TableField("base_price")
  private BigDecimal basePrice;

  /** 首次从平台模板导入时记录的来源模板 ID，手工菜品为空。 */
  @TableField("source_template_id")
  private Long sourceTemplateId;

  /** 菜品状态。 */
  @TableField("status")
  private String status;

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
   * 获取商户标识。
   *
   * @return 获取商户标识的结果
   */
  public Long getMerchantId() { return merchantId; }
  /**
   * 商户标识。
   */
  /**
   * 设置商户标识。
   *
   * @param merchantId 商户标识
   */
  public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
  /**
   * 获取Category标识。
   *
   * @return 获取Category标识的结果
   */
  public Long getCategoryId() { return categoryId; }
  /**
   * category标识。
   */
  /**
   * 设置Category标识。
   *
   * @param categoryId category标识
   */
  public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
  /**
   * 获取名称。
   *
   * @return 获取名称的结果
   */
  public String getName() { return name; }
  /**
   * 名称。
   */
  /**
   * 设置名称。
   *
   * @param name 名称
   */
  public void setName(String name) { this.name = name; }
  /**
   * 获取Description。
   *
   * @return 获取Description的结果
   */
  public String getDescription() { return description; }
  /**
   * description。
   */
  /**
   * 设置Description。
   *
   * @param description description
   */
  public void setDescription(String description) { this.description = description; }
  /**
   * 获取ImageUrl。
   *
   * @return 获取ImageUrl的结果
   */
  public String getImageUrl() { return imageUrl; }
  /**
   * imageUrl。
   */
  /**
   * 设置ImageUrl。
   *
   * @param imageUrl imageUrl
   */
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  /**
   * 获取BasePrice。
   *
   * @return 获取BasePrice的结果
   */
  public BigDecimal getBasePrice() { return basePrice; }
  /**
   * basePrice。
   */
  /**
   * 设置BasePrice。
   *
   * @param basePrice basePrice
   */
  public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
  /**
   * 获取来源平台模板 ID。
   * @return 来源平台模板 ID，手工菜品为空
   */
  public Long getSourceTemplateId() { return sourceTemplateId; }
  /**
   * 设置来源平台模板 ID。
   * @param sourceTemplateId 来源平台模板 ID
   */
  public void setSourceTemplateId(Long sourceTemplateId) { this.sourceTemplateId = sourceTemplateId; }
  /**
   * 获取状态。
   *
   * @return 获取状态的结果
   */
  public String getStatus() { return status; }
  /**
   * 状态。
   */
  /**
   * 设置状态。
   *
   * @param status 状态
   */
  public void setStatus(String status) { this.status = status; }
}
