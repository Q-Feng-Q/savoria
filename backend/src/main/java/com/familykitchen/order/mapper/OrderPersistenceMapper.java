package com.familykitchen.order.mapper;

import com.familykitchen.order.model.entity.OrderDeliverySnapshotEntity;
import com.familykitchen.order.model.entity.OrderItemEntity;
import com.familykitchen.order.model.entity.OrderRecordEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单持久化 Mapper。
 *
 * <p>通过显式 SQL 处理订单主表、订单明细和配送快照，确保所有数据库操作均可在启动期检查。</p>
 */
@Mapper
public interface OrderPersistenceMapper {

  /**
   * 新增订单并回填数据库生成的订单 ID。
   *
   * @param entity 订单持久化实体
   * @return 受影响行数
   */
  int insertOrder(OrderRecordEntity entity);

  /**
   * 查询OrdersBy家庭标识。
   *
   * @param familyId 家庭标识
   * @return 查询OrdersBy家庭标识的结果
   */
  List<OrderRecordEntity> selectOrdersByFamilyId(@Param("familyId") Long familyId);

  /**
   * 查询OrdersBy商户标识。
   *
   * @param merchantId 商户标识
   * @return 查询OrdersBy商户标识的结果
   */
  List<OrderRecordEntity> selectOrdersByMerchantId(@Param("merchantId") Long merchantId);

  /**
   * 查询订单By家庭标识。
   *
   * @param familyId 家庭标识
   * @param orderId 订单标识
   * @return 查询订单By家庭标识的结果
   */
  OrderRecordEntity selectOrderByFamilyId(@Param("familyId") Long familyId, @Param("orderId") Long orderId);

  /**
   * 查询订单By商户标识。
   *
   * @param merchantId 商户标识
   * @param orderId 订单标识
   * @return 查询订单By商户标识的结果
   */
  OrderRecordEntity selectOrderByMerchantId(@Param("merchantId") Long merchantId, @Param("orderId") Long orderId);

  /**
   * 查询订单项目列表By订单Ids。
   *
   * @param orderIds 订单Ids
   * @return 查询订单项目列表By订单Ids的结果
   */
  List<OrderItemEntity> selectOrderItemsByOrderIds(@Param("orderIds") List<Long> orderIds);

  /**
   * 查询配送SnapshotsBy订单Ids。
   *
   * @param orderIds 订单Ids
   * @return 查询配送SnapshotsBy订单Ids的结果
   */
  List<OrderDeliverySnapshotEntity> selectDeliverySnapshotsByOrderIds(@Param("orderIds") List<Long> orderIds);

  /**
   * 更新订单。
   *
   * @param entity 实体
   * @return 更新订单的结果
   */
  int updateOrder(OrderRecordEntity entity);

  /**
   * 删除订单项目列表。
   *
   * @param orderId 订单标识
   * @return 删除订单项目列表的结果
   */
  int deleteOrderItems(@Param("orderId") Long orderId);

  /**
   * 新增订单项目。
   *
   * @param entity 实体
   * @return 新增订单项目的结果
   */
  int insertOrderItem(OrderItemEntity entity);

  /**
   * 删除配送Snapshot。
   *
   * @param orderId 订单标识
   * @return 删除配送Snapshot的结果
   */
  int deleteDeliverySnapshot(@Param("orderId") Long orderId);

  /**
   * 新增配送Snapshot。
   *
   * @param entity 实体
   * @return 新增配送Snapshot的结果
   */
  int insertDeliverySnapshot(OrderDeliverySnapshotEntity entity);
}

