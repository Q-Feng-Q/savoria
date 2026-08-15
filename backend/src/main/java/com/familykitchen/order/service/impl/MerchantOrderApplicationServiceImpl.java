package com.familykitchen.order.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.order.mapper.OrderPersistenceMapper;
import com.familykitchen.order.model.bo.DeliverySnapshot;
import com.familykitchen.order.model.bo.OrderSubmissionResult;
import com.familykitchen.order.model.dto.OrderStatusRequest;
import com.familykitchen.order.model.entity.OrderDeliverySnapshotEntity;
import com.familykitchen.order.model.entity.OrderItemEntity;
import com.familykitchen.order.model.entity.OrderRecordEntity;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.model.vo.OrderView;
import com.familykitchen.order.service.MerchantOrderApplicationService;
import com.familykitchen.order.service.OrderStateMachine;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.wallet.model.bo.WalletAccount;
import com.familykitchen.wallet.model.bo.WalletChange;
import com.familykitchen.wallet.model.entity.WalletAccountDO;
import com.familykitchen.wallet.model.entity.WalletLedgerDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户侧订单应用服务实现。
 *
 * <p>采用标准 MVC 分层，直接在 Service 中调用 Mapper 完成确认、驳回、取消、
 * 调整配送费、推进状态以及钱包结算、通知发送等操作。</p>
 */
@Service
@Transactional
public class MerchantOrderApplicationServiceImpl implements MerchantOrderApplicationService {

  private final OrderStateMachine orderStateMachine;
  private final OrderPersistenceMapper orderMapper;
  private final WalletPersistenceMapper walletMapper;
  private final NotificationPersistenceMapper notificationMapper;

  /**
   * 创建商户订单实例。
   *
   * @param orderStateMachine 订单StateMachine
   * @param orderMapper 订单Mapper
   * @param walletMapper 钱包Mapper
   * @param notificationMapper 通知Mapper
   */
  public MerchantOrderApplicationServiceImpl(
    OrderStateMachine orderStateMachine,
    OrderPersistenceMapper orderMapper,
    WalletPersistenceMapper walletMapper,
    NotificationPersistenceMapper notificationMapper
  ) {
    this.orderStateMachine = orderStateMachine;
    this.orderMapper = orderMapper;
    this.walletMapper = walletMapper;
    this.notificationMapper = notificationMapper;
  }

  /**
   * 列出商户订单。
   *
   * @param user 用户
   * @return 列出的结果
   */
  @Override
  public List<OrderView> list(CurrentUserContext user) {
    return merchantOrders(user.merchantId()).stream()
      .map(MerchantOrderApplicationServiceImpl::toView)
      .toList();
  }

  /**
   * 处理商户订单。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @return 处理的结果
   */
  @Override
  public OrderView detail(CurrentUserContext user, Long orderId) {
    return toView(requireOrder(user, orderId));
  }

  /**
   * 处理商户订单。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @return 处理的结果
   */
  @Override
  public OrderStatus confirm(CurrentUserContext user, Long orderId) {
    OrderSubmissionResult.SubmittedOrder current = requireOrder(user, orderId);
    OrderStatus next = orderStateMachine.confirm(current.status());
    replaceAndNotify(current, next, current.deliveryFee(), current.totalAmount(), null, "订单已确认");
    return next;
  }

  /**
   * 拒绝商户订单。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @return 拒绝的结果
   */
  @Override
  public OrderStatus reject(CurrentUserContext user, Long orderId) {
    OrderSubmissionResult.SubmittedOrder current = requireOrder(user, orderId);
    OrderStatus next = orderStateMachine.reject(current.status());
    addWalletChanges(releaseOrderFunds(current));
    replaceAndNotify(current, next, current.deliveryFee(), current.totalAmount(), null, "订单已驳回");
    return next;
  }

