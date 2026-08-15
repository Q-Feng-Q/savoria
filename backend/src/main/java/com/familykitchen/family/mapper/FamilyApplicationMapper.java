package com.familykitchen.family.mapper;

import com.familykitchen.family.model.entity.FamilyApplicationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 家庭申请 Mapper。
 */
@Mapper
public interface FamilyApplicationMapper {

  /**
   * 查询By标识。
   *
   * @param id 标识
   * @return 查询By标识的结果
   */
  FamilyApplicationDO findById(@Param("id") Long id);

  /**
   * 列出By状态。
   *
   * @param status 状态
   * @return 列出By状态的结果
   */
  List<FamilyApplicationDO> listByStatus(@Param("status") String status);

  /**
   * 查询ByApplicantAnd状态。
   *
   * @param applicantMemberId applicant成员标识
   * @param status 状态
   * @return 查询ByApplicantAnd状态的结果
   */
  FamilyApplicationDO findByApplicantAndStatus(
      @Param("applicantMemberId") Long applicantMemberId,
      @Param("status") String status
  );

  /**
   * 查询LatestByApplicant。
   *
   * @param applicantMemberId applicant成员标识
   * @return 查询LatestByApplicant的结果
   */
  FamilyApplicationDO findLatestByApplicant(@Param("applicantMemberId") Long applicantMemberId);

  /**
   * 新增家庭申请。
   *
   * @param application 申请
   * @return 新增的结果
   */
  int insert(FamilyApplicationDO application);
  /**
   * 处理商户。
   *
   * @param id 标识
   * @param merchantId 商户标识
   * @return 处理商户的结果
   */
  int attachMerchant(@Param("id") Long id,@Param("merchantId") Long merchantId);

  /**
   * 更新状态。
   *
   * @param id 标识
   * @param status 状态
   * @param reviewedBy reviewedBy
   * @param reviewRemark 审核备注
   * @return 更新状态的结果
   */
  int updateStatus(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("reviewedBy") Long reviewedBy,
      @Param("reviewRemark") String reviewRemark
  );
}
