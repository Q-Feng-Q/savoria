package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.wallet.service.PersonalWalletCutoverGuard;
import org.junit.jupiter.api.Test;

/** Verifies non-zero legacy member wallets block new family-wallet orders. */
class PersonalWalletCutoverGuardTest {
  @Test void blocksOnlyWhenAnActiveFamilyMemberStillHasPersonalMoney() {
    WalletPersistenceMapper mapper = mock(WalletPersistenceMapper.class);
    PersonalWalletCutoverGuard guard = new PersonalWalletCutoverGuard(mapper);
    when(mapper.countNonZeroPersonalWallets(2L)).thenReturn(1);
    assertThatThrownBy(() -> guard.requireReady(2L)).isInstanceOf(BusinessException.class)
        .hasMessageContaining("个人钱包余额尚未清零");
    when(mapper.countNonZeroPersonalWallets(2L)).thenReturn(0);
    guard.requireReady(2L);
  }
}
