package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.wallet.model.enums.LedgerType;
import com.familykitchen.wallet.model.bo.WalletAccount;
import com.familykitchen.wallet.model.bo.WalletChange;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 验证钱包Account相关业务契约与回归场景。
 */
class WalletAccountTest {

  @Test
  void freezeReleaseAndSettleKeepBalanceConsistent() {
    WalletAccount account = new WalletAccount(1L, money("100.00"), money("0.00"));

    WalletChange frozen = account.freeze(money("38.50"));
    assertThat(account.balanceAmount()).isEqualByComparingTo("61.50");
    assertThat(account.frozenAmount()).isEqualByComparingTo("38.50");
    assertThat(frozen.type()).isEqualTo(LedgerType.FREEZE);

    WalletChange released = account.release(money("8.50"));
    assertThat(account.balanceAmount()).isEqualByComparingTo("70.00");
    assertThat(account.frozenAmount()).isEqualByComparingTo("30.00");
    assertThat(released.type()).isEqualTo(LedgerType.RELEASE);

    WalletChange settled = account.settle(money("30.00"));
    assertThat(account.balanceAmount()).isEqualByComparingTo("70.00");
    assertThat(account.frozenAmount()).isEqualByComparingTo("0.00");
    assertThat(settled.type()).isEqualTo(LedgerType.SETTLE);
  }

  @Test
  void freezeRejectsInsufficientBalance() {
    WalletAccount account = new WalletAccount(1L, money("12.00"), money("0.00"));

    assertThatThrownBy(() -> account.freeze(money("18.00")))
        .isInstanceOf(BusinessException.class);
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }
}

