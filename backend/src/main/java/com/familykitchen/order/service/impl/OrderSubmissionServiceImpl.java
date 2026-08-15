package com.familykitchen.order.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.order.model.bo.CheckoutIngredient;
import com.familykitchen.order.model.bo.CheckoutItem;
import com.familykitchen.order.model.bo.DeliverySnapshot;
import com.familykitchen.order.model.bo.FamilyDeliveryPolicy;
import com.familykitchen.order.model.bo.OrderCheckoutCommand;
import com.familykitchen.order.model.bo.OrderSubmissionResult;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.service.OrderSubmissionService;
import com.familykitchen.purchase.model.bo.IngredientDemand;
import com.familykitchen.purchase.model.bo.PurchaseDemand;
import com.familykitchen.purchase.model.enums.OrderSourceStatus;
import com.familykitchen.wallet.model.bo.WalletAccount;
import com.familykitchen.wallet.model.bo.WalletChange;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * 订单提交计算服务实现。
 *
 * <p>该实现不直接写数据库，只根据下单上下文计算订单快照、钱包冻结流水、采购需求和通知内容。
 * 这样家庭端新增订单和订单更新可以复用同一套金额与采购计算规则。</p>
 */
@Service
public class OrderSubmissionServiceImpl implements OrderSubmissionService {

  /**
   * 当前版本测试和内存场景使用的订单号序列；正式 MySQL 落库时会由数据库主键生成最终订单 ID。
   */
  private static final AtomicLong ORDER_ID_SEQUENCE = new AtomicLong(1L);

  /**
   * 提交订单Submission。
   *
   * @param command command
   * @param wallets wallets
   * @return 提交的结果
   */
  @Override
  public OrderSubmissionResult submit(OrderCheckoutCommand command, Map<Long, WalletAccount> wallets) {
    validateCommand(command);

    DeliveryPlan deliveryPlan = buildDeliveryPlan(
        command.deliveryMode(),
        command.familyDeliveryPolicy(),
        command.deliverySnapshot()
    );
    List<OrderSubmissionResult.SubmittedOrderItem> orderItems = buildOrderItems(command.items());
    Map<Long, BigDecimal> dishAmounts = aggregateDishAmounts(command.items());
    List<OrderSubmissionResult.MemberCharge> memberCharges = buildMemberCharges(
        dishAmounts,
        command.submitterMemberId(),
        deliveryPlan.deliveryFee()
    );

    // 先完成全部钱包校验，再写冻结流水，避免部分成员已冻结而另一个成员余额不足。
    precheckWallets(wallets, memberCharges);

    List<WalletChange> walletChanges = new ArrayList<>();
    for (OrderSubmissionResult.MemberCharge charge : memberCharges) {
      walletChanges.add(wallets.get(charge.memberId()).freeze(charge.totalAmount()));
    }

    Long orderId = ORDER_ID_SEQUENCE.getAndIncrement();
    OrderSubmissionResult.SubmittedOrder order = new OrderSubmissionResult.SubmittedOrder(
        orderId,
        command.merchantId(),
        command.familyId(),
        command.submitterMemberId(),
        command.mealSlotId(),
        command.serviceDate(),
        deliveryPlan.deliveryMode(),
        deliveryPlan.deliveryFee(),
        OrderStatus.PENDING,
        totalAmount(orderItems, deliveryPlan.deliveryFee()),
        command.remark(),
        null,
        deliveryPlan.snapshot(),
        orderItems
    );

    // 待确认订单也会进入采购需求，但来源状态为 PENDING，前端和采购汇总会显示为“预估”。
    List<PurchaseDemand> purchaseDemands = List.of(new PurchaseDemand(
        command.familyId(),
        command.mealSlotId(),
        orderId,
        null,
        command.serviceDate(),
        OrderSourceStatus.PENDING,
        buildIngredientDemands(command.items())
    ));

    List<OrderSubmissionResult.OrderNotification> notifications = List.of(
        new OrderSubmissionResult.OrderNotification(
            "merchant",
            command.merchantId(),
            "order",
            "收到新订单",
            "家庭 " + command.familyId() + " 提交了订单 #" + orderId
        )
    );

    return new OrderSubmissionResult(order, memberCharges, walletChanges, purchaseDemands, notifications);
  }

