package com.familykitchen.user.mapper;
import java.util.List;
import org.apache.ibatis.annotations.*;
/** 账号注销前置检查和冷静期 Mapper。 */
@Mapper public interface AccountLifecycleMapper {
  /**
   * 统计Active家庭。
   *
   * @param userId 用户标识
   * @return 统计Active家庭后的结果
   */
  @Select("SELECT COUNT(*) FROM family_user_relations WHERE user_id=#{userId} AND status='ACTIVE'") int countActiveFamily(Long userId);
  /**
   * 统计PendingOrders。
   *
   * @param userId 用户标识
   * @return 统计PendingOrders后的结果
   */
  @Select("SELECT COUNT(*) FROM orders WHERE submitter_user_id=#{userId} AND status NOT IN ('CANCELLED','COMPLETED','REJECTED')") int countPendingOrders(Long userId);
  /**
   * 统计Frozen钱包。
   *
   * @param userId 用户标识
   * @return 统计Frozen钱包后的结果
   */
  @Select("SELECT COUNT(*) FROM member_wallets WHERE user_id=#{userId} AND frozen_amount>0") int countFrozenWallet(Long userId);
  /**
   * 统计Pending请求参数。
   *
   * @param userId 用户标识
   * @return 统计Pending请求参数后的结果
   */
  @Select("SELECT COUNT(*) FROM account_cancellation_requests WHERE user_id=#{userId} AND status='PENDING'") int countPendingRequest(Long userId);
  /**
   * 新增请求参数。
   *
   * @param userId 用户标识
   * @return 新增请求参数后的结果
   */
  @Insert("INSERT INTO account_cancellation_requests(user_id,status,cooling_end_at) VALUES(#{userId},'PENDING',DATE_ADD(NOW(),INTERVAL 7 DAY))") int insertRequest(Long userId);
  /**
   * 取消请求参数。
   *
   * @param userId 用户标识
   * @return 取消请求参数后的结果
   */
  @Update("UPDATE account_cancellation_requests SET status='CANCELLED',cancelled_at=NOW() WHERE user_id=#{userId} AND status='PENDING'") int cancelRequest(Long userId);
  /**
   * 查询DueUsers。
   *
   * @return 查询DueUsers后的结果
   */
  @Select("SELECT user_id FROM account_cancellation_requests WHERE status='PENDING' AND cooling_end_at<=NOW() ORDER BY id LIMIT 100") List<Long> selectDueUsers();
  /**
   * 完成AccountLifecycle。
   *
   * @param userId 用户标识
   * @return 完成结果后的结果
   */
  @Update("UPDATE account_cancellation_requests SET status='COMPLETED',completed_at=NOW() WHERE user_id=#{userId} AND status='PENDING'") int complete(Long userId);
}
