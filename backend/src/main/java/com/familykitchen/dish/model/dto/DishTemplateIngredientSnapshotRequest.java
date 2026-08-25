package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 模板菜品覆盖快照中的单项食材。
 *
 * @param ingredientName 食材名称
 * @param ingredientCategory 食材分类
 * @param quantity 默认用量
 * @param unit 计量单位
 * @param calcType 用量计算方式
 * @param sortOrder 排序值
 */
@Schema(description = "模板菜品覆盖快照食材")
public record DishTemplateIngredientSnapshotRequest(
    @NotBlank(message = "食材名称不能为空")
    @Size(max = 100, message = "食材名称最多100个字符")
    @Schema(description = "食材名称", example = "豆角")
    String ingredientName,
    @NotBlank(message = "食材分类不能为空")
    @Size(max = 50, message = "食材分类最多50个字符")
    @Schema(description = "食材分类", example = "蔬菜")
    String ingredientCategory,
    @NotNull(message = "食材用量不能为空")
    @DecimalMin(value = "0.00", message = "食材用量不能小于0")
    @DecimalMax(value = "99999999.99", message = "食材用量超出范围")
    @Digits(integer = 8, fraction = 2, message = "食材用量最多保留2位小数")
    @Schema(description = "默认用量", example = "100.00")
    BigDecimal quantity,
    @NotBlank(message = "食材单位不能为空")
    @Size(max = 20, message = "食材单位最多20个字符")
    @Schema(description = "计量单位", example = "克")
    String unit,
    @NotBlank(message = "食材计算方式不能为空")
    @Schema(description = "用量计算方式", allowableValues = {"FIXED", "PER_PERSON", "NO_PURCHASE"})
    String calcType,
    @NotNull(message = "食材排序值不能为空")
    @Min(value = -1000000, message = "食材排序值不能小于-1000000")
    @Max(value = 1000000, message = "食材排序值不能大于1000000")
    @Schema(description = "排序值", example = "1")
    Integer sortOrder) {
}
