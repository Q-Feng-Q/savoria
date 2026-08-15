package com.familykitchen.purchase.service.impl;

import com.familykitchen.purchase.mapper.PurchaseMapper;
import com.familykitchen.purchase.model.dto.TempPurchaseItemRequest;
import com.familykitchen.purchase.model.entity.PurchaseDemandRow;
import com.familykitchen.purchase.model.entity.TempPurchaseItemEntity;
import com.familykitchen.purchase.model.enums.IngredientCalcType;
import com.familykitchen.purchase.model.enums.PurchaseSourceStatus;
import com.familykitchen.purchase.model.vo.CheckoutPurchaseItemView;
import com.familykitchen.purchase.model.vo.PurchaseItemSummary;
import com.familykitchen.purchase.service.PurchaseApplicationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 采购清单生成与商户备菜准备的应用服务实现。
 *
 * <p>聚合已确认订单、待确认订单以及商户临时采购项中的食材需求，
 * 输出汇总视图和按家庭拆分视图，方便商户统一备菜、勾选和处理采购内容。</p>
 */
@Service
public class PurchaseApplicationServiceImpl implements PurchaseApplicationService {

  private final PurchaseMapper purchaseMapper;

  /**
   * 创建采购实例。
   *
   * @param purchaseMapper 采购Mapper
   */
  public PurchaseApplicationServiceImpl(PurchaseMapper purchaseMapper) {
    this.purchaseMapper = purchaseMapper;
  }

  /**
   * 处理采购。
   *
   * @param merchantId 商户标识
   * @param date date
   * @param includePending includePending
   * @return 处理的结果
   */
  @Override
  public List<PurchaseItemSummary> summary(Long merchantId, LocalDate date, boolean includePending) {
    return aggregate(purchaseMapper.selectOrderPurchaseRows(merchantId, date, includePending), includePending);
  }

  /**
   * 处理家庭。
   *
   * @param merchantId 商户标识
   * @param familyId 家庭标识
   * @param date date
   * @param includePending includePending
   * @return 处理家庭的结果
   */
  @Override
  public List<CheckoutPurchaseItemView> byFamily(Long merchantId, Long familyId, LocalDate date, boolean includePending) {
    List<CheckoutPurchaseItemView> views = new ArrayList<>();
    for (PurchaseDemandRow row : purchaseMapper.selectOrderPurchaseRows(merchantId, date, includePending)) {
      if (!row.getFamilyId().equals(familyId)) {
        continue;
      }
      if (IngredientCalcType.valueOf(row.getCalcType()) == IngredientCalcType.NO_PURCHASE) {
        continue;
      }
      views.add(new CheckoutPurchaseItemView(
          null,
          row.getMealSlotId(),
          row.getIngredientName(),
          calculateQuantity(row),
          row.getUnit(),
          mapSourceStatus(row.getOrderStatus()),
          null,
          familyId,
          false,
          false
      ));
    }

    views.addAll(purchaseMapper.selectTempItems(merchantId, date, familyId).stream()
        .map(PurchaseApplicationServiceImpl::toTempView)
        .toList());
    return views;
  }

  /**
   * 查询商户指定日期的全部临时采购项。
   *
   * @param merchantId 商户标识
   * @param date 服务日期
   * @return 临时采购项视图
   */
  @Override
  public List<CheckoutPurchaseItemView> tempItems(Long merchantId, LocalDate date) {
    return purchaseMapper.selectAllTempItems(merchantId, date).stream()
        .map(PurchaseApplicationServiceImpl::toTempView)
        .toList();
  }

  /**
   * 处理采购。
   *
   * @param merchantId 商户标识
   * @param itemId 项目标识
   * @param checked checked
   */
  @Override
  @Transactional
  public void checked(Long merchantId, Long itemId, boolean checked) {
    purchaseMapper.updateTempChecked(merchantId, itemId, checked);
  }

  /**
   * 新增Temp项目。
   *
   * @param merchantId 商户标识
   * @param request 请求参数
   * @return 新增Temp项目的结果
   */
  @Override
  @Transactional
  public TempPurchaseItemRequest addTempItem(Long merchantId, TempPurchaseItemRequest request) {
    TempPurchaseItemEntity entity = new TempPurchaseItemEntity();
    entity.setMerchantId(merchantId);
    entity.setServiceDate(request.date());
    entity.setMealSlotId(request.mealSlotId());
    entity.setIngredientName(request.ingredientName());
    entity.setQuantity(request.quantity());
    entity.setUnit(request.unit());
    entity.setRemark(request.remark());
    entity.setChecked(false);
    purchaseMapper.insertTempItem(entity);
    return request;
  }

