package com.familykitchen.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审批请求。
 
 * @param remark 备注
 */
@Schema(description = "审批请求")
public record ApproveRequest(
    @Schema(description = "审核备注")
    String remark
) {}
