package com.familykitchen.order.service.impl;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.order.mapper.OrderPersistenceMapper;
import com.familykitchen.order.model.bo.CheckoutIngredient;
import com.familykitchen.order.model.bo.CheckoutItem;
import com.familykitchen.order.model.bo.DeliverySnapshot;
import com.familykitchen.order.model.bo.FamilyDeliveryPolicy;
import com.familykitchen.order.model.bo.OrderCartSnapshot;
import com.familykitchen.order.model.bo.OrderCheckoutCommand;
import com.familykitchen.order.model.bo.OrderSubmissionResult;
import com.familykitchen.order.model.dto.SubmitOrderRequest;
import com.familykitchen.order.model.entity.OrderDeliverySnapshotEntity;
import com.familykitchen.order.model.entity.OrderItemEntity;
import com.familykitchen.order.model.entity.OrderRecordEntity;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.model.vo.OrderView;
import com.familykitchen.order.service.FamilyOrderApplicationService;
import com.familykitchen.order.service.OrderStateMachine;
import com.familykitchen.order.service.OrderSubmissionService;
import com.familykitchen.purchase.model.enums.IngredientCalcType;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.wallet.model.bo.WalletAccount;
import com.familykitchen.wallet.model.bo.WalletChange;
import com.familykitchen.wallet.model.entity.WalletAccountDO;
import com.familykitchen.wallet.model.entity.WalletLedgerDO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 家庭侧订单应用服务实现。
 *
 * <p>采用标准 MVC 分层，直接在 Service 中协调多个 Mapper，完成下单、改单、取消订单、
 * 钱包冻结与解冻、订单通知发送等流程。</p>
 */
@Service
@Transactional
public class FamilyOrderApplicationServiceImpl implements FamilyOrderApplicationService {

  private final OrderSubmissionService orderSubmissionService;
  private final OrderPersistenceMapper orderMapper;
  private final CartMapper cartMapper;
  private final DishMapper dishMapper;
  private final FamilyMapper familyMapper;
  private final WalletPersistenceMapper walletMapper;
  private final NotificationPersistenceMapper notificationMapper;
  private final OrderStateMachine orderStateMachine;

  /**
   * 创建家庭订单实例。
   *
   * @param orderSubmissionService 订单SubmissionService
   * @param orderMapper 订单Mapper
   * @param cartMapper 购物车Mapper
   * @param dishMapper 菜品Mapper
   * @param familyMapper 家庭Mapper
   * @param walletMapper 钱包Mapper
   * @param notificationMapper 通知Mapper
   * @param orderStateMachine 订单StateMachine
   */
  public FamilyOrderApplicationServiceImpl(
      OrderSubmissionService orderSubmissionService,
      OrderPersistenceMapper orderMapper,
      CartMapper cartMapper,
      DishMapper dishMapper,
      FamilyMapper familyMapper,
      WalletPersistenceMapper walletMapper,
      NotificationPersistenceMapper notificationMapper,
      OrderStateMachine orderStateMachine
  ) {
    this.orderSubmissionService = orderSubmissionService;
    this.orderMapper = orderMapper;
    this.cartMapper = cartMapper;
    this.dishMapper = dishMapper;
    this.familyMapper = familyMapper;
    this.walletMapper = walletMapper;
    this.notificationMapper = notificationMapper;
    this.orderStateMachine = orderStateMachine;
  }

