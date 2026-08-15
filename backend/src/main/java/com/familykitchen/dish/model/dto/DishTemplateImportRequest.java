package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 商户选择性导入平台菜品模板的请求体。
 * @param templateIds 需要导入的平台模板 ID 列表
 */
@Schema(description = "批量导入平台菜品模板请求")
public record DishTemplateImportRequest(
    @NotEmpty(message = "请选择要导入的模板菜品")
    @Size(max = 100, message = "单次最多导入100道模板菜品")
    @Schema(description = "模板ID列表，去重后最多100项", example = "[1,2,3]")
    List<@NotNull(message = "模板ID不能为空") Long> templateIds) {
}
