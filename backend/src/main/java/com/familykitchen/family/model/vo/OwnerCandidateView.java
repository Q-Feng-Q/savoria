package com.familykitchen.family.model.vo;

/**
 * 可接任家庭负责人的隐私安全成员摘要。
 *
 * @param memberId 家庭端兼容成员标识
 * @param displayName 成员展示名称
 * @param phoneSuffix 手机号后四位
 */
public record OwnerCandidateView(Long memberId, String displayName, String phoneSuffix) {
}
