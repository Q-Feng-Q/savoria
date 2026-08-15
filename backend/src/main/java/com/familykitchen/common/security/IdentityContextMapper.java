package com.familykitchen.common.security;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 当前用户实时身份关系查询 Mapper。 */
@Mapper
public interface IdentityContextMapper {
  /**
   * 查询账号当前关联的家庭和商户身份。
   * @param userId 账号标识
   * @return 实时身份摘要，不存在时为空
   */
  IdentityContextRow findIdentity(@Param("userId") Long userId);
  /**
   * 查询账号的平台角色。
   * @param userId 账号标识
   * @return 平台角色编码列表
   */
  List<String> findPlatformRoles(@Param("userId") Long userId);
  /**
   * 查询账号在指定商户下的权限范围。
   * @param userId 账号标识
   * @param merchantId 商户标识
   * @return 商户权限编码列表
   */
  List<String> findMerchantScopes(@Param("userId") Long userId, @Param("merchantId") Long merchantId);
}
