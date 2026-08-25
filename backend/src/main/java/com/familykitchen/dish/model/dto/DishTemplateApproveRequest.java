package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 平台管理员通过模板菜品修改申请的请求体。
 * @param reason 可选审核意见
 */
@Schema(description = "通过模板菜品修改申请")
public record DishTemplateApproveRequest(
    @Size(max = 500, message = "审核意见最多500个字符") String reason) {
  /**
   * 返回规范化审核意见。
   * @return 去除首尾空白后的审核意见，无有效内容时为空
   */
  public String normalizedReason() { return reason == null || reason.isBlank() ? null : reason.trim(); }
}
