package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyApplicationMapper;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import com.familykitchen.family.mapper.FamilyWorkflowMapper;
import com.familykitchen.family.mapper.MerchantInvitationMapper;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.family.model.entity.FamilyApplicationDO;
import com.familykitchen.family.model.entity.FamilyMembershipRequestDO;
import com.familykitchen.family.service.impl.FamilyMemberApplicationServiceImpl;
import com.familykitchen.user.mapper.UserMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证家庭OnboardingService相关业务契约与回归场景。
 */
@ExtendWith(MockitoExtension.class)
class FamilyOnboardingServiceTest {
  @Mock private FamilyApplicationMapper applications;
  @Mock private FamilyRelationMapper relations;
  @Mock private FamilyWorkflowMapper workflows;
  @Mock private NotificationPersistenceMapper notifications;
  @Mock private UserMapper users;
  @Mock private MerchantInvitationMapper merchantInvitations;

  @Test
  void returnsOnlyCurrentUsersOnboardingState() {
    var application = new FamilyApplicationDO();
    application.setStatus("pending");
    var join = new FamilyMembershipRequestDO();
    join.setStatus("PENDING");
    when(applications.findLatestByApplicant(7L)).thenReturn(application);
    when(workflows.findLatestCodeApplication(7L)).thenReturn(join);
    var service = new FamilyMemberApplicationServiceImpl(applications, relations, workflows, users, merchantInvitations, notifications);
    var current = new CurrentUserContext(7L, null, null, 7L, "user", Set.of(), Set.of());

    var result = service.onboarding(current);

    assertThat(result.familyApplication()).isSameAs(application);
    assertThat(result.joinApplication()).isSameAs(join);
  }
}
