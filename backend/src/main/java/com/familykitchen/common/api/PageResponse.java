package com.familykitchen.common.api;

import java.util.List;

/**
 * 分页查询的统一响应，记录当前页参数、总量与本页数据。
 * @param page 当前页码
 * @param pageSize 每页期望条数
 * @param total 符合条件的记录总数
 * @param items 当前页数据
 * @param <T> 列表元素类型
 */
public record PageResponse<T>(int page, int pageSize, long total, List<T> items) {

  /**
   * 由已取得的一页数据创建分页响应；总数按当前列表大小计算。
   * @param page 当前页码
   * @param pageSize 每页期望条数
   * @param items 当前页数据
   * @param <T> 列表元素类型
   * @return 分页响应
   */
  public static <T> PageResponse<T> of(int page, int pageSize, List<T> items) {
    return new PageResponse<>(page, pageSize, items.size(), items);
  }
}

