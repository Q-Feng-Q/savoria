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
import com.familykitchen.order.model.entity.OrderItemSelectionEntity;
import com.familykitchen.order.model.entity.OrderRecordEntity;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.model.vo.OrderView;
import com.familykitchen.order.service.MerchantOrderApplicationService;
import com.familykitchen.order.service.OrderStateMachine;
import com.familykitchen.wallet.service.FamilyWalletService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
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
  private final FamilyWalletService wallet;
  private final NotificationPersistenceMapper notificationMapper;

  /**
   * 创建商户订单实例。
   *
   * @param orderStateMachine 订单StateMachine
   * @param orderMapper 订单Mapper
   * @param wallet family wallet service
   * @param notificationMapper 通知Mapper
   */
  public MerchantOrderApplicationServiceImpl(
    OrderStateMachine orderStateMachine,
    OrderPersistenceMapper orderMapper,
    FamilyWalletService wallet,
    NotificationPersistenceMapper notificationMapper
  ) {
    this.orderStateMachine = orderStateMachine;
    this.orderMapper = orderMapper;
    this.wallet = wallet;
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
    OrderSubmissionResult.SubmittedOrder current = requireOrderForUpdate(user, orderId);
    if (current.status() == OrderStatus.CONFIRMED) return current.status();
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
    OrderSubmissionResult.SubmittedOrder current = requireOrderForUpdate(user, orderId);
    if (current.status() == OrderStatus.REJECTED) return current.status();
    OrderStatus next = orderStateMachine.reject(current.status());
    wallet.release(current.familyId(),current.orderId(),user.userId(),current.totalAmount(),
        "order:"+current.orderId()+":merchant-reject");
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
    OrderSubmissionResult.SubmittedOrder current = requireOrderForUpdate(user, orderId);
    if (current.status() == OrderStatus.CANCELLED) return current.status();
    OrderStatus next = orderStateMachine.merchantCancel(current.status(), reason);
    wallet.release(current.familyId(),current.orderId(),user.userId(),current.totalAmount(),
        "order:"+current.orderId()+":merchant-cancel");
    replaceAndNotify(current, next, current.deliveryFee(), current.totalAmount(), blankToNull(reason), "订单已取消");
    return next;
  }

  /**
   * 处理配送费用。
   *
   * @param user 用户
   * @param orderId 订单标识
   * @param deliveryFee 配送费用
   * @param requestId 幂等请求号
   */
  @Override
  public void adjustDeliveryFee(
      CurrentUserContext user, Long orderId, BigDecimal deliveryFee, String requestId) {
    if (deliveryFee == null || deliveryFee.scale() > 2 || deliveryFee.signum() < 0
        || requestId == null || requestId.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "配送费和请求号格式不正确");
    }
    OrderSubmissionResult.SubmittedOrder current = requireOrderForUpdate(user, orderId);
    orderStateMachine.validateDeliveryFeeAdjustment(current.status());

    BigDecimal targetFee = deliveryFee.setScale(2);
    BigDecimal diff = targetFee.subtract(money(current.deliveryFee()));
    String businessKey = "order:" + current.orderId() + ":fee:" + requestId.trim();
    if (diff.compareTo(BigDecimal.ZERO) > 0) {
      wallet.appendFreeze(current.familyId(),current.orderId(),user.userId(),diff,
          businessKey);
    } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
      wallet.release(current.familyId(),current.orderId(),user.userId(),diff.abs(),
          businessKey);
    } else {
      return;
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
    OrderSubmissionResult.SubmittedOrder current = requireOrderForUpdate(user, orderId);
    if (current.status() == request.status()) return current.status();
    OrderStatus next = orderStateMachine.advance(current.status(), request.status(), request.reason());
    if (next == OrderStatus.DONE) {
      wallet.capture(current.familyId(),current.orderId(),user.userId(),current.totalAmount(),
          "order:"+current.orderId()+":complete");
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
    return hydrate(orderMapper.selectOrdersByMerchantId(merchantId), false);
  }

  /**
   * 查询单个商户订单并补全明细与配送快照。
   */
  private OrderSubmissionResult.SubmittedOrder findMerchantOrder(Long merchantId, Long orderId) {
    OrderRecordEntity order = orderMapper.selectOrderByMerchantId(merchantId, orderId);
    return order == null ? null : hydrate(List.of(order), true).get(0);
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
  }

  /** Locks and reads one merchant order before changing lifecycle or money state. */
  private OrderSubmissionResult.SubmittedOrder requireOrderForUpdate(
      CurrentUserContext user, Long orderId) {
    OrderRecordEntity row = orderMapper.selectOrderByMerchantIdForUpdate(
        user.merchantId(), orderId);
    if (row == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到订单");
    }
    return hydrate(List.of(row), true).get(0);
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
  private List<OrderSubmissionResult.SubmittedOrder> hydrate(
      List<OrderRecordEntity> orders, boolean includeSelections) {
    if (orders == null || orders.isEmpty()) {
      return List.of();
    }
    List<Long> ids = orders.stream().map(OrderRecordEntity::getId).toList();
    List<OrderItemEntity> itemRows = orderMapper.selectOrderItemsByOrderIds(ids);
    Map<Long, List<OrderItemEntity>> itemsByOrder = itemRows.stream()
      .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
    List<Long> itemIds = itemRows.stream().map(OrderItemEntity::getId)
        .filter(java.util.Objects::nonNull).toList();
    Map<Long, List<OrderItemSelectionEntity>> selectionsByItem = !includeSelections || itemIds.isEmpty()
        ? Map.of()
        : orderMapper.selectOrderItemSelections(itemIds).stream()
            .collect(Collectors.groupingBy(item -> item.orderItemId));
    Map<Long, OrderDeliverySnapshotEntity> snapshots = orderMapper.selectDeliverySnapshotsByOrderIds(ids).stream()
      .collect(Collectors.toMap(OrderDeliverySnapshotEntity::getOrderId, item -> item));
    return orders.stream()
      .map(order -> toSubmittedOrder(
        order,
        itemsByOrder.getOrDefault(order.getId(), List.of()),
        selectionsByItem,
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
    Map<Long, List<OrderItemSelectionEntity>> selectionsByItem,
    OrderDeliverySnapshotEntity snapshot
  ) {
    DeliverySnapshot deliverySnapshot = snapshot == null
      ? null
      : new DeliverySnapshot(snapshot.getContactName(), snapshot.getContactPhone(), snapshot.getAddressText());
    return new OrderSubmissionResult.SubmittedOrder(
      order.getId(),
      order.getSourceCartId(),
      order.getMerchantId(),
      order.getFamilyId(),
      order.getSubmitterMemberId(),
      order.getMealSlotId(),
      order.getServiceDate(),
      order.getExpectedMealTime(),
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
          item.getItemRemark(),
          selectionsByItem.getOrDefault(item.getId(), List.of()).stream()
              .map(selection -> new OrderSubmissionResult.MemberSelection(
                  selection.userId, selection.memberNameSnapshot,
                  selection.quantity, selection.itemRemark))
              .toList()
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
    entity.setSourceCartId(order.sourceCartId());
    entity.setMerchantId(order.merchantId());
    entity.setFamilyId(order.familyId());
    entity.setSubmitterMemberId(order.submitterMemberId());
    entity.setMealSlotId(order.mealSlotId());
    entity.setServiceDate(order.serviceDate());
    entity.setExpectedMealTime(order.expectedMealTime());
    entity.setDeliveryMode(order.deliveryMode().name());
    entity.setDeliveryFee(order.deliveryFee());
    entity.setDeliveryFeePayerMemberId(null);
    entity.setStatus(order.status().name());
    entity.setTotalAmount(order.totalAmount());
    entity.setRemark(order.remark());
    entity.setCancelReason(order.cancelReason());
    return entity;
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
      current.sourceCartId(),
      current.merchantId(),
      current.familyId(),
      current.submitterMemberId(),
      current.mealSlotId(),
      current.serviceDate(),
      current.expectedMealTime(),
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
      order.sourceCartId(),
      order.merchantId(),
      order.familyId(),
      order.submitterMemberId(),
      order.mealSlotId(),
      order.serviceDate(),
      order.expectedMealTime(),
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
          item.itemRemark(),
          item.selections().stream().map(selection ->
              new OrderView.MemberSelectionView(selection.userId(), selection.memberName(),
                  selection.quantity(), selection.itemRemark())).toList()
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
