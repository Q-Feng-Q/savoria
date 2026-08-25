package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 模板菜品修改申请列表项。
 * @param requestId 申请 ID
 * @param merchantId 提交商户 ID
 * @param merchantName 提交商户名称
 * @param templateId 模板菜品 ID
 * @param templateName 模板菜品名称
 * @param baseTemplateVersion 提交时模板版本
 * @param status 审核状态
 * @param submitNote 提交说明
 * @param submittedBy 提交用户 ID
 * @param submittedAt 提交时间
 * @param reviewedBy 审核管理员 ID
 * @param reviewReason 审核意见或驳回原因
 * @param reviewedAt 审核时间
 */
@Schema(description = "模板菜品修改申请列表项")
public record DishTemplateChangeItemView(Long requestId, Long merchantId, String merchantName, Long templateId,
    String templateName, Long baseTemplateVersion, String status, String submitNote, Long submittedBy,
    LocalDateTime submittedAt, Long reviewedBy, String reviewReason, LocalDateTime reviewedAt) {
}
