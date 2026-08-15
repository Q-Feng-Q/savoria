package com.familykitchen.purchase.service;

import com.familykitchen.purchase.model.dto.TempPurchaseItemRequest;
import com.familykitchen.purchase.model.vo.CheckoutPurchaseItemView;
import com.familykitchen.purchase.model.vo.PurchaseItemSummary;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购清单服务。
 *
 * <p>聚合订单产生的食材需求和商户临时追加采购项，生成按食材汇总或按家庭拆分的采购视图。
 * 可同时展示已确认订单和待确认订单，其中待确认订单会以预估来源标记。</p>
 */
public interface PurchaseApplicationService {

  /**
   * 查询指定商户某天的采购汇总。
   *
   * @param merchantId 商户 ID
   * @param date 服务日期
   * @param includePending 是否包含待确认订单产生的预估采购需求
   * @return 按食材聚合后的采购汇总
   */
  List<PurchaseItemSummary> summary(Long merchantId, LocalDate date, boolean includePending);

  /**
   * 查询指定家庭某天的采购明细。
   *
   * @param merchantId 商户 ID
   * @param familyId 家庭 ID
   * @param date 服务日期
   * @param includePending 是否包含待确认订单产生的预估采购需求
   * @return 家庭维度采购明细
   */
  List<CheckoutPurchaseItemView> byFamily(Long merchantId, Long familyId, LocalDate date, boolean includePending);

  /**
   * 查询指定商户某天的全部临时采购项。
   *
   * @param merchantId 商户 ID
   * @param date 服务日期
   * @return 临时采购项
   */
  List<CheckoutPurchaseItemView> tempItems(Long merchantId, LocalDate date);

  /**
   * 修改临时采购项勾选状态。
   *
   * @param merchantId 商户 ID
   * @param itemId 临时采购项 ID
   * @param checked 是否已勾选
   */
  void checked(Long merchantId, Long itemId, boolean checked);

  /**
   * 添加商户临时采购项。
   *
   * @param merchantId 商户 ID
   * @param request 临时采购项名称、数量、单位、日期和家庭归属
   * @return 保存后的临时采购项请求数据
   */
  TempPurchaseItemRequest addTempItem(Long merchantId, TempPurchaseItemRequest request);

  /**
   * 删除商户临时采购项。
   *
   * @param merchantId 商户 ID
   * @param itemId 临时采购项 ID
   */
  void deleteTempItem(Long merchantId, Long itemId);

  /**
   * 生成可复制的采购清单文本。
   *
   * @param merchantId 商户 ID
   * @param date 服务日期
   * @param mealSlotId 餐次 ID，可为空
   * @return 采购清单文本
   */
  String copyText(Long merchantId, LocalDate date, Long mealSlotId);
}
