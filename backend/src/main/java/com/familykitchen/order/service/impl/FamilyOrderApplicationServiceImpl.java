package com.familykitchen.order.service.impl;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.entity.CartItemSelectionEntity;
import com.familykitchen.cart.service.ExpectedMealTimePolicy;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.idempotency.CommandIdempotencyService;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import com.familykitchen.family.model.entity.FamilyMemberRecord;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.order.mapper.OrderPersistenceMapper;
import com.familykitchen.order.model.bo.CheckoutIngredient;
import com.familykitchen.order.model.bo.CheckoutItem;
import com.familykitchen.order.model.bo.DeliverySnapshot;
import com.familykitchen.order.model.bo.FamilyDeliveryPolicy;
import com.familykitchen.order.model.bo.OrderCheckoutCommand;
import com.familykitchen.order.model.bo.OrderSubmissionResult;
import com.familykitchen.order.model.dto.SubmitOrderRequest;
import com.familykitchen.order.model.entity.OrderDeliverySnapshotEntity;
import com.familykitchen.order.model.entity.OrderItemEntity;
import com.familykitchen.order.model.entity.OrderItemSelectionEntity;
import com.familykitchen.order.model.entity.OrderRecordEntity;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.model.vo.OrderView;
import com.familykitchen.order.service.FamilyOrderApplicationService;
import com.familykitchen.order.service.OrderStateMachine;
import com.familykitchen.order.service.OrderSubmissionService;
import com.familykitchen.purchase.model.enums.IngredientCalcType;
import com.familykitchen.wallet.service.FamilyWalletService;
import com.familykitchen.wallet.service.PersonalWalletCutoverGuard;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates immutable shared-cart order submission and family-wallet authorization. */
@Service
@Transactional
public class FamilyOrderApplicationServiceImpl implements FamilyOrderApplicationService {
  private final OrderSubmissionService calculator;
  private final OrderPersistenceMapper orders;
  private final CartMapper carts;
  private final DishMapper dishes;
  private final FamilyMapper families;
  private final FamilyRelationMapper relations;
  private final NotificationPersistenceMapper notifications;
  private final OrderStateMachine states;
  private final ExpectedMealTimePolicy times;
  private final FamilyWalletService wallet;
  private final CommandIdempotencyService commands;
  private final PersonalWalletCutoverGuard cutover;

  /** Creates the family order coordinator.
   * @param calculator aggregate order calculator
   * @param orders order persistence
   * @param carts cart persistence
   * @param dishes dish persistence
   * @param families family persistence
   * @param relations membership persistence
   * @param notifications notification persistence
   * @param states order state machine
   * @param times expected-time policy
   * @param wallet family wallet service
   * @param commands durable command coordinator
   * @param cutover personal-wallet cutover guard
   */
  public FamilyOrderApplicationServiceImpl(OrderSubmissionService calculator,
      OrderPersistenceMapper orders,CartMapper carts,DishMapper dishes,FamilyMapper families,
      FamilyRelationMapper relations,NotificationPersistenceMapper notifications,
      OrderStateMachine states,ExpectedMealTimePolicy times,FamilyWalletService wallet,
      CommandIdempotencyService commands,PersonalWalletCutoverGuard cutover){
    this.calculator=calculator;this.orders=orders;this.carts=carts;this.dishes=dishes;
    this.families=families;this.relations=relations;this.notifications=notifications;
    this.states=states;this.times=times;this.wallet=wallet;this.commands=commands;
    this.cutover=cutover;
  }

  /** {@inheritDoc} */
  @Override
  public OrderView submit(CurrentUserContext user,SubmitOrderRequest request){
    requireRequest(user,request);
    cutover.requireReady(user.familyId());
    String payload=payload(request);
    CommandIdempotencyService.Result result=commands.execute(
        new CommandIdempotencyService.Command(user.userId(),user.familyId(),"ORDER_SUBMIT",
            request.requestId(),payload),
        ()->new CommandIdempotencyService.Result("order",submitOnce(user,request),null));
    if(result.resourceId()==null)throw conflict("订单幂等结果缺少订单编号");
    return toView(requireOrder(user.familyId(),result.resourceId()));
  }

