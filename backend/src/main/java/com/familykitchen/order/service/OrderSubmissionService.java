package com.familykitchen.order.service;

import com.familykitchen.order.model.bo.OrderCheckoutCommand;
import com.familykitchen.order.model.bo.OrderSubmissionResult;
import com.familykitchen.wallet.model.bo.WalletAccount;
import java.util.Map;

/**
 * 订单提交计算服务。
 *
 * <p>该服务只负责纯业务计算：校验下单命令、生成订单快照、计算成员分摊金额、
 * 冻结钱包金额、生成采购需求和通知内容。实际数据库保存由上层订单应用服务完成。</p>
 */
public interface OrderSubmissionService {

  /**
   * 根据餐篮快照生成订单提交结果。
   *
   * @param command 下单上下文，包含家庭、餐次、配送、备注和餐篮菜品快照
   * @param wallets 参与付款成员的钱包账户，key 为成员 ID
   * @return 订单、成员扣款明细、钱包变动、采购需求和通知内容
   */
  OrderSubmissionResult submit(OrderCheckoutCommand command, Map<Long, WalletAccount> wallets);
}
