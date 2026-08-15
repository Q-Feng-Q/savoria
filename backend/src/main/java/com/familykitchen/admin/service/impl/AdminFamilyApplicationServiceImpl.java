package com.familykitchen.admin.service.impl;

import com.familykitchen.admin.service.AdminFamilyApplicationService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyApplicationMapper;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import com.familykitchen.family.mapper.FamilyWorkflowMapper;
import com.familykitchen.family.model.entity.FamilyApplicationDO;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.merchant.service.MerchantDefaultDataInitializer;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 平台家庭申请管理服务，所有列表均查询真实数据库。 */
@Service
public class AdminFamilyApplicationServiceImpl implements AdminFamilyApplicationService {
  private final FamilyApplicationMapper applicationMapper;
  private final FamilyRelationMapper relationMapper;
  private final FamilyWorkflowMapper workflowMapper;
  private final NotificationPersistenceMapper notificationMapper;
  private final MerchantDefaultDataInitializer merchantDefaults;

  /**
   * 创建平台管理家庭实例。
   *
   * @param applicationMapper 申请Mapper
   * @param relationMapper relationMapper
   * @param workflowMapper workflowMapper
   * @param notificationMapper 通知Mapper
   * @param merchantDefaults 商户默认目录初始化器
   */
  public AdminFamilyApplicationServiceImpl(FamilyApplicationMapper applicationMapper,
      FamilyRelationMapper relationMapper,FamilyWorkflowMapper workflowMapper,
      NotificationPersistenceMapper notificationMapper,
      MerchantDefaultDataInitializer merchantDefaults) {
    this.applicationMapper=applicationMapper;
    this.relationMapper=relationMapper;this.workflowMapper=workflowMapper;this.notificationMapper=notificationMapper;
    this.merchantDefaults=merchantDefaults;
  }

  /**
   * 列出Applications。
   *
   * @param status 状态
   * @return 列出Applications后的结果
   */
  @Override
  public List<FamilyApplicationDO> listApplications(String status) {
    return applicationMapper.listByStatus(status);
  }

  /**
   * 批准平台管理家庭。
   *
   * @param user 用户
   * @param applicationId 申请标识
   * @param remark 备注
   */
  @Override @Transactional
  public void approve(CurrentUserContext user, Long applicationId, String remark) {
    FamilyApplicationDO application=requirePending(applicationId);
    String merchantMode=application.getMerchantMode()==null?"":application.getMerchantMode().trim();
    boolean createMerchant="JOINT_CREATE".equalsIgnoreCase(merchantMode)||"CREATE".equalsIgnoreCase(merchantMode);
    if(createMerchant){
      if(application.getProposedMerchantName()==null||application.getProposedMerchantName().isBlank())throw new BusinessException(ErrorCode.BAD_REQUEST,"新私厨名称不能为空");
      if(workflowMapper.insertMerchant(application)!=1||application.getMerchantId()==null)throw new BusinessException(ErrorCode.SYSTEM_ERROR,"新私厨创建失败");
      merchantDefaults.initialize(application.getMerchantId());
      if(workflowMapper.insertMerchantOwner(application.getApplicantMemberId(),application.getMerchantId())!=1)
        throw new BusinessException(ErrorCode.SYSTEM_ERROR,"私厨负责人关系创建失败");
      applicationMapper.attachMerchant(applicationId,application.getMerchantId());
    }
    if(application.getMerchantId()==null||workflowMapper.countActiveMerchant(application.getMerchantId())==0)
      throw new BusinessException(ErrorCode.BUSINESS_INVALID,"申请关联的商户不存在或未启用");
    if(relationMapper.findActiveFamilyId(application.getApplicantMemberId())!=null)
      throw new BusinessException(ErrorCode.USER_ALREADY_IN_FAMILY,"申请人已经加入家庭");
    FamilyRecord family=new FamilyRecord();family.setFamilyName(application.getFamilyName());family.setMerchantId(application.getMerchantId());
    if(workflowMapper.insertFamily(family)!=1||family.getFamilyId()==null)
      throw new BusinessException(ErrorCode.SYSTEM_ERROR,"家庭创建失败");
    if(relationMapper.insertActive(application.getApplicantMemberId(),family.getFamilyId(),"OWNER","CREATOR",null,application.getApplicantMemberId())!=1)
      throw new BusinessException(ErrorCode.SYSTEM_ERROR,"家庭负责人关系创建失败");
    if(applicationMapper.updateStatus(applicationId,"approved",user.userId(),remark)==0)
      throw new BusinessException(ErrorCode.STATE_CONFLICT,"家庭申请状态已变化");
    notificationMapper.insertNotification("user",application.getApplicantMemberId(),"user","family","家庭创建申请已通过","家庭已创建，可以进入个人中心开始使用");
  }

  /**
   * 拒绝平台管理家庭。
   *
   * @param user 用户
   * @param applicationId 申请标识
   * @param remark 备注
   */
  @Override @Transactional
  public void reject(CurrentUserContext user, Long applicationId, String remark) {
    FamilyApplicationDO application=requirePending(applicationId);
    if(applicationMapper.updateStatus(applicationId,"rejected",user.userId(),remark)==0)
      throw new BusinessException(ErrorCode.STATE_CONFLICT,"家庭申请状态已变化");
    notificationMapper.insertNotification("user",application.getApplicantMemberId(),"user","family","家庭创建申请未通过",remark==null?"请完善资料后重新申请":remark);
  }

  private FamilyApplicationDO requirePending(Long id) {
    FamilyApplicationDO application=applicationMapper.findById(id);
    if (application==null) throw new BusinessException(ErrorCode.NOT_FOUND,"家庭申请不存在");
    if (!"pending".equals(application.getStatus())) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT,"家庭申请状态不允许操作");
    }
    return application;
  }
}
