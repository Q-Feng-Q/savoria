package com.familykitchen.dish.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 商户提交平台模板菜品完整修改的请求体。
 * @param submitNote 提交说明
 * @param targetSnapshot 目标完整快照
 */
@Schema(description = "模板菜品修改申请")
public record DishTemplateChangeSubmitRequest(
    @Size(max = 500, message = "提交说明最多500个字符")
    @Schema(description = "提交说明，最多500个字符") String submitNote,
    @NotNull(message = "模板菜品修改内容不能为空")
    @Schema(description = "审核通过后覆盖模板的完整业务快照",
        implementation = DishTemplateSnapshotRequest.class)
    JsonNode targetSnapshot) {
  /**
   * 规范化可选提交说明。
   * @return 去除首尾空白后的提交说明，无有效内容时为空
   */
  public String normalizedSubmitNote() {
    return submitNote == null || submitNote.isBlank() ? null : submitNote.trim();
  }
}
