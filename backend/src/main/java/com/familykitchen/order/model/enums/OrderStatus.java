package com.familykitchen.order.model.enums;

/**
 * 定义订单状态可使用的状态或类型。
 */
public enum OrderStatus {
  /**
   * 表示等待处理状态。
   */
  PENDING,
  /**
   * 表示 {@code CONFIRMED} 对应的业务取值。
   */
  CONFIRMED,
  /**
   * 表示 {@code PREPARING} 对应的业务取值。
   */
  PREPARING,
  /**
   * 表示 {@code READY} 对应的业务取值。
   */
  READY,
  /**
   * 表示 {@code DONE} 对应的业务取值。
   */
  DONE,
  /**
   * 表示已取消状态。
   */
  CANCELLED,
  /**
   * 表示已停用、拒绝或失败状态。
   */
  REJECTED
}

