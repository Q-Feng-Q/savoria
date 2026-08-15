package com.familykitchen.family.model.dto;
import jakarta.validation.constraints.Size;
/** 家庭邀请或申请处理请求。 
 * @param reason 原因
 */
public record MembershipDecisionRequest(@Size(max=500) String reason) {}
