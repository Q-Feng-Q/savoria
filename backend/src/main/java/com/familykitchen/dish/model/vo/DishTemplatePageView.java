package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 平台菜品模板分页结果。
 * @param items 当前页模板列表
 * @param total 匹配模板总数
 * @param page 当前页码
 * @param pageSize 每页数量
 */
@Schema(description = "平台菜品模板分页结果")
public record DishTemplatePageView(List<DishTemplateView> items, long total, int page, int pageSize) { }
