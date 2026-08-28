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
    verifyNoMoreInteractions(walletMapper);
  }

  @Test
  void authorizedPersonalAdjustmentReturnsExplicitUpgradeErrorWithoutMutation() {
    CurrentUserContext merchant = merchant(31L);
    when(walletMapper.countMerchantMember(31L, 99L)).thenReturn(1);
    MerchantWalletApplicationServiceImpl service = new MerchantWalletApplicationServiceImpl(walletMapper);

    assertThatThrownBy(() -> service.adjustBalance(merchant, 99L,
        new AdjustMemberBalanceRequest(
            LedgerType.MANUAL_CREDIT, new BigDecimal("10.00"), "test")))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> org.assertj.core.api.Assertions.assertThat(error.errorCode())
                .isEqualTo(ErrorCode.CLIENT_UPGRADE_REQUIRED));

    verify(walletMapper).countMerchantMember(31L, 99L);
    verifyNoMoreInteractions(walletMapper);
  }

  private static CurrentUserContext merchant(Long merchantId) {
    return new CurrentUserContext(7L, merchantId, null, null, null,
        Set.of("MERCHANT_ADMIN"), Set.of());
  }
}
