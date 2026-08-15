package com.familykitchen.auth.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 负责Auth上下文相关数据的数据库访问与持久化更新。
 */
@Mapper
public interface AuthContextMapper {
  /**
   * 负责家庭上下文相关数据的数据库访问与持久化更新。
   *
   * @param familyId 家庭标识
   * @param merchantId 商户标识
   * @param familyRole 家庭角色
   */
  record FamilyContext(Long familyId, Long merchantId, String familyRole) {}
  /**
   * 负责商户上下文相关数据的数据库访问与持久化更新。
   *
   * @param merchantId 商户标识
   * @param merchantRole 商户角色
   */
  record MerchantContext(Long merchantId, String merchantRole) {}

  /**
   * 查询Active家庭。
   *
   * @param userId 用户标识
   * @return 查询Active家庭后的结果
   */
  @Select("""
      select r.family_id, f.merchant_id, r.family_role
      from family_user_relations r join families f on f.id = r.family_id
      where r.user_id = #{userId} and r.status = 'ACTIVE' and f.status = 'active' limit 1
      """)
  FamilyContext findActiveFamily(@Param("userId") Long userId);

  /**
   * 查询Active商户。
   *
   * @param userId 用户标识
   * @return 查询Active商户后的结果
   */
  @Select("""
      select merchant_id, merchant_role from merchant_user_relations
      where user_id = #{userId} and status = 'ACTIVE' order by id limit 1
      """)
  MerchantContext findActiveMerchant(@Param("userId") Long userId);

  /**
   * 查询PlatformRoles。
   *
   * @param userId 用户标识
   * @return 查询PlatformRoles后的结果
   */
  @Select("select role_code from user_role_relations where user_id = #{userId} and status = 'ACTIVE'")
  List<String> findPlatformRoles(@Param("userId") Long userId);
}
