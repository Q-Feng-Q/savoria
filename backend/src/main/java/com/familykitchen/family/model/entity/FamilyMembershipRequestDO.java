package com.familykitchen.family.model.entity;
import java.time.LocalDateTime;
/** 家庭定向邀请或邀请码申请实体。 */
public class FamilyMembershipRequestDO {
  /**
   * reviewedBy。
   */
  /**
   * 创建By。
   */
  /**
   * 邀请编码标识。
   */
  /**
   * applicant用户标识。
   */
  /**
   * 目标用户标识。
   */
  /**
   * 家庭标识。
   */
  /**
   * 标识。
   */
  private Long id,familyId,targetUserId,applicantUserId,invitationCodeId,createdBy,reviewedBy;
  /**
   * handled时间。
   */
  /**
   * 创建时间。
   */
  /**
   * 原因。
   */
  /**
   * 状态。
   */
  /**
   * 请求参数类型。
   */
  private String requestType,status,reason;
  /** 创建时间。 */
  private LocalDateTime createdAt,
      /** 处理时间。 */ handledAt;
  /**
   * 获取家庭标识。
   *
   * @return 获取家庭标识的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置标识。
   *
   * @param v 字段值
   */
  /**
   * 获取标识。
   *
   * @return 获取标识的结果
   */
  public Long getId(){return id;}
  /**
   * 字段值。
   */
  /**
   * 设置标识。
   *
   * @param v 字段值
   */
  public void setId(Long v){id=v;}
  /**
   * 获取家庭标识。
   *
   * @return 获取家庭标识的结果
   */
  public Long getFamilyId(){return familyId;}
  /**
   * 获取目标用户标识。
   *
   * @return 获取目标用户标识的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置家庭标识。
   *
   * @param v 字段值
   */
  
  public void setFamilyId(Long v){familyId=v;}
  /**
   * 获取目标用户标识。
   *
   * @return 获取目标用户标识的结果
   */
  public Long getTargetUserId(){return targetUserId;}
  /**
   * 获取Applicant用户标识。
   *
   * @return 获取Applicant用户标识的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置目标用户标识。
   *
   * @param v 字段值
   */
  
  public void setTargetUserId(Long v){targetUserId=v;}
  /**
   * 获取Applicant用户标识。
   *
   * @return 获取Applicant用户标识的结果
   */
  public Long getApplicantUserId(){return applicantUserId;}
  /**
   * 获取邀请编码标识。
   *
   * @return 获取邀请编码标识的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置Applicant用户标识。
   *
   * @param v 字段值
   */
  
  public void setApplicantUserId(Long v){applicantUserId=v;}
  /**
   * 获取邀请编码标识。
   *
   * @return 获取邀请编码标识的结果
   */
  public Long getInvitationCodeId(){return invitationCodeId;}
  /**
   * 获取创建By。
   *
   * @return 获取创建By的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置邀请编码标识。
   *
   * @param v 字段值
   */
  
  public void setInvitationCodeId(Long v){invitationCodeId=v;}
  /**
   * 获取创建By。
   *
   * @return 获取创建By的结果
   */
  public Long getCreatedBy(){return createdBy;}
  /**
   * 获取ReviewedBy。
   *
   * @return 获取ReviewedBy的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置创建By。
   *
   * @param v 字段值
   */
  
  public void setCreatedBy(Long v){createdBy=v;}
  /**
   * 获取ReviewedBy。
   *
   * @return 获取ReviewedBy的结果
   */
  public Long getReviewedBy(){return reviewedBy;}
  /**
   * 获取请求参数类型。
   *
   * @return 获取请求参数类型的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置ReviewedBy。
   *
   * @param v 字段值
   */
  
  public void setReviewedBy(Long v){reviewedBy=v;}
  /**
   * 获取请求参数类型。
   *
   * @return 获取请求参数类型的结果
   */
  public String getRequestType(){return requestType;}
  /**
   * 获取状态。
   *
   * @return 获取状态的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置请求参数类型。
   *
   * @param v 字段值
   */
  
  public void setRequestType(String v){requestType=v;}
  /**
   * 获取状态。
   *
   * @return 获取状态的结果
   */
  public String getStatus(){return status;}
  /**
   * 字段值。
   */
  /**
   * 设置原因。
   *
   * @param v 字段值
   */
  /**
   * 获取原因。
   *
   * @return 获取原因的结果
   */
  /**
   * 字段值。
   */
  /**
   * 设置状态。
   *
   * @param v 字段值
   */
  
  public void setStatus(String v){status=v;}
  /**
   * 获取原因。
   *
   * @return 获取原因的结果
   */
  public String getReason(){return reason;}
  /**
   * 字段值。
   */
  /**
   * 设置原因。
   *
   * @param v 字段值
   */
  public void setReason(String v){reason=v;}
  /**
   * 字段值。
   */
  /**
   * 设置创建时间。
   *
   * @param v 字段值
   */
  /**
   * 获取创建时间。
   *
   * @return 获取创建时间的结果
   */
  public LocalDateTime getCreatedAt(){return createdAt;}
  /**
   * 字段值。
   */
  /**
   * 设置创建时间。
   *
   * @param v 字段值
   */
  public void setCreatedAt(LocalDateTime v){createdAt=v;}
  /**
   * 字段值。
   */
  /**
   * 设置Handled时间。
   *
   * @param v 字段值
   */
  /**
   * 获取Handled时间。
   *
   * @return 获取Handled时间的结果
   */
  public LocalDateTime getHandledAt(){return handledAt;}
  /**
   * 字段值。
   */
  /**
   * 设置Handled时间。
   *
   * @param v 字段值
   */
  public void setHandledAt(LocalDateTime v){handledAt=v;}
}
