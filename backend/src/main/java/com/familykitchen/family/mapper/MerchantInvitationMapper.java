package com.familykitchen.family.mapper;

import org.apache.ibatis.annotations.*;

/**
 * 负责商户邀请相关数据的数据库访问与持久化更新。
 */
@Mapper
public interface MerchantInvitationMapper {
  /**
   * 查询Valid标识。
   *
   * @param hash hash
   * @return 查询Valid标识的结果
   */
  @Select("SELECT id FROM merchant_invitation_codes WHERE code_hash=#{hash} AND status='ACTIVE' AND expires_at>NOW() LIMIT 1") Long findValidId(String hash);
  /**
   * 查询商户标识。
   *
   * @param id 标识
   * @return 查询商户标识的结果
   */
  @Select("SELECT merchant_id FROM merchant_invitation_codes WHERE id=#{id}") Long findMerchantId(Long id);
  /**
   * 停用Active。
   *
   * @param merchantId 商户标识
   * @return 停用Active的结果
   */
  @Update("UPDATE merchant_invitation_codes SET status='INACTIVE',disabled_at=NOW() WHERE merchant_id=#{merchantId} AND status='ACTIVE'") int disableActive(Long merchantId);
  /**
   * 新增商户邀请。
   *
   * @param merchantId 商户标识
   * @param hash hash
   * @param userId 用户标识
   * @return 新增的结果
   */
  @Insert("INSERT INTO merchant_invitation_codes(merchant_id,code_hash,status,expires_at,created_by) VALUES(#{merchantId},#{hash},'ACTIVE',DATE_ADD(NOW(),INTERVAL 7 DAY),#{userId})") int insert(@Param("merchantId")Long merchantId,@Param("hash")String hash,@Param("userId")Long userId);
}
