package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 平台模板管理分页结果。
 * @param items 当前页数据
 * @param total 匹配总数
 * @param page 当前页码
 * @param pageSize 每页数量
 */
@Schema(description = "平台模板管理分页结果")
public record AdminDishTemplatePageView(List<AdminDishTemplateView> items, long total,
    int page, int pageSize) { }
