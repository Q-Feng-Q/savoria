package com.familykitchen.purchase.mapper;

import com.familykitchen.purchase.model.entity.PurchaseDemandRow;
import com.familykitchen.purchase.model.entity.TempPurchaseItemEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 采购清单 MyBatis Mapper。
 *
 * <p>负责查询订单推导出的采购需求，以及商户手工维护的临时采购项。</p>
 */
@Mapper
public interface PurchaseMapper {

  /**
   * 查询订单采购Rows。
   *
   * @param merchantId 商户标识
   * @param date date
   * @param includePending includePending
   * @return 查询订单采购Rows的结果
   */
  List<PurchaseDemandRow> selectOrderPurchaseRows(
      @Param("merchantId") Long merchantId,
      @Param("date") LocalDate date,
      @Param("includePending") boolean includePending
  );

  /**
   * 查询Temp项目列表。
   *
   * @param merchantId 商户标识
   * @param date date
   * @param familyId 家庭标识
   * @return 查询Temp项目列表的结果
   */
  List<TempPurchaseItemEntity> selectTempItems(
      @Param("merchantId") Long merchantId,
      @Param("date") LocalDate date,
      @Param("familyId") Long familyId
  );

  /**
   * Query all temporary items for a merchant date without a nullable scope sentinel.
   *
   * @param merchantId merchant identifier
   * @param date service date
   * @return all temporary purchase items for the merchant and date
   */
  List<TempPurchaseItemEntity> selectAllTempItems(
      @Param("merchantId") Long merchantId,
      @Param("date") LocalDate date
  );

  /**
   * 更新TempChecked。
   *
   * @param merchantId 商户标识
   * @param itemId 项目标识
   * @param checked checked
   * @return 更新TempChecked的结果
   */
  int updateTempChecked(@Param("merchantId") Long merchantId, @Param("itemId") Long itemId, @Param("checked") boolean checked);

  /**
   * 新增Temp项目。
   *
   * @param entity 实体
   * @return 新增Temp项目的结果
   */
  int insertTempItem(TempPurchaseItemEntity entity);

  /**
   * 删除Temp项目。
   *
   * @param merchantId 商户标识
   * @param itemId 项目标识
   * @return 删除Temp项目的结果
   */
  int deleteTempItem(@Param("merchantId") Long merchantId, @Param("itemId") Long itemId);
}
