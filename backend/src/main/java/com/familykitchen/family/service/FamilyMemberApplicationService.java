package com.familykitchen.family.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.model.dto.FamilyApplyRequest;
import com.familykitchen.family.model.dto.JoinFamilyRequest;
import com.familykitchen.family.model.dto.DirectInvitationRequest;
import com.familykitchen.family.model.entity.FamilyMembershipRequestDO;
import java.util.List;
import com.familykitchen.family.model.vo.FamilyOnboardingView;

/**
 * 家庭成员管理服务。
 */
public interface FamilyMemberApplicationService {
  /**
   * 处理家庭成员。
   *
   * @param user 用户
   * @return 处理的结果
   */
  FamilyOnboardingView onboarding(CurrentUserContext user);

  /**
   * 申请创建家庭。
   
   * @param user 用户
   * @param request 请求参数
   */
  void applyFamily(CurrentUserContext user, FamilyApplyRequest request);

  /**
   * 生成邀请码。
   
   * @param user 用户
   * @return 处理邀请的结果
   */
  String generateInvitation(CurrentUserContext user);

  /**
   * 通过邀请码加入家庭。
   
   * @param user 用户
   * @param request 请求参数
   */
  void joinFamily(CurrentUserContext user, JoinFamilyRequest request);

  /** 退出当前家庭。 
   * @param user 用户
   * @param reason 原因
   */
  void exitFamily(CurrentUserContext user, String reason);

  /**
   * 处理Invite。
   *
   * @param user 用户
   * @param request 请求参数
   */
  void directInvite(CurrentUserContext user, DirectInvitationRequest request);
  /**
   * 处理Invitations。
   *
   * @param user 用户
   * @return 处理Invitations的结果
   */
  List<FamilyMembershipRequestDO> myInvitations(CurrentUserContext user);
  /**
   * 处理Applications。
   *
   * @param user 用户
   * @return 处理Applications的结果
   */
  List<FamilyMembershipRequestDO> pendingApplications(CurrentUserContext user);
  /**
   * 处理邀请。
   *
   * @param user 用户
   * @param requestId 请求参数标识
   */
  void acceptInvitation(CurrentUserContext user, Long requestId);
  /**
   * 拒绝邀请。
   *
   * @param user 用户
   * @param requestId 请求参数标识
   * @param reason 原因
   */
  void rejectInvitation(CurrentUserContext user, Long requestId, String reason);
  /**
   * 批准申请。
   *
   * @param user 用户
   * @param requestId 请求参数标识
   */
  void approveApplication(CurrentUserContext user, Long requestId);
  /**
   * 拒绝申请。
   *
   * @param user 用户
   * @param requestId 请求参数标识
   * @param reason 原因
   */
  void rejectApplication(CurrentUserContext user, Long requestId, String reason);
  /**
   * 移交负责人。
   *
   * @param user 用户
   * @param targetUserId 目标用户标识
   */
  void transferOwner(CurrentUserContext user, Long targetUserId);
  /**
   * 处理家庭。
   *
   * @param user 用户
   */
  void dissolveFamily(CurrentUserContext user);
}
