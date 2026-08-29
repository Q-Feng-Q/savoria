package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.wallet.mapper.FamilyWalletMapper;
import com.familykitchen.wallet.model.entity.FamilyWalletAccountDO;
import com.familykitchen.wallet.service.FamilyWalletAccountInitializer;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Verifies first-use family-wallet account creation semantics. */
class FamilyWalletAccountInitializerTest {
  @Test
  void existingAccountIsNotInsertedAgain() {
    FamilyWalletMapper mapper = mock(FamilyWalletMapper.class);
    when(mapper.selectAccount(7L)).thenReturn(new FamilyWalletAccountDO());

    new FamilyWalletAccountInitializer(mapper).ensure(7L);

    verify(mapper, never()).insertAccountIfMissing(7L);
  }

  @Test
  void missingAccountUsesIdempotentInsert() {
    FamilyWalletMapper mapper = mock(FamilyWalletMapper.class);

    new FamilyWalletAccountInitializer(mapper).ensure(7L);

    verify(mapper).insertAccountIfMissing(7L);
  }

  @Test
  void ensureCommitsBeforeTheCallerLocksTheWallet() throws Exception {
    Method ensure = FamilyWalletAccountInitializer.class.getMethod("ensure", long.class);
    Transactional transactional = ensure.getAnnotation(Transactional.class);

    assertThat(transactional).isNotNull();
    assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
  }
}
