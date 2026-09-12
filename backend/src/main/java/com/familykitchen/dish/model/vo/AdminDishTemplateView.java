package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 平台模板管理列表项。
 * @param templateId 模板ID
 * @param templateCode 稳定模板编码
 * @param name 模板名称
 * @param sourceCategory 来源分类
 * @param sourceType 来源类型
 * @param templateType 模板类型
 * @param dataStatus 数据完整状态
 * @param procurementReady 采购用量是否就绪
 * @param imageRightsStatus 图片权利状态
 * @param imageUrl 公共图片地址
 * @param referencePrice 参考价格
 * @param missingSteps 是否缺少制作步骤
 * @param sourceRevision 来源固定版本
 * @param enabled 是否启用
 * @param version 并发版本号
 */
@Schema(description = "平台模板管理列表项")
public record AdminDishTemplateView(Long templateId, String templateCode, String name,
    String sourceCategory, String sourceType, String templateType, String dataStatus,
    boolean procurementReady, String imageRightsStatus, String imageUrl, BigDecimal referencePrice,
    boolean missingSteps, String sourceRevision, boolean enabled, Long version) { }
