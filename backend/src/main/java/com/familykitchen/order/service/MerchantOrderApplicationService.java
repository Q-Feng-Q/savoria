package com.familykitchen.order.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.order.model.dto.OrderStatusRequest;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.model.vo.OrderView;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商户端订单服务。
 *
 * <p>负责商户查看订单、确认/驳回订单、备菜中取消订单、调整配送费，以及推进订单状态。
 * 涉及资金冻结、释放、结算和通知发送的动作都在服务层统一完成。</p>
 */
public interface MerchantOrderApplicationService {

  /**
   * 查询当前商户订单列表。
   *
   * @param user 当前登录用户上下文
   * @return 订单列表
   */
  List<OrderView> list(CurrentUserContext user);

  /**
   * 查询当前商户订单详情。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   * @return 订单详情
   */
  OrderView detail(CurrentUserContext user, Long orderId);

  /**
   * 确认订单，订单从待确认进入后续备菜流程。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   * @return 确认后的订单状态
   */
  OrderStatus confirm(CurrentUserContext user, Long orderId);

  /**
   * 驳回订单，并释放订单冻结金额。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   * @return 驳回后的订单状态
   */
  OrderStatus reject(CurrentUserContext user, Long orderId);

  /**
   * 商户取消订单，并释放订单冻结金额。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   * @param reason 取消原因，备菜中取消时必须填写
   * @return 取消后的订单状态
   */
  OrderStatus cancel(CurrentUserContext user, Long orderId, String reason);

  /**
   * 在商户确认前调整配送费，多退少补会同步处理提交成员钱包冻结金额。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   * @param deliveryFee 新配送费
   */
  void adjustDeliveryFee(CurrentUserContext user, Long orderId, BigDecimal deliveryFee);

  /**
   * 推进订单状态。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   * @param request 目标状态和原因
   * @return 推进后的订单状态
   */
  OrderStatus advance(CurrentUserContext user, Long orderId, OrderStatusRequest request);
}
