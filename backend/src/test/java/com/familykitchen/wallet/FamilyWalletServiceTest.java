package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.idempotency.CommandIdempotencyService;
import com.familykitchen.wallet.mapper.FamilyWalletMapper;
import com.familykitchen.wallet.model.entity.FamilyWalletAccountDO;
import com.familykitchen.wallet.model.entity.FamilyWalletLedgerDO;
import com.familykitchen.wallet.model.entity.FamilyWalletOrderHoldDO;
import com.familykitchen.wallet.service.FamilyWalletAccountInitializer;
import com.familykitchen.wallet.service.impl.FamilyWalletServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.function.Supplier;

/** Verifies family-wallet transitions and database lock order. */
class FamilyWalletServiceTest {
  @Test
  void newFreezeLocksWalletAndCreatesHoldAndLedger() {
    FamilyWalletMapper mapper=mock(FamilyWalletMapper.class);
    CommandIdempotencyService commands=commands();
    FamilyWalletAccountInitializer initializer=mock(FamilyWalletAccountInitializer.class);
    when(mapper.lockAccount(7L)).thenReturn(account("100.00","0.00"));
    when(mapper.updateAccount(any(Long.class),any(),any())).thenReturn(1);
    FamilyWalletServiceImpl service=new FamilyWalletServiceImpl(mapper,commands,initializer);

    service.freezeNewOrder(7,91,3,money("38.00"),"submit:req-1");

    var order=inOrder(initializer,mapper);order.verify(initializer).ensure(7);order.verify(mapper).lockAccount(7);order.verify(mapper).lockHold(91);
    ArgumentCaptor<FamilyWalletOrderHoldDO> hold=ArgumentCaptor.forClass(FamilyWalletOrderHoldDO.class);
    verify(mapper).insertHold(hold.capture());
    assertThat(hold.getValue().remainingFrozenAmount).isEqualByComparingTo("38.00");
    verify(mapper).updateAccount(7,money("62.00"),money("38.00"));
    ArgumentCaptor<FamilyWalletLedgerDO> ledger=ArgumentCaptor.forClass(FamilyWalletLedgerDO.class);
    verify(mapper).insertLedger(ledger.capture());
    assertThat(ledger.getValue().businessType).isEqualTo("ORDER_FREEZE");
  }

