package com.familykitchen.family.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyApplicationMapper;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import com.familykitchen.family.mapper.FamilyWorkflowMapper;
import com.familykitchen.family.mapper.MerchantInvitationMapper;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.family.model.dto.DirectInvitationRequest;
import com.familykitchen.family.model.dto.FamilyApplyRequest;
import com.familykitchen.family.model.dto.JoinFamilyRequest;
import com.familykitchen.family.model.entity.FamilyApplicationDO;
import com.familykitchen.family.model.entity.FamilyMembershipRequestDO;
import com.familykitchen.family.service.FamilyMemberApplicationService;
import com.familykitchen.family.model.vo.FamilyOnboardingView;
import com.familykitchen.user.mapper.UserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 家庭成员、邀请、审批、转让和解散服务。 */
@Service
public class FamilyMemberApplicationServiceImpl implements FamilyMemberApplicationService {
  private final FamilyApplicationMapper applicationMapper; private final FamilyRelationMapper relationMapper;
  private final FamilyWorkflowMapper workflowMapper; private final UserMapper userMapper;
  private final MerchantInvitationMapper merchantInvitations;
  private final NotificationPersistenceMapper notificationMapper;
  /**
   * 创建家庭成员实例。
   *
   * @param applicationMapper 申请Mapper
   * @param relationMapper relationMapper
   * @param workflowMapper workflowMapper
   * @param userMapper 用户Mapper
   * @param merchantInvitations 商户Invitations
   * @param notificationMapper 通知Mapper
   */
  public FamilyMemberApplicationServiceImpl(FamilyApplicationMapper applicationMapper,FamilyRelationMapper relationMapper,
      FamilyWorkflowMapper workflowMapper,UserMapper userMapper,MerchantInvitationMapper merchantInvitations,NotificationPersistenceMapper notificationMapper){this.applicationMapper=applicationMapper;
    this.relationMapper=relationMapper;this.workflowMapper=workflowMapper;this.userMapper=userMapper;this.merchantInvitations=merchantInvitations;this.notificationMapper=notificationMapper;}

  /**
   * 处理家庭成员。
   *
   * @param user 用户
   * @return 处理的结果
   */
  @Override public FamilyOnboardingView onboarding(CurrentUserContext user){
    return new FamilyOnboardingView(applicationMapper.findLatestByApplicant(user.userId()),workflowMapper.findLatestCodeApplication(user.userId()));
  }

