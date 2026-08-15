package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 平台菜品模板数据库实体，不包含商户制作步骤。 */
@TableName("dish_templates")
@Schema(description = "平台菜品模板实体")
public class DishTemplateEntity {
  /** 数据库主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 稳定模板编码。 */
  private String templateCode;
  /** 模板分类ID。 */
  private Long categoryId;
  /** 模板分类名称。 */
  private String categoryName;
  /** 名称。 */
  private String name;
  /** 简介。 */
  private String description;
  /** 本地图片访问地址。 */
  private String imageUrl;
  /** 图片原始来源页面。 */
  private String imageSourceUrl;
  /** 图片作者或来源平台。 */
  private String imageAuthor;
  /** 图片许可证。 */
  private String imageLicense;
  /** 参考价格。 */
  private BigDecimal referencePrice;
  /** 口味标签JSON。 */
  private String tasteTags;
  /** 推荐餐次JSON。 */
  private String mealTags;
  /** 排序值。 */
  private Integer sortOrder;
  /** 是否启用。 */
  private Boolean enabled;
  /** 当前商户是否已导入。 */
  private Boolean imported;
  /** 食材数量。 */
  private Integer ingredientCount;
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
   * 获取稳定模板编码。
   * @return 稳定模板编码
   */
  public String getTemplateCode() { return templateCode; }
  /**
   * 设置稳定模板编码。
   * @param templateCode 稳定模板编码
   */
  public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
  /**
   * 获取模板分类ID。
   * @return 模板分类ID
   */
  public Long getCategoryId() { return categoryId; }
  /**
   * 设置模板分类ID。
   * @param categoryId 模板分类ID
   */
  public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
  /**
   * 获取模板分类名称。
   * @return 模板分类名称
   */
  public String getCategoryName() { return categoryName; }
  /**
   * 设置模板分类名称。
   * @param categoryName 模板分类名称
   */
  public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
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
   * 获取简介。
   * @return 简介
   */
  public String getDescription() { return description; }
  /**
   * 设置简介。
   * @param description 简介
   */
  public void setDescription(String description) { this.description = description; }
  /**
   * 获取本地图片访问地址。
   * @return 本地图片访问地址
   */
  public String getImageUrl() { return imageUrl; }
  /**
   * 设置本地图片访问地址。
   * @param imageUrl 本地图片访问地址
   */
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  /**
   * 获取图片原始来源页面。
   * @return 图片原始来源页面
   */
  public String getImageSourceUrl() { return imageSourceUrl; }
  /**
   * 设置图片原始来源页面。
   * @param imageSourceUrl 图片原始来源页面
   */
  public void setImageSourceUrl(String imageSourceUrl) { this.imageSourceUrl = imageSourceUrl; }
  /**
   * 获取图片作者或来源平台。
   * @return 图片作者或来源平台
   */
  public String getImageAuthor() { return imageAuthor; }
  /**
   * 设置图片作者或来源平台。
   * @param imageAuthor 图片作者或来源平台
   */
  public void setImageAuthor(String imageAuthor) { this.imageAuthor = imageAuthor; }
  /**
   * 获取图片许可证。
   * @return 图片许可证
   */
  public String getImageLicense() { return imageLicense; }
  /**
   * 设置图片许可证。
   * @param imageLicense 图片许可证
   */
  public void setImageLicense(String imageLicense) { this.imageLicense = imageLicense; }
  /**
   * 获取参考价格。
   * @return 参考价格
   */
  public BigDecimal getReferencePrice() { return referencePrice; }
  /**
   * 设置参考价格。
   * @param referencePrice 参考价格
   */
  public void setReferencePrice(BigDecimal referencePrice) { this.referencePrice = referencePrice; }
  /**
   * 获取口味标签JSON。
   * @return 口味标签JSON
   */
  public String getTasteTags() { return tasteTags; }
  /**
   * 设置口味标签JSON。
   * @param tasteTags 口味标签JSON
   */
  public void setTasteTags(String tasteTags) { this.tasteTags = tasteTags; }
  /**
   * 获取推荐餐次JSON。
   * @return 推荐餐次JSON
   */
  public String getMealTags() { return mealTags; }
  /**
   * 设置推荐餐次JSON。
   * @param mealTags 推荐餐次JSON
   */
  public void setMealTags(String mealTags) { this.mealTags = mealTags; }
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
  /**
   * 获取当前商户是否已导入。
   * @return 当前商户是否已导入
   */
  public Boolean getImported() { return imported; }
  /**
   * 设置当前商户是否已导入。
   * @param imported 当前商户是否已导入
   */
  public void setImported(Boolean imported) { this.imported = imported; }
  /**
   * 获取食材数量。
   * @return 食材数量
   */
  public Integer getIngredientCount() { return ingredientCount; }
  /**
   * 设置食材数量。
   * @param ingredientCount 食材数量
   */
  public void setIngredientCount(Integer ingredientCount) { this.ingredientCount = ingredientCount; }
}
