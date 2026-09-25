package com.familykitchen.cart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.dto.CartMutationRequest;
import com.familykitchen.cart.model.dto.CartRemarkRequest;
import com.familykitchen.cart.model.dto.ExpectedMealTimeRequest;
import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.entity.CartItemSelectionEntity;
import com.familykitchen.cart.model.vo.CartView;
import com.familykitchen.cart.service.CartApplicationService;
import com.familykitchen.cart.service.ExpectedMealTimePolicy;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.idempotency.CommandIdempotencyService;
import com.familykitchen.common.security.CurrentUserContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements the versioned, single-active-cart family collaboration model. */
@Service
public class CartApplicationServiceImpl implements CartApplicationService {
  private static final int ITEM_REMARK_MAX = 255;
  private static final int CART_REMARK_MAX = 500;
  private final CartMapper mapper;
  private final ExpectedMealTimePolicy timePolicy;
  private final CommandIdempotencyService commands;
  private final ObjectMapper objectMapper;

  /**
   * Creates the shared-cart service.
   *
   * @param mapper persistence mapper
   * @param timePolicy expected-time policy
   * @param commands durable command service
   * @param objectMapper response snapshot serializer
   */
  public CartApplicationServiceImpl(
      CartMapper mapper, ExpectedMealTimePolicy timePolicy, CommandIdempotencyService commands,
      ObjectMapper objectMapper) {
    this.mapper = mapper;
    this.timePolicy = timePolicy;
    this.commands = commands;
    this.objectMapper = objectMapper;
  }

  /**
   * Returns the current active family cart.
   *
   * @param user current user
   * @return authoritative cart view
   */
  @Override
  @Transactional
  public CartView cart(CurrentUserContext user) {
    requireFamilyMember(user);
    return buildView(getOrCreateCart(user), user.memberId());
  }

  /**
   * Sets the current member's absolute dish quantity.
   *
   * @param user current user
   * @param request versioned mutation
   * @return authoritative cart view
   */
  @Override
  @Transactional
  public CartView mutateItem(CurrentUserContext user, CartMutationRequest request) {
    requireFamilyMember(user);
    String remark = normalizeRemark(request.itemRemark(), ITEM_REMARK_MAX, "单品备注");
    return execute(user, "CART_SET_ITEM", request.requestId(),
        "cart=" + number(request.cartId()) + "&version=" + request.cartVersion()
            + "&dish=" + request.dishId() + "&quantity=" + request.quantity()
            + "&remark=" + encoded(remark),
        () -> {
          lockDishAvailability(user, request.dishId());
          CartDishSnapshot newCartDish = null;
          if (request.quantity() > 0 && request.cartId() == null && request.cartVersion() == 0) {
            newCartDish = requireAvailableDish(user.familyId(), request.dishId());
          }
          CartEntity cart = requireMutableCart(user, request.cartId(), request.cartVersion());
          CartItemEntity item = mapper.selectCartItem(cart.getId(), request.dishId());
          CartItemSelectionEntity current = item == null ? null
              : mapper.selectSelectionForUpdate(item.getId(), user.memberId());
          int currentQuantity = current == null ? 0 : current.quantity;
          CartDishSnapshot dish = request.quantity() > currentQuantity
              ? newCartDish == null
                  ? requireAvailableDish(user.familyId(), request.dishId()) : newCartDish
              : null;
          bump(cart, request.cartVersion());
          applySelection(cart, item, user.memberId(), request.dishId(), request.quantity(), remark, dish);
          return cart.getId();
        });
  }

