package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模板编辑或图片发布后的服务端派生状态。
 * @param templateId 模板ID
 * @param version 变更后的版本号
 * @param dataStatus 服务端重新计算的数据状态
 * @param procurementReady 服务端重新计算的采购就绪状态
 */
@Schema(description = "平台模板变更结果")
public record DishTemplateMutationView(Long templateId, Long version, String dataStatus,
    boolean procurementReady) { }
