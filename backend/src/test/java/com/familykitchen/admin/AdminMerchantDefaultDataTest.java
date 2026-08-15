package com.familykitchen.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.familykitchen.admin.mapper.AdminMerchantMapper;
import com.familykitchen.admin.model.dto.AdminMerchantRequest;
import com.familykitchen.admin.model.entity.AdminMerchantDO;
import com.familykitchen.admin.service.AdminMerchantService;
import com.familykitchen.family.mapper.MerchantInvitationMapper;
import com.familykitchen.merchant.service.MerchantDefaultDataInitializer;
import org.junit.jupiter.api.Test;

/** 验证平台创建商户时会同步生成默认目录。 */
class AdminMerchantDefaultDataTest {

  @Test
  void initializesDefaultCatalogAfterMerchantIsCreated() {
    AdminMerchantMapper merchants = mock(AdminMerchantMapper.class);
    MerchantInvitationMapper codes = mock(MerchantInvitationMapper.class);
    MerchantDefaultDataInitializer defaults = mock(MerchantDefaultDataInitializer.class);
    when(merchants.countActiveUser(8L)).thenReturn(1);
    doAnswer(invocation -> {
      invocation.getArgument(0, AdminMerchantDO.class).setId(31L);
      return 1;
    }).when(merchants).insert(any(AdminMerchantDO.class));

    AdminMerchantService service = new AdminMerchantService(merchants, codes, defaults);
    Long merchantId = service.create(new AdminMerchantRequest(
        "食光私厨", "张三", "13800000000", 8L, "active"));

    assertEquals(31L, merchantId);
    var order = inOrder(merchants, defaults);
    order.verify(merchants).insert(any(AdminMerchantDO.class));
    order.verify(defaults).initialize(31L);
    order.verify(merchants).insertOwner(31L, 8L);
  }
}
