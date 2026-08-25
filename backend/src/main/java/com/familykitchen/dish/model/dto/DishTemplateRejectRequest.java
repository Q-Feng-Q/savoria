package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 平台管理员驳回模板菜品修改申请的请求体。
 * @param reason 必填驳回原因
 */
@Schema(description = "驳回模板菜品修改申请")
public record DishTemplateRejectRequest(
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因最多500个字符") String reason) {
  /**
   * 返回规范化驳回原因。
   * @return 去除首尾空白后的驳回原因
   */
  public String normalizedReason() { return reason == null ? null : reason.trim(); }
}
