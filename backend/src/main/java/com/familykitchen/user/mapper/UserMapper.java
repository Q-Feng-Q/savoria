package com.familykitchen.user.mapper;

import com.familykitchen.user.model.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 统一用户数据库访问接口。
 */
@Mapper
public interface UserMapper {
  /**
   * 查询By标识。
   *
   * @param id 标识
   * @return 查询By标识后的结果
   */
  UserDO findById(@Param("id") Long id);
  /**
   * 查询ByLoginIdentifier。
   *
   * @param identifier identifier
   * @return 查询ByLoginIdentifier后的结果
   */
  UserDO findByLoginIdentifier(@Param("identifier") String identifier);
  /**
   * 查询By用户名。
   *
   * @param username 用户名
   * @return 查询By用户名后的结果
   */
  UserDO findByUsername(@Param("username") String username);
  /**
   * 查询By邮箱。
   *
   * @param email 邮箱
   * @return 查询By邮箱后的结果
   */
  UserDO findByEmail(@Param("email") String email);
  /**
   * 查询By微信开放标识标识。
   *
   * @param openId 开放标识标识
   * @return 查询By微信开放标识标识后的结果
   */
  UserDO findByWechatOpenId(@Param("openId") String openId);
  /**
   * 新增用户。
   *
   * @param user 用户
   * @return 新增结果后的结果
   */
  int insert(UserDO user);
  /**
   * 更新LastLogin。
   *
   * @param id 标识
   * @return 更新LastLogin后的结果
   */
  int updateLastLogin(@Param("id") Long id);
  /**
   * 更新密码。
   *
   * @param id 标识
   * @param passwordHash 密码Hash
   * @param passwordAlgorithm 密码Algorithm
   * @return 更新密码后的结果
   */
  int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash,
                     @Param("passwordAlgorithm") String passwordAlgorithm);
  /**
   * 更新资料。
   *
   * @param id 标识
   * @param nickname nickname
   * @param avatarUrl avatarUrl
   * @param mobile 手机号
   * @return 更新资料后的结果
   */
  int updateProfile(@Param("id") Long id, @Param("nickname") String nickname,
                    @Param("avatarUrl") String avatarUrl, @Param("mobile") String mobile);
  /**
   * 绑定邮箱。
   *
   * @param id 标识
   * @param email 邮箱
   * @return 绑定邮箱后的结果
   */
  int bindEmail(@Param("id") Long id, @Param("email") String email);
  /**
   * 绑定微信。
   *
   * @param id 标识
   * @param openId 开放标识标识
   * @return 绑定微信后的结果
   */
  int bindWechat(@Param("id") Long id, @Param("openId") String openId);
  /**
   * 解绑微信。
   *
   * @param id 标识
   * @return 解绑微信后的结果
   */
  int unbindWechat(@Param("id") Long id);
  /**
   * 更新状态。
   *
   * @param id 标识
   * @param status 状态
   * @return 更新状态后的结果
   */
  int updateStatus(@Param("id") Long id, @Param("status") String status);
  /**
   * 更新用户名Once。
   *
   * @param id 标识
   * @param username 用户名
   * @return 更新用户名Once后的结果
   */
  int updateUsernameOnce(@Param("id")Long id,@Param("username")String username);
  /**
   * 处理用户。
   *
   * @param id 标识
   * @param username 用户名
   * @return 处理结果
   */
  int anonymize(@Param("id")Long id,@Param("username")String username);
}
