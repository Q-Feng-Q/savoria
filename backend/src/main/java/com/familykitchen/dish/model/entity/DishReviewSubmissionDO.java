package com.familykitchen.dish.model.entity;
import java.time.LocalDateTime;
/** 菜品审核提交实体。 */
public class DishReviewSubmissionDO {
  /**
   * reviewedBy。
   */
  /**
   * submittedBy。
   */
  /**
   * 目标菜品标识。
   */
  /**
   * 商户标识。
   */
  /**
   * 标识。
   */
  private Long id,merchantId,targetDishId,submittedBy,reviewedBy;
  /**
   * 审核原因。
   */
  /**
   * 状态。
   */
  /**
   * snapshotJson。
   */
  /**
   * submission类型。
   */
  private String submissionType,snapshotJson,status,reviewReason;
  /**
   * reviewed时间。
   */
  /**
   * submitted时间。
   */
  private LocalDateTime submittedAt,reviewedAt;
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
   * 字段值。
   */
  /**
   * 设置商户标识。
   *
   * @param v 字段值
   */
  /**
   * 获取商户标识。
   *
   * @return 获取商户标识的结果
   */
  public Long getMerchantId(){return merchantId;}
  /**
   * 字段值。
   */
  /**
   * 设置商户标识。
   *
   * @param v 字段值
   */
  public void setMerchantId(Long v){merchantId=v;}
  /**
   * 字段值。
   */
  /**
   * 设置目标菜品标识。
   *
   * @param v 字段值
   */
  /**
   * 获取目标菜品标识。
   *
   * @return 获取目标菜品标识的结果
   */
  public Long getTargetDishId(){return targetDishId;}
  /**
   * 字段值。
   */
  /**
   * 设置目标菜品标识。
   *
   * @param v 字段值
   */
  public void setTargetDishId(Long v){targetDishId=v;}
  /**
   * 字段值。
   */
  /**
   * 设置SubmittedBy。
   *
   * @param v 字段值
   */
  /**
   * 获取SubmittedBy。
   *
   * @return 获取SubmittedBy的结果
   */
  public Long getSubmittedBy(){return submittedBy;}
  /**
   * 字段值。
   */
  /**
   * 设置SubmittedBy。
   *
   * @param v 字段值
   */
  public void setSubmittedBy(Long v){submittedBy=v;}
  /**
   * 字段值。
   */
  /**
   * 设置ReviewedBy。
   *
   * @param v 字段值
   */
  /**
   * 获取ReviewedBy。
   *
   * @return 获取ReviewedBy的结果
   */
  public Long getReviewedBy(){return reviewedBy;}
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
   * 字段值。
   */
  /**
   * 设置Submission类型。
   *
   * @param v 字段值
   */
  /**
   * 获取Submission类型。
   *
   * @return 获取Submission类型的结果
   */
  public String getSubmissionType(){return submissionType;}
  /**
   * 字段值。
   */
  /**
   * 设置Submission类型。
   *
   * @param v 字段值
   */
  public void setSubmissionType(String v){submissionType=v;}
  /**
   * 字段值。
   */
  /**
   * 设置SnapshotJson。
   *
   * @param v 字段值
   */
  /**
   * 获取SnapshotJson。
   *
   * @return 获取SnapshotJson的结果
   */
  public String getSnapshotJson(){return snapshotJson;}
  /**
   * 字段值。
   */
  /**
   * 设置SnapshotJson。
   *
   * @param v 字段值
   */
  public void setSnapshotJson(String v){snapshotJson=v;}
  /**
   * 字段值。
   */
  /**
   * 设置状态。
   *
   * @param v 字段值
   */
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
   * 设置状态。
   *
   * @param v 字段值
   */
  public void setStatus(String v){status=v;}
  /**
   * 字段值。
   */
  /**
   * 设置审核原因。
   *
   * @param v 字段值
   */
  /**
   * 获取审核原因。
   *
   * @return 获取审核原因的结果
   */
  public String getReviewReason(){return reviewReason;}
  /**
   * 字段值。
   */
  /**
   * 设置审核原因。
   *
   * @param v 字段值
   */
  public void setReviewReason(String v){reviewReason=v;}
  /**
   * 字段值。
   */
  /**
   * 设置Submitted时间。
   *
   * @param v 字段值
   */
  /**
   * 获取Submitted时间。
   *
   * @return 获取Submitted时间的结果
   */
  public LocalDateTime getSubmittedAt(){return submittedAt;}
  /**
   * 字段值。
   */
  /**
   * 设置Submitted时间。
   *
   * @param v 字段值
   */
  public void setSubmittedAt(LocalDateTime v){submittedAt=v;}
  /**
   * 字段值。
   */
  /**
   * 设置Reviewed时间。
   *
   * @param v 字段值
   */
  /**
   * 获取Reviewed时间。
   *
   * @return 获取Reviewed时间的结果
   */
  public LocalDateTime getReviewedAt(){return reviewedAt;}
  /**
   * 字段值。
   */
  /**
   * 设置Reviewed时间。
   *
   * @param v 字段值
   */
  public void setReviewedAt(LocalDateTime v){reviewedAt=v;}
}
