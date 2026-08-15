package com.familykitchen.cart.service;

import com.familykitchen.cart.model.dto.CartItemRequest;
import com.familykitchen.cart.model.dto.CartRemarkRequest;
import com.familykitchen.cart.model.vo.CartView;
import com.familykitchen.common.security.CurrentUserContext;
import java.time.LocalDate;

/**
 * 餐篮服务。
 *
 * <p>餐篮是家庭端下单前的购物车，按家庭、成员、餐次和日期隔离。服务负责查询餐篮、
 * 添加菜品、修改数量、删除菜品和维护点餐备注。</p>
 */
public interface CartApplicationService {

  /**
   * 查询指定日期和餐次的餐篮。
   *
   * @param user 当前登录用户上下文
   * @param mealSlotId 餐次 ID
   * @param date 服务日期
   * @return 餐篮视图
   */
  CartView cart(CurrentUserContext user, Long mealSlotId, LocalDate date);

  /**
   * 添加菜品到餐篮。
   *
   * @param user 当前登录用户上下文
   * @param request 菜品、数量、餐次、日期和备注
   * @return 添加后的餐篮菜品视图
   */
  CartView.CartItemView addItem(CurrentUserContext user, CartItemRequest request);

  /**
   * 修改餐篮菜品数量、日期、餐次或备注。
   *
   * @param user 当前登录用户上下文
   * @param itemId 餐篮菜品 ID
   * @param request 修改内容
   */
  void updateItem(CurrentUserContext user, Long itemId, CartItemRequest request);

  /**
   * 删除餐篮菜品。
   *
   * @param user 当前登录用户上下文
   * @param itemId 餐篮菜品 ID
   */
  void deleteItem(CurrentUserContext user, Long itemId);

  /**
   * 修改餐篮整体点餐备注。
   *
   * @param user 当前登录用户上下文
   * @param request 餐次、日期和备注内容
   */
  void updateRemark(CurrentUserContext user, CartRemarkRequest request);
}
