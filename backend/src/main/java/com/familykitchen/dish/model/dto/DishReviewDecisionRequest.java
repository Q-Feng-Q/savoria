package com.familykitchen.dish.model.dto;
import jakarta.validation.constraints.Size;
/** 菜品审核决定请求；拒绝时 reason 必填。 
 * @param reason 原因
 */
public record DishReviewDecisionRequest(@Size(max=500) String reason) {}
