package com.familykitchen.admin;
import static org.mockito.Mockito.*;
import com.familykitchen.admin.service.impl.AdminFamilyApplicationServiceImpl;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.*;
import com.familykitchen.family.model.entity.FamilyApplicationDO;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.merchant.service.MerchantDefaultDataInitializer;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 验证Joint家庭商户Approval相关业务契约与回归场景。
 */
class JointFamilyMerchantApprovalTest {
 @Test void jointCreateModeCreatesMerchantFamilyAndBothOwnerRelations(){
  var apps=mock(FamilyApplicationMapper.class);var relations=mock(FamilyRelationMapper.class);var workflow=mock(FamilyWorkflowMapper.class);var notices=mock(NotificationPersistenceMapper.class);var defaults=mock(MerchantDefaultDataInitializer.class);
  var application=new FamilyApplicationDO();application.setId(7L);application.setApplicantMemberId(9L);application.setFamilyName("新家庭");application.setMerchantMode(" JOINT_CREATE ");application.setProposedMerchantName("新私厨");application.setStatus("pending");
  when(apps.findById(7L)).thenReturn(application);when(relations.findActiveFamilyId(9L)).thenReturn(null);when(workflow.insertMerchant(application)).thenAnswer(i->{application.setMerchantId(33L);return 1;});when(workflow.insertMerchantOwner(9L,33L)).thenReturn(1);when(workflow.countActiveMerchant(33L)).thenReturn(1);when(workflow.insertFamily(any())).thenAnswer(i->{((com.familykitchen.family.model.entity.FamilyRecord)i.getArgument(0)).setFamilyId(44L);return 1;});when(relations.insertActive(9L,44L,"OWNER","CREATOR",null,9L)).thenReturn(1);when(apps.updateStatus(7L,"approved",1L,"ok")).thenReturn(1);
  new AdminFamilyApplicationServiceImpl(apps,relations,workflow,notices,defaults).approve(new CurrentUserContext(1L,null,null,null,null,Set.of("PLATFORM_ADMIN"),Set.of()),7L,"ok");
  var order=inOrder(workflow,defaults);order.verify(workflow).insertMerchant(application);order.verify(defaults).initialize(33L);order.verify(workflow).insertMerchantOwner(9L,33L);order.verify(workflow).insertFamily(any());
  verify(apps).attachMerchant(7L,33L);
  verify(relations).insertActive(9L,44L,"OWNER","CREATOR",null,9L);
 }
}
