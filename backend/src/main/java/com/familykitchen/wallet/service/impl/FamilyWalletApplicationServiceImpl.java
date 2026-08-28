package com.familykitchen.wallet.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.wallet.mapper.FamilyWalletMapper;
import com.familykitchen.wallet.model.dto.AdjustFamilyBalanceRequest;
import com.familykitchen.wallet.model.entity.FamilyWalletAccountDO;
import com.familykitchen.wallet.model.entity.FamilyWalletLedgerDO;
import com.familykitchen.wallet.model.enums.LedgerType;
import com.familykitchen.wallet.model.vo.FamilyWalletLedgerView;
import com.familykitchen.wallet.model.vo.FamilyWalletSummaryView;
import com.familykitchen.wallet.service.FamilyWalletApplicationService;
import com.familykitchen.wallet.service.FamilyWalletService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Default authorization facade for family-wallet APIs. */
@Service
public class FamilyWalletApplicationServiceImpl implements FamilyWalletApplicationService {
  private final FamilyWalletService wallet;
  private final FamilyWalletMapper mapper;
  private final FamilyMapper families;

  /** Creates the family wallet application service.
   * @param wallet mutation service
   * @param mapper wallet reads
   * @param families ownership reads
   */
  public FamilyWalletApplicationServiceImpl(
      FamilyWalletService wallet, FamilyWalletMapper mapper, FamilyMapper families) {
    this.wallet = wallet; this.mapper = mapper; this.families = families;
  }

  /** {@inheritDoc} */
  @Override public FamilyWalletSummaryView summary(CurrentUserContext user) {
    return summary(requireFamily(user));
  }
  /** {@inheritDoc} */
  @Override public List<FamilyWalletLedgerView> ledgers(
      CurrentUserContext user, int page, int pageSize) {
    return ledgers(requireFamily(user), page, pageSize);
  }
  /** {@inheritDoc} */
  @Override public FamilyWalletSummaryView summaryForMerchant(
      CurrentUserContext user, Long familyId) {
    requireOwnedFamily(user, familyId); return summary(familyId);
  }
  /** {@inheritDoc} */
  @Override public List<FamilyWalletLedgerView> ledgersForMerchant(
      CurrentUserContext user, Long familyId, int page, int pageSize) {
    requireOwnedFamily(user, familyId); return ledgers(familyId, page, pageSize);
  }
  /** {@inheritDoc} */
  @Override public FamilyWalletSummaryView adjustForMerchant(
      CurrentUserContext user, Long familyId, AdjustFamilyBalanceRequest request) {
    requireOwnedFamily(user, familyId);
    if (request == null || request.requestId() == null || request.requestId().isBlank()
        || request.amount() == null || request.remark() == null || request.remark().isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "调账请求不完整");
    }
    String key = "family-wallet:" + familyId + ":adjust:" + request.requestId().trim();
    if (request.type() == LedgerType.MANUAL_CREDIT) {
      wallet.manualCredit(familyId, user.userId(), request.amount(), key, request.remark());
    } else if (request.type() == LedgerType.MANUAL_DEBIT) {
      wallet.manualDebit(familyId, user.userId(), request.amount(), key, request.remark());
    } else {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "家庭钱包仅支持人工加款或扣款");
    }
    return summary(familyId);
  }

  private FamilyWalletSummaryView summary(long familyId) {
    FamilyWalletAccountDO row = wallet.get(familyId);
    return new FamilyWalletSummaryView(row.familyId, row.availableAmount, row.frozenAmount,
        row.availableAmount.add(row.frozenAmount), row.version, row.updatedAt);
  }
  private List<FamilyWalletLedgerView> ledgers(long familyId, int page, int pageSize) {
    int safePage = Math.max(1, page); int safeSize = Math.min(100, Math.max(1, pageSize));
    return mapper.selectLedgersPage(familyId, (safePage - 1) * safeSize, safeSize).stream()
        .map(FamilyWalletApplicationServiceImpl::view).toList();
  }
  private static FamilyWalletLedgerView view(FamilyWalletLedgerDO row) {
    return new FamilyWalletLedgerView(row.id, row.familyId, row.orderId, row.operatorUserId,
        row.businessType, row.amount, row.availableBefore, row.availableAfter,
        row.frozenBefore, row.frozenAfter, row.remark, row.createdAt);
  }
  private static long requireFamily(CurrentUserContext user) {
    if (user == null || user.familyId() == null) {
      throw new BusinessException(ErrorCode.FAMILY_NOT_JOINED, "尚未加入家庭");
    }
    return user.familyId();
  }
  private void requireOwnedFamily(CurrentUserContext user, Long familyId) {
    if (user == null || user.merchantId() == null || familyId == null
        || families.countFamilyOwnership(user.merchantId(), familyId) != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到商户服务范围内的家庭");
    }
  }
}
