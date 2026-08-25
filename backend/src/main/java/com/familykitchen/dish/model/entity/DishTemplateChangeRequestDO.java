package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 平台模板菜品修改审核申请数据库实体。 */
@TableName("dish_template_change_requests")
@Schema(description = "平台模板菜品修改审核申请实体")
public class DishTemplateChangeRequestDO {
  /** 申请数据库主键。 */
  @TableId(value = "id", type = IdType.AUTO) private Long id;
  /** 提交商户 ID。 */
  @TableField("merchant_id") private Long merchantId;
  /** 目标平台模板菜品 ID。 */
  @TableField("template_id") private Long templateId;
  /** 提交时模板并发版本号。 */
  @TableField("base_template_version") private Long baseTemplateVersion;
  /** 提交时原模板完整业务快照 JSON。 */
  @TableField("base_snapshot_json") private String baseSnapshotJson;
  /** 申请覆盖后的完整业务快照 JSON。 */
  @TableField("snapshot_json") private String snapshotJson;
  /** 商户提交说明。 */
  @TableField("submit_note") private String submitNote;
  /** 审核状态。 */
  @TableField("status") private String status;
  /** 提交用户 ID。 */
  @TableField("submitted_by") private Long submittedBy;
  /** 审核平台管理员用户 ID。 */
  @TableField("reviewed_by") private Long reviewedBy;
  /** 审核意见或驳回原因。 */
  @TableField("review_reason") private String reviewReason;
  /** 提交时间。 */
  @TableField("submitted_at") private LocalDateTime submittedAt;
  /** 审核完成时间。 */
  @TableField("reviewed_at") private LocalDateTime reviewedAt;
  /** 撤回用户 ID。 */
  @TableField("withdrawn_by") private Long withdrawnBy;
  /** 撤回时间。 */
  @TableField("withdrawn_at") private LocalDateTime withdrawnAt;
  /** 审核结果通知 ID。 */
  @TableField("result_notification_id") private Long resultNotificationId;
  /** 查询结果中的商户名称，不映射数据库字段。 */
  @TableField(exist = false) private String merchantName;
  /** 查询结果中的模板菜名，不映射数据库字段。 */
  @TableField(exist = false) private String templateName;
  /** 查询结果中的当前模板版本，不映射数据库字段。 */
  @TableField(exist = false) private Long currentTemplateVersion;

