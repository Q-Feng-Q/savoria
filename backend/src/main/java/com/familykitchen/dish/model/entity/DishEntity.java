package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品持久化实体。
 */
@TableName("dishes")
public class DishEntity {

  @TableField("product_type")
  private String productType;
  public String getProductType() { return productType == null ? "NORMAL" : productType; }
  public void setProductType(String value) { this.productType = value; }

  @TableField("nourishment_description")
  private String nourishmentDescription;
  public String getNourishmentDescription() { return nourishmentDescription; }
  public void setNourishmentDescription(String value) { this.nourishmentDescription = value; }

  @TableField("serving_advice")
  private String servingAdvice;
  public String getServingAdvice() { return servingAdvice; }
  public void setServingAdvice(String value) { this.servingAdvice = value; }

  @TableField("precautions")
  private String precautions;
  public String getPrecautions() { return precautions; }
  public void setPrecautions(String value) { this.precautions = value; }


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

  /** 商户推荐时间；为空表示非推荐菜。 */
  @TableField("featured_at")
  private LocalDateTime featuredAt;

  /** 菜品状态。 */
  @TableField("status")
  private String status;

  /** 逻辑删除时间。 */
  @TableField("deleted_at")
  private LocalDateTime deletedAt;

  /** 执行逻辑删除的用户 ID。 */
  @TableField("deleted_by")
  private Long deletedBy;

  /** 删除操作人展示名，仅用于查询投影。 */
  @TableField(exist = false)
  private String deletedByName;

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
   * 获取商户推荐时间。
   * @return 商户推荐时间；非推荐菜为空
   */
  public LocalDateTime getFeaturedAt() { return featuredAt; }
  /**
   * 设置商户推荐时间。
   * @param featuredAt 商户推荐时间
   */
  public void setFeaturedAt(LocalDateTime featuredAt) { this.featuredAt = featuredAt; }
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
  /** Returns logical deletion time.
   * @return deletion time, or null
   */
  public LocalDateTime getDeletedAt() { return deletedAt; }
  /** Sets logical deletion time.
   * @param deletedAt deletion time
   */
  public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
  /** Returns deletion operator identifier.
   * @return operator identifier, or null
   */
  public Long getDeletedBy() { return deletedBy; }
  /** Sets deletion operator identifier.
   * @param deletedBy operator identifier
   */
  public void setDeletedBy(Long deletedBy) { this.deletedBy = deletedBy; }
  /** Returns deletion operator display name.
   * @return operator display name, or null
   */
  public String getDeletedByName() { return deletedByName; }
  /** Sets deletion operator display name.
   * @param deletedByName operator display name
   */
  public void setDeletedByName(String deletedByName) { this.deletedByName = deletedByName; }
}
