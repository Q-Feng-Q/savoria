package com.familykitchen.order.service;

import com.familykitchen.order.model.bo.OrderCheckoutCommand;
import com.familykitchen.order.model.bo.OrderSubmissionResult;

/**
 * 订单提交计算服务。
 *
 * <p>该服务只负责纯业务计算：校验聚合数量和成员归属、生成订单快照、
 * 采购需求和通知内容。家庭钱包冻结由事务编排层统一完成。</p>
 */
public interface OrderSubmissionService {

  /**
   * 根据餐篮快照生成订单提交结果。
   *
   * @param command 下单上下文，包含共享餐篮、预计时间、配送和菜品快照
   * @return 聚合订单、采购需求和通知内容
   */
  OrderSubmissionResult submit(OrderCheckoutCommand command);
}
