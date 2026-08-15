package com.familykitchen.admin.mapper;

import com.familykitchen.admin.model.vo.AdminUserView;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 负责平台管理用户相关数据的数据库访问与持久化更新。
 */
@Mapper
public interface AdminUserMapper {
  /**
   * 列出平台管理用户。
   *
   * @param keyword keyword
   * @return 列出结果后的结果
   */
  @Select("""
      SELECT u.id user_id,u.username,u.nickname,u.mobile,u.email,u.status,
        COALESCE((SELECT GROUP_CONCAT(urr.role_code) FROM user_role_relations urr WHERE urr.user_id=u.id AND urr.status='ACTIVE'),'') platform_roles,
        (SELECT f.name FROM family_user_relations fur JOIN families f ON f.id=fur.family_id WHERE fur.user_id=u.id AND fur.status='ACTIVE' LIMIT 1) family_name,
        (SELECT m.name FROM merchant_user_relations mur JOIN merchants m ON m.id=mur.merchant_id WHERE mur.user_id=u.id AND mur.status='ACTIVE' LIMIT 1) merchant_name,
        u.last_login_at,u.created_at
      FROM users u
      WHERE u.status<>'CANCELLED'
        AND (#{keyword} IS NULL OR #{keyword}='' OR u.username LIKE CONCAT('%',#{keyword},'%') OR u.nickname LIKE CONCAT('%',#{keyword},'%') OR u.mobile LIKE CONCAT('%',#{keyword},'%'))
      ORDER BY u.id DESC LIMIT 500
      """)
  List<AdminUserView> list(@Param("keyword") String keyword);

  /**
   * 更新状态。
   *
   * @param userId 用户标识
   * @param status 状态
   * @return 更新状态后的结果
   */
  @Update("UPDATE users SET status=#{status} WHERE id=#{userId}")
  int updateStatus(@Param("userId") Long userId, @Param("status") String status);

  /**
   * 停用Platform角色。
   *
   * @param userId 用户标识
   * @return 停用Platform角色后的结果
   */
  @Update("UPDATE user_role_relations SET status='INACTIVE' WHERE user_id=#{userId} AND role_code IN ('platform_admin','PLATFORM_ADMIN')")
  int disablePlatformRole(Long userId);

  /**
   * 启用Platform角色。
   *
   * @param userId 用户标识
   * @return 启用Platform角色后的结果
   */
  @Insert("INSERT INTO user_role_relations(user_id,role_code,status) VALUES(#{userId},'PLATFORM_ADMIN','ACTIVE') ON DUPLICATE KEY UPDATE status='ACTIVE'")
  int enablePlatformRole(Long userId);

  /**
   * 处理用户。
   *
   * @param userId 用户标识
   * @return 处理用户后的结果
   */
  @Update("""
      UPDATE users SET
        username=CONCAT('deleted_',id,'_',REPLACE(UUID(),'-','')),
        nickname='已删除用户',mobile=NULL,email=NULL,email_verified=0,wechat_open_id=NULL,avatar_url=NULL,
        password_hash=CONCAT('deleted:',REPLACE(UUID(),'-','')),password_algorithm='DISABLED',
        credential_status='DISABLED',status='CANCELLED'
      WHERE id=#{userId} AND status<>'CANCELLED'
      """)
  int anonymizeUser(Long userId);

  /**
   * 停用PlatformRoles。
   *
   * @param userId 用户标识
   * @return 停用PlatformRoles后的结果
   */
  @Update("UPDATE user_role_relations SET status='INACTIVE' WHERE user_id=#{userId} AND status='ACTIVE'")
  int disablePlatformRoles(Long userId);

  /**
   * 停用家庭Relations。
   *
   * @param userId 用户标识
   * @return 停用家庭Relations后的结果
   */
  @Update("UPDATE family_user_relations SET status='REMOVED',ended_at=NOW(),end_reason='平台管理员删除用户' WHERE user_id=#{userId} AND status='ACTIVE'")
  int disableFamilyRelations(Long userId);

  /**
   * 停用商户Relations。
   *
   * @param userId 用户标识
   * @return 停用商户Relations后的结果
   */
  @Update("UPDATE merchant_user_relations SET status='INACTIVE' WHERE user_id=#{userId} AND status='ACTIVE'")
  int disableMerchantRelations(Long userId);
}
