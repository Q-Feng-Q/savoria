package com.familykitchen.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.cart.service.CartApplicationService;
import com.familykitchen.cart.service.ExpectedMealTimePolicy;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.order.model.dto.SubmitOrderRequest;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.vo.OrderView;
import com.familykitchen.order.service.FamilyOrderApplicationService;
import com.familykitchen.testsupport.SafeTestDatabaseProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Exercises immutable order submission, replay and same-cart races on disposable MySQL 8. */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test-container")
@SpringBootTest(properties = {
    "family-kitchen.instance.lease-enabled=false", "family-kitchen.migration.mode=OFF",
    "spring.mail.host=localhost", "spring.mail.username=test", "spring.mail.password=test",
    "family-kitchen.wechat.app-id=test", "family-kitchen.wechat.app-secret=test",
    "family-kitchen.jwt.secret=test-jwt-secret-with-more-than-32-bytes",
    "family-kitchen.auth.bootstrap-admin.password=test-admin-password",
    "family-kitchen.account-cancellation.cron=-"
})
class FamilyOrderSubmissionMySqlTest {
  @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_order_submission").withUsername("order_test")
      .withPassword("order_test");
  @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry){
    SafeTestDatabaseProperties.register(registry, MYSQL);
  }

  @Autowired FamilyOrderApplicationService orders;
  @Autowired CartApplicationService carts;
  @Autowired ExpectedMealTimePolicy times;
  @Autowired JdbcTemplate jdbc;
  private final CurrentUserContext user=new CurrentUserContext(
      9511L,9501L,9501L,9511L,"owner",Set.of(),Set.of());
  private LocalDateTime expectedTime;

  @BeforeEach void reset(){
    jdbc.update("delete from command_idempotency where family_id=9501");
    jdbc.update("delete from family_wallet_ledgers where family_id=9501");
    jdbc.update("delete from family_wallet_order_holds where family_id=9501");
    jdbc.update("delete ois from order_item_selections ois join order_items oi on oi.id=ois.order_item_id "
        + "join orders o on o.id=oi.order_id where o.family_id=9501");
    jdbc.update("delete oi from order_items oi join orders o on o.id=oi.order_id where o.family_id=9501");
    jdbc.update("delete from order_delivery_snapshots where order_id in "
        + "(select id from orders where family_id=9501)");
    jdbc.update("delete from orders where family_id=9501");
    jdbc.update("delete cis from cart_item_selections cis join cart_items ci on ci.id=cis.cart_item_id "
        + "join carts c on c.id=ci.cart_id where c.family_id=9501");
    jdbc.update("delete ci from cart_items ci join carts c on c.id=ci.cart_id where c.family_id=9501");
    jdbc.update("delete from carts where family_id=9501");
    jdbc.update("delete from family_wallets where family_id=9501");
    jdbc.update("delete from family_menu_items where family_id=9501");
    jdbc.update("delete from dishes where id=9521");jdbc.update("delete from dish_categories where id=9520");
    jdbc.update("delete from family_user_relations where family_id=9501");
    jdbc.update("delete from families where id=9501");
    jdbc.update("delete from users where id in (9511,9512)");
    jdbc.update("delete from merchants where id=9501");
    jdbc.update("insert into merchants(id,name,status) values(9501,''订单私厨'',''active'')");
    jdbc.update("insert into users(id,username,password_hash,nickname) values"
        + "(9511,''order-race-1'',''x'',''小林''),(9512,''order-race-2'',''x'',''阿禾'')");
    jdbc.update("insert into families(id,merchant_id,name,status,delivery_enabled,delivery_fee_default,delivery_free) "
        + "values(9501,9501,''订单家庭'',''active'',0,0,0)");
    jdbc.update("insert into family_user_relations(user_id,family_id,family_role,status,join_source) "
        + "values(9511,9501,''OWNER'',''ACTIVE'',''TEST''),(9512,9501,''MEMBER'',''ACTIVE'',''TEST'')");
    jdbc.update("insert into dish_categories(id,merchant_id,name) values(9520,9501,''订单分类'')");
    jdbc.update("insert into dishes(id,merchant_id,category_id,name,base_price,status) "
        + "values(9521,9501,9520,''番茄牛腩'',38,''active'')");
    jdbc.update("insert into family_menu_items(family_id,dish_id,enabled,final_price) "
        + "values(9501,9521,1,36)");
    jdbc.update("insert into family_wallets(family_id,available_amount,frozen_amount) values(9501,500,0)");
    expectedTime=times.snapshot().minimumExpectedMealTime();
    jdbc.update("insert into carts(id,merchant_id,family_id,status,version,expected_meal_time) "
        + "values(9541,9501,9501,''active'',3,?)",expectedTime);
    jdbc.update("insert into cart_items(id,cart_id,dish_id,dish_name_snapshot,price,quantity) "
        + "values(9551,9541,9521,''旧菜名'',1,3)");
    jdbc.update("insert into cart_item_selections(cart_item_id,user_id,quantity,item_remark) "
        + "values(9551,9511,1,null),(9551,9512,2,''少盐'')");
  }

  @Test void differentRequestIdsRaceToOneImmutableOrderAndNextGetCreatesNewCart() throws Exception{
    List<Object> outcomes=race(
        ()->orders.submit(user,request("submit-a",DeliveryMode.PICKUP)),
        ()->orders.submit(user,request("submit-b",DeliveryMode.PICKUP)));
    assertEquals(1,outcomes.stream().filter(OrderView.class::isInstance).count());
    assertEquals(1,outcomes.stream().filter(this::submittedConflict).count());
    assertEquals(1,count("select count(*) from orders where source_cart_id=9541"));
    assertEquals(1,count("select count(*) from order_items where order_id in "
        + "(select id from orders where source_cart_id=9541)"));
    assertEquals(2,count("select count(*) from order_item_selections where order_item_id in "
        + "(select id from order_items where order_id in (select id from orders where source_cart_id=9541))"));
    assertEquals(10800,jdbc.queryForObject(
        "select round(frozen_amount*100) from family_wallets where family_id=9501",Integer.class));
    assertTrue(carts.cart(user).cartId()!=9541L);
  }

  @Test void sameRequestReplaysAndSameKeyWithDifferentPayloadConflicts(){
    OrderView first=orders.submit(user,request("durable-submit",DeliveryMode.PICKUP));
    OrderView replay=orders.submit(user,request("durable-submit",DeliveryMode.PICKUP));
    assertEquals(first.orderId(),replay.orderId());
    try{
      orders.submit(user,request("durable-submit",DeliveryMode.DELIVERY));
      throw new AssertionError("expected idempotency conflict");
    }catch(BusinessException failure){assertEquals(ErrorCode.STATE_CONFLICT,failure.errorCode());}
    assertEquals(1,count("select count(*) from orders where source_cart_id=9541"));
  }

  private SubmitOrderRequest request(String requestId,DeliveryMode mode){
    return new SubmitOrderRequest(9541L,3L,requestId,mode,null,null);
  }
  private boolean submittedConflict(Object value){
    return value instanceof BusinessException failure&&failure.errorCode()==ErrorCode.CART_SUBMITTED;
  }
  private int count(String sql){return jdbc.queryForObject(sql,Integer.class);}
  private static List<Object> race(java.util.concurrent.Callable<?> first,
      java.util.concurrent.Callable<?> second)throws Exception{
    ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(2);
    CountDownLatch start=new CountDownLatch(1);
    try{
      List<Future<Object>> futures=List.of(pool.submit(()->run(first,ready,start)),
          pool.submit(()->run(second,ready,start)));
      assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();List<Object> values=new ArrayList<>();
      for(Future<Object> future:futures)values.add(future.get(30,TimeUnit.SECONDS));return values;
    }finally{pool.shutdownNow();}
  }
  private static Object run(java.util.concurrent.Callable<?> action,CountDownLatch ready,
      CountDownLatch start){ready.countDown();try{start.await(10,TimeUnit.SECONDS);return action.call();}
    catch(Throwable failure){return failure;}}
}
