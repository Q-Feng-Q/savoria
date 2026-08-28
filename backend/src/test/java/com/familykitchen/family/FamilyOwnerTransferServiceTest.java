package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyApplicationMapper;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import com.familykitchen.family.mapper.FamilyWorkflowMapper;
import com.familykitchen.family.mapper.MerchantInvitationMapper;
import com.familykitchen.family.model.vo.OwnerCandidateView;
import com.familykitchen.family.service.impl.FamilyMemberApplicationServiceImpl;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.user.mapper.UserMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** 负责人候选和事务移交服务测试。 */
class FamilyOwnerTransferServiceTest {
  private FamilyWorkflowMapper workflow;
  private FamilyMemberApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    workflow = mock(FamilyWorkflowMapper.class);
    service = new FamilyMemberApplicationServiceImpl(
        mock(FamilyApplicationMapper.class),
        mock(FamilyRelationMapper.class),
        workflow,
        mock(UserMapper.class),
        mock(MerchantInvitationMapper.class),
        mock(NotificationPersistenceMapper.class),
        mock(com.familykitchen.cart.service.ActiveCartMemberCleanupService.class));
  }

  @Test
  void ownerReadsPrivacySafeCandidatesInMapperOrder() {
    List<OwnerCandidateView> candidates = List.of(
        new OwnerCandidateView(11L, "小林", "2318"),
        new OwnerCandidateView(12L, "小林", null));
    when(workflow.selectOwnerCandidates(8L, 2L)).thenReturn(candidates);

    assertThat(service.ownerCandidates(owner())).containsExactlyElementsOf(candidates);
  }

  @Test
  void adminCannotReadCandidatesOrTransferOwnership() {
    assertThatThrownBy(() -> service.ownerCandidates(context("admin")))
        .isInstanceOf(BusinessException.class).hasMessageContaining("只有负责人");
    assertThatThrownBy(() -> service.transferOwner(context("admin"), 11L))
        .isInstanceOf(BusinessException.class).hasMessageContaining("只有负责人");
  }

  @Test
  void transferLocksFamilyAndRevalidatesEveryInvariant() {
    when(workflow.lockFamily(8L)).thenReturn(8L);
    when(workflow.lockRole(2L, 8L)).thenReturn("OWNER");
    when(workflow.countActiveOwners(8L)).thenReturn(1);
    when(workflow.lockRole(11L, 8L)).thenReturn("MEMBER");
    when(workflow.countEligibleOwnerCandidate(8L, 11L)).thenReturn(1);
    when(workflow.updateRoleIfCurrent(2L, 8L, "OWNER", "MEMBER")).thenReturn(1);
    when(workflow.updateRoleIfCurrent(11L, 8L, "MEMBER", "OWNER")).thenReturn(1);

    service.transferOwner(owner(), 11L);

    InOrder order = inOrder(workflow);
    order.verify(workflow).lockFamily(8L);
    order.verify(workflow).lockRole(2L, 8L);
    order.verify(workflow).countActiveOwners(8L);
    order.verify(workflow).lockRole(11L, 8L);
    order.verify(workflow).countEligibleOwnerCandidate(8L, 11L);
    order.verify(workflow).updateRoleIfCurrent(2L, 8L, "OWNER", "MEMBER");
    order.verify(workflow).updateRoleIfCurrent(11L, 8L, "MEMBER", "OWNER");
    order.verify(workflow).countActiveOwners(8L);
  }

  @Test
  void invalidOrStaleCandidateRollsBackBeforeRoleUpdates() {
    when(workflow.lockFamily(8L)).thenReturn(8L);
    when(workflow.lockRole(2L, 8L)).thenReturn("OWNER");
    when(workflow.countActiveOwners(8L)).thenReturn(1);
    when(workflow.lockRole(11L, 8L)).thenReturn("MEMBER");
    when(workflow.countEligibleOwnerCandidate(8L, 11L)).thenReturn(0);

    assertThatThrownBy(() -> service.transferOwner(owner(), 11L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("目标不是有效家庭成员");
  }

  private static CurrentUserContext owner() {
    return context("owner");
  }

  private static CurrentUserContext context(String role) {
    return new CurrentUserContext(2L, null, 8L, 2L, role, Set.of(), Set.of());
  }
}