  /**
   * 提交家庭。
   *
   * @param user 用户
   * @param request 请求参数
   */
  @Override @Transactional public void applyFamily(CurrentUserContext user,FamilyApplyRequest request){
    requireNoFamily(user.userId());
    if(applicationMapper.findByApplicantAndStatus(user.userId(),"pending")!=null)
      throw new BusinessException(ErrorCode.STATE_CONFLICT,"已有待审批的家庭创建申请");
    FamilyApplicationDO e=new FamilyApplicationDO();e.setApplicantMemberId(user.userId());e.setFamilyName(request.familyName().trim());
    String merchantMode="CREATE".equals(request.merchantMode())?"JOINT_CREATE":request.merchantMode();
    e.setMerchantMode(merchantMode);
    if("INVITATION".equals(merchantMode)){if(request.merchantInvitationCode()==null||request.merchantInvitationCode().isBlank())
        throw new BusinessException(ErrorCode.BAD_REQUEST,"请填写商户邀请码");
      Long codeId=merchantInvitations.findValidId(hash(request.merchantInvitationCode()));
      if(codeId==null)throw new BusinessException(ErrorCode.BUSINESS_INVALID,"商户邀请码无效或已过期");e.setMerchantInvitationId(codeId);e.setMerchantId(merchantInvitations.findMerchantId(codeId));}
    else {if(request.newMerchantName()==null||request.newMerchantName().isBlank())throw new BusinessException(ErrorCode.BAD_REQUEST,"请填写私厨名称");
      e.setProposedMerchantName(request.newMerchantName().trim());e.setProposedContactName(request.contactName());e.setProposedContactPhone(request.contactPhone());}
    e.setStatus("pending");applicationMapper.insert(e);notifyUser(user.userId(),"family","家庭创建申请已提交","申请已提交，平台审核结果会在这里通知您");
  }
  /**
   * 处理邀请。
   *
   * @param user 用户
   * @return 处理邀请的结果
   */
  @Override @Transactional public String generateInvitation(CurrentUserContext user){requireFamilyAdmin(user);
    workflowMapper.disableCodes(user.familyId());String code=randomCode();workflowMapper.insertCode(user.familyId(),hash(code),user.userId());return code;}
  /**
   * 处理家庭。
   *
   * @param user 用户
   * @param request 请求参数
   */
  @Override @Transactional public void joinFamily(CurrentUserContext user,JoinFamilyRequest request){requireNoFamily(user.userId());
    Long codeId=workflowMapper.findCodeId(hash(request.code()));if(codeId==null)throw new BusinessException(ErrorCode.BUSINESS_INVALID,"邀请码无效或已过期");
    FamilyMembershipRequestDO e=new FamilyMembershipRequestDO();e.setRequestType("CODE_APPLICATION");e.setFamilyId(workflowMapper.findCodeFamily(codeId));
    e.setApplicantUserId(user.userId());e.setInvitationCodeId(codeId);e.setCreatedBy(user.userId());workflowMapper.insertRequest(e);notifyUser(user.userId(),"family","加入家庭申请已提交","家庭负责人处理后会在这里通知您");}
  /**
   * 处理Invite。
   *
   * @param user 用户
   * @param request 请求参数
   */
  @Override @Transactional public void directInvite(CurrentUserContext user,DirectInvitationRequest request){requireFamilyAdmin(user);
    var target=userMapper.findByLoginIdentifier(request.userIdentifier().trim().toLowerCase());
    if(target==null)throw new BusinessException(ErrorCode.NOT_FOUND,"目标用户不存在");requireNoFamily(target.getId());
    FamilyMembershipRequestDO e=new FamilyMembershipRequestDO();e.setRequestType("DIRECT_INVITATION");e.setFamilyId(user.familyId());
    e.setTargetUserId(target.getId());e.setCreatedBy(user.userId());workflowMapper.insertRequest(e);notifyUser(target.getId(),"family","家庭邀请","您收到一个家庭邀请，请进入家庭设置处理");}
  /**
   * 处理Invitations。
   *
   * @param user 用户
   * @return 处理Invitations的结果
   */
  @Override public List<FamilyMembershipRequestDO> myInvitations(CurrentUserContext user){return workflowMapper.selectUserInvitations(user.userId());}
  /**
   * 处理Applications。
   *
   * @param user 用户
   * @return 处理Applications的结果
   */
  @Override public List<FamilyMembershipRequestDO> pendingApplications(CurrentUserContext user){requireFamilyAdmin(user);return workflowMapper.selectFamilyApplications(user.familyId());}
  /**
   * 处理邀请。
   *
   * @param user 用户
   * @param id 标识
   */
  @Override @Transactional public void acceptInvitation(CurrentUserContext user,Long id){requireNoFamily(user.userId());
    FamilyMembershipRequestDO e=requirePending(id);if(!"DIRECT_INVITATION".equals(e.getRequestType())||!user.userId().equals(e.getTargetUserId()))
      throw new BusinessException(ErrorCode.FORBIDDEN,"无权处理该邀请");
    requireActiveFamily(e.getFamilyId());
    relationMapper.insertActive(user.userId(),e.getFamilyId(),"MEMBER","DIRECT_INVITATION",e.getId(),e.getCreatedBy());finish(e,user.userId(),"ACCEPTED",null);}
  /**
   * 拒绝邀请。
   *
   * @param user 用户
   * @param id 标识
   * @param reason 原因
   */
  @Override @Transactional public void rejectInvitation(CurrentUserContext user,Long id,String reason){FamilyMembershipRequestDO e=requirePending(id);
    if(!user.userId().equals(e.getTargetUserId()))throw new BusinessException(ErrorCode.FORBIDDEN,"无权处理该邀请");finish(e,user.userId(),"REJECTED",reason);}
  /**
   * 批准申请。
   *
   * @param user 用户
   * @param id 标识
   */
  @Override @Transactional public void approveApplication(CurrentUserContext user,Long id){requireFamilyAdmin(user);FamilyMembershipRequestDO e=requirePending(id);
    if(!user.familyId().equals(e.getFamilyId())||!"CODE_APPLICATION".equals(e.getRequestType()))throw new BusinessException(ErrorCode.FORBIDDEN,"无权审批该申请");
    requireActiveFamily(e.getFamilyId());requireNoFamily(e.getApplicantUserId());relationMapper.insertActive(e.getApplicantUserId(),e.getFamilyId(),"MEMBER","INVITATION_CODE",e.getId(),user.userId());finish(e,user.userId(),"ACCEPTED",null);}
  /**
   * 拒绝申请。
   *
   * @param user 用户
   * @param id 标识
   * @param reason 原因
   */
  @Override @Transactional public void rejectApplication(CurrentUserContext user,Long id,String reason){requireFamilyAdmin(user);FamilyMembershipRequestDO e=requirePending(id);
    if(!user.familyId().equals(e.getFamilyId()))throw new BusinessException(ErrorCode.FORBIDDEN,"无权审批该申请");finish(e,user.userId(),"REJECTED",reason);}
  /**
   * 处理家庭。
   *
   * @param user 用户
   * @param reason 原因
   */
  @Override @Transactional public void exitFamily(CurrentUserContext user,String reason){if(relationMapper.exit(user.userId(),user.userId(),reason)==0)
    throw new BusinessException(ErrorCode.BUSINESS_INVALID,"家庭负责人需先转让或解散家庭");}
  /**
   * 移交负责人。
   *
   * @param user 用户
   * @param target 目标
   */
  @Override @Transactional public void transferOwner(CurrentUserContext user,Long target){if(!"owner".equalsIgnoreCase(user.roleTemplate()))throw new BusinessException(ErrorCode.FORBIDDEN,"只有负责人可以转让家庭");
    String targetRole=workflowMapper.lockRole(target,user.familyId());String ownerRole=workflowMapper.lockRole(user.userId(),user.familyId());
    if(targetRole==null||!"OWNER".equalsIgnoreCase(ownerRole))throw new BusinessException(ErrorCode.BUSINESS_INVALID,"目标不是有效家庭成员");
    workflowMapper.updateRole(user.userId(),user.familyId(),"ADMIN");workflowMapper.updateRole(target,user.familyId(),"OWNER");}
  /**
   * 处理家庭。
   *
   * @param user 用户
   */
  @Override @Transactional public void dissolveFamily(CurrentUserContext user){if(!"owner".equalsIgnoreCase(user.roleTemplate()))throw new BusinessException(ErrorCode.FORBIDDEN,"只有负责人可以解散家庭");
    workflowMapper.invalidateRequests(user.familyId());workflowMapper.disableCodes(user.familyId());workflowMapper.dissolveRelations(user.familyId(),user.userId());workflowMapper.disableFamily(user.familyId());}
  private void requireFamilyAdmin(CurrentUserContext u){if(!u.hasFamilyAdminAccess())throw new BusinessException(ErrorCode.FORBIDDEN,"无家庭管理权限");}
  private void requireNoFamily(Long userId){if(relationMapper.findActiveFamilyId(userId)!=null)throw new BusinessException(ErrorCode.USER_ALREADY_IN_FAMILY,"用户已经加入家庭");}
  private void requireActiveFamily(Long familyId){if(workflowMapper.countActiveFamily(familyId)==0)
    throw new BusinessException(ErrorCode.BUSINESS_INVALID,"家庭不存在或已停用");}
  private FamilyMembershipRequestDO requirePending(Long id){FamilyMembershipRequestDO e=workflowMapper.lockRequest(id);if(e==null)throw new BusinessException(ErrorCode.NOT_FOUND,"邀请或申请不存在");
    if(!"PENDING".equals(e.getStatus()))throw new BusinessException(ErrorCode.INVITATION_STATE_CONFLICT,"邀请或申请已处理");return e;}
  private void finish(FamilyMembershipRequestDO e,Long userId,String status,String reason){if(workflowMapper.finish(e.getId(),status,userId,reason)==0)
    throw new BusinessException(ErrorCode.INVITATION_STATE_CONFLICT,"邀请或申请状态已变化");}
  private static String randomCode(){SecureRandom r=new SecureRandom();byte[] bytes=new byte[9];r.nextBytes(bytes);return HexFormat.of().formatHex(bytes).toUpperCase();}
  private void notifyUser(Long userId,String category,String title,String content){notificationMapper.insertNotification("user",userId,"user",category,title,content);}
  private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.trim().toUpperCase().getBytes(StandardCharsets.UTF_8)));}
    catch(Exception e){throw new IllegalStateException(e);}}
}
