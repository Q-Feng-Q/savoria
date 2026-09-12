package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 平台菜品模板的内部图片审核资产实体，不可直接返回家庭端或商户端。 */
@TableName("dish_template_image_assets")
@Schema(description = "平台菜品模板内部图片审核资产实体")
public class DishTemplateImageAssetEntity {
  /** 数据库主键。 */ @TableId(value = "id", type = IdType.AUTO) private Long id;
  /** 模板ID。 */ private Long templateId;
  /** 来源记录ID。 */ private Long sourceRecordId;
  /** 内部私有存储键。 */ private String internalStorageKey;
  /** 图片内容摘要。 */ private String contentSha256;
  /** MIME类型。 */ private String mimeType;
  /** 文件字节数。 */ private Long fileSize;
  /** 来源图片相对路径。 */ private String sourceImagePath;
  /** 来源页面。 */ private String sourceUrl;
  /** 来源固定提交。 */ private String sourceRevision;
  /** 资产状态。 */ private String assetStatus;
  /** 发布后的公共图片地址。 */ private String publicImageUrl;
  /** 审核确认的图片作者。 */ private String imageAuthor;
  /** 审核确认的图片许可证。 */ private String imageLicense;
  /** 审核用户ID。 */ private Long reviewedBy;
  /** 审核时间。 */ private LocalDateTime reviewedAt;
  /** 拒绝原因，仅拒绝状态可填写。 */ private String rejectionReason;
  /** 创建时间。 */ private LocalDateTime createdAt;
  /** 更新时间。 */ private LocalDateTime updatedAt;

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
   * 获取来源记录ID。
   *
   * @return 来源记录ID
   */
  public Long getSourceRecordId() { return sourceRecordId; }
  /**
   * 设置来源记录ID。
   *
   * @param sourceRecordId 来源记录ID
   */
  public void setSourceRecordId(Long sourceRecordId) { this.sourceRecordId = sourceRecordId; }
  /**
   * 获取内部存储键。
   *
   * @return 内部存储键
   */
  public String getInternalStorageKey() { return internalStorageKey; }
  /**
   * 设置内部存储键。
   *
   * @param internalStorageKey 内部存储键
   */
  public void setInternalStorageKey(String internalStorageKey) { this.internalStorageKey = internalStorageKey; }
  /**
   * 获取内容摘要。
   *
   * @return 内容摘要
   */
  public String getContentSha256() { return contentSha256; }
  /**
   * 设置内容摘要。
   *
   * @param contentSha256 内容摘要
   */
  public void setContentSha256(String contentSha256) { this.contentSha256 = contentSha256; }
  /**
   * 获取MIME类型。
   *
   * @return MIME类型
   */
  public String getMimeType() { return mimeType; }
  /**
   * 设置MIME类型。
   *
   * @param mimeType MIME类型
   */
  public void setMimeType(String mimeType) { this.mimeType = mimeType; }
  /**
   * 获取文件字节数。
   *
   * @return 文件字节数
   */
  public Long getFileSize() { return fileSize; }
  /**
   * 设置文件字节数。
   *
   * @param fileSize 文件字节数
   */
  public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
  /**
   * 获取来源图片路径。
   *
   * @return 来源图片路径
   */
  public String getSourceImagePath() { return sourceImagePath; }
  /**
   * 设置来源图片路径。
   *
   * @param sourceImagePath 来源图片路径
   */
  public void setSourceImagePath(String sourceImagePath) { this.sourceImagePath = sourceImagePath; }
  /**
   * 获取来源页面。
   *
   * @return 来源页面
   */
  public String getSourceUrl() { return sourceUrl; }
  /**
   * 设置来源页面。
   *
   * @param sourceUrl 来源页面
   */
  public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
  /**
   * 获取来源固定提交。
   *
   * @return 来源固定提交
   */
  public String getSourceRevision() { return sourceRevision; }
  /**
   * 设置来源固定提交。
   *
   * @param sourceRevision 来源固定提交
   */
  public void setSourceRevision(String sourceRevision) { this.sourceRevision = sourceRevision; }
  /**
   * 获取资产状态。
   *
   * @return 资产状态
   */
  public String getAssetStatus() { return assetStatus; }
  /**
   * 设置资产状态。
   *
   * @param assetStatus 资产状态
   */
  public void setAssetStatus(String assetStatus) { this.assetStatus = assetStatus; }
  /**
   * 获取公共图片地址。
   *
   * @return 公共图片地址
   */
  public String getPublicImageUrl() { return publicImageUrl; }
  /**
   * 设置公共图片地址。
   *
   * @param publicImageUrl 公共图片地址
   */
  public void setPublicImageUrl(String publicImageUrl) { this.publicImageUrl = publicImageUrl; }
  /**
   * 获取图片作者。
   *
   * @return 图片作者
   */
  public String getImageAuthor() { return imageAuthor; }
  /**
   * 设置图片作者。
   *
   * @param imageAuthor 图片作者
   */
  public void setImageAuthor(String imageAuthor) { this.imageAuthor = imageAuthor; }
  /**
   * 获取图片许可证。
   *
   * @return 图片许可证
   */
  public String getImageLicense() { return imageLicense; }
  /**
   * 设置图片许可证。
   *
   * @param imageLicense 图片许可证
   */
  public void setImageLicense(String imageLicense) { this.imageLicense = imageLicense; }
  /**
   * 获取审核用户ID。
   *
   * @return 审核用户ID
   */
  public Long getReviewedBy() { return reviewedBy; }
  /**
   * 设置审核用户ID。
   *
   * @param reviewedBy 审核用户ID
   */
  public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
  /**
   * 获取审核时间。
   *
   * @return 审核时间
   */
  public LocalDateTime getReviewedAt() { return reviewedAt; }
  /**
   * 设置审核时间。
   *
   * @param reviewedAt 审核时间
   */
  public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
  /**
   * 获取拒绝原因。
   *
   * @return 拒绝原因
   */
  public String getRejectionReason() { return rejectionReason; }
  /**
   * 设置拒绝原因。
   *
   * @param rejectionReason 拒绝原因
   */
  public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
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
  /**
   * 获取更新时间。
   *
   * @return 更新时间
   */
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  /**
   * 设置更新时间。
   *
   * @param updatedAt 更新时间
   */
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
