package com.familykitchen.family.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 承载Save家庭菜单相关的请求参数。
 *
 * @param items 项目列表
 */
public record SaveFamilyMenuRequest(
    @NotEmpty List<@Valid MenuItem> items
) {

  /**
   * 承载菜单项目相关的请求参数。
   *
   * @param dishId 菜品标识
   * @param enabled 是否启用
   * @param sortOrder sort订单
   * @param familyFinalPrice 家庭FinalPrice
   */
  public record MenuItem(
      @NotNull Long dishId,
      boolean enabled,
      int sortOrder,
      @DecimalMin("0.00")
      BigDecimal familyFinalPrice
  ) {
  }
}

