package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 平台菜品模板与固定来源文件之间的追踪记录实体。 */
@TableName("dish_template_source_records")
@Schema(description = "平台菜品模板来源记录实体")
public class DishTemplateSourceRecordEntity {
  /** 数据库主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 模板ID。 */
  private Long templateId;
  /** 来源文件稳定键。 */
  private String sourceKey;
  /** 来源标题。 */
  private String sourceTitle;
  /** 来源分类。 */
  private String sourceCategory;
  /** 来源仓库相对路径。 */
  private String sourcePath;
  /** 来源页面。 */
  private String sourceUrl;
  /** 来源固定提交。 */
  private String sourceRevision;
  /** 来源记录类型。 */
  private String recordType;
  /** 别名或路径改名原因。 */
  private String aliasReason;
  /** 规范化内容摘要。 */
  private String contentSha256;
  /** 创建时间。 */
  private LocalDateTime createdAt;

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
   * 获取来源文件稳定键。
   *
   * @return 来源文件稳定键
   */
  public String getSourceKey() { return sourceKey; }
  /**
   * 设置来源文件稳定键。
   *
   * @param sourceKey 来源文件稳定键
   */
  public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }
  /**
   * 获取来源标题。
   *
   * @return 来源标题
   */
  public String getSourceTitle() { return sourceTitle; }
  /**
   * 设置来源标题。
   *
   * @param sourceTitle 来源标题
   */
  public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
  /**
   * 获取来源分类。
   *
   * @return 来源分类
   */
  public String getSourceCategory() { return sourceCategory; }
  /**
   * 设置来源分类。
   *
   * @param sourceCategory 来源分类
   */
  public void setSourceCategory(String sourceCategory) { this.sourceCategory = sourceCategory; }
  /**
   * 获取来源相对路径。
   *
   * @return 来源相对路径
   */
  public String getSourcePath() { return sourcePath; }
  /**
   * 设置来源相对路径。
   *
   * @param sourcePath 来源相对路径
   */
  public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
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
   * 获取来源记录类型。
   *
   * @return 来源记录类型
   */
  public String getRecordType() { return recordType; }
  /**
   * 设置来源记录类型。
   *
   * @param recordType 来源记录类型
   */
  public void setRecordType(String recordType) { this.recordType = recordType; }
  /**
   * 获取别名原因。
   *
   * @return 别名原因
   */
  public String getAliasReason() { return aliasReason; }
  /**
   * 设置别名原因。
   *
   * @param aliasReason 别名原因
   */
  public void setAliasReason(String aliasReason) { this.aliasReason = aliasReason; }
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