  /**
   * 提交家庭订单。
   *
   * @param user 用户
   * @param request 请求参数
   * @return 提交的结果
   */
  @Override
  public OrderView submit(CurrentUserContext user, SubmitOrderRequest request) {
    // 下单以当前餐篮为唯一事实来源，避免前端直接传金额或菜品快照导致数据不一致。
    OrderCartSnapshot cart = loadCart(user.familyId(), user.memberId(), request.mealSlotId(), request.date());
    validateCart(cart);

    DeliverySnapshot deliverySnapshot = request.deliveryMode() == DeliveryMode.DELIVERY
        ? loadDeliverySnapshot(user.familyId(), request.addressId())
        : null;
    FamilyDeliveryPolicy deliveryPolicy = loadDeliveryPolicy(user.familyId());

    Set<Long> memberIds = collectMemberIds(cart.items(), user.memberId());
    Map<Long, WalletAccount> wallets = loadWallets(memberIds);

    OrderCheckoutCommand command = new OrderCheckoutCommand(
        user.merchantId(),
        user.familyId(),
        user.memberId(),
        request.mealSlotId(),
        request.date(),
        request.deliveryMode(),
        request.remark() == null || request.remark().isBlank() ? cart.remark() : request.remark(),
        deliverySnapshot,
        deliveryPolicy,
        cart.items()
    );

    OrderSubmissionResult result = orderSubmissionService.submit(command, wallets);
    saveSubmittedOrder(result);
    clearCart(user.familyId(), user.memberId(), request.mealSlotId(), request.date());
    return toView(result.order());
  }

  /**
   * 更新家庭订单。
   *
   * @param user 用户
   * @param orderId 订单标识
   */
  @Override
  public void update(CurrentUserContext user, Long orderId) {
    // 改单沿用“先改餐篮、再重算订单”的模式，确保金额、配方和备注全部基于最新餐篮重建。
    OrderSubmissionResult.SubmittedOrder current = requireOrder(user.familyId(), orderId);
    orderStateMachine.validateFamilyEdit(current.status());

    OrderCartSnapshot cart = loadCart(
        user.familyId(),
        user.memberId(),
        current.mealSlotId(),
        current.serviceDate()
    );
    validateCart(cart);

    Map<Long, BigDecimal> previousAmounts = buildReleaseAmounts(current);
    Set<Long> walletMemberIds = collectMemberIds(cart.items(), current.submitterMemberId());
    walletMemberIds.addAll(previousAmounts.keySet());
    Map<Long, WalletAccount> wallets = loadWallets(walletMemberIds);

    List<WalletChange> releaseChanges = new ArrayList<>();
    for (Map.Entry<Long, BigDecimal> entry : previousAmounts.entrySet()) {
      WalletAccount wallet = wallets.get(entry.getKey());
      if (wallet == null) {
        throw new BusinessException(ErrorCode.BUSINESS_INVALID, "未找到成员钱包");
      }
      releaseChanges.add(wallet.release(entry.getValue()));
    }

    OrderCheckoutCommand command = new OrderCheckoutCommand(
        current.merchantId(),
        current.familyId(),
        current.submitterMemberId(),
        current.mealSlotId(),
        current.serviceDate(),
        current.deliveryMode(),
        cart.remark() == null || cart.remark().isBlank() ? current.remark() : cart.remark(),
        current.deliverySnapshot(),
        loadDeliveryPolicy(current.familyId()),
        cart.items()
    );

    OrderSubmissionResult result = orderSubmissionService.submit(command, wallets);
    replaceOrder(copyOrder(
        current,
        result.order().deliveryFee(),
        result.order().totalAmount(),
        result.order().remark(),
        result.order().items()
    ));
    addWalletChanges(releaseChanges);
    addWalletChanges(result.walletChanges());
    addNotification(
        "merchant",
        current.merchantId(),
        "order",
        "订单已更新",
        "家庭 " + current.familyId() + " 更新了订单 #" + current.orderId()
    );
    clearCart(current.familyId(), user.memberId(), current.mealSlotId(), current.serviceDate());
  }

  /**
   * 列出家庭订单。
   *
   * @param user 用户
   * @return 列出的结果
   */
  @Override
  public List<OrderView> list(CurrentUserContext user) {
    return listOrders(user.familyId()).stream()
        .map(FamilyOrderApplicationServiceImpl::toView)
        .toList();
  }

  /**
   * 处理家庭订单。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @return 处理的结果
   */
  @Override
  public OrderView detail(CurrentUserContext user, Long orderId) {
    return toView(requireOrder(user.familyId(), orderId));
  }

