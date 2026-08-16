package com.familykitchen.family.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 商户为家庭设置首页推荐菜请求。
 *
 * @param dishId 推荐菜品标识
 */
public record SetFeaturedDishRequest(@NotNull Long dishId) {
}
