package com.familykitchen.family.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import org.apache.ibatis.type.Alias;

/**
 * 家庭创建申请持久化实体。
 */
@Alias("familyApplicationDO")
@TableName("family_applications")
public class FamilyApplicationDO {

  /**
   * 标识。
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /**
   * applicant成员标识。
   */
  @TableField("applicant_user_id")
  private Long applicantMemberId;
  /**
   * 商户标识。
   */
  private Long merchantId;
  /**
   * proposed联系人联系电话。
   */
  /**
   * proposed联系人名称。
   */
  /**
   * proposed商户名称。
   */
  /**
   * 商户Mode。
   */
  private String merchantMode,proposedMerchantName,proposedContactName,proposedContactPhone;
  /**
   * 商户邀请标识。
   */
  private Long merchantInvitationId;

  /**
   * 家庭名称。
   */
  @TableField("family_name")
  private String familyName;

  /**
   * 状态。
   */
  @TableField("status")
  private String status;

  /**
   * 审核备注。
   */
  @TableField("review_remark")
  private String reviewRemark;

  /**
   * reviewedBy。
   */
  @TableField("reviewed_by")
  private Long reviewedBy;

  /**
   * reviewed时间。
   */
  @TableField("reviewed_at")
  private LocalDateTime reviewedAt;

  /**
   * 创建时间。
   */
  @TableField("created_at")
  private LocalDateTime createdAt;

  /**
   * 更新时间。
   */
  @TableField("updated_at")
  private LocalDateTime updatedAt;

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
   * 获取Applicant成员标识。
   *
   * @return 获取Applicant成员标识的结果
   */
  public Long getApplicantMemberId() { return applicantMemberId; }
  /**
   * applicant成员标识。
   */
  /**
   * 设置Applicant成员标识。
   *
   * @param applicantMemberId applicant成员标识
   */
  
  public void setApplicantMemberId(Long applicantMemberId) { this.applicantMemberId = applicantMemberId; }
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
   * 字段值。
   */
  /**
   * 设置商户Mode。
   *
   * @param v 字段值
   */
  /**
   * 获取商户Mode。
   *
   * @return 获取商户Mode的结果
   */
  public String getMerchantMode(){return merchantMode;}
  /**
   * 字段值。
   */
  /**
   * 设置商户Mode。
   *
   * @param v 字段值
   */
  public void setMerchantMode(String v){merchantMode=v;}
  /**
   * 字段值。
   */
  /**
   * 设置商户邀请标识。
   *
   * @param v 字段值
   */
  /**
   * 获取商户邀请标识。
   *
   * @return 获取商户邀请标识的结果
   */
  public Long getMerchantInvitationId(){return merchantInvitationId;}
  /**
   * 字段值。
   */
  /**
   * 设置商户邀请标识。
   *
   * @param v 字段值
   */
  public void setMerchantInvitationId(Long v){merchantInvitationId=v;}
  /**
   * 字段值。
   */
  /**
   * 设置Proposed商户名称。
   *
   * @param v 字段值
   */
  /**
   * 获取Proposed商户名称。
   *
   * @return 获取Proposed商户名称的结果
   */
  public String getProposedMerchantName(){return proposedMerchantName;}
  /**
   * 字段值。
   */
  /**
   * 设置Proposed商户名称。
   *
   * @param v 字段值
   */
  public void setProposedMerchantName(String v){proposedMerchantName=v;}
  /**
   * 字段值。
   */
  /**
   * 设置Proposed联系人名称。
   *
   * @param v 字段值
   */
  /**
   * 获取Proposed联系人名称。
   *
   * @return 获取Proposed联系人名称的结果
   */
  public String getProposedContactName(){return proposedContactName;}
  /**
   * 字段值。
   */
  /**
   * 设置Proposed联系人名称。
   *
   * @param v 字段值
   */
  public void setProposedContactName(String v){proposedContactName=v;}
  /**
   * 字段值。
   */
  /**
   * 设置Proposed联系人联系电话。
   *
   * @param v 字段值
   */
  /**
   * 获取Proposed联系人联系电话。
   *
   * @return 获取Proposed联系人联系电话的结果
   */
  public String getProposedContactPhone(){return proposedContactPhone;}
  /**
   * 字段值。
   */
  /**
   * 设置Proposed联系人联系电话。
   *
   * @param v 字段值
   */
  public void setProposedContactPhone(String v){proposedContactPhone=v;}
  /**
   * 获取家庭名称。
   *
   * @return 获取家庭名称的结果
   */
  public String getFamilyName() { return familyName; }
  /**
   * 家庭名称。
   */
  /**
   * 设置家庭名称。
   *
   * @param familyName 家庭名称
   */
  
  public void setFamilyName(String familyName) { this.familyName = familyName; }
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
  /**
   * 获取审核备注。
   *
   * @return 获取审核备注的结果
   */
  public String getReviewRemark() { return reviewRemark; }
  /**
   * 审核备注。
   */
  /**
   * 设置审核备注。
   *
   * @param reviewRemark 审核备注
   */
  
  public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
  /**
   * 获取ReviewedBy。
   *
   * @return 获取ReviewedBy的结果
   */
  public Long getReviewedBy() { return reviewedBy; }
  /**
   * reviewedBy。
   */
  /**
   * 设置ReviewedBy。
   *
   * @param reviewedBy reviewedBy
   */
  
  public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
  /**
   * 获取Reviewed时间。
   *
   * @return 获取Reviewed时间的结果
   */
  public LocalDateTime getReviewedAt() { return reviewedAt; }
  /**
   * reviewed时间。
   */
  /**
   * 设置Reviewed时间。
   *
   * @param reviewedAt reviewed时间
   */
  
  public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
  /**
   * 获取创建时间。
   *
   * @return 获取创建时间的结果
   */
  public LocalDateTime getCreatedAt() { return createdAt; }
  /**
   * 创建时间。
   */
  /**
   * 设置创建时间。
   *
   * @param createdAt 创建时间
   */
  
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  /**
   * 获取更新时间。
   *
   * @return 获取更新时间的结果
   */
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  /**
   * 更新时间。
   */
  /**
   * 设置更新时间。
   *
   * @param updatedAt 更新时间
   */
  
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
