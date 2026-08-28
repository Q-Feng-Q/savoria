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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Calculates immutable aggregate order snapshots without mutating any wallet. */
@Service
public class OrderSubmissionServiceImpl implements OrderSubmissionService {
  /** {@inheritDoc} */
  @Override
  public OrderSubmissionResult submit(OrderCheckoutCommand command) {
    validate(command);
    DeliveryPlan delivery=delivery(command.deliveryMode(),command.familyDeliveryPolicy(),
        command.deliverySnapshot());
    List<OrderSubmissionResult.SubmittedOrderItem> items=new ArrayList<>();
    BigDecimal total=money(BigDecimal.ZERO);
    for(CheckoutItem item:command.items()){
      validateItem(item);
      BigDecimal amount=money(item.price()).multiply(BigDecimal.valueOf(item.quantity()))
          .setScale(2,RoundingMode.HALF_UP);
      total=total.add(amount);
      List<OrderSubmissionResult.MemberSelection> selections=item.selections().stream()
          .map(row->new OrderSubmissionResult.MemberSelection(row.userId(),row.memberName(),
              row.quantity(),row.itemRemark())).toList();
      items.add(new OrderSubmissionResult.SubmittedOrderItem(item.dishId(),item.dishName(),null,
          money(item.price()),item.quantity(),amount,item.itemRemark(),selections));
    }
    total=money(total.add(delivery.fee()));
    OrderSubmissionResult.SubmittedOrder order=new OrderSubmissionResult.SubmittedOrder(
        null,command.sourceCartId(),command.merchantId(),command.familyId(),
        command.submitterMemberId(),null,null,command.expectedMealTime(),delivery.mode(),
        delivery.fee(),OrderStatus.PENDING,total,command.remark(),null,delivery.snapshot(),items);
    PurchaseDemand purchase=new PurchaseDemand(command.familyId(),null,null,null,
        command.expectedMealTime().toLocalDate(),OrderSourceStatus.PENDING,
        ingredientDemands(command.items()));
    OrderSubmissionResult.OrderNotification notification=
        new OrderSubmissionResult.OrderNotification("merchant",command.merchantId(),"order",
            "收到新订单","家庭 "+command.familyId()+" 提交了新订单");
    return new OrderSubmissionResult(order,List.of(),List.of(),List.of(purchase),
        List.of(notification));
  }

  private static void validate(OrderCheckoutCommand command){
    if(command==null)bad(ErrorCode.BAD_REQUEST,"下单参数不能为空");
    if(command.sourceCartId()==null||command.familyId()==null||command.expectedMealTime()==null)
      bad(ErrorCode.BAD_REQUEST,"餐篮与预计用餐时间不能为空");
    if(command.items()==null||command.items().isEmpty())
      bad(ErrorCode.BUSINESS_INVALID,"订单项不能为空");
  }

  private static void validateItem(CheckoutItem item){
    if(item==null||item.quantity()<=0||item.price()==null||item.price().signum()<0)
      bad(ErrorCode.BUSINESS_INVALID,"订单项不合法");
    if(item.selections()==null||item.selections().isEmpty())
      bad(ErrorCode.STATE_CONFLICT,"订单项缺少成员选择");
    int quantity=0;
    for(CheckoutItem.MemberSelection row:item.selections()){
      if(row.userId()==null||row.memberName()==null||row.memberName().isBlank()||row.quantity()<=0)
        bad(ErrorCode.STATE_CONFLICT,"成员选择不合法");
      quantity+=row.quantity();
    }
    if(quantity!=item.quantity())bad(ErrorCode.STATE_CONFLICT,"成员选择数量与菜品总数不一致");
  }

  private static DeliveryPlan delivery(DeliveryMode requested,FamilyDeliveryPolicy policy,
      DeliverySnapshot snapshot){
    if(requested==null)bad(ErrorCode.BAD_REQUEST,"配送方式不能为空");
    if(requested==DeliveryMode.DELIVERY){
      if(policy==null||!policy.deliveryEnabled())
        bad(ErrorCode.BUSINESS_INVALID,"当前家庭未开通配送");
      if(snapshot==null)bad(ErrorCode.BAD_REQUEST,"选择配送时必须提供配送地址");
      BigDecimal fee=policy.deliveryFeeFree()?money(BigDecimal.ZERO):money(policy.deliveryFeeDefault());
      return new DeliveryPlan(DeliveryMode.DELIVERY,fee,snapshot);
    }
    return new DeliveryPlan(DeliveryMode.PICKUP,money(BigDecimal.ZERO),null);
  }

  private static List<IngredientDemand> ingredientDemands(List<CheckoutItem> items){
    List<IngredientDemand> result=new ArrayList<>();
    for(CheckoutItem item:items){
      if(item.ingredients()==null)continue;
      for(CheckoutIngredient ingredient:item.ingredients()){
        result.add(new IngredientDemand(ingredient.ingredientName(),money(ingredient.quantity()),
            ingredient.unit(),ingredient.calcType(),item.quantity()));
      }
    }
    return result;
  }

  private static BigDecimal money(BigDecimal value){
    return (value==null?BigDecimal.ZERO:value).setScale(2,RoundingMode.HALF_UP);
  }
  private static void bad(ErrorCode code,String message){throw new BusinessException(code,message);}
  /** Calculated delivery outcome.
   * @param mode effective delivery mode
   * @param fee delivery fee
   * @param snapshot address snapshot
   */
  private record DeliveryPlan(DeliveryMode mode,BigDecimal fee,DeliverySnapshot snapshot){}
}
