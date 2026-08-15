package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.wallet.model.dto.AdjustMemberBalanceRequest;
import com.familykitchen.wallet.model.entity.WalletAccountDO;
import com.familykitchen.wallet.model.entity.WalletLedgerDO;
import com.familykitchen.wallet.model.enums.LedgerType;
import com.familykitchen.wallet.service.impl.MerchantWalletApplicationServiceImpl;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies merchant wallet operations cannot cross merchant-family boundaries. */
@ExtendWith(MockitoExtension.class)
class MerchantWalletApplicationServiceTest {

  @Mock private WalletPersistenceMapper walletMapper;

  @Test
  void foreignMemberLedgerIsBlockedBeforeWalletQuery() {
    CurrentUserContext merchant = merchant(31L);
    when(walletMapper.countMerchantMember(31L, 99L)).thenReturn(0);
    MerchantWalletApplicationServiceImpl service = new MerchantWalletApplicationServiceImpl(walletMapper);

    assertThatThrownBy(() -> service.walletLedgers(merchant, 99L))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> org.assertj.core.api.Assertions.assertThat(error.errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

    verify(walletMapper).countMerchantMember(31L, 99L);
    verify(walletMapper, never()).selectWalletLedgers(99L);
    verifyNoMoreInteractions(walletMapper);
  }

  @Test
  void foreignMemberAdjustmentIsBlockedBeforeWalletReadOrMutation() {
    CurrentUserContext merchant = merchant(31L);
    when(walletMapper.countMerchantMember(31L, 99L)).thenReturn(0);
    MerchantWalletApplicationServiceImpl service = new MerchantWalletApplicationServiceImpl(walletMapper);
    AdjustMemberBalanceRequest request = new AdjustMemberBalanceRequest(
        LedgerType.MANUAL_CREDIT, new BigDecimal("10.00"), "test");

    assertThatThrownBy(() -> service.adjustBalance(merchant, 99L, request))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> org.assertj.core.api.Assertions.assertThat(error.errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

    verify(walletMapper).countMerchantMember(31L, 99L);
    verify(walletMapper, never()).selectWalletByMemberId(99L);
    verify(walletMapper, never()).updateWalletAmounts(org.mockito.ArgumentMatchers.anyLong(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(walletMapper, never()).insertWalletLedger(org.mockito.ArgumentMatchers.any(WalletLedgerDO.class));
    verifyNoMoreInteractions(walletMapper);
  }

  @Test
  void authorizedAdjustmentLocksWalletRowAndNeverUsesUnlockedRead() {
    CurrentUserContext merchant = merchant(31L);
    WalletAccountDO wallet = new WalletAccountDO();
    wallet.setMemberId(99L);
    wallet.setBalanceAmount(new BigDecimal("20.00"));
    wallet.setFrozenAmount(BigDecimal.ZERO);
    when(walletMapper.countMerchantMember(31L, 99L)).thenReturn(1);
    when(walletMapper.selectWalletByMemberIdForUpdate(99L)).thenReturn(wallet);
    MerchantWalletApplicationServiceImpl service = new MerchantWalletApplicationServiceImpl(walletMapper);

    service.adjustBalance(merchant, 99L, new AdjustMemberBalanceRequest(
        LedgerType.MANUAL_CREDIT, new BigDecimal("10.00"), "test"));

    verify(walletMapper).selectWalletByMemberIdForUpdate(99L);
    verify(walletMapper, never()).selectWalletByMemberId(99L);
    verify(walletMapper).updateWalletAmounts(99L, new BigDecimal("30.00"), new BigDecimal("0.00"));
  }

  private static CurrentUserContext merchant(Long merchantId) {
    return new CurrentUserContext(7L, merchantId, null, null, null,
        Set.of("MERCHANT_ADMIN"), Set.of());
  }
}
