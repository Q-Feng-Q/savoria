package com.familykitchen.family.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import org.apache.ibatis.type.Alias;

/**
 * 家庭邀请码持久化实体。
 */
@Alias("familyInvitationDO")
@TableName("family_invitations")
public class FamilyInvitationDO {

  /**
   * 标识。
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /**
   * 家庭标识。
   */
  @TableField("family_id")
  private Long familyId;

  /**
   * inviter成员标识。
   */
  @TableField("inviter_user_id")
  private Long inviterMemberId;

  /**
   * 编码。
   */
  @TableField("code")
  private String code;

  /**
   * 状态。
   */
  @TableField("status")
  private String status;

  /**
   * expire时间。
   */
  @TableField("expire_at")
  private LocalDateTime expireAt;

  /**
   * usedBy成员标识。
   */
  @TableField("used_by_user_id")
  private Long usedByMemberId;

  /**
   * used时间。
   */
  @TableField("used_at")
  private LocalDateTime usedAt;

  /**
   * 创建时间。
   */
  @TableField("created_at")
  private LocalDateTime createdAt;

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
   * 获取家庭标识。
   *
   * @return 获取家庭标识的结果
   */
  public Long getFamilyId() { return familyId; }
  /**
   * 家庭标识。
   */
  /**
   * 设置家庭标识。
   *
   * @param familyId 家庭标识
   */
  public void setFamilyId(Long familyId) { this.familyId = familyId; }
  /**
   * 获取Inviter成员标识。
   *
   * @return 获取Inviter成员标识的结果
   */
  public Long getInviterMemberId() { return inviterMemberId; }
  /**
   * inviter成员标识。
   */
  /**
   * 设置Inviter成员标识。
   *
   * @param inviterMemberId inviter成员标识
   */
  public void setInviterMemberId(Long inviterMemberId) { this.inviterMemberId = inviterMemberId; }
  /**
   * 获取编码。
   *
   * @return 获取编码的结果
   */
  public String getCode() { return code; }
  /**
   * 编码。
   */
  /**
   * 设置编码。
   *
   * @param code 编码
   */
  public void setCode(String code) { this.code = code; }
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
   * 获取Expire时间。
   *
   * @return 获取Expire时间的结果
   */
  public LocalDateTime getExpireAt() { return expireAt; }
  /**
   * expire时间。
   */
  /**
   * 设置Expire时间。
   *
   * @param expireAt expire时间
   */
  public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
  /**
   * 获取UsedBy成员标识。
   *
   * @return 获取UsedBy成员标识的结果
   */
  public Long getUsedByMemberId() { return usedByMemberId; }
  /**
   * usedBy成员标识。
   */
  /**
   * 设置UsedBy成员标识。
   *
   * @param usedByMemberId usedBy成员标识
   */
  public void setUsedByMemberId(Long usedByMemberId) { this.usedByMemberId = usedByMemberId; }
  /**
   * 获取Used时间。
   *
   * @return 获取Used时间的结果
   */
  public LocalDateTime getUsedAt() { return usedAt; }
  /**
   * used时间。
   */
  /**
   * 设置Used时间。
   *
   * @param usedAt used时间
   */
  public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
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
}
