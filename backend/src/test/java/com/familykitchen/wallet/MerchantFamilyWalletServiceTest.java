package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.wallet.mapper.FamilyWalletMapper;
import com.familykitchen.wallet.model.dto.AdjustFamilyBalanceRequest;
import com.familykitchen.wallet.model.enums.LedgerType;
import com.familykitchen.wallet.model.entity.FamilyWalletAccountDO;
import com.familykitchen.wallet.service.FamilyWalletService;
import com.familykitchen.wallet.service.impl.FamilyWalletApplicationServiceImpl;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies merchant family-wallet authorization and adjustment routing. */
class MerchantFamilyWalletServiceTest {
  private final FamilyWalletService wallet = mock(FamilyWalletService.class);
  private final FamilyWalletMapper mapper = mock(FamilyWalletMapper.class);
  private final FamilyMapper families = mock(FamilyMapper.class);
  private final FamilyWalletApplicationServiceImpl service =
      new FamilyWalletApplicationServiceImpl(wallet, mapper, families);
  private final CurrentUserContext merchant = new CurrentUserContext(
      9L, 1L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());

  @Test void creditUsesPathFamilyAndRequestId() {
    when(families.countFamilyOwnership(1L, 2L)).thenReturn(1);
    FamilyWalletAccountDO account = new FamilyWalletAccountDO();
    account.familyId = 2L; account.availableAmount = new BigDecimal("20.00");
    account.frozenAmount = BigDecimal.ZERO; account.version = 1L;
    when(wallet.get(2L)).thenReturn(account);
    service.adjustForMerchant(merchant, 2L, new AdjustFamilyBalanceRequest(
        "credit-1", LedgerType.MANUAL_CREDIT, new BigDecimal("20.00"), "充值"));
    verify(wallet).manualCredit(2L, 9L, new BigDecimal("20.00"),
        "family-wallet:2:adjust:credit-1", "充值");
  }

  @Test void wrongMerchantAndUnsupportedTypesAreRejected() {
    assertThatThrownBy(() -> service.adjustForMerchant(merchant, 2L,
        new AdjustFamilyBalanceRequest("x", LedgerType.MANUAL_CREDIT,
            BigDecimal.ONE, "x"))).isInstanceOf(BusinessException.class);
    when(families.countFamilyOwnership(1L, 2L)).thenReturn(1);
    assertThatThrownBy(() -> service.adjustForMerchant(merchant, 2L,
        new AdjustFamilyBalanceRequest("x", LedgerType.FREEZE,
            BigDecimal.ONE, "x"))).isInstanceOf(BusinessException.class);
  }
}