  /**
   * Updates the expected meal time.
   *
   * @param user current user
   * @param request versioned mutation
   * @return authoritative cart view
   */
  @Override
  @Transactional
  public CartView updateExpectedMealTime(CurrentUserContext user, ExpectedMealTimeRequest request) {
    requireFamilyMember(user);
    return execute(user, "CART_SET_EXPECTED_TIME", request.requestId(),
        "cart=" + request.cartId() + "&version=" + request.cartVersion()
            + "&expected=" + request.expectedMealTime(),
        () -> {
          LocalDateTime selected = timePolicy.requireValid(request.expectedMealTime());
          CartEntity cart = requireMutableCart(user, request.cartId(), request.cartVersion());
          bump(cart, request.cartVersion());
          if (mapper.updateExpectedMealTime(cart.getId(), selected) != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "餐篮预计用餐时间更新失败");
          }
          return cart.getId();
        });
  }

  /**
   * Updates the shared cart remark.
   *
   * @param user current user
   * @param request versioned mutation
   * @return authoritative cart view
   */
  @Override
  @Transactional
  public CartView updateRemark(CurrentUserContext user, CartRemarkRequest request) {
    requireFamilyMember(user);
    String remark = normalizeRemark(request.remark(), CART_REMARK_MAX, "餐篮备注");
    return execute(user, "CART_SET_REMARK", request.requestId(),
        "cart=" + request.cartId() + "&version=" + request.cartVersion()
            + "&remark=" + encoded(remark),
        () -> {
          CartEntity cart = requireMutableCart(user, request.cartId(), request.cartVersion());
          bump(cart, request.cartVersion());
          if (mapper.updateCartRemark(cart.getId(), remark) != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "餐篮备注更新失败");
          }
          return cart.getId();
        });
  }

  private CartView execute(
      CurrentUserContext user, String operation, String requestId, String payload,
      java.util.function.Supplier<Long> action) {
    CommandIdempotencyService.Result result = commands.execute(new CommandIdempotencyService.Command(
        user.userId(), user.familyId(), operation, requestId, payload), () -> {
          Long cartId = action.get();
          CartView view = buildView(
              mapper.selectFamilyCart(cartId, user.familyId()), user.memberId());
          return new CommandIdempotencyService.Result("cart", cartId, write(view));
        });
    return read(result.body());
  }

  private CartEntity requireMutableCart(
      CurrentUserContext user, Long requestedCartId, long expectedVersion) {
    CartEntity active = mapper.selectFamilyActiveCart(user.familyId());
    if (active == null) {
      if (requestedCartId != null || expectedVersion != 0) {
        submitted("餐篮已提交，请刷新后继续点餐");
      }
      Long merchantId = mapper.lockFamilyForCart(user.familyId());
      if (merchantId == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "家庭不存在");
      }
      active = mapper.selectFamilyActiveCartForUpdate(user.familyId());
      if (active == null) {
        active = createCart(user, merchantId);
      }
    } else if (requestedCartId == null) {
      if (expectedVersion != 0 || version(active) != 0) {
        changed("家庭餐篮已由其他成员更新，请刷新后重试");
      }
    } else if (!requestedCartId.equals(active.getId())) {
      CartEntity requested = mapper.selectFamilyCart(requestedCartId, user.familyId());
      if (requested == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "餐篮不存在");
      }
      submitted("餐篮已提交，请刷新后继续点餐");
    }
    if (version(active) != expectedVersion) {
      changed("餐篮版本已变化，请刷新后重试");
    }
    return active;
  }

  private CartEntity createCart(CurrentUserContext user, Long merchantId) {
    CartEntity created = new CartEntity();
    created.setMerchantId(merchantId);
    created.setFamilyId(user.familyId());
    created.setMemberId(null);
    created.setMealSlotId(null);
    created.setServiceDate(null);
    created.setStatus("active");
    created.setVersion(0L);
    try {
      mapper.insertCart(created);
      return created;
    } catch (DuplicateKeyException duplicate) {
      CartEntity winner = mapper.selectFamilyActiveCartForUpdate(user.familyId());
      if (winner == null) {
        throw duplicate;
      }
      return winner;
    }
  }

  private CartEntity getOrCreateCart(CurrentUserContext user) {
    CartEntity active = mapper.selectFamilyActiveCart(user.familyId());
    if (active != null) {
      return active;
    }
    Long merchantId = mapper.lockFamilyForCart(user.familyId());
    if (merchantId == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "家庭不存在");
    }
    active = mapper.selectFamilyActiveCartForUpdate(user.familyId());
    return active == null ? createCart(user, merchantId) : active;
  }

  private void bump(CartEntity cart, long expectedVersion) {
    if (mapper.bumpVersion(cart.getId(), cart.getFamilyId(), expectedVersion) != 1) {
      CartEntity latest = mapper.selectFamilyCartForUpdate(cart.getId(), cart.getFamilyId());
      if (latest == null || !"active".equalsIgnoreCase(latest.getStatus())) {
        submitted("餐篮已提交，请刷新后继续点餐");
      }
      changed("餐篮版本已变化，请刷新后重试");
    }
  }

  private void applySelection(
      CartEntity cart, CartItemEntity item, Long memberId, Long dishId, int quantity, String remark,
      CartDishSnapshot dish) {
    if (item == null && quantity == 0) {
      return;
    }
    if (item == null) {
      item = new CartItemEntity();
      item.setCartId(cart.getId());
      item.setDishId(dish.dishId());
      item.setDishNameSnapshot(dish.dishName());
      item.setPrice(money(dish.price()));
      item.setQuantity(quantity);
      item.setItemRemark(null);
      mapper.insertCartItem(item);
    } else if (quantity > 0 && dish != null) {
      item.setDishNameSnapshot(dish.dishName());
      item.setPrice(money(dish.price()));
    }
    if (quantity == 0) {
      mapper.deleteSelection(item.getId(), memberId);
    } else {
      mapper.upsertSelection(item.getId(), memberId, quantity, remark);
    }
    int aggregate = mapper.sumSelections(item.getId());
    if (aggregate == 0) {
      mapper.deleteAggregateItem(item.getId());
    } else {
      item.setQuantity(aggregate);
      item.setItemRemark(null);
      mapper.updateCartItem(item);
    }
  }

  private CartDishSnapshot requireAvailableDish(Long familyId, Long dishId) {
    CartDishSnapshot dish = mapper.selectAvailableDish(familyId, dishId);
    if (dish == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到可点菜品");
    }
    return dish;
  }

  private void lockDishAvailability(CurrentUserContext user, Long dishId) {
    Long merchantId = mapper.selectFamilyMerchant(user.familyId());
    if (merchantId == null || merchantId <= 0) merchantId = user.merchantId();
    if (merchantId == null || merchantId <= 0)
      throw new BusinessException(ErrorCode.NOT_FOUND, "家庭所属商户不存在");
    mapper.lockMerchantForCart(merchantId);
    List<Long> dishIds = List.of(dishId);
    mapper.lockDishesForCart(merchantId, dishIds);
    mapper.lockFamilyMenuItemsForCart(user.familyId(), dishIds);
  }

  private CartView buildView(CartEntity cart, Long currentMemberId) {
    ExpectedMealTimePolicy.Snapshot time = timePolicy.snapshot();
    if (cart == null) {
      return new CartView(time.serverNow(), time.serverDate(), time.minimumExpectedMealTime(),
          time.timeStepMinutes(), time.bookingEnded(), null, null, 0, null, null, 0,
          money(null), List.of());
    }
    List<CartView.CartItemView> items = mapper.selectCartItems(cart.getId()).stream()
        .map(item -> toItemView(item, currentMemberId))
        .toList();
    int totalQuantity = items.stream().mapToInt(CartView.CartItemView::quantity).sum();
    BigDecimal totalAmount = items.stream()
        .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(money(null), BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
    return new CartView(time.serverNow(), time.serverDate(), time.minimumExpectedMealTime(),
        time.timeStepMinutes(), time.bookingEnded(), cart.getId(), cart.getFamilyId(),
        version(cart), cart.getExpectedMealTime(), cart.getRemark(), totalQuantity,
        totalAmount, items);
  }

  private CartView.CartItemView toItemView(CartItemEntity item, Long currentMemberId) {
    List<CartItemSelectionEntity> rows = mapper.selectSelections(item.getId());
    CartItemSelectionEntity own = rows.stream()
        .filter(row -> currentMemberId.equals(row.userId))
        .findFirst().orElse(null);
    List<CartView.SelectionView> selections = rows.stream()
        .map(row -> new CartView.SelectionView(
            row.userId, row.memberName, row.quantity, row.itemRemark))
        .toList();
    return new CartView.CartItemView(item.getId(), item.getDishId(),
        item.getDishNameSnapshot(), money(item.getPrice()), item.getQuantity(),
        own == null ? 0 : own.quantity, own == null ? null : own.itemRemark, selections,
        !Boolean.FALSE.equals(item.getAvailable()), item.getUnavailableReason());
  }

  private static long version(CartEntity cart) {
    return cart.getVersion() == null ? 0 : cart.getVersion();
  }

  private static void requireFamilyMember(CurrentUserContext user) {
    if (user == null || user.userId() == null || user.familyId() == null
        || user.memberId() == null) {
      throw new BusinessException(ErrorCode.FAMILY_NOT_JOINED, "当前账号尚未加入可点餐家庭");
    }
  }

  private static String normalizeRemark(String value, int max, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > max) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, field + "不能超过 " + max + " 个字符");
    }
    return normalized;
  }

  private static String encoded(String value) {
    return value == null ? "-1:" : value.length() + ":" + value;
  }

  private static String number(Long value) {
    return value == null ? "null" : value.toString();
  }

  private static BigDecimal money(BigDecimal value) {
    return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
  }

  private String write(CartView view) {
    try {
      return objectMapper.writeValueAsString(view);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "餐篮响应序列化失败");
    }
  }

  private CartView read(String body) {
    if (body == null || body.isBlank()) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "餐篮幂等响应缺失");
    }
    try {
      return objectMapper.readValue(body, CartView.class);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "餐篮幂等响应解析失败");
    }
  }

  private static void changed(String message) {
    throw new BusinessException(ErrorCode.CART_CHANGED, message);
  }

  private static void submitted(String message) {
    throw new BusinessException(ErrorCode.CART_SUBMITTED, message);
  }
}