  @Test
  void existingHoldOperationsLockHoldBeforeWalletAndPreventOverRefund() {
    FamilyWalletMapper mapper=mock(FamilyWalletMapper.class);
    CommandIdempotencyService commands=commands();
    FamilyWalletAccountInitializer initializer=mock(FamilyWalletAccountInitializer.class);
    FamilyWalletOrderHoldDO hold=hold("0.00","20.00","30.00","0.00");
    when(mapper.lockHold(91L)).thenReturn(hold);
    when(mapper.lockOrder(91L)).thenReturn(7L);
    when(mapper.lockAccount(7L)).thenReturn(account("70.00","20.00"));
    when(mapper.updateAccount(any(Long.class),any(),any())).thenReturn(1);
    FamilyWalletServiceImpl service=new FamilyWalletServiceImpl(mapper,commands,initializer);

    service.capture(7,91,3,money("10.00"),"capture:req-1");

    var order=inOrder(mapper);order.verify(mapper).lockOrder(91);order.verify(mapper).lockHold(91);order.verify(mapper).lockAccount(7);
    assertThat(hold.capturedAmount).isEqualByComparingTo("40.00");
    assertThat(hold.remainingFrozenAmount).isEqualByComparingTo("10.00");

    hold.refundedAmount=money("39.00");
    assertThatThrownBy(() -> service.refund(7,91,3,money("2.00"),"refund:req-2"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void releaseRejectsMoreThanOrderHoldEvenWhenWalletHasOtherFrozenMoney() {
    FamilyWalletMapper mapper=mock(FamilyWalletMapper.class);
    CommandIdempotencyService commands=commands();
    FamilyWalletAccountInitializer initializer=mock(FamilyWalletAccountInitializer.class);
    when(mapper.lockOrder(91L)).thenReturn(7L);
    when(mapper.lockHold(91L)).thenReturn(hold("0.00","3.00","0.00","0.00"));
    when(mapper.lockAccount(7L)).thenReturn(account("10.00","50.00"));
    FamilyWalletServiceImpl service=new FamilyWalletServiceImpl(mapper,commands,initializer);
    assertThatThrownBy(() -> service.release(7,91,3,money("4.00"),"release:req"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void manualAdjustmentPayloadDistinguishesNullFromLiteralNullAndNormalizesWhitespace() {
    FamilyWalletMapper mapper=mock(FamilyWalletMapper.class);
    CommandIdempotencyService commands=commands();
    FamilyWalletAccountInitializer initializer=mock(FamilyWalletAccountInitializer.class);
    when(mapper.lockAccount(7L)).thenReturn(account("100.00","0.00"));
    when(mapper.updateAccount(any(Long.class),any(),any())).thenReturn(1);
    FamilyWalletServiceImpl service=new FamilyWalletServiceImpl(mapper,commands,initializer);

    service.manualCredit(7,3,money("1.00"),"null-remark",null);
    service.manualCredit(7,3,money("1.00"),"text-remark","  null  ");

    ArgumentCaptor<CommandIdempotencyService.Command> command=ArgumentCaptor.forClass(CommandIdempotencyService.Command.class);
    verify(commands,times(2)).execute(command.capture(),any());
    assertThat(command.getAllValues().get(0).payload()).endsWith("remark=-1:");
    assertThat(command.getAllValues().get(1).payload()).endsWith("remark=4:null");
  }

  @Test
  void getReturnsExistingAccountWithoutInitializing() {
    FamilyWalletMapper mapper=mock(FamilyWalletMapper.class);
    FamilyWalletAccountInitializer initializer=mock(FamilyWalletAccountInitializer.class);
    FamilyWalletAccountDO account=account("12.00","3.00");
    when(mapper.selectAccount(7L)).thenReturn(account);
    FamilyWalletServiceImpl service=new FamilyWalletServiceImpl(mapper,commands(),initializer);

    assertThat(service.get(7L)).isSameAs(account);
    verify(initializer,never()).ensure(7L);
  }

  @Test
  void getInitializesAndReloadsAMissingAccount() {
    FamilyWalletMapper mapper=mock(FamilyWalletMapper.class);
    FamilyWalletAccountInitializer initializer=mock(FamilyWalletAccountInitializer.class);
    FamilyWalletAccountDO created=account("0.00","0.00");
    when(mapper.selectAccount(7L)).thenReturn(null,created);
    FamilyWalletServiceImpl service=new FamilyWalletServiceImpl(mapper,commands(),initializer);

    assertThat(service.get(7L)).isSameAs(created);
    var order=inOrder(mapper,initializer);
    order.verify(mapper).selectAccount(7L);
    order.verify(initializer).ensure(7L);
    order.verify(mapper).selectAccount(7L);
  }

  @Test
  void getKeepsNotFoundWhenAnActiveFamilyCouldNotBeInitialized() {
    FamilyWalletMapper mapper=mock(FamilyWalletMapper.class);
    FamilyWalletAccountInitializer initializer=mock(FamilyWalletAccountInitializer.class);
    FamilyWalletServiceImpl service=new FamilyWalletServiceImpl(mapper,commands(),initializer);

    assertThatThrownBy(() -> service.get(7L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("家庭钱包不存在");
    verify(mapper,times(2)).selectAccount(7L);
    verify(initializer).ensure(7L);
  }

  private static FamilyWalletAccountDO account(String available,String frozen){FamilyWalletAccountDO a=new FamilyWalletAccountDO();a.familyId=7L;a.availableAmount=money(available);a.frozenAmount=money(frozen);return a;}
  private static FamilyWalletOrderHoldDO hold(String additional,String remaining,String captured,String refunded){FamilyWalletOrderHoldDO h=new FamilyWalletOrderHoldDO();h.id=1L;h.orderId=91L;h.familyId=7L;h.initialAmount=money("50.00");h.additionalFrozenAmount=money(additional);h.remainingFrozenAmount=money(remaining);h.capturedAmount=money(captured);h.releasedAmount=money("0.00");h.refundedAmount=money(refunded);h.status="ACTIVE";return h;}
  private static BigDecimal money(String value){return new BigDecimal(value);}
  @SuppressWarnings("unchecked")
  private static CommandIdempotencyService commands(){CommandIdempotencyService service=mock(CommandIdempotencyService.class);when(service.execute(any(),any())).thenAnswer(invocation->{Supplier<CommandIdempotencyService.Result> action=invocation.getArgument(1);return action.get();});return service;}
}
