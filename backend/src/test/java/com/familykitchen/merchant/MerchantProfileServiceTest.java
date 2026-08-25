package com.familykitchen.merchant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.merchant.mapper.MerchantProfileMapper;
import com.familykitchen.merchant.model.dto.UpdateMerchantProfileRequest;
import com.familykitchen.merchant.model.vo.MerchantProfileView;
import com.familykitchen.merchant.service.impl.MerchantProfileServiceImpl;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 商户负责人自助资料服务测试。 */
class MerchantProfileServiceTest {
  private MerchantProfileMapper mapper;
  private MerchantProfileServiceImpl service;

  @BeforeEach
  void setUp() {
    mapper = mock(MerchantProfileMapper.class);
    service = new MerchantProfileServiceImpl(mapper);
  }

  @Test
  void readsOnlyProfileBoundToCurrentUserAndMerchant() {
    MerchantProfileView expected = new MerchantProfileView("暖炉小馆", "林女士", "13800000000");
    when(mapper.selectProfile(2L, 7L)).thenReturn(expected);

    assertThat(service.profile(owner())).isEqualTo(expected);
  }

  @Test
  void updateTrimsFieldsAndConvertsBlankOptionalValuesToNull() {
    when(mapper.updateProfile(2L, 7L, "暖炉小馆", null, "13800000000")).thenReturn(1);

    service.updateProfile(owner(), new UpdateMerchantProfileRequest(
        " 暖炉小馆 ", "   ", " 13800000000 "));

    verify(mapper).updateProfile(2L, 7L, "暖炉小馆", null, "13800000000");
  }

  @Test
  void missingMerchantOrRevokedRelationIsRejected() {
    CurrentUserContext noMerchant = new CurrentUserContext(
        2L, null, null, null, "user", Set.of(), Set.of());
    assertThatThrownBy(() -> service.profile(noMerchant))
        .isInstanceOf(BusinessException.class).hasMessageContaining("商户");

    when(mapper.selectProfile(2L, 7L)).thenReturn(null);
    assertThatThrownBy(() -> service.profile(owner()))
        .isInstanceOf(BusinessException.class).hasMessageContaining("权限");

    when(mapper.updateProfile(2L, 7L, "暖炉", null, null)).thenReturn(0);
    assertThatThrownBy(() -> service.updateProfile(
        owner(), new UpdateMerchantProfileRequest("暖炉", null, null)))
        .isInstanceOf(BusinessException.class).hasMessageContaining("权限");
  }

  private static CurrentUserContext owner() {
    return new CurrentUserContext(
        2L, 7L, null, null, "merchant_admin", Set.of("merchant_admin"), Set.of("MERCHANT_ADMIN"));
  }
}
