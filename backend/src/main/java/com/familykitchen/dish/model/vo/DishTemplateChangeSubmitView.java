package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 模板菜品修改申请提交结果。
 * @param requestId 申请 ID
 * @param status 初始审核状态
 * @param submittedAt 提交时间
 */
@Schema(description = "模板菜品修改申请提交结果")
public record DishTemplateChangeSubmitView(Long requestId, String status, LocalDateTime submittedAt) {
}
