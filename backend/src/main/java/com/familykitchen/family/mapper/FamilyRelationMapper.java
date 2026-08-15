package com.familykitchen.family.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 家庭用户关系 Mapper。 */
@Mapper
public interface FamilyRelationMapper {
  /**
   * 查询Active家庭标识。
   *
   * @param userId 用户标识
   * @return 查询Active家庭标识的结果
   */
  Long findActiveFamilyId(@Param("userId") Long userId);
  /**
   * 新增Active。
   *
   * @param userId 用户标识
   * @param familyId 家庭标识
   * @param familyRole 家庭角色
   * @param joinSource join来源
   * @param invitationId 邀请标识
   * @param createdBy 创建By
   * @return 新增Active的结果
   */
  int insertActive(@Param("userId") Long userId, @Param("familyId") Long familyId,
                   @Param("familyRole") String familyRole, @Param("joinSource") String joinSource,
                   @Param("invitationId") Long invitationId, @Param("createdBy") Long createdBy);
  /**
   * 处理家庭Relation。
   *
   * @param userId 用户标识
   * @param endedBy endedBy
   * @param reason 原因
   * @return 处理的结果
   */
  int exit(@Param("userId") Long userId, @Param("endedBy") Long endedBy,
           @Param("reason") String reason);
}
