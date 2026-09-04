package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模板审核快照中的稳定制作步骤。
 * @param itemId 跨快照稳定的步骤项ID
 * @param stepNo 连续步骤序号
 * @param title 步骤标题
 * @param content 完整操作内容
 * @param durationSeconds 持续秒数
 * @param temperatureText 温度说明
 * @param heatLevel 火候说明
 * @param componentTemplateId 当前步骤引用的组件模板ID
 */
@Schema(description = "模板审核快照制作步骤")
public record DishTemplateCookingStepSnapshotRequest(String itemId, Integer stepNo, String title,
    String content, Integer durationSeconds, String temperatureText, String heatLevel,
    Long componentTemplateId) { }
