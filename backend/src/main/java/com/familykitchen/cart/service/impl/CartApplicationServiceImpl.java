package com.familykitchen.cart.service.impl;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.dto.CartItemRequest;
import com.familykitchen.cart.model.dto.CartRemarkRequest;
import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.vo.CartView;
import com.familykitchen.cart.service.CartApplicationService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 家庭端餐篮应用服务实现。
 *
 * <p>提供餐篮查询、加菜、改数量、删菜和备注维护能力，所有操作都围绕当前家庭成员在
 * 指定餐次与日期下的餐篮进行。</p>
 */
@Service
public class CartApplicationServiceImpl implements CartApplicationService {

  private final CartMapper cartMapper;

  /**
   * 创建购物车实例。
   *
   * @param cartMapper 购物车Mapper
   */
  public CartApplicationServiceImpl(CartMapper cartMapper) {
    this.cartMapper = cartMapper;
  }

  /**
   * 处理购物车。
   *
   * @param user 用户
   * @param mealSlotId mealSlot标识
   * @param date date
   * @return 处理的结果
   */
  @Override
  public CartView cart(CurrentUserContext user, Long mealSlotId, LocalDate date) {
    CartEntity cart = cartMapper.selectActiveCart(user.familyId(), user.memberId(), mealSlotId, date);
    if (cart == null) {
      return emptyCart(user.familyId(), mealSlotId, date);
    }
    return buildCartView(cart, user);
  }

  /**
   * 新增项目。
   *
   * @param user 用户
   * @param request 请求参数
   * @return 新增项目的结果
   */
  @Override
  @Transactional
  public CartView.CartItemView addItem(CurrentUserContext user, CartItemRequest request) {
    // 同菜品重复加入时直接累计数量，避免餐篮里出现多条相同菜品记录。
    CartDishSnapshot dish = requireAvailableDish(user.familyId(), request.dishId());
    CartEntity cart = ensureCart(user, request.mealSlotId(), request.date());
    CartItemEntity item = cartMapper.selectCartItem(cart.getId(), request.dishId());
    if (item == null) {
      item = new CartItemEntity();
      item.setCartId(cart.getId());
      item.setDishId(dish.dishId());
      item.setDishNameSnapshot(dish.dishName());
      item.setPrice(money(dish.price()));
      item.setQuantity(request.quantity());
      item.setItemRemark(request.itemRemark());
      cartMapper.insertCartItem(item);
    } else {
      item.setQuantity(item.getQuantity() + request.quantity());
      item.setItemRemark(request.itemRemark());
      cartMapper.updateCartItem(item);
    }
    return toItemView(item, user.memberId(), true);
  }

  /**
   * 更新项目。
   *
   * @param user 用户
   * @param itemId 项目标识
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void updateItem(CurrentUserContext user, Long itemId, CartItemRequest request) {
    CartItemEntity item = requireCartItem(user, itemId);
    item.setQuantity(request.quantity());
    item.setItemRemark(request.itemRemark());
    cartMapper.updateCartItem(item);
  }

  /**
   * 删除项目。
   *
   * @param user 用户
   * @param itemId 项目标识
   */
  @Override
  @Transactional
  public void deleteItem(CurrentUserContext user, Long itemId) {
    int affected = cartMapper.deleteCartItem(user.familyId(), user.memberId(), itemId);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到餐篮项");
    }
  }

  /**
   * 更新备注。
   *
   * @param user 用户
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void updateRemark(CurrentUserContext user, CartRemarkRequest request) {
    CartEntity cart = ensureCart(user, request.mealSlotId(), request.date());
    cartMapper.updateCartRemark(cart.getId(), request.remark());
  }

  /**
   * 确保当前成员在指定餐次和日期下存在活动餐篮，不存在则自动创建。
   */
  private CartEntity ensureCart(CurrentUserContext user, Long mealSlotId, LocalDate date) {
    if (cartMapper.countAvailableMealSlot(user.familyId(), mealSlotId) == 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "餐次不存在、未启用或不属于当前家庭");
    }
    CartEntity cart = cartMapper.selectActiveCart(user.familyId(), user.memberId(), mealSlotId, date);
    if (cart != null) {
      return cart;
    }
    CartEntity created = new CartEntity();
    created.setMerchantId(user.merchantId());
    created.setFamilyId(user.familyId());
    created.setMemberId(user.memberId());
    created.setMealSlotId(mealSlotId);
    created.setServiceDate(date);
    created.setStatus("active");
    cartMapper.insertCart(created);
    return created;
  }

  /**
   * 校验菜品对当前家庭可见且可售。
   */
  private CartDishSnapshot requireAvailableDish(Long familyId, Long dishId) {
    CartDishSnapshot dish = cartMapper.selectAvailableDish(familyId, dishId);
    if (dish == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到可点菜品");
    }
    return dish;
  }

  /**
   * 校验餐篮项属于当前家庭成员，避免跨成员修改。
   */
  private CartItemEntity requireCartItem(CurrentUserContext user, Long itemId) {
    CartItemEntity item = cartMapper.selectCartItemById(user.familyId(), user.memberId(), itemId);
    if (item == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到餐篮项");
    }
    return item;
  }

  /**
   * 组装餐篮视图，并同步计算总份数与总金额。
   */
  private CartView buildCartView(CartEntity cart, CurrentUserContext user) {
    List<CartView.CartItemView> items = cartMapper.selectCartItems(cart.getId()).stream()
        .map(item -> toItemView(item, user.memberId(), true))
        .toList();
    int totalQuantity = items.stream().mapToInt(CartView.CartItemView::quantity).sum();
    BigDecimal totalAmount = items.stream()
        .map(item -> money(item.price()).multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
    return new CartView(cart.getFamilyId(), cart.getMealSlotId(), cart.getServiceDate(), cart.getRemark(), totalQuantity, totalAmount, items);
  }

  /**
   * 返回空餐篮视图，供前端直接渲染初始状态。
   */
  private CartView emptyCart(Long familyId, Long mealSlotId, LocalDate date) {
    return new CartView(
        familyId,
        mealSlotId,
        date,
        null,
        0,
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
        List.of()
    );
  }

  /**
   * 统一转换餐篮项返回视图。
   */
  private static CartView.CartItemView toItemView(CartItemEntity item, Long memberId, boolean editable) {
    return new CartView.CartItemView(
        item.getId(),
        item.getDishId(),
        item.getDishNameSnapshot(),
        money(item.getPrice()),
        item.getQuantity(),
        memberId,
        null,
        editable,
        item.getItemRemark()
    );
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
