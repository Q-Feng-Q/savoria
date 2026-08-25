package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 商户将已导入菜品申请同步回来源模板的请求。
 *
 * @param submitNote 可选提交说明
 */
@Schema(description = "已导入菜品同步模板申请")
public record ImportedDishTemplateSyncRequest(
    @Size(max = 500, message = "提交说明最多500个字符")
    @Schema(description = "提交说明，最多500个字符", example = "采用商户实测后的食材用量")
    String submitNote) {

  /**
   * 返回去除首尾空白后的说明。
   *
   * @return 规范化说明；没有有效内容时返回 {@code null}
   */
  public String normalizedSubmitNote() {
    return submitNote == null || submitNote.isBlank() ? null : submitNote.trim();
  }
}
