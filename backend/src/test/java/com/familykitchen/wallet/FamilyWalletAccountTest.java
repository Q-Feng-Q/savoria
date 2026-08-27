package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.wallet.model.bo.FamilyWalletAccount;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Verifies exact family-wallet amount rules. */
class FamilyWalletAccountTest {
  @Test
  void freezeReleaseCaptureRefundAndManualAdjustmentsPreserveBalances() {
    FamilyWalletAccount account = new FamilyWalletAccount(7L, money("100.00"), money("0.00"));

    account.freeze(money("40.00"));
    account.release(money("5.00"));
    account.capture(money("30.00"));
    account.refund(money("10.00"));
    account.manualCredit(money("7.00"));
    account.manualDebit(money("2.00"));

    assertThat(account.availableAmount()).isEqualByComparingTo("80.00");
    assertThat(account.frozenAmount()).isEqualByComparingTo("5.00");
  }

  @Test
  void rejectsInsufficientOrOverReleaseAndInvalidPrecision() {
    FamilyWalletAccount account = new FamilyWalletAccount(7L, money("10.00"), money("2.00"));

    assertThatThrownBy(() -> account.freeze(money("10.01")))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> account.release(money("2.01")))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> account.capture(money("2.01")))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> account.manualCredit(new BigDecimal("1.001")))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> account.manualCredit(new BigDecimal("10000000000000000.00")))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> account.manualDebit(BigDecimal.ZERO))
        .isInstanceOf(BusinessException.class);
  }

  private static BigDecimal money(String value) { return new BigDecimal(value); }
}
