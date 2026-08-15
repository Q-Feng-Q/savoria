package com.familykitchen.family.mapper;

import com.familykitchen.family.model.entity.FamilyInvitationDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 家庭邀请码 Mapper。
 */
@Mapper
public interface FamilyInvitationMapper {

  /**
   * 查询By编码。
   *
   * @param code 编码
   * @return 查询By编码的结果
   */
  FamilyInvitationDO findByCode(@Param("code") String code);

  /**
   * 新增家庭邀请。
   *
   * @param invitation 邀请
   * @return 新增的结果
   */
  int insert(FamilyInvitationDO invitation);

  /**
   * 更新状态。
   *
   * @param id 标识
   * @param status 状态
   * @param usedByMemberId usedBy成员标识
   * @param usedAt used时间
   * @return 更新状态的结果
   */
  int updateStatus(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("usedByMemberId") Long usedByMemberId,
      @Param("usedAt") LocalDateTime usedAt
  );
}