  private Long submitOnce(CurrentUserContext user,SubmitOrderRequest request){
    CartEntity cart=carts.selectFamilyCartForUpdate(request.cartId(),user.familyId());
    if(cart==null)throw new BusinessException(ErrorCode.NOT_FOUND,"餐篮不存在");
    if(!"active".equalsIgnoreCase(cart.getStatus()))
      throw new BusinessException(ErrorCode.CART_SUBMITTED,"餐篮已经提交，请使用新的餐篮继续点餐");
    long actualVersion=cart.getVersion()==null?0:cart.getVersion();
    if(actualVersion!=request.cartVersion())
      throw new BusinessException(ErrorCode.CART_CHANGED,"餐篮内容已变化，请刷新后重新确认");
    times.requireValid(cart.getExpectedMealTime());
    List<CartItemEntity> cartItems=carts.selectCartItemsForUpdate(cart.getId());
    if(cartItems.isEmpty())throw new BusinessException(ErrorCode.BUSINESS_INVALID,"餐篮为空");

    Map<Long,List<CartItemSelectionEntity>> selected=new LinkedHashMap<>();
    Set<Long> participantIds=new LinkedHashSet<>();
    for(CartItemEntity item:cartItems){
      List<CartItemSelectionEntity> rows=carts.selectSelectionsForUpdate(item.getId());
      selected.put(item.getId(),rows);
      for(CartItemSelectionEntity row:rows)participantIds.add(row.userId);
    }
    if(participantIds.isEmpty())throw conflict("餐篮缺少成员选择");
    List<Long> locked=relations.lockActiveParticipants(user.familyId(),new ArrayList<>(participantIds));
    if(locked==null||!new LinkedHashSet<>(locked).equals(participantIds))
      throw conflict("点菜成员已退出家庭，请刷新餐篮");
    Map<Long,String> names=families.selectMembers(user.familyId()).stream()
        .filter(row->participantIds.contains(row.getMemberId()))
        .collect(Collectors.toMap(FamilyMemberRecord::getMemberId,FamilyMemberRecord::getName));

    List<CheckoutItem> checkoutItems=new ArrayList<>();
    for(CartItemEntity item:cartItems){
      CartDishSnapshot current=carts.selectAvailableDishForUpdate(user.familyId(),item.getDishId());
      if(current==null)throw new BusinessException(ErrorCode.BUSINESS_INVALID,
          "菜品已下架或不再属于家庭菜单："+item.getDishNameSnapshot());
      List<CheckoutItem.MemberSelection> snapshots=selected.get(item.getId()).stream()
          .map(row->new CheckoutItem.MemberSelection(row.userId,requireName(names,row.userId),
              row.quantity,row.itemRemark)).toList();
      int aggregate=snapshots.stream().mapToInt(CheckoutItem.MemberSelection::quantity).sum();
      if(aggregate!=item.getQuantity())throw conflict("成员选择数量与餐篮总数不一致");
      checkoutItems.add(new CheckoutItem(current.dishId(),current.dishName(),current.price(),
          aggregate,item.getItemRemark(),ingredients(item.getDishId()),snapshots));
    }

    DeliverySnapshot delivery=request.deliveryMode()==DeliveryMode.DELIVERY
        ?delivery(user.familyId(),request.addressId()):null;
    OrderCheckoutCommand command=new OrderCheckoutCommand(cart.getId(),cart.getMerchantId(),
        user.familyId(),user.userId(),cart.getExpectedMealTime(),request.deliveryMode(),
        blank(request.remark())?cart.getRemark():request.remark().trim(),delivery,
        deliveryPolicy(user.familyId()),checkoutItems);
    OrderSubmissionResult calculated=calculator.submit(command);
    OrderRecordEntity order=toEntity(calculated.order());
    if(orders.insertOrder(order)!=1||order.getId()==null)throw conflict("订单创建失败");
    saveChildren(order.getId(),calculated.order());
    wallet.freezeNewOrder(user.familyId(),order.getId(),user.userId(),
        calculated.order().totalAmount(),"order:"+order.getId()+":initial");
    if(carts.submitFamilyCart(cart.getId(),user.familyId(),actualVersion)!=1)
      throw new BusinessException(ErrorCode.CART_CHANGED,"餐篮内容已变化，请刷新后重新确认");
    notifications.insertNotification("merchant",cart.getMerchantId(),"merchant","order",
        "收到新订单","家庭 "+user.familyId()+" 提交了订单 #"+order.getId());
    return order.getId();
  }

  /** {@inheritDoc} */
  @Override public List<OrderView> list(CurrentUserContext user){
    return hydrate(orders.selectOrdersByFamilyId(user.familyId())).stream()
        .map(FamilyOrderApplicationServiceImpl::toView).toList();
  }
  /** {@inheritDoc} */
  @Override public OrderView detail(CurrentUserContext user,Long orderId){
    return toView(requireOrder(user.familyId(),orderId));
  }
  /** {@inheritDoc} */
  @Override public void cancel(CurrentUserContext user,Long orderId,String reason){
    OrderSubmissionResult.SubmittedOrder current=requireOrderForUpdate(user.familyId(),orderId);
    if(current.status()==OrderStatus.CANCELLED)return;
    OrderStatus next=states.familyCancel(current.status());
    wallet.release(user.familyId(),orderId,user.userId(),current.totalAmount(),
        "order:"+orderId+":family-cancel");
    OrderRecordEntity row=toEntity(current);row.setStatus(next.name());row.setCancelReason(normalize(reason));
    if(orders.updateOrder(row)!=1)throw conflict("订单状态更新失败");
    notifications.insertNotification("merchant",current.merchantId(),"merchant","order",
        "家庭取消订单","家庭 "+user.familyId()+" 取消了订单 #"+orderId);
  }

