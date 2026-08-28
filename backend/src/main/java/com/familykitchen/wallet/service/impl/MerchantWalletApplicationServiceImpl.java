package com.familykitchen.wallet.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.wallet.model.dto.AdjustMemberBalanceRequest;
import com.familykitchen.wallet.model.entity.WalletLedgerDO;
import com.familykitchen.wallet.model.enums.LedgerType;
import com.familykitchen.wallet.model.vo.WalletLedgerView;
import com.familykitchen.wallet.service.MerchantWalletApplicationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户侧钱包应用服务实现。
 *
 * <p>提供商户查看家庭成员钱包流水、执行余额调整等后台能力，统一封装钱包相关的商户操作入口。</p>
 */
@Service
public class MerchantWalletApplicationServiceImpl implements MerchantWalletApplicationService {

  private final WalletPersistenceMapper walletMapper;

  /**
   * 创建商户钱包实例。
   *
   * @param walletMapper 钱包Mapper
   */
  public MerchantWalletApplicationServiceImpl(WalletPersistenceMapper walletMapper) {
    this.walletMapper = walletMapper;
  }

  /**
   * 处理Ledgers。
   *
   * @param user 用户
   * @param memberId 成员标识
   * @return 处理Ledgers的结果
   */
  @Override
  public List<WalletLedgerView> walletLedgers(CurrentUserContext user, Long memberId) {
    requireMerchantMember(user, memberId);
    return walletMapper.selectWalletLedgers(memberId).stream()
        .map(MerchantWalletApplicationServiceImpl::toView)
        .toList();
  }

  /**
   * 处理余额。
   *
   * @param user 用户
   * @param memberId 成员标识
   * @param request 请求参数
   * @return 处理余额的结果
   */
  @Override
  @Transactional
  public WalletLedgerView adjustBalance(CurrentUserContext user, Long memberId, AdjustMemberBalanceRequest request) {
    requireMerchantMember(user, memberId);
    throw new BusinessException(ErrorCode.CLIENT_UPGRADE_REQUIRED,
        "个人钱包调账已停用，请改用商户家庭钱包调账接口");
  }

  private void requireMerchantMember(CurrentUserContext user, Long memberId) {
    if (user == null || user.merchantId() == null
        || walletMapper.countMerchantMember(user.merchantId(), memberId) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到商户服务范围内的家庭成员");
    }
  }

  private static WalletLedgerView toView(WalletLedgerDO ledger) {
    return new WalletLedgerView(
        ledger.getId(),
        ledger.getMemberId(),
        LedgerType.valueOf(ledger.getType()),
        money(ledger.getAmount()),
        money(ledger.getBalanceBefore()),
        money(ledger.getBalanceAfter()),
        money(ledger.getFrozenBefore()),
        money(ledger.getFrozenAfter()),
        ledger.getRemark(),
        ledger.getCreatedAt()
    );
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
