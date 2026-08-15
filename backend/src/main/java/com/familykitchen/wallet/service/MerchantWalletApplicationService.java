package com.familykitchen.wallet.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.wallet.model.dto.AdjustMemberBalanceRequest;
import com.familykitchen.wallet.model.vo.WalletLedgerView;
import java.util.List;

/**
 * 商户端钱包服务。
 *
 * <p>用于商户后台查看家庭成员钱包流水，并对成员余额进行人工调整。</p>
 */
public interface MerchantWalletApplicationService {

  /**
   * 查询指定成员的钱包流水。
   *
   * @param user 当前登录用户上下文
   * @param memberId 成员 ID
   * @return 钱包流水列表
   */
  List<WalletLedgerView> walletLedgers(CurrentUserContext user, Long memberId);

  /**
   * 调整指定成员钱包余额。
   *
   * @param user 当前登录用户上下文
   * @param memberId 成员 ID
   * @param request 调整类型、金额和备注
   * @return 本次余额调整流水
   */
  WalletLedgerView adjustBalance(CurrentUserContext user, Long memberId, AdjustMemberBalanceRequest request);
}
