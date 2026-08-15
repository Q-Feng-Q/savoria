package com.familykitchen.notification.service.impl;

import com.familykitchen.common.api.PageResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.notification.model.entity.NotificationDO;
import com.familykitchen.notification.model.vo.NotificationView;
import com.familykitchen.notification.service.NotificationApplicationService;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知中心应用服务实现。
 *
 * <p>为家庭端与商户端提供通知列表读取、单条已读和全部已读能力，
 * 统一封装消息中心相关读写操作。</p>
 */
@Service
public class NotificationApplicationServiceImpl implements NotificationApplicationService {

  private final NotificationPersistenceMapper notificationMapper;

  /**
   * 创建通知实例。
   *
   * @param notificationMapper 通知Mapper
   */
  public NotificationApplicationServiceImpl(NotificationPersistenceMapper notificationMapper) {
    this.notificationMapper = notificationMapper;
  }

  /**
   * 列出通知。
   *
   * @param user 用户
   * @param receiverScope receiverScope
   * @param readStatus read状态
   * @param category category
   * @param page 分页
   * @param pageSize 分页Size
   * @return 列出的结果
   */
  @Override
  public PageResponse<NotificationView> list(
      CurrentUserContext user,
      String receiverScope,
      String readStatus,
      String category,
      int page,
      int pageSize
  ) {
    // Mapper 返回当前接收方的通知，应用服务负责按已读状态和分类做轻量筛选。
    String scope=normalizeReceiverScope(user,receiverScope);
    List<NotificationDO> source=new ArrayList<>();
    if("account".equals(scope)){
      source.addAll(notificationMapper.selectNotifications("user",user.userId()));
      if(user.familyId()!=null)source.addAll(notificationMapper.selectNotifications("family",user.familyId()));
      source.sort(Comparator.comparing(NotificationDO::getCreatedAt,Comparator.nullsLast(Comparator.reverseOrder())));
    }else source.addAll(notificationMapper.selectNotifications(scope,receiverId(user,scope)));
    List<NotificationView> items = source
        .stream()
        .filter(item -> matchReadStatus(item, readStatus))
        .filter(item -> category == null || category.isBlank() || category.equals(item.getCategory()))
        .map(NotificationApplicationServiceImpl::toView)
        .toList();
    return PageResponse.of(page, pageSize, items);
  }

  /**
   * 处理通知。
   *
   * @param user 用户
   * @param notificationId 通知标识
   */
  @Override
  @Transactional
  public void read(CurrentUserContext user, Long notificationId) {
    boolean owned=notificationMapper.countNotificationOwnership(notificationId,"user",user.userId())>0;
    if(!owned&&user.familyId()!=null)owned=notificationMapper.countNotificationOwnership(notificationId,"family",user.familyId())>0;
    if(!owned&&user.merchantId()!=null&&user.hasMerchantBackendAccess())owned=notificationMapper.countNotificationOwnership(notificationId,"merchant",user.merchantId())>0;
    if (!owned) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到通知");
    }
    notificationMapper.markNotificationRead(notificationId);
  }

  /**
   * 处理All。
   *
   * @param user 用户
   * @param receiverScope receiverScope
   */
  @Override
  @Transactional
  public void readAll(CurrentUserContext user, String receiverScope) {
    String scope = normalizeReceiverScope(user, receiverScope);
    if("account".equals(scope)){
      notificationMapper.markAllNotificationsRead("user",user.userId());
      if(user.familyId()!=null)notificationMapper.markAllNotificationsRead("family",user.familyId());
      return;
    }
    notificationMapper.markAllNotificationsRead(scope, receiverId(user, scope));
  }

  private static NotificationView toView(NotificationDO entity) {
    return new NotificationView(
        entity.getId(),
        entity.getReceiverScope(),
        entity.getCategory(),
        entity.getTitle(),
        entity.getContent(),
        entity.getReadAt() != null,
        entity.getCreatedAt()
    );
  }

  private static boolean matchReadStatus(NotificationDO entity, String readStatus) {
    if (readStatus == null || readStatus.isBlank() || "all".equalsIgnoreCase(readStatus)) {
      return true;
    }
    if ("read".equalsIgnoreCase(readStatus)) {
      return entity.getReadAt() != null;
    }
    if ("unread".equalsIgnoreCase(readStatus)) {
      return entity.getReadAt() == null;
    }
    return true;
  }

  private static String normalizeReceiverScope(CurrentUserContext user, String receiverScope) {
    String scope = receiverScope == null || receiverScope.isBlank()
        ? defaultReceiverScope(user) : receiverScope.trim().toLowerCase();
    if ("account".equals(scope)||"user".equals(scope)) return scope;
    if ("family".equals(scope) && user.familyId() != null) return scope;
    if ("merchant".equals(scope) && user.merchantId() != null && user.hasMerchantBackendAccess()) return scope;
    throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该通知范围");
  }

  private static String defaultReceiverScope(CurrentUserContext user) {
    return "user";
  }

  private static Long receiverId(CurrentUserContext user, String receiverScope) {
    if ("merchant".equals(receiverScope)) {
      return user.merchantId();
    }
    if ("family".equals(receiverScope)) return user.familyId();
    return user.userId();
  }
}