  /**
   * 取消家庭订单。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @param reason 原因
   */
  @Override
  public void cancel(CurrentUserContext user, Long orderId, String reason) {
    // 家庭取消订单只做冻结释放，不进入任何结算逻辑。
    OrderSubmissionResult.SubmittedOrder current = requireOrder(user.familyId(), orderId);
    OrderStatus nextStatus = orderStateMachine.familyCancel(current.status());

    Map<Long, BigDecimal> releaseAmounts = buildReleaseAmounts(current);
    Map<Long, WalletAccount> wallets = loadWallets(releaseAmounts.keySet());

    List<WalletChange> walletChanges = new ArrayList<>();
    for (Map.Entry<Long, BigDecimal> entry : releaseAmounts.entrySet()) {
      WalletAccount wallet = wallets.get(entry.getKey());
      if (wallet == null) {
        throw new BusinessException(ErrorCode.BUSINESS_INVALID, "未找到成员钱包");
      }
      walletChanges.add(wallet.release(entry.getValue()));
    }

    replaceOrder(new OrderSubmissionResult.SubmittedOrder(
        current.orderId(),
        current.merchantId(),
        current.familyId(),
        current.submitterMemberId(),
        current.mealSlotId(),
        current.serviceDate(),
        current.deliveryMode(),
        current.deliveryFee(),
        nextStatus,
        current.totalAmount(),
        current.remark(),
        blankToNull(reason),
        current.deliverySnapshot(),
        current.items()
    ));
    addWalletChanges(walletChanges);
    addNotification(
        "merchant",
        current.merchantId(),
        "order",
        "家庭取消订单",
        "家庭 " + current.familyId() + " 取消了订单 #" + current.orderId()
    );
    addNotification(
        "family",
        current.familyId(),
        "order",
        "订单已取消",
        "订单 #" + current.orderId() + " 已取消"
    );
  }

