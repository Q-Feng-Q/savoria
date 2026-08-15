package com.familykitchen.order.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.order.model.dto.SubmitOrderRequest;
import com.familykitchen.order.model.vo.OrderView;
import java.util.List;

/**
 * 家庭端订单服务。
 *
 * <p>负责家庭成员下单、修改未确认订单、查询订单和取消订单。服务会统一处理餐篮快照、
 * 配送快照、钱包冻结/释放、采购需求生成和通知发送等订单闭环逻辑。</p>
 */
public interface FamilyOrderApplicationService {

  /**
   * 按当前成员餐篮提交订单。
   *
   * @param user 当前登录用户上下文
   * @param request 下单日期、餐次、配送方式、地址和备注
   * @return 新生成的订单视图
   */
  OrderView submit(CurrentUserContext user, SubmitOrderRequest request);

  /**
   * 使用当前餐篮内容覆盖更新一笔未确认订单。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   */
  void update(CurrentUserContext user, Long orderId);

  /**
   * 查询当前家庭订单列表。
   *
   * @param user 当前登录用户上下文
   * @return 订单列表
   */
  List<OrderView> list(CurrentUserContext user);

  /**
   * 查询当前家庭订单详情。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   * @return 订单详情
   */
  OrderView detail(CurrentUserContext user, Long orderId);

  /**
   * 家庭端取消订单。
   *
   * @param user 当前登录用户上下文
   * @param orderId 订单 ID
   * @param reason 取消原因，可为空
   */
  void cancel(CurrentUserContext user, Long orderId, String reason);
}
