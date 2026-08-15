package com.familykitchen.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.apache.ibatis.type.Alias;

/**
 * 全系统统一用户持久化实体。
 *
 * <p>该实体只描述用户自身，不保存家庭、商户或平台角色。</p>
 */
@Alias("userDO")
@TableName("users")
@Schema(description = "统一用户实体")
public class UserDO {
  /**
   * 标识。
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /**
   * 用户名。
   */
  @TableField("username")
  private String username;
  /**
   * 用户名Changed。
   */
  @TableField("username_changed")
  private Boolean usernameChanged;
  /**
   * 密码Hash。
   */
  @TableField("password_hash")
  private String passwordHash;
  /**
   * 密码Algorithm。
   */
  @TableField("password_algorithm")
  private String passwordAlgorithm;
  /**
   * credential状态。
   */
  @TableField("credential_status")
  private String credentialStatus;
  /**
   * 邮箱。
   */
  @TableField("email")
  private String email;
  /**
   * 邮箱Verified。
   */
  @TableField("email_verified")
  private Boolean emailVerified;
  /**
   * 微信开放标识标识。
   */
  @TableField("wechat_open_id")
  private String wechatOpenId;
  /**
   * nickname。
   */
  @TableField("nickname")
  private String nickname;
  /**
   * avatarUrl。
   */
  @TableField("avatar_url")
  private String avatarUrl;
  /**
   * 手机号。
   */
  @TableField("mobile")
  private String mobile;
  /**
   * 状态。
   */
  @TableField("status")
  private String status;
  /**
   * 密码Changed时间。
   */
  @TableField("password_changed_at")
  private LocalDateTime passwordChangedAt;
  /**
   * lastLogin时间。
   */
  @TableField("last_login_at")
  private LocalDateTime lastLoginAt;
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
   * @return 获取标识后的结果
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
   * 获取用户名。
   *
   * @return 获取用户名后的结果
   */
  public String getUsername() { return username; }
  /**
   * 用户名。
   */
  /**
   * 设置用户名。
   *
   * @param username 用户名
   */
  public void setUsername(String username) { this.username = username; }
  /**
   * 获取用户名Changed。
   *
   * @return 是否满足对应条件
   */
  public Boolean getUsernameChanged() { return usernameChanged; }
  /**
   * 用户名Changed。
   */
  /**
   * 设置用户名Changed。
   *
   * @param usernameChanged 用户名Changed
   */
  public void setUsernameChanged(Boolean usernameChanged) { this.usernameChanged = usernameChanged; }
  /**
   * 获取密码Hash。
   *
   * @return 获取密码Hash后的结果
   */
  public String getPasswordHash() { return passwordHash; }
  /**
   * 密码Hash。
   */
  /**
   * 设置密码Hash。
   *
   * @param passwordHash 密码Hash
   */
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  /**
   * 获取密码Algorithm。
   *
   * @return 获取密码Algorithm后的结果
   */
  public String getPasswordAlgorithm() { return passwordAlgorithm; }
  /**
   * 密码Algorithm。
   */
  /**
   * 设置密码Algorithm。
   *
   * @param passwordAlgorithm 密码Algorithm
   */
  public void setPasswordAlgorithm(String passwordAlgorithm) { this.passwordAlgorithm = passwordAlgorithm; }
  /**
   * 获取Credential状态。
   *
   * @return 获取Credential状态后的结果
   */
  public String getCredentialStatus() { return credentialStatus; }
  /**
   * credential状态。
   */
  /**
   * 设置Credential状态。
   *
   * @param credentialStatus credential状态
   */
  public void setCredentialStatus(String credentialStatus) { this.credentialStatus = credentialStatus; }
  /**
   * 获取邮箱。
   *
   * @return 获取邮箱后的结果
   */
  public String getEmail() { return email; }
  /**
   * 邮箱。
   */
  /**
   * 设置邮箱。
   *
   * @param email 邮箱
   */
  public void setEmail(String email) { this.email = email; }
  /**
   * 获取邮箱Verified。
   *
   * @return 是否满足对应条件
   */
  public Boolean getEmailVerified() { return emailVerified; }
  /**
   * 邮箱Verified。
   */
  /**
   * 设置邮箱Verified。
   *
   * @param emailVerified 邮箱Verified
   */
  public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
  /**
   * 获取微信开放标识标识。
   *
   * @return 获取微信开放标识标识后的结果
   */
  public String getWechatOpenId() { return wechatOpenId; }
  /**
   * 微信开放标识标识。
   */
  /**
   * 设置微信开放标识标识。
   *
   * @param wechatOpenId 微信开放标识标识
   */
  public void setWechatOpenId(String wechatOpenId) { this.wechatOpenId = wechatOpenId; }
  /**
   * 获取Nickname。
   *
   * @return 获取Nickname后的结果
   */
  public String getNickname() { return nickname; }
  /**
   * nickname。
   */
  /**
   * 设置Nickname。
   *
   * @param nickname nickname
   */
  public void setNickname(String nickname) { this.nickname = nickname; }
  /**
   * 获取AvatarUrl。
   *
   * @return 获取AvatarUrl后的结果
   */
  public String getAvatarUrl() { return avatarUrl; }
  /**
   * avatarUrl。
   */
  /**
   * 设置AvatarUrl。
   *
   * @param avatarUrl avatarUrl
   */
  public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
  /**
   * 获取手机号。
   *
   * @return 获取手机号后的结果
   */
  public String getMobile() { return mobile; }
  /**
   * 手机号。
   */
  /**
   * 设置手机号。
   *
   * @param mobile 手机号
   */
  public void setMobile(String mobile) { this.mobile = mobile; }
  /**
   * 获取状态。
   *
   * @return 获取状态后的结果
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
   * 获取密码Changed时间。
   *
   * @return 获取密码Changed时间后的结果
   */
  public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
  /**
   * 取值。
   */
  /**
   * 设置密码Changed时间。
   *
   * @param value 取值
   */
  public void setPasswordChangedAt(LocalDateTime value) { this.passwordChangedAt = value; }
  /**
   * 获取LastLogin时间。
   *
   * @return 获取LastLogin时间后的结果
   */
  public LocalDateTime getLastLoginAt() { return lastLoginAt; }
  /**
   * 取值。
   */
  /**
   * 设置LastLogin时间。
   *
   * @param value 取值
   */
  public void setLastLoginAt(LocalDateTime value) { this.lastLoginAt = value; }
  /**
   * 获取创建时间。
   *
   * @return 获取创建时间后的结果
   */
  public LocalDateTime getCreatedAt() { return createdAt; }
  /**
   * 取值。
   */
  /**
   * 设置创建时间。
   *
   * @param value 取值
   */
  public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
  /**
   * 获取更新时间。
   *
   * @return 获取更新时间后的结果
   */
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  /**
   * 取值。
   */
  /**
   * 设置更新时间。
   *
   * @param value 取值
   */
  public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
