package com.familykitchen.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.common.idempotency.CommandIdempotencyService;
import com.familykitchen.common.idempotency.CommandIdempotencyMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.wallet.service.FamilyWalletService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Real MySQL row-lock, terminal-transition, and persistent replay races. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties={"family-kitchen.instance.lease-enabled=false","family-kitchen.migration.mode=OFF",
    "spring.mail.host=localhost","spring.mail.username=test","spring.mail.password=test",
    "family-kitchen.wechat.app-id=test","family-kitchen.wechat.app-secret=test",
    "family-kitchen.jwt.secret=test-jwt-secret-with-more-than-32-bytes",
    "family-kitchen.auth.bootstrap-admin.password=test-admin-password","family-kitchen.account-cancellation.cron=-"})
class FamilyWalletConcurrencyMySqlTest {
  @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_wallet_concurrency").withUsername("wallet_test").withPassword("wallet_test");
  @DynamicPropertySource static void datasource(DynamicPropertyRegistry r){r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);r.add("spring.datasource.driver-class-name",MYSQL::getDriverClassName);}
  @Autowired FamilyWalletService wallets;@Autowired CommandIdempotencyService commands;@Autowired JdbcTemplate jdbc;
  @Autowired CommandIdempotencyMapper commandMapper;@Autowired PlatformTransactionManager transactionManager;
  /** Exceptions observed by the latest race. */
  private final List<Throwable> raceFailures=Collections.synchronizedList(new ArrayList<>());
  @BeforeEach void reset(){raceFailures.clear();jdbc.update("delete from family_wallet_ledgers where family_id=7001");jdbc.update("delete from family_wallet_order_holds where family_id=7001");jdbc.update("delete from command_idempotency where family_id=7001");jdbc.update("delete from orders where id=7200");jdbc.update("delete from family_wallets where family_id=7001");jdbc.update("insert into family_wallets(family_id,available_amount,frozen_amount) values(7001,100,0)");jdbc.update("insert into orders(id,merchant_id,family_id,submitter_user_id,meal_slot_id,service_date,delivery_mode,delivery_fee,delivery_fee_payer_user_id,status,total_amount) values(7200,1,7001,3,1,current_date,'PICKUP',0,3,'PENDING',100)");}

  @Test void twoFreezesCannotOverdrawOneFamilyBalance() throws Exception {
    int successes=race(i->{wallets.freezeNewOrder(7001,7100+i,3,new BigDecimal("80.00"),"freeze:"+i);return true;});
    assertNoDeadlock();
    assertEquals(1,successes);assertEquals(new BigDecimal("20.00"),money("available_amount"));assertEquals(new BigDecimal("80.00"),money("frozen_amount"));
    assertEquals(1,jdbc.queryForObject("select count(*) from family_wallet_order_holds where family_id=7001",Integer.class));
  }

  @Test void releaseVersusCaptureHasExactlyOneTerminalTransition() throws Exception {
    wallets.freezeNewOrder(7001,7200,3,new BigDecimal("100.00"),"freeze:terminal");
    int successes=race(i->{if(i==0)wallets.capture(7001,7200,3,new BigDecimal("100.00"),"capture:terminal");else wallets.release(7001,7200,3,new BigDecimal("100.00"),"release:terminal");return true;});
    assertNoDeadlock();
    assertEquals(1,successes);String status=jdbc.queryForObject("select status from family_wallet_order_holds where order_id=7200",String.class);assertTrue(List.of("CAPTURED","RELEASED").contains(status));
  }

  @Test void concurrentSameCommandRunsBusinessActionOnceAndReplaysResult() throws Exception {
    AtomicInteger calls=new AtomicInteger();int successes=race(i->{var result=commands.execute(new CommandIdempotencyService.Command(3,7001,"TEST","same","payload"),()->{calls.incrementAndGet();return new CommandIdempotencyService.Result("test",44L,"{}");});return result.resourceId()==44L;});
    assertEquals(2,successes);assertEquals(1,calls.get());assertEquals(1,jdbc.queryForObject("select count(*) from command_idempotency where family_id=7001",Integer.class));
  }

  @Test void replaySurvivesAServiceInstanceRestartAndPayloadConflictIsStable(){
    CommandIdempotencyService.Command command=new CommandIdempotencyService.Command(3,7001,"RESTART","restart-1","amount=1.00");
    commands.execute(command,()->new CommandIdempotencyService.Result("wallet",7001L,"{}"));
    CommandIdempotencyService restarted=new CommandIdempotencyService(commandMapper);
    CommandIdempotencyService.Result replay=new TransactionTemplate(transactionManager).execute(status->restarted.execute(command,()->{throw new AssertionError("replay executed action");}));
    assertEquals(7001L,replay.resourceId());
    CommandIdempotencyService.Command conflict=new CommandIdempotencyService.Command(3,7001,"RESTART","restart-1","amount=2.00");
    org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,()->new TransactionTemplate(transactionManager).execute(status->restarted.execute(conflict,()->null)));
  }

  @Test void failedActionRollsBackClaimAndLostResponseReplaysCommittedResult(){
    CommandIdempotencyService.Command failed=new CommandIdempotencyService.Command(3,7001,"FAIL","before-commit","x");
    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->commands.execute(failed,()->{throw new IllegalStateException("injected before commit");}));
    assertEquals(0,jdbc.queryForObject("select count(*) from command_idempotency where request_id='before-commit'",Integer.class));
    CommandIdempotencyService.Command committed=new CommandIdempotencyService.Command(3,7001,"LOST_RESPONSE","after-commit","x");
    commands.execute(committed,()->new CommandIdempotencyService.Result("wallet",7001L,"{}"));
    AtomicInteger calls=new AtomicInteger();CommandIdempotencyService.Result replay=commands.execute(committed,()->{calls.incrementAndGet();return null;});
    assertEquals(7001L,replay.resourceId());assertEquals(0,calls.get());
  }

  @Test void merchantDebitVersusSubmitFreezeSerializesWithoutDeadlock() throws Exception {
    int successes=race(i->{if(i==0)wallets.manualDebit(7001,8,new BigDecimal("30.00"),"merchant-debit","adjust");else wallets.freezeNewOrder(7001,7300,3,new BigDecimal("80.00"),"submit-freeze");return true;});
    assertEquals(1,successes);assertNoDeadlock();BigDecimal available=money("available_amount"),frozen=money("frozen_amount");assertTrue(available.signum()>=0&&frozen.signum()>=0);
  }

  private BigDecimal money(String column){return jdbc.queryForObject("select "+column+" from family_wallets where family_id=7001",BigDecimal.class);}
  private int race(ThrowingAction action)throws Exception{raceFailures.clear();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);try{List<Future<Boolean>> futures=new ArrayList<>();for(int i=0;i<2;i++){int index=i;futures.add(pool.submit(()->{ready.countDown();start.await(10,TimeUnit.SECONDS);try{return action.run(index);}catch(Exception failure){raceFailures.add(failure);return false;}}));}assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();int count=0;for(Future<Boolean> f:futures)if(f.get(20,TimeUnit.SECONDS))count++;return count;}finally{pool.shutdownNow();}}
  private void assertNoDeadlock(){assertTrue(raceFailures.stream().noneMatch(failure->{String text=String.valueOf(failure.getMessage()).toLowerCase();return text.contains("deadlock")||text.contains("40001");}),"race must not hide a database deadlock: "+raceFailures);}
  /** Concurrent test action. */
  @FunctionalInterface interface ThrowingAction{
    /** Runs one contender.
     * @param index contender index
     * @return true on success
     * @throws Exception test failure
     */
    boolean run(int index)throws Exception;
  }
}