  /**
   * 删除Temp项目。
   *
   * @param merchantId 商户标识
   * @param itemId 项目标识
   */
  @Override
  @Transactional
  public void deleteTempItem(Long merchantId, Long itemId) {
    purchaseMapper.deleteTempItem(merchantId, itemId);
  }

  /**
   * 复制Text。
   *
   * @param merchantId 商户标识
   * @param date date
   * @param mealSlotId mealSlot标识
   * @return 复制Text的结果
   */
  @Override
  public String copyText(Long merchantId, LocalDate date, Long mealSlotId) {
    List<PurchaseDemandRow> rows = purchaseMapper.selectOrderPurchaseRows(merchantId, date, true);
    if (mealSlotId != null) {
      rows = rows.stream()
          .filter(row -> mealSlotId.equals(row.getMealSlotId()))
          .toList();
    }
    return aggregate(rows, true).stream()
        .map(item -> item.ingredientName() + " " + item.quantity().stripTrailingZeros().toPlainString() + item.unit())
        .reduce((left, right) -> left + System.lineSeparator() + right)
        .orElse("");
  }

  /**
   * 聚合采购需求，按食材名、单位和来源状态分组。
   */
  private List<PurchaseItemSummary> aggregate(List<PurchaseDemandRow> rows, boolean includePending) {
    Map<PurchaseKey, MutableSummary> grouped = new LinkedHashMap<>();
    for (PurchaseDemandRow row : rows) {
      PurchaseSourceStatus sourceStatus = mapSourceStatus(row.getOrderStatus());
      if (sourceStatus == PurchaseSourceStatus.ESTIMATED && !includePending) {
        continue;
      }
      if (IngredientCalcType.valueOf(row.getCalcType()) == IngredientCalcType.NO_PURCHASE) {
        continue;
      }
      PurchaseKey key = new PurchaseKey(row.getIngredientName(), row.getUnit(), sourceStatus);
      grouped.computeIfAbsent(key, MutableSummary::new).add(calculateQuantity(row), row);
    }
    return grouped.values().stream()
        .map(MutableSummary::toSummary)
        .sorted(Comparator
            .comparing(PurchaseItemSummary::ingredientName)
            .thenComparing(item -> item.sourceStatus().name()))
        .toList();
  }

  /**
   * 采购数量统一保留两位小数；按份计算的食材需要乘以点菜数量。
   */
  private BigDecimal calculateQuantity(PurchaseDemandRow row) {
    BigDecimal base = row.getQuantity().setScale(2, RoundingMode.HALF_UP);
    if (IngredientCalcType.valueOf(row.getCalcType()) == IngredientCalcType.FIXED) {
      return base;
    }
    return base.multiply(BigDecimal.valueOf(row.getDishQuantity()))
        .setScale(2, RoundingMode.HALF_UP);
  }

  private static PurchaseSourceStatus mapSourceStatus(String orderStatus) {
    return "PENDING".equals(orderStatus)
        ? PurchaseSourceStatus.ESTIMATED
        : PurchaseSourceStatus.CONFIRMED;
  }

  private static CheckoutPurchaseItemView toTempView(TempPurchaseItemEntity entity) {
    return new CheckoutPurchaseItemView(
        entity.getId(),
        entity.getMealSlotId(),
        entity.getIngredientName(),
        entity.getQuantity(),
        entity.getUnit(),
        PurchaseSourceStatus.CONFIRMED,
        entity.getRemark(),
        entity.getFamilyId(),
        Boolean.TRUE.equals(entity.getChecked()),
        true
    );
  }

  /**
   * 采购聚合分组键。
   
   * @param ingredientName 食材名称
   * @param unit unit
   * @param sourceStatus 来源状态
   */
  private record PurchaseKey(String ingredientName, String unit, PurchaseSourceStatus sourceStatus) {
  }

  /**
   * 聚合过程中的可变汇总对象，最后转换为只读 VO。
   */
  private static final class MutableSummary {

    private final PurchaseKey key;
    private BigDecimal quantity = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private final List<PurchaseItemSummary.PurchaseSource> sources = new ArrayList<>();

    private MutableSummary(PurchaseKey key) {
      this.key = key;
    }

    private void add(BigDecimal amount, PurchaseDemandRow row) {
      quantity = quantity.add(amount).setScale(2, RoundingMode.HALF_UP);
      sources.add(new PurchaseItemSummary.PurchaseSource(
          row.getFamilyId(),
          row.getMealSlotId(),
          row.getOrderId(),
          row.getDishId(),
          row.getServiceDate()
      ));
    }

    private PurchaseItemSummary toSummary() {
      return new PurchaseItemSummary(
          key.ingredientName(),
          quantity,
          key.unit(),
          key.sourceStatus(),
          List.copyOf(sources)
      );
    }
  }
}
