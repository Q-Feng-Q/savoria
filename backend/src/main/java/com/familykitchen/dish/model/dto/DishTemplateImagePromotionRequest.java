package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 将内部审核图片发布为公共模板图片的授权声明。
 * @param internalAssetId 内部受控图片资源ID
 * @param author 图片作者或来源平台
 * @param sourceUrl 图片来源页面
 * @param license 图片许可证或授权说明
 * @param expectedVersion 期望模板版本
 */
@Schema(description = "模板图片审核发布请求")
public record DishTemplateImagePromotionRequest(
    @NotNull Long internalAssetId,
    @NotBlank @Size(max = 255) String author,
    @NotBlank @Size(max = 1000) String sourceUrl,
    @NotBlank @Size(max = 255) String license,
    @NotNull Long expectedVersion) { }
