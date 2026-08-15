package com.familykitchen.auth.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/** 用户登录会话 Mapper。 */
@Mapper public interface UserSessionMapper {
  /**
   * 新增用户会话。
   *
   * @param id 标识
   * @param userId 用户标识
   * @param tokenHash 令牌Hash
   * @return 新增结果后的结果
   */
  int insert(@Param("id")String id,@Param("userId")Long userId,@Param("tokenHash")String tokenHash);
  /**
   * 统计Active。
   *
   * @param id 标识
   * @param userId 用户标识
   * @return 统计Active后的结果
   */
  int countActive(@Param("id")String id,@Param("userId")Long userId);
  /**
   * 撤销用户会话。
   *
   * @param id 标识
   * @param userId 用户标识
   * @return 撤销结果后的结果
   */
  int revoke(@Param("id")String id,@Param("userId")Long userId);
  /**
   * 撤销All。
   *
   * @param userId 用户标识
   * @return 撤销All后的结果
   */
  int revokeAll(@Param("userId")Long userId);
}