  /**
   * 按家庭读取订单，不允许跨家庭访问。
   */
  private OrderSubmissionResult.SubmittedOrder requireOrder(Long familyId, Long orderId) {
    OrderSubmissionResult.SubmittedOrder order = findOrder(familyId, orderId);
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到订单");
    }
    return order;
  }

  /**
   * 从真实餐篮加载下单快照，作为家庭下单的唯一事实来源。
   */
  private OrderCartSnapshot loadCart(Long familyId, Long memberId, Long mealSlotId, LocalDate serviceDate) {
    CartEntity cart = cartMapper.selectActiveCart(familyId, memberId, mealSlotId, serviceDate);
    if (cart == null) {
      return null;
    }
    List<CheckoutItem> items = cartMapper.selectCartItems(cart.getId()).stream()
        .map(item -> toCheckoutItem(memberId, item))
        .toList();
    return new OrderCartSnapshot(
        cart.getMerchantId(),
        familyId,
        memberId,
        mealSlotId,
        serviceDate,
        cart.getRemark(),
        items
    );
  }

  /**
   * 加载配送地址快照，确保历史订单不受后续地址修改影响。
   */
  private DeliverySnapshot loadDeliverySnapshot(Long familyId, Long addressId) {
    return familyMapper.selectAddresses(familyId).stream()
        .filter(item -> item.getId().equals(addressId))
        .findFirst()
        .map(item -> new DeliverySnapshot(item.getContactName(), item.getContactPhone(), item.getAddressText()))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "未找到配送地址"));
  }

  /**
   * 加载家庭配送策略，用于下单金额计算和配送费判定。
   */
  private FamilyDeliveryPolicy loadDeliveryPolicy(Long familyId) {
    FamilyRecord family = familyMapper.selectFamily(null, familyId);
    if (family == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到家庭");
    }
    return new FamilyDeliveryPolicy(
        Boolean.TRUE.equals(family.getDeliveryEnabled()),
        family.getDeliveryFeeDefault(),
        Boolean.TRUE.equals(family.getDeliveryFree())
    );
  }

  /**
   * 批量加载订单涉及的成员钱包。
   */
  private Map<Long, WalletAccount> loadWallets(Set<Long> memberIds) {
    if (memberIds == null || memberIds.isEmpty()) {
      return Map.of();
    }
    return walletMapper.selectWalletsByMemberIdsForUpdate(memberIds).stream()
        .collect(Collectors.toMap(
            WalletAccountDO::getMemberId,
            item -> new WalletAccount(item.getMemberId(), item.getBalanceAmount(), item.getFrozenAmount()),
            (left, right) -> left,
            LinkedHashMap::new
        ));
  }

  /**
   * 持久化订单、订单明细、地址快照、钱包流水以及通知。
   */
  private void saveSubmittedOrder(OrderSubmissionResult result) {
    OrderRecordEntity order = toOrderEntity(result.order());
    orderMapper.insertOrder(order);
    saveOrderChildren(order.getId(), result.order());
    addWalletChanges(result.walletChanges());
    for (OrderSubmissionResult.OrderNotification notification : result.notifications()) {
      addNotification(
          notification.scope(),
          notification.receiverId(),
          notification.category(),
          notification.title(),
          notification.content()
      );
    }
  }

  /**
   * 将当前活动餐篮标记为已提交，避免重复下单。
   */
  private void clearCart(Long familyId, Long memberId, Long mealSlotId, LocalDate serviceDate) {
    cartMapper.submitActiveCart(familyId, memberId, mealSlotId, serviceDate);
  }

  /**
   * 按家庭查询订单列表，并补全明细与配送快照。
   */
  private List<OrderSubmissionResult.SubmittedOrder> listOrders(Long familyId) {
    return hydrate(orderMapper.selectOrdersByFamilyId(familyId));
  }

  /**
   * 按家庭读取单个订单，并补全明细与配送快照。
   */
  private OrderSubmissionResult.SubmittedOrder findOrder(Long familyId, Long orderId) {
    OrderRecordEntity order = orderMapper.selectOrderByFamilyId(familyId, orderId);
    return order == null ? null : hydrate(List.of(order)).get(0);
  }

  /**
   * 替换订单主记录及其子表快照。
   */
  private void replaceOrder(OrderSubmissionResult.SubmittedOrder order) {
    orderMapper.updateOrder(toOrderEntity(order));
    orderMapper.deleteOrderItems(order.orderId());
    orderMapper.deleteDeliverySnapshot(order.orderId());
    saveOrderChildren(order.orderId(), order);
  }

  /**
   * 写入钱包余额与流水。
   */
  private void addWalletChanges(List<WalletChange> changes) {
    if (changes == null || changes.isEmpty()) {
      return;
    }
    for (WalletChange change : changes) {
      walletMapper.updateWalletAmounts(change.memberId(), change.balanceAfter(), change.frozenAfter());
      WalletLedgerDO ledger = new WalletLedgerDO();
      ledger.setMemberId(change.memberId());
      ledger.setType(change.type().name());
      ledger.setAmount(change.amount());
      ledger.setBalanceBefore(change.balanceBefore());
      ledger.setBalanceAfter(change.balanceAfter());
      ledger.setFrozenBefore(change.frozenBefore());
      ledger.setFrozenAfter(change.frozenAfter());
      ledger.setRemark("order");
      walletMapper.insertWalletLedger(ledger);
    }
  }

  /**
   * 写入通知中心消息。
   */
  private void addNotification(
      String receiverType,
      Long receiverId,
      String category,
      String title,
      String content
  ) {
    notificationMapper.insertNotification(receiverType, receiverId, receiverType, category, title, content);
  }

  /**
   * 将餐篮项转换为订单结算项，并实时读取菜品食材配方。
   */
  private CheckoutItem toCheckoutItem(Long memberId, CartItemEntity item) {
    List<CheckoutIngredient> ingredients = dishMapper.selectDishIngredients(item.getDishId()).stream()
        .map(ingredient -> new CheckoutIngredient(
            ingredient.getIngredientName(),
            ingredient.getQuantity(),
            ingredient.getUnit(),
            IngredientCalcType.valueOf(ingredient.getCalcType())
        ))
        .toList();
    return new CheckoutItem(
        item.getDishId(),
        item.getDishNameSnapshot(),
        memberId,
        null,
        item.getPrice(),
        item.getQuantity(),
        item.getItemRemark(),
        ingredients
    );
  }

  /**
   * 将订单主记录批量补全为完整的提交订单视图对象。
   */
  private List<OrderSubmissionResult.SubmittedOrder> hydrate(List<OrderRecordEntity> orders) {
    if (orders == null || orders.isEmpty()) {
      return List.of();
    }
    List<Long> ids = orders.stream().map(OrderRecordEntity::getId).toList();
    Map<Long, List<OrderItemEntity>> itemsByOrder = orderMapper.selectOrderItemsByOrderIds(ids).stream()
        .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
    Map<Long, OrderDeliverySnapshotEntity> snapshots = orderMapper.selectDeliverySnapshotsByOrderIds(ids).stream()
        .collect(Collectors.toMap(OrderDeliverySnapshotEntity::getOrderId, item -> item));
    return orders.stream()
        .map(order -> toSubmittedOrder(
            order,
            itemsByOrder.getOrDefault(order.getId(), List.of()),
            snapshots.get(order.getId())
        ))
        .toList();
  }

  /**
   * 将数据库订单记录转换为业务层订单快照。
   */
  private OrderSubmissionResult.SubmittedOrder toSubmittedOrder(
      OrderRecordEntity order,
      List<OrderItemEntity> items,
      OrderDeliverySnapshotEntity snapshot
  ) {
    DeliverySnapshot deliverySnapshot = snapshot == null
        ? null
        : new DeliverySnapshot(snapshot.getContactName(), snapshot.getContactPhone(), snapshot.getAddressText());
    return new OrderSubmissionResult.SubmittedOrder(
        order.getId(),
        order.getMerchantId(),
        order.getFamilyId(),
        order.getSubmitterMemberId(),
        order.getMealSlotId(),
        order.getServiceDate(),
        DeliveryMode.valueOf(order.getDeliveryMode()),
        order.getDeliveryFee(),
        OrderStatus.valueOf(order.getStatus()),
        order.getTotalAmount(),
        order.getRemark(),
        order.getCancelReason(),
        deliverySnapshot,
        items.stream()
            .map(item -> new OrderSubmissionResult.SubmittedOrderItem(
                item.getDishId(),
                item.getDishNameSnapshot(),
                item.getOwnerMemberId(),
                item.getPrice(),
                item.getQuantity(),
                item.getAmount(),
                item.getItemRemark()
            ))
            .toList()
    );
  }

  /**
   * 将业务订单对象转换为主表实体。
   */
  private OrderRecordEntity toOrderEntity(OrderSubmissionResult.SubmittedOrder order) {
    OrderRecordEntity entity = new OrderRecordEntity();
    entity.setId(order.orderId());
    entity.setMerchantId(order.merchantId());
    entity.setFamilyId(order.familyId());
    entity.setSubmitterMemberId(order.submitterMemberId());
    entity.setMealSlotId(order.mealSlotId());
    entity.setServiceDate(order.serviceDate());
    entity.setDeliveryMode(order.deliveryMode().name());
    entity.setDeliveryFee(order.deliveryFee());
    entity.setDeliveryFeePayerMemberId(order.submitterMemberId());
    entity.setStatus(order.status().name());
    entity.setTotalAmount(order.totalAmount());
    entity.setRemark(order.remark());
    entity.setCancelReason(order.cancelReason());
    return entity;
  }

  /**
   * 保存订单明细和配送快照子表。
   */
  private void saveOrderChildren(Long orderId, OrderSubmissionResult.SubmittedOrder order) {
    for (OrderSubmissionResult.SubmittedOrderItem item : order.items()) {
      OrderItemEntity entity = new OrderItemEntity();
      entity.setOrderId(orderId);
      entity.setDishId(item.dishId());
      entity.setOwnerMemberId(item.ownerMemberId());
      entity.setDishNameSnapshot(item.dishName());
      entity.setPrice(item.price());
      entity.setQuantity(item.quantity());
      entity.setAmount(item.amount());
      entity.setItemRemark(item.itemRemark());
      orderMapper.insertOrderItem(entity);
    }
    if (order.deliverySnapshot() != null) {
      OrderDeliverySnapshotEntity snapshot = new OrderDeliverySnapshotEntity();
      snapshot.setOrderId(orderId);
      snapshot.setContactName(order.deliverySnapshot().contactName());
      snapshot.setContactPhone(order.deliverySnapshot().contactPhone());
      snapshot.setAddressText(order.deliverySnapshot().addressText());
      orderMapper.insertDeliverySnapshot(snapshot);
    }
  }

  /**
   * 校验餐篮必须存在且至少包含一个菜品。
   */
  private static void validateCart(OrderCartSnapshot cart) {
    if (cart == null || cart.items() == null || cart.items().isEmpty()) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "餐篮为空");
    }
  }

  /**
   * 将订单领域对象转换为接口返回视图。
   */
  private static OrderView toView(OrderSubmissionResult.SubmittedOrder order) {
    return new OrderView(
        order.orderId(),
        order.merchantId(),
        order.familyId(),
        order.submitterMemberId(),
        order.mealSlotId(),
        order.serviceDate(),
        order.deliveryMode(),
        order.deliveryFee(),
        order.status(),
        order.totalAmount(),
        order.remark(),
        order.cancelReason(),
        order.items().stream()
            .map(item -> new OrderView.OrderItemView(
                item.dishId(),
                item.dishName(),
                item.ownerMemberId(),
                item.price(),
                item.quantity(),
                item.amount(),
                item.itemRemark()
            ))
            .toList()
    );
  }

  /**
   * 更新订单时保留原订单身份与状态，仅替换金额、备注和明细。
   */
  private static OrderSubmissionResult.SubmittedOrder copyOrder(
      OrderSubmissionResult.SubmittedOrder current,
      BigDecimal deliveryFee,
      BigDecimal totalAmount,
      String remark,
      List<OrderSubmissionResult.SubmittedOrderItem> items
  ) {
    return new OrderSubmissionResult.SubmittedOrder(
        current.orderId(),
        current.merchantId(),
        current.familyId(),
        current.submitterMemberId(),
        current.mealSlotId(),
        current.serviceDate(),
        current.deliveryMode(),
        money(deliveryFee),
        current.status(),
        money(totalAmount),
        remark,
        current.cancelReason(),
        current.deliverySnapshot(),
        items
    );
  }

  /**
   * 汇总订单涉及的钱包成员。
   */
  private static Set<Long> collectMemberIds(List<CheckoutItem> items, Long submitterMemberId) {
    Set<Long> memberIds = items.stream()
        .map(CheckoutItem::ownerMemberId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    memberIds.add(submitterMemberId);
    return memberIds;
  }

  /**
   * 按成员聚合需要释放的冻结金额。
   */
  private static Map<Long, BigDecimal> buildReleaseAmounts(OrderSubmissionResult.SubmittedOrder order) {
    Map<Long, BigDecimal> result = new LinkedHashMap<>();
    for (OrderSubmissionResult.SubmittedOrderItem item : order.items()) {
      result.merge(item.ownerMemberId(), money(item.amount()), BigDecimal::add);
    }
    result.merge(order.submitterMemberId(), money(order.deliveryFee()), BigDecimal::add);
    return result;
  }

  /**
   * 统一金额精度。
   */
  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * 空白字符串统一按 null 处理。
   */
  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
