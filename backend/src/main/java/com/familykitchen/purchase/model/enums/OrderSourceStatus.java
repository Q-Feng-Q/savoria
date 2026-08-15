package com.familykitchen.purchase.model.enums;

/**
 * 定义订单来源状态可使用的状态或类型。
 */
public enum OrderSourceStatus {
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
  READY
}


