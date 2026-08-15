package com.familykitchen.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockingDetails;

import com.familykitchen.purchase.mapper.PurchaseMapper;
import com.familykitchen.purchase.model.entity.TempPurchaseItemEntity;
import com.familykitchen.purchase.model.entity.PurchaseDemandRow;
import com.familykitchen.purchase.service.impl.PurchaseApplicationServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证采购应用服务的临时采购项查询契约。
 */
@ExtendWith(MockitoExtension.class)
class PurchaseApplicationServiceTest {

  @Mock private PurchaseMapper purchaseMapper;

  @Test
  void tempItemsQueriesAllMerchantRowsWithoutNullableFamilySentinel() {
    LocalDate date = LocalDate.of(2026, 8, 2);
    TempPurchaseItemEntity entity = new TempPurchaseItemEntity();
    entity.setId(71L);
    entity.setMealSlotId(42L);
    entity.setIngredientName("餐巾纸");
    entity.setQuantity(BigDecimal.valueOf(2));
    entity.setUnit("包");
    entity.setRemark("补货");
    entity.setChecked(false);
    when(purchaseMapper.selectAllTempItems(3L, date)).thenReturn(List.of(entity));

    var result = new PurchaseApplicationServiceImpl(purchaseMapper).tempItems(3L, date);

    assertThat(mockingDetails(purchaseMapper).getInvocations())
        .extracting(invocation -> invocation.getMethod().getName())
        .containsExactly("selectAllTempItems");
    assertThat(result).singleElement().satisfies(item -> {
      assertThat(item.itemId()).isEqualTo(71L);
      assertThat(item.mealSlotId()).isEqualTo(42L);
      assertThat(item.temporary()).isTrue();
    });
  }

  @Test
  void copyTextFiltersDemandRowsBeforeAggregatingSelectedMealQuantity() {
    LocalDate date = LocalDate.of(2026, 8, 2);
    PurchaseDemandRow lunch = demandRow(42L, BigDecimal.valueOf(2));
    PurchaseDemandRow dinner = demandRow(73L, BigDecimal.valueOf(5));
    when(purchaseMapper.selectOrderPurchaseRows(3L, date, true))
        .thenReturn(List.of(lunch, dinner));
    PurchaseApplicationServiceImpl service = new PurchaseApplicationServiceImpl(purchaseMapper);

    assertThat(service.copyText(3L, date, 42L)).isEqualTo("土豆 2斤");
    assertThat(service.copyText(3L, date, null)).isEqualTo("土豆 7斤");
  }

  private static PurchaseDemandRow demandRow(Long mealSlotId, BigDecimal quantity) {
    PurchaseDemandRow row = new PurchaseDemandRow();
    row.setFamilyId(2L);
    row.setMealSlotId(mealSlotId);
    row.setOrderId(mealSlotId);
    row.setDishId(100L);
    row.setServiceDate(LocalDate.of(2026, 8, 2));
    row.setOrderStatus("CONFIRMED");
    row.setIngredientName("土豆");
    row.setQuantity(quantity);
    row.setUnit("斤");
    row.setCalcType("FIXED");
    row.setDishQuantity(1);
    return row;
  }
}
