package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 模板菜品完整业务快照，不包含数据库主键、模板编码和创建时间等技术字段。
 *
 * @param schemaVersion 快照结构版本
 * @param categoryId 平台模板分类 ID
 * @param name 菜品名称
 * @param description 菜品简介
 * @param imageUrl 本地图片访问路径
 * @param imageSourceUrl 图片原始来源页面
 * @param imageAuthor 图片作者或来源平台
 * @param imageLicense 图片授权说明
 * @param referencePrice 参考价格
 * @param tasteTags 口味标签
 * @param mealTags 推荐餐次
 * @param sortOrder 排序值
 * @param enabled 是否启用
 * @param ingredients 完整食材列表
 */
@Schema(description = "模板菜品完整覆盖快照")
public record DishTemplateSnapshotRequest(
    @NotNull(message = "快照版本不能为空")
    @Schema(description = "快照结构版本，当前固定为1", example = "1")
    Integer schemaVersion,
    @NotNull(message = "模板分类不能为空")
    @Min(value = 1, message = "模板分类ID必须为正整数")
    @Schema(description = "平台模板分类ID", example = "1")
    Long categoryId,
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称最多100个字符")
    @Schema(description = "菜品名称", example = "豆角焖面")
    String name,
    @NotBlank(message = "菜品简介不能为空")
    @Size(max = 255, message = "菜品简介最多255个字符")
    @Schema(description = "菜品简介", example = "北方家常焖面")
    String description,
    @NotBlank(message = "菜品图片不能为空")
    @Size(max = 500, message = "菜品图片地址最多500个字符")
    @Schema(description = "本地图片访问路径", example = "/images/dish-templates/dou-jiao-men-mian.jpg")
    String imageUrl,
    @NotBlank(message = "图片来源地址不能为空")
    @Size(max = 1000, message = "图片来源地址最多1000个字符")
    @Schema(description = "图片原始来源页面")
    String imageSourceUrl,
    @NotBlank(message = "图片作者不能为空")
    @Size(max = 255, message = "图片作者最多255个字符")
    @Schema(description = "图片作者或来源平台")
    String imageAuthor,
    @NotBlank(message = "图片授权说明不能为空")
    @Size(max = 255, message = "图片授权说明最多255个字符")
    @Schema(description = "图片许可证或授权说明")
    String imageLicense,
    @NotNull(message = "参考价格不能为空")
    @DecimalMin(value = "0.00", message = "参考价格不能小于0")
    @DecimalMax(value = "99999999.99", message = "参考价格超出范围")
    @Digits(integer = 8, fraction = 2, message = "参考价格最多保留2位小数")
    @Schema(description = "参考价格", example = "18.00")
    BigDecimal referencePrice,
    @NotNull(message = "口味标签不能为空")
    @Size(max = 10, message = "口味标签最多10项")
    @Schema(description = "口味标签，每项最多20个字符")
    List<String> tasteTags,
    @NotNull(message = "推荐餐次不能为空")
    @Size(max = 3, message = "推荐餐次最多3项")
    @Schema(description = "推荐餐次", allowableValues = {"BREAKFAST", "LUNCH", "DINNER"})
    List<String> mealTags,
    @NotNull(message = "模板排序值不能为空")
    @Min(value = -1000000, message = "模板排序值不能小于-1000000")
    @Max(value = 1000000, message = "模板排序值不能大于1000000")
    @Schema(description = "排序值", example = "10")
    Integer sortOrder,
    @NotNull(message = "启用状态不能为空")
    @Schema(description = "审核通过后模板是否启用", example = "true")
    Boolean enabled,
    @NotEmpty(message = "模板菜品至少需要1项食材")
    @Size(max = 100, message = "模板菜品最多包含100项食材")
    @Valid
    @Schema(description = "完整食材列表，审核通过时整体替换")
    List<DishTemplateIngredientSnapshotRequest> ingredients) {
}