  /**
   * 返回申请 ID。
   * @return 申请 ID
   */
  public Long getId() { return id; }
  /**
   * 设置申请 ID。
   * @param id 申请 ID
   */
  public void setId(Long id) { this.id = id; }
  /**
   * 返回提交商户 ID。
   * @return 提交商户 ID
   */
  public Long getMerchantId() { return merchantId; }
  /**
   * 设置提交商户 ID。
   * @param merchantId 提交商户 ID
   */
  public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
  /**
   * 返回模板菜品 ID。
   * @return 模板菜品 ID
   */
  public Long getTemplateId() { return templateId; }
  /**
   * 设置模板菜品 ID。
   * @param templateId 模板菜品 ID
   */
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  /**
   * 返回基础模板版本。
   * @return 基础模板版本
   */
  public Long getBaseTemplateVersion() { return baseTemplateVersion; }
  /**
   * 设置基础模板版本。
   * @param baseTemplateVersion 基础模板版本
   */
  public void setBaseTemplateVersion(Long baseTemplateVersion) { this.baseTemplateVersion = baseTemplateVersion; }
  /**
   * 返回原始快照 JSON。
   * @return 原始快照 JSON
   */
  public String getBaseSnapshotJson() { return baseSnapshotJson; }
  /**
   * 设置原始快照 JSON。
   * @param baseSnapshotJson 原始快照 JSON
   */
  public void setBaseSnapshotJson(String baseSnapshotJson) { this.baseSnapshotJson = baseSnapshotJson; }
  /**
   * 返回目标快照 JSON。
   * @return 目标快照 JSON
   */
  public String getSnapshotJson() { return snapshotJson; }
  /**
   * 设置目标快照 JSON。
   * @param snapshotJson 目标快照 JSON
   */
  public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
  /**
   * 返回提交说明。
   * @return 提交说明
   */
  public String getSubmitNote() { return submitNote; }
  /**
   * 设置提交说明。
   * @param submitNote 提交说明
   */
  public void setSubmitNote(String submitNote) { this.submitNote = submitNote; }
  /**
   * 返回审核状态。
   * @return 审核状态
   */
  public String getStatus() { return status; }
  /**
   * 设置审核状态。
   * @param status 审核状态
   */
  public void setStatus(String status) { this.status = status; }
  /**
   * 返回提交用户 ID。
   * @return 提交用户 ID
   */
  public Long getSubmittedBy() { return submittedBy; }
  /**
   * 设置提交用户 ID。
   * @param submittedBy 提交用户 ID
   */
  public void setSubmittedBy(Long submittedBy) { this.submittedBy = submittedBy; }
  /**
   * 返回审核用户 ID。
   * @return 审核用户 ID
   */
  public Long getReviewedBy() { return reviewedBy; }
  /**
   * 设置审核用户 ID。
   * @param reviewedBy 审核用户 ID
   */
  public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
  /**
   * 返回审核意见。
   * @return 审核意见或驳回原因
   */
  public String getReviewReason() { return reviewReason; }
  /**
   * 设置审核意见。
   * @param reviewReason 审核意见或驳回原因
   */
  public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
  /**
   * 返回提交时间。
   * @return 提交时间
   */
  public LocalDateTime getSubmittedAt() { return submittedAt; }
  /**
   * 设置提交时间。
   * @param submittedAt 提交时间
   */
  public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
  /**
   * 返回审核时间。
   * @return 审核时间
   */
  public LocalDateTime getReviewedAt() { return reviewedAt; }
  /**
   * 设置审核时间。
   * @param reviewedAt 审核时间
   */
  public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
  /**
   * 返回撤回用户 ID。
   * @return 撤回用户 ID
   */
  public Long getWithdrawnBy() { return withdrawnBy; }
  /**
   * 设置撤回用户 ID。
   * @param withdrawnBy 撤回用户 ID
   */
  public void setWithdrawnBy(Long withdrawnBy) { this.withdrawnBy = withdrawnBy; }
  /**
   * 返回撤回时间。
   * @return 撤回时间
   */
  public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
  /**
   * 设置撤回时间。
   * @param withdrawnAt 撤回时间
   */
  public void setWithdrawnAt(LocalDateTime withdrawnAt) { this.withdrawnAt = withdrawnAt; }
  /**
   * 返回结果通知 ID。
   * @return 结果通知 ID
   */
  public Long getResultNotificationId() { return resultNotificationId; }
  /**
   * 设置结果通知 ID。
   * @param resultNotificationId 结果通知 ID
   */
  public void setResultNotificationId(Long resultNotificationId) { this.resultNotificationId = resultNotificationId; }
  /**
   * 返回商户名称。
   * @return 商户名称
   */
  public String getMerchantName() { return merchantName; }
  /**
   * 设置商户名称。
   * @param merchantName 商户名称
   */
  public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
  /**
   * 返回模板菜名。
   * @return 模板菜名
   */
  public String getTemplateName() { return templateName; }
  /**
   * 设置模板菜名。
   * @param templateName 模板菜名
   */
  public void setTemplateName(String templateName) { this.templateName = templateName; }
  /**
   * 返回当前模板版本。
   * @return 当前模板版本
   */
  public Long getCurrentTemplateVersion() { return currentTemplateVersion; }
  /**
   * 设置当前模板版本。
   * @param currentTemplateVersion 当前模板版本
   */
  public void setCurrentTemplateVersion(Long currentTemplateVersion) { this.currentTemplateVersion = currentTemplateVersion; }
}
