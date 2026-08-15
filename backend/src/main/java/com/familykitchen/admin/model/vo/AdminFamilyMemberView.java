package com.familykitchen.admin.model.vo;

import java.math.BigDecimal;

/**
 * 封装返回给调用方的平台管理家庭成员数据。
 *
 * @param memberId 成员标识
 * @param username 用户名
 * @param name 名称
 * @param mobile 手机号
 * @param activationStatus activation状态
 * @param roleTemplate 角色Template
 * @param availableBalance available余额
 * @param frozenBalance frozen余额
 */
public record AdminFamilyMemberView(
    Long memberId,
    String username,
    String name,
    String mobile,
    String activationStatus,
    String roleTemplate,
    BigDecimal availableBalance,
    BigDecimal frozenBalance
) {
}
