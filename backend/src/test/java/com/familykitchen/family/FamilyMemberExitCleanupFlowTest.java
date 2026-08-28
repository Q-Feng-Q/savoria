package com.familykitchen.family;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.familykitchen.cart.service.ActiveCartMemberCleanupService;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyApplicationMapper;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import com.familykitchen.family.mapper.FamilyWorkflowMapper;
import com.familykitchen.family.mapper.MerchantInvitationMapper;
import com.familykitchen.family.service.impl.FamilyMemberApplicationServiceImpl;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.user.mapper.UserMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies membership transactions use the cart-to-relation lock order. */
class FamilyMemberExitCleanupFlowTest {

  @Test
  void voluntaryExitCleansCartBeforeLockingAndEndingMembership() {
    FamilyRelationMapper relations = mock(FamilyRelationMapper.class);
    FamilyWorkflowMapper workflows = mock(FamilyWorkflowMapper.class);
    ActiveCartMemberCleanupService cleanup = mock(ActiveCartMemberCleanupService.class);
    when(relations.lockActiveRole(7L, 13L)).thenReturn("MEMBER");
    when(relations.exit(7L, 7L, "搬家")).thenReturn(1);
    FamilyMemberApplicationServiceImpl service = service(relations, workflows, cleanup);

    service.exitFamily(user("member"), "搬家");

    org.mockito.InOrder order = inOrder(cleanup, relations);
    order.verify(cleanup).removeMember(13L, 7L);
    order.verify(relations).lockActiveRole(7L, 13L);
    order.verify(relations).exit(7L, 7L, "搬家");
  }

  @Test
  void dissolutionCleansWholeCartBeforeLockingAllMemberships() {
    FamilyRelationMapper relations = mock(FamilyRelationMapper.class);
    FamilyWorkflowMapper workflows = mock(FamilyWorkflowMapper.class);
    ActiveCartMemberCleanupService cleanup = mock(ActiveCartMemberCleanupService.class);
    when(workflows.lockActiveMemberIds(13L)).thenReturn(List.of(7L, 9L));
    FamilyMemberApplicationServiceImpl service = service(relations, workflows, cleanup);

    service.dissolveFamily(user("owner"));

    org.mockito.InOrder order = inOrder(cleanup, workflows);
    order.verify(cleanup).removeFamily(13L);
    order.verify(workflows).lockActiveMemberIds(13L);
    order.verify(workflows).invalidateRequests(13L);
    order.verify(workflows).disableCodes(13L);
    order.verify(workflows).dissolveRelations(13L, 7L);
    order.verify(workflows).disableFamily(13L);
  }

  private static FamilyMemberApplicationServiceImpl service(
      FamilyRelationMapper relations, FamilyWorkflowMapper workflows,
      ActiveCartMemberCleanupService cleanup) {
    return new FamilyMemberApplicationServiceImpl(
        mock(FamilyApplicationMapper.class), relations, workflows, mock(UserMapper.class),
        mock(MerchantInvitationMapper.class), mock(NotificationPersistenceMapper.class), cleanup);
  }

  private static CurrentUserContext user(String role) {
    return new CurrentUserContext(7L, 11L, 13L, 7L, role, Set.of(), Set.of());
  }
}
