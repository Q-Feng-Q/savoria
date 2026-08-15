package com.familykitchen.notification.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import org.apache.ibatis.type.Alias;

/**
 * 通知中心持久化实体。
 *
 * <p>对应 `notifications` 表，保存消息中心展示所需的通知数据。</p>
 */
@Alias("notificationDO")
@TableName("notifications")
public class NotificationDO {

  /** 通知 ID。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 接收范围，merchant 或 family。 */
  @TableField("receiver_scope")
  private String receiverScope;

  /** 通知分类。 */
  @TableField("category")
  private String category;

  /** 通知标题。 */
  @TableField("title")
  private String title;

  /** 通知内容。 */
  @TableField("content")
  private String content;

  /** 已读时间，未读时为空。 */
  @TableField("read_at")
  private LocalDateTime readAt;

  /** 创建时间。 */
  @TableField("created_at")
  private LocalDateTime createdAt;

  /**
   * 获取标识。
   *
   * @return 获取标识的结果
   */
  public Long getId() { return id; }
  /**
   * 标识。
   */
  /**
   * 设置标识。
   *
   * @param id 标识
   */
  public void setId(Long id) { this.id = id; }
  /**
   * 获取ReceiverScope。
   *
   * @return 获取ReceiverScope的结果
   */
  public String getReceiverScope() { return receiverScope; }
  /**
   * receiverScope。
   */
  /**
   * 设置ReceiverScope。
   *
   * @param receiverScope receiverScope
   */
  public void setReceiverScope(String receiverScope) { this.receiverScope = receiverScope; }
  /**
   * 获取Category。
   *
   * @return 获取Category的结果
   */
  public String getCategory() { return category; }
  /**
   * category。
   */
  /**
   * 设置Category。
   *
   * @param category category
   */
  public void setCategory(String category) { this.category = category; }
  /**
   * 获取Title。
   *
   * @return 获取Title的结果
   */
  public String getTitle() { return title; }
  /**
   * title。
   */
  /**
   * 设置Title。
   *
   * @param title title
   */
  public void setTitle(String title) { this.title = title; }
  /**
   * 获取Content。
   *
   * @return 获取Content的结果
   */
  public String getContent() { return content; }
  /**
   * content。
   */
  /**
   * 设置Content。
   *
   * @param content content
   */
  public void setContent(String content) { this.content = content; }
  /**
   * 获取Read时间。
   *
   * @return 获取Read时间的结果
   */
  public LocalDateTime getReadAt() { return readAt; }
  /**
   * read时间。
   */
  /**
   * 设置Read时间。
   *
   * @param readAt read时间
   */
  public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
  /**
   * 获取创建时间。
   *
   * @return 获取创建时间的结果
   */
  public LocalDateTime getCreatedAt() { return createdAt; }
  /**
   * 创建时间。
   */
  /**
   * 设置创建时间。
   *
   * @param createdAt 创建时间
   */
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
