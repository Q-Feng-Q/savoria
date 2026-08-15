package com.familykitchen.user.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户平台角色 Mapper。 */
@Mapper
public interface UserRoleMapper {
  /**
   * 新增用户角色。
   *
   * @param userId 用户标识
   * @param roleCode 角色编码
   * @return 新增结果后的结果
   */
  int insert(@Param("userId") Long userId, @Param("roleCode") String roleCode);
  /**
   * 查询ActiveRoles。
   *
   * @param userId 用户标识
   * @return 查询ActiveRoles后的结果
   */
  List<String> findActiveRoles(@Param("userId") Long userId);
  /**
   * 统计角色。
   *
   * @param userId 用户标识
   * @param roleCode 角色编码
   * @return 统计角色后的结果
   */
  int countRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
