package com.familykitchen.admin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.admin.mapper.AdminUserMapper;
import com.familykitchen.admin.service.AdminUserService;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.cart.service.ActiveCartMemberCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证平台管理用户Service相关业务契约与回归场景。
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
  @Mock private AdminUserMapper mapper;
  @Mock private SessionService sessions;
  @Mock private UserMapper users;
  @Mock private PasswordCodec passwords;
  @Mock private WalletPersistenceMapper wallets;
  @Mock private ActiveCartMemberCleanupService cartCleanup;
  private AdminUserService service;

  @BeforeEach
  void setUp() {
    service = new AdminUserService(mapper, sessions, users, passwords, wallets, cartCleanup);
  }

  @Test
  void safelyDeletesUserAndRevokesSessions() {
    when(mapper.lockStatus(9L)).thenReturn("ACTIVE");
    when(mapper.findActiveFamilyId(9L)).thenReturn(13L);
    when(mapper.lockActiveFamilyId(9L)).thenReturn(13L);
    when(mapper.anonymizeUser(9L)).thenReturn(1);

    service.delete(1L, 9L);

    InOrder order = inOrder(mapper, cartCleanup);
    order.verify(mapper).lockStatus(9L);
    order.verify(mapper).findActiveFamilyId(9L);
    order.verify(cartCleanup).removeMember(13L, 9L);
    order.verify(mapper).lockActiveFamilyId(9L);
    order.verify(mapper).anonymizeUser(9L);

    verify(mapper).disablePlatformRoles(9L);
    verify(mapper).disableFamilyRelations(9L);
    verify(mapper).disableMerchantRelations(9L);
    verify(sessions).revokeAll(9L);
  }

  @Test
  void refusesToDeleteCurrentAdministrator() {
    assertThatThrownBy(() -> service.delete(1L, 1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不能删除当前登录账号");

    verify(mapper, never()).anonymizeUser(1L);
  }

  @Test
  void reportsMissingOrAlreadyDeletedUser() {
    assertThatThrownBy(() -> service.delete(1L, 99L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("用户不存在或已删除");
  }
}