  /**
   * 取消商户订单。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @param reason 原因
   * @return 取消的结果
   */
  @Override
  public OrderStatus cancel(CurrentUserContext user, Long orderId, String reason) {
    OrderSubmissionResult.SubmittedOrder current = requireOrder(user, orderId);
    OrderStatus next = orderStateMachine.merchantCancel(current.status(), reason);
    addWalletChanges(releaseOrderFunds(current));
    replaceAndNotify(current, next, current.deliveryFee(), current.totalAmount(), blankToNull(reason), "订单已取消");
    return next;
  }

  /**
   * 处理配送费用。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @param deliveryFee 配送费用
   */
  @Override
  public void adjustDeliveryFee(CurrentUserContext user, Long orderId, BigDecimal deliveryFee) {
    // 配送费调整只影响提交人承担部分，因此只对提交人的钱包做补冻或释放。
    OrderSubmissionResult.SubmittedOrder current = requireOrder(user, orderId);
    orderStateMachine.validateDeliveryFeeAdjustment(current.status());

    BigDecimal targetFee = money(deliveryFee);
    BigDecimal diff = targetFee.subtract(money(current.deliveryFee())).setScale(2, RoundingMode.HALF_UP);
    WalletAccount submitterWallet = walletOf(current.submitterMemberId());

    if (diff.compareTo(BigDecimal.ZERO) > 0) {
      addWalletChanges(List.of(submitterWallet.freeze(diff)));
    } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
      addWalletChanges(List.of(submitterWallet.release(diff.abs())));
    }

