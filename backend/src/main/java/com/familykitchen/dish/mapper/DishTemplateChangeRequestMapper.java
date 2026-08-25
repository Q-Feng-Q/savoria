package com.familykitchen.dish.mapper;

import com.familykitchen.dish.model.entity.DishTemplateChangeRequestDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 模板菜品修改审核申请的 MyBatis 持久化接口。 */
@Mapper
public interface DishTemplateChangeRequestMapper {
  /**
   * 新增待审核申请。
   * @param entity 申请实体
   * @return 新增行数
   */
  int insert(DishTemplateChangeRequestDO entity);
  /**
   * 统计商户模板的待审核申请。
   * @param merchantId 商户 ID
   * @param templateId 模板 ID
   * @return 待审核数量
   */
  int countPending(@Param("merchantId") Long merchantId, @Param("templateId") Long templateId);
  /**
   * 锁定申请。
   * @param requestId 申请 ID
   * @return 被锁定申请
   */
  DishTemplateChangeRequestDO selectForUpdate(@Param("requestId") Long requestId);
  /**
   * 按商户范围锁定申请。
   * @param requestId 申请 ID
   * @param merchantId 商户 ID
   * @return 被锁定申请
   */
  DishTemplateChangeRequestDO selectMerchantForUpdate(@Param("requestId") Long requestId,
      @Param("merchantId") Long merchantId);
  /**
   * 查询平台申请详情。
   * @param requestId 申请 ID
   * @return 申请详情
   */
  DishTemplateChangeRequestDO selectDetail(@Param("requestId") Long requestId);
  /**
   * 查询商户范围内申请详情。
   * @param requestId 申请 ID
   * @param merchantId 商户 ID
   * @return 申请详情
   */
  DishTemplateChangeRequestDO selectMerchantDetail(@Param("requestId") Long requestId,
      @Param("merchantId") Long merchantId);
  /**
   * 统计商户申请。
   * @param merchantId 商户 ID
   * @param status 状态
   * @param keyword 菜名关键词
   * @return 匹配数量
   */
  long countMerchant(@Param("merchantId") Long merchantId, @Param("status") String status,
      @Param("keyword") String keyword);
  /**
   * 查询商户申请分页。
   * @param merchantId 商户 ID
   * @param status 状态
   * @param keyword 菜名关键词
   * @param offset 偏移量
   * @param pageSize 每页数量
   * @return 申请列表
   */
  List<DishTemplateChangeRequestDO> selectMerchantPage(@Param("merchantId") Long merchantId,
      @Param("status") String status, @Param("keyword") String keyword, @Param("offset") int offset,
      @Param("pageSize") int pageSize);
  /**
   * 统计平台申请。
   * @param status 状态
   * @param merchantId 商户 ID
   * @param templateId 模板 ID
   * @param keyword 菜名关键词
   * @return 匹配数量
   */
  long countAdmin(@Param("status") String status, @Param("merchantId") Long merchantId,
      @Param("templateId") Long templateId, @Param("keyword") String keyword);
  /**
   * 查询平台申请分页。
   * @param status 状态
   * @param merchantId 商户 ID
   * @param templateId 模板 ID
   * @param keyword 菜名关键词
   * @param offset 偏移量
   * @param pageSize 每页数量
   * @return 申请列表
   */
  List<DishTemplateChangeRequestDO> selectAdminPage(@Param("status") String status,
      @Param("merchantId") Long merchantId, @Param("templateId") Long templateId,
      @Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);
  /**
   * 标记申请已撤回。
   * @param requestId 申请 ID
   * @param withdrawnBy 撤回用户 ID
   * @return 更新行数
   */
  int markWithdrawn(@Param("requestId") Long requestId, @Param("withdrawnBy") Long withdrawnBy);
  /**
   * 标记申请已通过。
   * @param requestId 申请 ID
   * @param reviewedBy 审核用户 ID
   * @param reason 审核意见
   * @return 更新行数
   */
  int markApproved(@Param("requestId") Long requestId, @Param("reviewedBy") Long reviewedBy,
      @Param("reason") String reason);
  /**
   * 标记申请已驳回。
   * @param requestId 申请 ID
   * @param reviewedBy 审核用户 ID
   * @param reason 驳回原因
   * @return 更新行数
   */
  int markRejected(@Param("requestId") Long requestId, @Param("reviewedBy") Long reviewedBy,
      @Param("reason") String reason);
  /**
   * 回写结果通知 ID。
   * @param requestId 申请 ID
   * @param notificationId 通知 ID
   * @return 更新行数
   */
  int setResultNotificationId(@Param("requestId") Long requestId,
      @Param("notificationId") Long notificationId);
}