  private OrderSubmissionResult.SubmittedOrder requireOrder(Long familyId,Long orderId){
    OrderRecordEntity row=orders.selectOrderByFamilyId(familyId,orderId);
    if(row==null)throw new BusinessException(ErrorCode.NOT_FOUND,"未找到订单");
    return hydrate(List.of(row)).get(0);
  }

  private OrderSubmissionResult.SubmittedOrder requireOrderForUpdate(Long familyId,Long orderId){
    OrderRecordEntity row=orders.selectOrderByFamilyIdForUpdate(familyId,orderId);
    if(row==null)throw new BusinessException(ErrorCode.NOT_FOUND,"未找到订单");
    return hydrate(List.of(row)).get(0);
  }

  private List<OrderSubmissionResult.SubmittedOrder> hydrate(List<OrderRecordEntity> rows){
    if(rows==null||rows.isEmpty())return List.of();
    List<Long> orderIds=rows.stream().map(OrderRecordEntity::getId).toList();
    List<OrderItemEntity> itemRows=orders.selectOrderItemsByOrderIds(orderIds);
    Map<Long,List<OrderItemEntity>> byOrder=itemRows.stream()
        .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
    List<Long> itemIds=itemRows.stream().map(OrderItemEntity::getId).filter(java.util.Objects::nonNull).toList();
    Map<Long,List<OrderItemSelectionEntity>> byItem=itemIds.isEmpty()?Map.of():
        orders.selectOrderItemSelections(itemIds).stream()
            .collect(Collectors.groupingBy(row->row.orderItemId));
    Map<Long,OrderDeliverySnapshotEntity> deliveries=orders.selectDeliverySnapshotsByOrderIds(orderIds)
        .stream().collect(Collectors.toMap(OrderDeliverySnapshotEntity::getOrderId,row->row));
    return rows.stream().map(row->fromEntity(row,byOrder.getOrDefault(row.getId(),List.of()),
        byItem,deliveries.get(row.getId()))).toList();
  }

  private static OrderSubmissionResult.SubmittedOrder fromEntity(OrderRecordEntity row,
      List<OrderItemEntity> items,Map<Long,List<OrderItemSelectionEntity>> byItem,
      OrderDeliverySnapshotEntity delivery){
    DeliverySnapshot snapshot=delivery==null?null:new DeliverySnapshot(delivery.getContactName(),
        delivery.getContactPhone(),delivery.getAddressText());
    List<OrderSubmissionResult.SubmittedOrderItem> itemViews=items.stream().map(item->
        new OrderSubmissionResult.SubmittedOrderItem(item.getDishId(),item.getDishNameSnapshot(),
            item.getOwnerMemberId(),item.getPrice(),item.getQuantity(),item.getAmount(),
            item.getItemRemark(),byItem.getOrDefault(item.getId(),List.of()).stream().map(selection->
                new OrderSubmissionResult.MemberSelection(selection.userId,
                    selection.memberNameSnapshot,selection.quantity,selection.itemRemark)).toList()))
        .toList();
    return new OrderSubmissionResult.SubmittedOrder(row.getId(),row.getSourceCartId(),
        row.getMerchantId(),row.getFamilyId(),row.getSubmitterMemberId(),row.getMealSlotId(),
        row.getServiceDate(),row.getExpectedMealTime(),DeliveryMode.valueOf(row.getDeliveryMode()),
        row.getDeliveryFee(),OrderStatus.valueOf(row.getStatus()),row.getTotalAmount(),
        row.getRemark(),row.getCancelReason(),snapshot,itemViews);
  }

  private void saveChildren(Long orderId,OrderSubmissionResult.SubmittedOrder order){
    for(OrderSubmissionResult.SubmittedOrderItem item:order.items()){
      OrderItemEntity row=new OrderItemEntity();row.setOrderId(orderId);row.setDishId(item.dishId());
      row.setOwnerMemberId(null);row.setDishNameSnapshot(item.dishName());row.setPrice(item.price());
      row.setQuantity(item.quantity());row.setAmount(item.amount());row.setItemRemark(item.itemRemark());
      orders.insertOrderItem(row);
      for(OrderSubmissionResult.MemberSelection selection:item.selections()){
        OrderItemSelectionEntity detail=new OrderItemSelectionEntity();detail.orderItemId=row.getId();
        detail.userId=selection.userId();detail.quantity=selection.quantity();
        detail.memberNameSnapshot=selection.memberName();detail.itemRemark=selection.itemRemark();
        orders.insertOrderItemSelection(detail);
      }
    }
    if(order.deliverySnapshot()!=null){
      OrderDeliverySnapshotEntity row=new OrderDeliverySnapshotEntity();row.setOrderId(orderId);
      row.setContactName(order.deliverySnapshot().contactName());
      row.setContactPhone(order.deliverySnapshot().contactPhone());
      row.setAddressText(order.deliverySnapshot().addressText());orders.insertDeliverySnapshot(row);
    }
  }

