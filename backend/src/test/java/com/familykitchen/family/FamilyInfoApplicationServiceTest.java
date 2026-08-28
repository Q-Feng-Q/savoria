package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.dto.UpdateFamilyInfoRequest;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.service.impl.FamilyApplicationServiceImpl;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 家庭资料读取、白名单更新与权限测试。 */
class FamilyInfoApplicationServiceTest {
  private FamilyMapper familyMapper;
  private FamilyApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    familyMapper = mock(FamilyMapper.class);
    service = new FamilyApplicationServiceImpl(
        familyMapper,
        mock(DishMapper.class), mock(ObjectMapper.class));
  }

  @Test
  void readsOnlyEditableAndReadonlyBusinessFieldsFromCurrentFamily() {
    FamilyRecord family = family();
    when(familyMapper.selectFamily(null, 8L)).thenReturn(family);

    var result = service.familyInfo(context("owner"));

    assertThat(result.familyName()).isEqualTo("林家小院");
    assertThat(result.note()).isEqualTo("晚餐少辣");
    assertThat(result.merchantName()).isEqualTo("老祁私厨");
    assertThat(result.deliveryEnabled()).isTrue();
    assertThat(result.deliveryFeeDefault()).isEqualByComparingTo("6.00");
    assertThat(result.deliveryFree()).isFalse();
  }

  @Test
  void ownerUpdateTrimsNameAndStoresBlankNoteAsNull() {
    when(familyMapper.updateFamilyInfo(8L, "林家新桌", null)).thenReturn(1);

    service.updateFamilyInfo(context("owner"), new UpdateFamilyInfoRequest("  林家新桌  ", "   "));

    verify(familyMapper).updateFamilyInfo(8L, "林家新桌", null);
  }

  @Test
  void adminCanUpdateFamilyProfile() {
    when(familyMapper.updateFamilyInfo(8L, "林家", "周末聚餐")).thenReturn(1);

    service.updateFamilyInfo(context("admin"), new UpdateFamilyInfoRequest("林家", " 周末聚餐 "));

    verify(familyMapper).updateFamilyInfo(8L, "林家", "周末聚餐");
  }

  @Test
  void memberCannotUpdateFamilyProfile() {
    assertThatThrownBy(() -> service.updateFamilyInfo(
        context("member"), new UpdateFamilyInfoRequest("林家", null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("仅家庭管理员");
  }

  private static CurrentUserContext context(String role) {
    return new CurrentUserContext(2L, null, 8L, 2L, role, Set.of(), Set.of());
  }

  private static FamilyRecord family() {
    FamilyRecord family = new FamilyRecord();
    family.setFamilyId(8L);
    family.setFamilyName("林家小院");
    family.setNote("晚餐少辣");
    family.setMerchantName("老祁私厨");
    family.setDeliveryEnabled(true);
    family.setDeliveryFeeDefault(new BigDecimal("6.00"));
    family.setDeliveryFree(false);
    return family;
  }
}
