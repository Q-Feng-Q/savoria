package com.familykitchen.notification.model.vo;

import java.time.LocalDateTime;

/**
 * 封装返回给调用方的通知数据。
 *
 * @param notificationId 通知标识
 * @param receiverScope receiverScope
 * @param category category
 * @param title title
 * @param content content
 * @param read read
 * @param createdAt 创建时间
 */
public record NotificationView(
    Long notificationId,
    String receiverScope,
    String category,
    String title,
    String content,
    boolean read,
    LocalDateTime createdAt
) {
}