    BigDecimal totalAmount = money(current.totalAmount()).subtract(money(current.deliveryFee())).add(targetFee);
    replaceOrder(copyOrder(current, current.status(), targetFee, totalAmount, current.cancelReason()));
    addNotification("family", current.familyId(), "order", "配送费已更新", "配送费已更新为 " + targetFee);
  }

  /**
   * 处理商户订单。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @param request 请求参数
   * @return 处理的结果
   */
  @Override
  public OrderStatus advance(CurrentUserContext user, Long orderId, OrderStatusRequest request) {
    OrderSubmissionResult.SubmittedOrder current = requireOrder(user, orderId);
    OrderStatus next = orderStateMachine.advance(current.status(), request.status(), request.reason());
    if (next == OrderStatus.DONE) {
      addWalletChanges(settleOrderFunds(current));
      replaceAndNotify(current, next, current.deliveryFee(), current.totalAmount(), current.cancelReason(), "订单已完成");
      return next;
    }
    replaceAndNotify(current, next, current.deliveryFee(), current.totalAmount(), current.cancelReason(), statusTitle(next));
    return next;
  }

  /**
   * 读取当前商户名下订单，防止跨商户访问。
   */
  private OrderSubmissionResult.SubmittedOrder requireOrder(CurrentUserContext user, Long orderId) {
    OrderSubmissionResult.SubmittedOrder order = findMerchantOrder(user.merchantId(), orderId);
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到订单");
    }
    return order;
  }

  /**
   * 查询商户订单列表并补全明细与配送快照。
   */
  private List<OrderSubmissionResult.SubmittedOrder> merchantOrders(Long merchantId) {
    return hydrate(orderMapper.selectOrdersByMerchantId(merchantId));
  }

  /**
   * 查询单个商户订单并补全明细与配送快照。
   */
  private OrderSubmissionResult.SubmittedOrder findMerchantOrder(Long merchantId, Long orderId) {
    OrderRecordEntity order = orderMapper.selectOrderByMerchantId(merchantId, orderId);
    return order == null ? null : hydrate(List.of(order)).get(0);
  }

  /**
   * 批量加载成员钱包。
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
   * 读取单个成员钱包。
   */
  private WalletAccount walletOf(Long memberId) {
    WalletAccountDO wallet = walletMapper.selectWalletByMemberIdForUpdate(memberId);
    if (wallet == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到成员钱包");
    }
    return new WalletAccount(wallet.getMemberId(), wallet.getBalanceAmount(), wallet.getFrozenAmount());
  }

  /**
   * 释放订单关联的全部冻结资金，用于驳回和取消。
   */
  private List<WalletChange> releaseOrderFunds(OrderSubmissionResult.SubmittedOrder order) {
    Map<Long, BigDecimal> amounts = memberAmounts(order);
    Map<Long, WalletAccount> wallets = loadWallets(amounts.keySet());
    List<WalletChange> changes = new ArrayList<>();
    for (Map.Entry<Long, BigDecimal> entry : amounts.entrySet()) {
      changes.add(wallets.get(entry.getKey()).release(entry.getValue()));
    }
    return changes;
  }

  /**
   * 将订单冻结金额结算为正式消费。
   */
  private List<WalletChange> settleOrderFunds(OrderSubmissionResult.SubmittedOrder order) {
    Map<Long, BigDecimal> amounts = memberAmounts(order);
    Map<Long, WalletAccount> wallets = loadWallets(amounts.keySet());
    List<WalletChange> changes = new ArrayList<>();
    for (Map.Entry<Long, BigDecimal> entry : amounts.entrySet()) {
      changes.add(wallets.get(entry.getKey()).settle(entry.getValue()));
    }
    return changes;
  }

  /**
   * 计算每个成员在订单中的应付金额。
   */
  private Map<Long, BigDecimal> memberAmounts(OrderSubmissionResult.SubmittedOrder order) {
    Map<Long, BigDecimal> result = new LinkedHashMap<>();
    for (OrderSubmissionResult.SubmittedOrderItem item : order.items()) {
      result.merge(item.ownerMemberId(), money(item.amount()), BigDecimal::add);
    }
    result.merge(order.submitterMemberId(), money(order.deliveryFee()), BigDecimal::add);
    return result;
  }

  /**
   * 替换订单状态并发送通知。
   */
  private void replaceAndNotify(
    OrderSubmissionResult.SubmittedOrder current,
    OrderStatus status,
    BigDecimal deliveryFee,
    BigDecimal totalAmount,
    String cancelReason,
    String title
  ) {
    replaceOrder(copyOrder(current, status, deliveryFee, totalAmount, cancelReason));
    addNotification("family", current.familyId(), "order", title, title + " #" + current.orderId());
  }

  /**
   * 替换订单主记录及子表快照。
   */
  private void replaceOrder(OrderSubmissionResult.SubmittedOrder order) {
    orderMapper.updateOrder(toOrderEntity(order));
    orderMapper.deleteOrderItems(order.orderId());
    orderMapper.deleteDeliverySnapshot(order.orderId());
    saveOrderChildren(order.orderId(), order);
  }

  /**
   * 写入钱包变更及流水。
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
   * 发送通知中心消息。
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
   * 为订单列表补全订单明细和配送地址快照。
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
   * 保存订单明细和配送快照。
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
   * 生成订单替换快照。
   */
  private static OrderSubmissionResult.SubmittedOrder copyOrder(
    OrderSubmissionResult.SubmittedOrder current,
    OrderStatus status,
    BigDecimal deliveryFee,
    BigDecimal totalAmount,
    String cancelReason
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
      status,
      money(totalAmount),
      current.remark(),
      cancelReason,
      current.deliverySnapshot(),
      current.items()
    );
  }

  /**
   * 转换为接口视图对象。
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
   * 按订单状态生成通知标题。
   */
  private static String statusTitle(OrderStatus status) {
    return switch (status) {
      case PREPARING -> "订单备菜中";
      case READY -> "订单待出餐";
      case DONE -> "订单已完成";
      default -> "订单状态已更新";
    };
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

  /**
   * 统一金额精度。
   */
  private static BigDecimal money(BigDecimal value) {
    return value == null
      ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
      : value.setScale(2, RoundingMode.HALF_UP);
  }
}
