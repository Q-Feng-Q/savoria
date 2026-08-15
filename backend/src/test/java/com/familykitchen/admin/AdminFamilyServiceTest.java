package com.familykitchen.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.admin.mapper.AdminFamilyMapper;
import com.familykitchen.admin.model.vo.AdminFamilyOptionView;
import com.familykitchen.admin.service.impl.AdminFamilyServiceImpl;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.service.MerchantFamilyMenuApplicationService;
import com.familykitchen.wallet.service.MerchantWalletApplicationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证平台管理家庭Service相关业务契约与回归场景。
 */
@ExtendWith(MockitoExtension.class)
class AdminFamilyServiceTest {

  @Mock
  private AdminFamilyMapper mapper;
  @Mock private FamilyMapper familyMapper;
  @Mock private MerchantFamilyMenuApplicationService menuService;
  @Mock private MerchantWalletApplicationService walletService;

  @Test
  void listsEveryFamilyOptionAcrossMerchants() {
    when(mapper.selectFamilyOptions()).thenReturn(List.of(
        new AdminFamilyOptionView(3L, "林家餐桌", 2L, "暖食厨房", "active", 4)
    ));

    var service = new AdminFamilyServiceImpl(mapper, menuService, walletService);

    assertThat(service.options()).extracting(AdminFamilyOptionView::familyName)
        .containsExactly("林家餐桌");
  }

  @Test
  void deletingFamilyDisablesItToPreserveBusinessHistory() {
    var service = new AdminFamilyServiceImpl(mapper, menuService, walletService);

    service.disableFamily(3L);

    verify(mapper).disableFamily(3L);
  }

  @Test
  void platformMenuUsesTheSelectedFamilyMerchantContext() {
    when(mapper.selectFamilyOption(3L)).thenReturn(
        new AdminFamilyOptionView(3L, "林家餐桌", 2L, "暖食厨房", "active", 4));
    var service = new AdminFamilyServiceImpl(mapper, menuService, walletService);
    var platform = new CurrentUserContext(1L, null, null, null, "platform_admin",
        java.util.Set.of("platform_admin"), java.util.Set.of());

    service.menu(platform, 3L);

    verify(menuService).menu(org.mockito.ArgumentMatchers.argThat(user -> user.merchantId().equals(2L)), org.mockito.ArgumentMatchers.eq(3L));
  }
}
