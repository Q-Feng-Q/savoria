package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 内部图片审核状态变更结果。
 * @param assetId 受控图片资源ID
 * @param assetStatus 变更后的审核状态
 */
@Schema(description = "模板内部图片审核状态")
public record DishTemplateImageAssetStatusView(Long assetId, String assetStatus) { }
