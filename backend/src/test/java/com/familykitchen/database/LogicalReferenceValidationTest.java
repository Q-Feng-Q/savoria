package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.dto.CartItemRequest;
import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.service.impl.CartApplicationServiceImpl;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.dish.service.impl.DishApplicationServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.service.impl.MerchantFamilyMenuApplicationServiceImpl;
import com.familykitchen.system.service.SystemSettingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 验证移除数据库外键后由应用服务承担关键跨表引用校验。 */
class LogicalReferenceValidationTest {

  private static final CurrentUserContext USER = new CurrentUserContext(
      7L, 11L, 13L, 7L, "MEMBER", Set.of("MERCHANT_ADMIN"), Set.of());

  @Test
  void createDishRejectsCategoryOutsideCurrentMerchant() {
    DishMapper mapper = mock(DishMapper.class);
    DishApplicationServiceImpl service = new DishApplicationServiceImpl(
        mapper, mock(SystemSettingService.class), mock(DishReviewService.class));
    DishRequest request = new DishRequest("测试菜", 99L, null, null,
        BigDecimal.TEN, List.of(), List.of(), "active");

    assertThrows(BusinessException.class, () -> service.createDish(USER, request));
    verify(mapper, never()).insertDish(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void addCartItemRejectsMealSlotOutsideCurrentFamily() {
    CartMapper mapper = mock(CartMapper.class);
    when(mapper.selectAvailableDish(13L, 21L))
        .thenReturn(new CartDishSnapshot(21L, "测试菜", BigDecimal.TEN));
    CartApplicationServiceImpl service = new CartApplicationServiceImpl(mapper);

    assertThrows(BusinessException.class, () -> service.addItem(USER,
        new CartItemRequest(31L, LocalDate.now(), 21L, 1, null)));
    verify(mapper, never()).insertCart(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void saveFamilyMenuRejectsDishOutsideCurrentMerchant() {
    FamilyMapper mapper = mock(FamilyMapper.class);
    when(mapper.countFamilyOwnership(11L, 13L)).thenReturn(1);
    MerchantFamilyMenuApplicationServiceImpl service =
        new MerchantFamilyMenuApplicationServiceImpl(mapper);

    assertThrows(BusinessException.class, () -> service.saveMenu(USER, 13L,
        new SaveFamilyMenuRequest(List.of(
            new SaveFamilyMenuRequest.MenuItem(21L, true, 1, BigDecimal.TEN)))));
    verify(mapper, never()).deleteFamilyMenu(13L);
  }
}
