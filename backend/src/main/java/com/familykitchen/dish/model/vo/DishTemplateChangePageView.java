package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 模板菜品修改申请分页结果。
 * @param items 当前页申请列表
 * @param total 匹配申请总数
 * @param page 当前页码
 * @param pageSize 每页数量
 */
@Schema(description = "模板菜品修改申请分页结果")
public record DishTemplateChangePageView(List<DishTemplateChangeItemView> items, long total, int page,
    int pageSize) {
}
