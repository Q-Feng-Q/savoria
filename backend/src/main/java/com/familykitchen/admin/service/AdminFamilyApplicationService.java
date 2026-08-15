package com.familykitchen.admin.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.model.entity.FamilyApplicationDO;

import java.util.List;

/**
 * 后台家庭申请管理服务。
 */
public interface AdminFamilyApplicationService {

  /**
   * 查询家庭申请列表。
   
   * @param status 状态
   * @return 列出Applications后的结果
   */
  List<FamilyApplicationDO> listApplications(String status);

  /**
   * 审批通过。
   
   * @param user 用户
   * @param applicationId 申请标识
   * @param remark 备注
   */
  void approve(CurrentUserContext user, Long applicationId, String remark);

  /**
   * 审批拒绝。
   
   * @param user 用户
   * @param applicationId 申请标识
   * @param remark 备注
   */
  void reject(CurrentUserContext user, Long applicationId, String remark);
}
