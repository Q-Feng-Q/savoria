package com.familykitchen.notification.service;

import com.familykitchen.common.api.PageResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.notification.model.vo.NotificationView;

/**
 * 通知中心服务。
 *
 * <p>为家庭端和商户端提供通知列表、单条已读和批量已读能力。</p>
 */
public interface NotificationApplicationService {

  /**
   * 分页查询通知列表。
   *
   * @param user 当前登录用户上下文
   * @param receiverScope 接收端范围，例如 family 或 merchant
   * @param readStatus 已读状态筛选，可为空
   * @param category 通知分类筛选，可为空
   * @param page 页码
   * @param pageSize 每页数量
   * @return 通知分页结果
   */
  PageResponse<NotificationView> list(
      CurrentUserContext user,
      String receiverScope,
      String readStatus,
      String category,
      int page,
      int pageSize
  );

  /**
   * 将指定通知标记为已读。
   *
   * @param user 当前登录用户上下文
   * @param notificationId 通知 ID
   */
  void read(CurrentUserContext user, Long notificationId);

  /**
   * 将指定接收端范围内的通知全部标记为已读。
   *
   * @param user 当前登录用户上下文
   * @param receiverScope 接收端范围，例如 family 或 merchant
   */
  void readAll(CurrentUserContext user, String receiverScope);
}
