package com.familykitchen.dish.model.vo;

import com.familykitchen.dish.model.dto.DishTemplateSnapshotRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 模板菜品修改申请详情及提交前后完整快照。
 * @param requestId 申请 ID
 * @param merchantId 提交商户 ID
 * @param merchantName 提交商户名称
 * @param templateId 模板菜品 ID
 * @param templateName 模板菜品名称
 * @param baseTemplateVersion 提交时模板版本
 * @param currentTemplateVersion 当前模板版本
 * @param stale 申请是否已经过期
 * @param status 审核状态
 * @param submitNote 提交说明
 * @param baseSnapshot 提交时原始快照
 * @param targetSnapshot 目标完整快照
 * @param submittedBy 提交用户 ID
 * @param submittedAt 提交时间
 * @param reviewedBy 审核管理员 ID
 * @param reviewReason 审核意见或驳回原因
 * @param reviewedAt 审核时间
 * @param withdrawnBy 撤回用户 ID
 * @param withdrawnAt 撤回时间
 * @param resultNotificationId 审核结果通知 ID
 */
@Schema(description = "模板菜品修改申请详情")
public record DishTemplateChangeDetailView(Long requestId, Long merchantId, String merchantName, Long templateId,
    String templateName, Long baseTemplateVersion, Long currentTemplateVersion, boolean stale, String status,
    String submitNote, DishTemplateSnapshotRequest baseSnapshot, DishTemplateSnapshotRequest targetSnapshot,
    Long submittedBy, LocalDateTime submittedAt, Long reviewedBy, String reviewReason,
    LocalDateTime reviewedAt, Long withdrawnBy, LocalDateTime withdrawnAt, Long resultNotificationId) {
}
