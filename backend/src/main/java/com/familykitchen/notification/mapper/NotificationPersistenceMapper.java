package com.familykitchen.notification.mapper;

import com.familykitchen.notification.model.entity.NotificationDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 通知持久化 Mapper。
 *
 * <p>仅暴露通知中心实际使用的显式数据库操作。</p>
 */
@Mapper
public interface NotificationPersistenceMapper {

  /**
   * 新增通知。
   *
   * @param receiverType receiver类型
   * @param receiverId receiver标识
   * @param receiverScope receiverScope
   * @param category category
   * @param title title
   * @param content content
   * @return 新增通知的结果
   */
  int insertNotification(
      @Param("receiverType") String receiverType,
      @Param("receiverId") Long receiverId,
      @Param("receiverScope") String receiverScope,
      @Param("category") String category,
      @Param("title") String title,
      @Param("content") String content
  );

  /**
   * 查询Notifications。
   *
   * @param receiverScope receiverScope
   * @param receiverId receiver标识
   * @return 查询Notifications的结果
   */
  List<NotificationDO> selectNotifications(@Param("receiverScope") String receiverScope, @Param("receiverId") Long receiverId);

  /**
   * 统计通知Ownership。
   *
   * @param notificationId 通知标识
   * @param receiverScope receiverScope
   * @param receiverId receiver标识
   * @return 统计通知Ownership的结果
   */
  int countNotificationOwnership(
      @Param("notificationId") Long notificationId,
      @Param("receiverScope") String receiverScope,
      @Param("receiverId") Long receiverId
  );

  /**
   * 标记通知Read。
   *
   * @param notificationId 通知标识
   * @return 标记通知Read的结果
   */
  int markNotificationRead(@Param("notificationId") Long notificationId);

  /**
   * 标记AllNotificationsRead。
   *
   * @param receiverScope receiverScope
   * @param receiverId receiver标识
   * @return 标记AllNotificationsRead的结果
   */
  int markAllNotificationsRead(@Param("receiverScope") String receiverScope, @Param("receiverId") Long receiverId);
}
