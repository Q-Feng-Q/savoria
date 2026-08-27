package com.familykitchen.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.common.idempotency.CommandIdempotencyService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
  @BeforeEach void reset(){jdbc.update("delete from family_wallet_ledgers where family_id=7001");jdbc.update("delete from family_wallet_order_holds where family_id=7001");jdbc.update("delete from command_idempotency where family_id=7001");jdbc.update("delete from family_wallets where family_id=7001");jdbc.update("insert into family_wallets(family_id,available_amount,frozen_amount) values(7001,100,0)");}

  @Test void twoFreezesCannotOverdrawOneFamilyBalance() throws Exception {
    int successes=race(i->{wallets.freezeNewOrder(7001,7100+i,3,new BigDecimal("80.00"),"freeze:"+i);return true;});
    assertEquals(1,successes);assertEquals(new BigDecimal("20.00"),money("available_amount"));assertEquals(new BigDecimal("80.00"),money("frozen_amount"));
    assertEquals(1,jdbc.queryForObject("select count(*) from family_wallet_order_holds where family_id=7001",Integer.class));
  }

  @Test void releaseVersusCaptureHasExactlyOneTerminalTransition() throws Exception {
    wallets.freezeNewOrder(7001,7200,3,new BigDecimal("100.00"),"freeze:terminal");
    int successes=race(i->{if(i==0)wallets.capture(7001,7200,3,new BigDecimal("100.00"),"capture:terminal");else wallets.release(7001,7200,3,new BigDecimal("100.00"),"release:terminal");return true;});
    assertEquals(1,successes);String status=jdbc.queryForObject("select status from family_wallet_order_holds where order_id=7200",String.class);assertTrue(List.of("CAPTURED","RELEASED").contains(status));
  }

  @Test void concurrentSameCommandRunsBusinessActionOnceAndReplaysResult() throws Exception {
    AtomicInteger calls=new AtomicInteger();int successes=race(i->{var result=commands.execute(new CommandIdempotencyService.Command(3,7001,"TEST","same","payload"),()->{calls.incrementAndGet();return new CommandIdempotencyService.Result("test",44L,"{}");});return result.resourceId()==44L;});
    assertEquals(2,successes);assertEquals(1,calls.get());assertEquals(1,jdbc.queryForObject("select count(*) from command_idempotency where family_id=7001",Integer.class));
  }

  private BigDecimal money(String column){return jdbc.queryForObject("select "+column+" from family_wallets where family_id=7001",BigDecimal.class);}
  private int race(ThrowingAction action)throws Exception{ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);try{List<Future<Boolean>> futures=new ArrayList<>();for(int i=0;i<2;i++){int index=i;futures.add(pool.submit(()->{ready.countDown();start.await(10,TimeUnit.SECONDS);try{return action.run(index);}catch(Exception expected){return false;}}));}assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();int count=0;for(Future<Boolean> f:futures)if(f.get(20,TimeUnit.SECONDS))count++;return count;}finally{pool.shutdownNow();}}
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
