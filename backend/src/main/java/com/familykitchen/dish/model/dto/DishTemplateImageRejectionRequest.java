package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 驳回内部模板图片的审核请求。
 * @param reason 明确的驳回原因
 */
@Schema(description = "模板内部图片驳回请求")
public record DishTemplateImageRejectionRequest(
    @NotBlank(message = "驳回原因不能为空") @Size(max = 500) String reason) { }