  /**
   * 校验下单命令的最小合法条件：命令存在，且餐篮明细不为空。
   */
  private static void validateCommand(OrderCheckoutCommand command) {
    if (command == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "下单参数不能为空");
    }
    if (command.items() == null || command.items().isEmpty()) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "订单项不能为空");
    }
  }

  /**
   * 计算本次订单的配送方式、配送费和配送地址快照。
   *
   * <p>当家庭关闭配送或本次未选择配送时，统一降级为自提并将配送费置零。</p>
   */
  private static DeliveryPlan buildDeliveryPlan(
      DeliveryMode requestedMode,
      FamilyDeliveryPolicy policy,
      DeliverySnapshot deliverySnapshot
  ) {
    if (requestedMode == DeliveryMode.DELIVERY && policy != null && policy.deliveryEnabled()) {
      if (deliverySnapshot == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "选择配送时必须提供配送地址");
      }
      BigDecimal fee = policy.deliveryFeeFree()
          ? BigDecimal.ZERO
          : money(policy.deliveryFeeDefault());
      return new DeliveryPlan(DeliveryMode.DELIVERY, fee, deliverySnapshot);
    }
    return new DeliveryPlan(DeliveryMode.PICKUP, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), null);
  }

  /**
   * 将餐篮菜品转换为订单菜品快照，锁定菜名、单价、数量和备注。
   */
  private static List<OrderSubmissionResult.SubmittedOrderItem> buildOrderItems(List<CheckoutItem> items) {
    List<OrderSubmissionResult.SubmittedOrderItem> result = new ArrayList<>();
    for (CheckoutItem item : items) {
      BigDecimal amount = money(item.price()).multiply(BigDecimal.valueOf(item.quantity()))
          .setScale(2, RoundingMode.HALF_UP);
      result.add(new OrderSubmissionResult.SubmittedOrderItem(
          item.dishId(),
          item.dishName(),
          item.ownerMemberId(),
          money(item.price()),
          item.quantity(),
          amount,
          item.itemRemark()
      ));
    }
    return result;
  }

  /**
   * 按成员聚合菜品金额，用于生成成员扣款明细。
   */
  private static Map<Long, BigDecimal> aggregateDishAmounts(List<CheckoutItem> items) {
    Map<Long, BigDecimal> result = new LinkedHashMap<>();
    for (CheckoutItem item : items) {
      BigDecimal itemAmount = money(item.price()).multiply(BigDecimal.valueOf(item.quantity()))
          .setScale(2, RoundingMode.HALF_UP);
      result.merge(item.ownerMemberId(), itemAmount, BigDecimal::add);
    }
    return result;
  }

  /**
   * 构造成员扣款明细。配送费只分配给提交订单的成员，其他成员只承担自己的菜品金额。
   */
  private static List<OrderSubmissionResult.MemberCharge> buildMemberCharges(
      Map<Long, BigDecimal> dishAmounts,
      Long submitterMemberId,
      BigDecimal deliveryFee
  ) {
    List<OrderSubmissionResult.MemberCharge> result = new ArrayList<>();
    for (Map.Entry<Long, BigDecimal> entry : dishAmounts.entrySet()) {
      BigDecimal deliveryFeeAmount = entry.getKey().equals(submitterMemberId)
          ? deliveryFee
          : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
      BigDecimal total = entry.getValue().add(deliveryFeeAmount).setScale(2, RoundingMode.HALF_UP);
      result.add(new OrderSubmissionResult.MemberCharge(
          entry.getKey(),
          entry.getValue().setScale(2, RoundingMode.HALF_UP),
          deliveryFeeAmount,
          total
      ));
    }
    return result;
  }

  /**
   * 预校验所有成员钱包是否存在且余额充足，保证冻结动作要么全部成功，要么全部不发生。
   */
  private static void precheckWallets(
      Map<Long, WalletAccount> wallets,
      List<OrderSubmissionResult.MemberCharge> memberCharges
  ) {
    for (OrderSubmissionResult.MemberCharge charge : memberCharges) {
      WalletAccount wallet = wallets.get(charge.memberId());
      if (wallet == null) {
        throw new BusinessException(ErrorCode.BUSINESS_INVALID, "未找到成员钱包");
      }
      if (wallet.balanceAmount().compareTo(charge.totalAmount()) < 0) {
        throw new BusinessException(ErrorCode.BUSINESS_INVALID, "成员余额不足");
      }
    }
  }

  /**
   * 从菜品配方中生成采购需求明细，跳过标记为无需采购的食材。
   */
  private static List<IngredientDemand> buildIngredientDemands(List<CheckoutItem> items) {
    List<IngredientDemand> result = new ArrayList<>();
    for (CheckoutItem item : items) {
      if (item.ingredients() == null) {
        continue;
      }
      for (CheckoutIngredient ingredient : item.ingredients()) {
        result.add(new IngredientDemand(
            ingredient.ingredientName(),
            money(ingredient.quantity()),
            ingredient.unit(),
            ingredient.calcType(),
            item.quantity()
        ));
      }
    }
    return result;
  }

  /**
   * 计算订单总额，菜品总额加配送费。
   */
  private static BigDecimal totalAmount(
      List<OrderSubmissionResult.SubmittedOrderItem> items,
      BigDecimal deliveryFee
  ) {
    BigDecimal dishTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (OrderSubmissionResult.SubmittedOrderItem item : items) {
      dishTotal = dishTotal.add(item.amount()).setScale(2, RoundingMode.HALF_UP);
    }
    return dishTotal.add(deliveryFee).setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * 金额统一按两位小数保存。
   */
  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * 配送计算结果，封装最终配送方式、配送费和地址快照。
   
   * @param deliveryMode 配送Mode
   * @param deliveryFee 配送费用
   * @param snapshot snapshot
   */
  private record DeliveryPlan(
      DeliveryMode deliveryMode,
      BigDecimal deliveryFee,
      DeliverySnapshot snapshot
  ) {
  }
}
