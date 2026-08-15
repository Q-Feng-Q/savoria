package com.familykitchen.family.model.vo;

import com.familykitchen.family.model.entity.FamilyApplicationDO;
import com.familykitchen.family.model.entity.FamilyMembershipRequestDO;

/**
 * 封装返回给调用方的家庭Onboarding数据。
 *
 * @param familyApplication 家庭申请
 * @param joinApplication join申请
 */
public record FamilyOnboardingView(
    FamilyApplicationDO familyApplication,
    FamilyMembershipRequestDO joinApplication
) {}
