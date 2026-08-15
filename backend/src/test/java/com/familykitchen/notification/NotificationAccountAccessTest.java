package com.familykitchen.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.notification.model.entity.NotificationDO;
import com.familykitchen.notification.service.impl.NotificationApplicationServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 验证通知AccountAccess相关业务契约与回归场景。
 */
class NotificationAccountAccessTest {
  @Test void accountScopeMergesUserAndFamilyNotifications() {
    var mapper=mock(NotificationPersistenceMapper.class);
    var service=new NotificationApplicationServiceImpl(mapper);
    when(mapper.selectNotifications("user",10L)).thenReturn(List.of(notification(1L,"user",LocalDateTime.now().minusMinutes(2))));
    when(mapper.selectNotifications("family",20L)).thenReturn(List.of(notification(2L,"family",LocalDateTime.now())));
    var result=service.list(user(10L,20L),"account","all",null,1,100);
    assertEquals(List.of(2L,1L),result.items().stream().map(v->v.notificationId()).toList());
  }

  @Test void accountScopeWorksWithoutFamily() {
    var mapper=mock(NotificationPersistenceMapper.class);
    var service=new NotificationApplicationServiceImpl(mapper);
    when(mapper.selectNotifications("user",10L)).thenReturn(List.of(notification(1L,"user",LocalDateTime.now())));
    assertEquals(1,service.list(user(10L,null),"account","all",null,1,100).total());
    verify(mapper,never()).selectNotifications(eq("family"),any());
  }

  @Test void readAcceptsOwnedFamilyNotificationForNormalUser() {
    var mapper=mock(NotificationPersistenceMapper.class);
    var service=new NotificationApplicationServiceImpl(mapper);
    when(mapper.countNotificationOwnership(8L,"user",10L)).thenReturn(0);
    when(mapper.countNotificationOwnership(8L,"family",20L)).thenReturn(1);
    service.read(user(10L,20L),8L);
    verify(mapper).markNotificationRead(8L);
  }

  private static CurrentUserContext user(Long id,Long family){return new CurrentUserContext(id,null,family,family==null?null:id,"member",Set.of(),Set.of());}
  private static NotificationDO notification(Long id,String scope,LocalDateTime at){var n=new NotificationDO();n.setId(id);n.setReceiverScope(scope);n.setCategory("order");n.setTitle("状态变化");n.setContent("内容");n.setCreatedAt(at);return n;}
}
