package com.familykitchen.order.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.order.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 订单状态机。
 *
 * <p>集中定义订单在家庭端和商户端可执行的状态流转规则，
 * 并在非法流转时抛出统一的业务异常。</p>
 */
@Component
public class OrderStateMachine {

  /**
   * 处理订单StateMachine。
   *
   * @param current 当前
   * @return 处理的结果
   */
  public OrderStatus confirm(OrderStatus current) {
    require(current == OrderStatus.PENDING, "只有待处理订单才能确认");
    return OrderStatus.CONFIRMED;
  }

  /**
   * 拒绝订单StateMachine。
   *
   * @param current 当前
   * @return 拒绝的结果
   */
  public OrderStatus reject(OrderStatus current) {
    require(current == OrderStatus.PENDING, "只有待处理订单才能驳回");
    return OrderStatus.REJECTED;
  }

  /**
   * 处理Cancel。
   *
   * @param current 当前
   * @return 处理Cancel的结果
   */
  public OrderStatus familyCancel(OrderStatus current) {
    require(current == OrderStatus.PENDING || current == OrderStatus.CONFIRMED, "家庭只能在备餐前取消订单");
    return OrderStatus.CANCELLED;
  }

  /**
   * 处理Cancel。
   *
   * @param current 当前
   * @param reason 原因
   * @return 处理Cancel的结果
   */
  public OrderStatus merchantCancel(OrderStatus current, String reason) {
    require(current == OrderStatus.CONFIRMED || current == OrderStatus.PREPARING, "商户只能取消已确认或备餐中的订单");
    if (current == OrderStatus.PREPARING && (reason == null || reason.isBlank())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "备餐中取消订单必须填写原因");
    }
    return OrderStatus.CANCELLED;
  }

  /**
   * 处理订单StateMachine。
   *
   * @param current 当前
   * @param target 目标
   * @param reason 原因
   * @return 处理的结果
   */
  public OrderStatus advance(OrderStatus current, OrderStatus target, String reason) {
    if (target == OrderStatus.PREPARING) {
      require(current == OrderStatus.CONFIRMED, "只有已确认订单才能进入备餐中");
      return OrderStatus.PREPARING;
    }
    if (target == OrderStatus.READY) {
      require(current == OrderStatus.PREPARING, "只有备餐中的订单才能进入待出餐");
      return OrderStatus.READY;
    }
    if (target == OrderStatus.DONE) {
      require(current == OrderStatus.READY, "只有待出餐订单才能完成");
      return OrderStatus.DONE;
    }
    if (target == OrderStatus.CANCELLED) {
      return merchantCancel(current, reason);
    }
    throw new BusinessException(ErrorCode.STATE_CONFLICT, "不支持的订单状态流转");
  }

  /**
   * 处理家庭Edit。
   *
   * @param current 当前
   */
  public void validateFamilyEdit(OrderStatus current) {
    require(current == OrderStatus.PENDING, "家庭只能编辑待处理订单");
  }

  /**
   * 处理配送费用Adjustment。
   *
   * @param current 当前
   */
  public void validateDeliveryFeeAdjustment(OrderStatus current) {
    require(current == OrderStatus.PENDING, "配送费只能在确认前调整");
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, message);
    }
  }
}
