package com.familykitchen.admin.model.vo;

import java.time.LocalDateTime;

/** Platform user directory row. 
 * @param userId 用户标识
 * @param username 用户名
 * @param nickname nickname
 * @param mobile 手机号
 * @param email 邮箱
 * @param status 状态
 * @param platformRoles platformRoles
 * @param familyName 家庭名称
 * @param merchantName 商户名称
 * @param lastLoginAt lastLogin时间
 * @param createdAt 创建时间
 */
public record AdminUserView(Long userId, String username, String nickname, String mobile,
    String email, String status, String platformRoles, String familyName, String merchantName,
    LocalDateTime lastLoginAt, LocalDateTime createdAt) {}