  private List<CheckoutIngredient> ingredients(Long dishId){
    return dishes.selectDishIngredients(dishId).stream().map(row->new CheckoutIngredient(
        row.getIngredientName(),row.getQuantity(),row.getUnit(),
        IngredientCalcType.valueOf(row.getCalcType()))).toList();
  }
  private DeliverySnapshot delivery(Long familyId,Long addressId){
    if(addressId==null)throw new BusinessException(ErrorCode.BAD_REQUEST,"配送地址不能为空");
    return families.selectAddresses(familyId).stream().filter(row->addressId.equals(row.getId()))
        .findFirst().map(row->new DeliverySnapshot(row.getContactName(),row.getContactPhone(),
            row.getAddressText())).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND,
                "未找到配送地址"));
  }
  private FamilyDeliveryPolicy deliveryPolicy(Long familyId){
    FamilyRecord row=families.selectFamily(null,familyId);
    if(row==null)throw new BusinessException(ErrorCode.NOT_FOUND,"未找到家庭");
    return new FamilyDeliveryPolicy(Boolean.TRUE.equals(row.getDeliveryEnabled()),
        row.getDeliveryFeeDefault(),Boolean.TRUE.equals(row.getDeliveryFree()));
  }
  private static OrderRecordEntity toEntity(OrderSubmissionResult.SubmittedOrder order){
    OrderRecordEntity row=new OrderRecordEntity();row.setId(order.orderId());
    row.setSourceCartId(order.sourceCartId());row.setMerchantId(order.merchantId());
    row.setFamilyId(order.familyId());row.setSubmitterMemberId(order.submitterMemberId());
    row.setMealSlotId(order.mealSlotId());row.setServiceDate(order.serviceDate());
    row.setExpectedMealTime(order.expectedMealTime());row.setDeliveryMode(order.deliveryMode().name());
    row.setDeliveryFee(order.deliveryFee());row.setDeliveryFeePayerMemberId(null);
    row.setStatus(order.status().name());row.setTotalAmount(order.totalAmount());
    row.setRemark(order.remark());row.setCancelReason(order.cancelReason());return row;
  }
  private static OrderView toView(OrderSubmissionResult.SubmittedOrder order){
    return new OrderView(order.orderId(),order.sourceCartId(),order.merchantId(),order.familyId(),
        order.submitterMemberId(),order.mealSlotId(),order.serviceDate(),order.expectedMealTime(),
        order.deliveryMode(),order.deliveryFee(),order.status(),order.totalAmount(),order.remark(),
        order.cancelReason(),order.items().stream().map(item->new OrderView.OrderItemView(
            item.dishId(),item.dishName(),item.ownerMemberId(),item.price(),item.quantity(),
            item.amount(),item.itemRemark(),item.selections().stream().map(selection->
                new OrderView.MemberSelectionView(selection.userId(),selection.memberName(),
                    selection.quantity(),selection.itemRemark())).toList())).toList());
  }
  private static void requireRequest(CurrentUserContext user,SubmitOrderRequest request){
    if(user==null||user.familyId()==null)throw new BusinessException(ErrorCode.FAMILY_NOT_JOINED,
        "尚未加入家庭");
    if(request==null||request.cartId()==null||request.cartVersion()==null||
        request.requestId()==null||request.requestId().isBlank()||request.deliveryMode()==null)
      throw new BusinessException(ErrorCode.BAD_REQUEST,"餐篮版本、请求号和配送方式不能为空");
  }
  private static String payload(SubmitOrderRequest request){
    String remark=normalize(request.remark());
    return "cart="+request.cartId()+"&version="+request.cartVersion()+"&mode="+
        request.deliveryMode()+"&address="+request.addressId()+"&remark="+
        (remark==null?"-":remark.length()+":"+remark);
  }
  private static String requireName(Map<Long,String> names,Long userId){
    String name=names.get(userId);if(name==null||name.isBlank())throw conflict("点菜成员资料不存在");
    return name;
  }
  private static boolean blank(String value){return value==null||value.isBlank();}
  private static String normalize(String value){return blank(value)?null:value.trim();}
  private static BusinessException conflict(String message){
    return new BusinessException(ErrorCode.STATE_CONFLICT,message);
  }
}
