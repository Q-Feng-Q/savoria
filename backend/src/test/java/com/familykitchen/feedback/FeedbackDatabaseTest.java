package com.familykitchen.feedback;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import com.familykitchen.common.security.CurrentUserContext;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Each test owns a random in-memory H2 database; Spring Boot and Flyway are never started. */
class FeedbackDatabaseTest {
  @TempDir Path temp;
  FeedbackMapper mapper;FeedbackService service;TransactionTemplate tx;DataSourceTransactionManager manager;
  final CurrentUserContext user=new CurrentUserContext(1L,null,null,1L,"user",Set.of(),Set.of());
  final CurrentUserContext admin=new CurrentUserContext(2L,null,null,2L,"user",Set.of("PLATFORM_ADMIN"),Set.of());
  @BeforeEach void initializeIsolatedDatabase() throws Exception {
    var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:feedback_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
    String init=Files.readString(Path.of("src/main/resources/db/migration/V1__init_schema.sql"));
    try(var c=ds.getConnection();var stmt=c.createStatement()) {
      stmt.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,nickname VARCHAR(80))");stmt.execute("INSERT INTO users VALUES(1,'Member'),(2,'Admin')");
      for(String table:List.of("user_feedback","feedback_images","feedback_history")) {
        int start=init.indexOf("CREATE TABLE "+table+" (");String ddl=init.substring(start,init.indexOf(';',start));
        ddl=ddl.replaceAll(" CHARACTER SET ascii COLLATE ascii_bin","").replaceAll("\\) ENGINE=InnoDB.*$",")");stmt.execute(ddl);
      }
    }
    var config=new Configuration(new Environment("isolated-feedback",new SpringManagedTransactionFactory(),ds));config.setMapUnderscoreToCamelCase(true);
    try(var input=getClass().getResourceAsStream("/mapper/feedback/FeedbackMapper.xml")){new XMLMapperBuilder(input,config,"feedback",config.getSqlFragments()).parse();}
    mapper=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config)).getMapper(FeedbackMapper.class);
    service=new FeedbackService(mapper);manager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(manager);
  }
  FeedbackService.Create body(String request,List<String> ids){return new FeedbackService.Create(request,"BUG"," detail ",ids);}
  FeedbackImage image(String id,long owner,LocalDateTime date) {
    var i=new FeedbackImage();i.id=id;i.ownerUserId=owner;i.objectKey=id+".png";i.mime="image/png";i.width=1;i.height=1;i.byteSize=3;i.createdAt=date;mapper.insertImage(i);return i;
  }
  @Test void realMapperBindsImagesKeepsIdempotencyAndModerationAudit() {
    image("image-a",1L,LocalDateTime.now());
    var result=tx.execute(s->service.create(user,body("req",List.of("image-a"))));long id=(Long)result.get("feedbackId");
    assertThat(mapper.image("image-a").feedbackId).isEqualTo(id);
    var retried=tx.execute(s->service.create(user,body("req",List.of("image-a"))));assertThat(retried).isEqualTo(result);
    assertThatThrownBy(()->tx.execute(s->service.create(user,body("other",List.of("image-a"))))).hasMessageContaining("图片");
    var updated=tx.execute(s->service.update(admin,id,new FeedbackService.Update("RESOLVED","fixed",0)));
    assertThat(updated.get("ownerName")).isEqualTo("Member");
    assertThat(updated.get("version")).isEqualTo(1);assertThat(mapper.histories(id)).hasSize(1);
    assertThat(mapper.histories(id).get(0)).containsEntry("fromStatus","OPEN");
    assertThatThrownBy(()->tx.execute(s->service.update(admin,id,new FeedbackService.Update("CLOSED","done",0)))).hasMessageContaining("刷新");
    assertThat(service.detail(user,id,false)).doesNotContainKeys("history","ownerUserId","handledBy");
    assertThat(service.list(admin,true,"BUG","RESOLVED",1,20).get("total")).isEqualTo(1L);
    assertThat(service.list(admin,true,"BUG","RESOLVED",1,20)).containsEntry("page",1).containsEntry("pageSize",20);
  }
  @Test void rejectsForeignExpiredAndInvalidImagesWithoutCreatingRecord() {
    image("foreign",2L,LocalDateTime.now());image("expired",1L,LocalDateTime.now().minusHours(25));
    for(String id:List.of("foreign","expired","missing"))assertThatThrownBy(()->tx.execute(s->service.create(user,body(id,List.of(id))))).hasMessageContaining("图片");
    assertThat(mapper.count(1L,null,null)).isZero();
  }
  @Test void concurrentSameRequestCreatesOneRecord() throws Exception {
    var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
    try {
      Callable<Object> call=()->{start.await();return tx.execute(s->service.create(user,body("same",List.of()))).get("feedbackId");};
      var a=pool.submit(call);var b=pool.submit(call);start.countDown();assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));
      assertThat(mapper.count(1L,null,null)).isEqualTo(1);
    }finally{pool.shutdownNow();}
  }
  @Test void concurrentSubmissionsCannotExceedTenPerHour() throws Exception {
    for(int n=0;n<9;n++){String id="seed"+n;tx.execute(s->service.create(user,body(id,List.of())));}
    var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
    try {
      Callable<Boolean> call=()->{start.await();try{tx.execute(s->service.create(user,body(UUID.randomUUID().toString(),List.of())));return true;}catch(com.familykitchen.common.error.BusinessException e){return false;}};
      var a=pool.submit(call);var b=pool.submit(call);start.countDown();assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
      assertThat(mapper.count(1L,null,null)).isEqualTo(10);
    }finally{pool.shutdownNow();}
  }
  @Test void cleanupDeletesOnlyExpiredUnboundAndRetriesFilesystemFailure() throws Exception {
    var images=new FeedbackImageService(mapper,manager,temp.resolve("private").toString(),temp.resolve("public").toString());
    image("old",1L,LocalDateTime.now().minusHours(25));Files.write(temp.resolve("private/old.png"),new byte[]{1});
    image("new",1L,LocalDateTime.now());Files.write(temp.resolve("private/new.png"),new byte[]{1});
    image("bound",1L,LocalDateTime.now().minusHours(25));mapper.bind("bound",4L,0);Files.write(temp.resolve("private/bound.png"),new byte[]{1});
    image("retry",1L,LocalDateTime.now().minusHours(25));Files.createDirectory(temp.resolve("private/retry.png"));Files.write(temp.resolve("private/retry.png/child"),new byte[]{1});
    images.cleanupExpired();assertThat(mapper.image("old")).isNull();assertThat(mapper.image("retry")).isNotNull();
    assertThat(Files.exists(temp.resolve("private/new.png"))).isTrue();assertThat(Files.exists(temp.resolve("private/bound.png"))).isTrue();
    Files.delete(temp.resolve("private/retry.png/child"));images.cleanupExpired();assertThat(mapper.image("retry")).isNull();
  }
  @Test void cleanupWaitsForAttachmentLockAndRechecksBinding() throws Exception {
    image("racing",1L,LocalDateTime.now().minusHours(25));
    var images=new FeedbackImageService(mapper,manager,temp.resolve("private").toString(),temp.resolve("public").toString());Files.write(temp.resolve("private/racing.png"),new byte[]{1});
    var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
    try {
      var bind=pool.submit(()->tx.executeWithoutResult(s->{mapper.lockImage("racing");locked.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}mapper.bind("racing",9L,0);}));
      assertThat(locked.await(5,TimeUnit.SECONDS)).isTrue();var cleanup=pool.submit(images::cleanupExpired);release.countDown();bind.get(10,TimeUnit.SECONDS);cleanup.get(10,TimeUnit.SECONDS);
      assertThat(mapper.image("racing").feedbackId).isEqualTo(9L);assertThat(Files.exists(temp.resolve("private/racing.png"))).isTrue();
    }finally{release.countDown();pool.shutdownNow();}
  }
}
