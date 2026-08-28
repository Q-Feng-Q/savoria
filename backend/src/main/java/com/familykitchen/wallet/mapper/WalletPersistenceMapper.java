package com.familykitchen.wallet.mapper;

import com.familykitchen.wallet.model.entity.WalletAccountDO;
import com.familykitchen.wallet.model.entity.WalletLedgerDO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 钱包持久化 Mapper。
 *
 * <p>仅暴露钱包业务实际使用的显式数据库操作。</p>
 */
@Mapper
public interface WalletPersistenceMapper {

  /** Counts active members whose retired personal wallet still contains money.
   * @param familyId family identifier
   * @return non-zero legacy wallet count
   */
  @Select("""
      SELECT COUNT(*) FROM family_user_relations fr
      JOIN member_wallets mw ON mw.user_id=fr.user_id
      WHERE fr.family_id=#{familyId} AND fr.status='ACTIVE'
        AND (mw.balance_amount<>0 OR mw.frozen_amount<>0)
      """)
  int countNonZeroPersonalWallets(@Param("familyId") Long familyId);

  /**
   * Counts active family membership belonging to the specified merchant.
   *
   * @param merchantId merchant identifier
   * @param memberId member user identifier
   * @return matching active relation count
   */
  @Select("""
      SELECT COUNT(*)
      FROM family_user_relations fr
      JOIN families f ON f.id = fr.family_id
      WHERE fr.user_id = #{memberId}
        AND fr.status = 'ACTIVE'
        AND f.merchant_id = #{merchantId}
      """)
  int countMerchantMember(@Param("merchantId") Long merchantId, @Param("memberId") Long memberId);

  /**
   * 新增钱包Account。
   *
   * @param entity 实体
   * @return 新增钱包Account的结果
   */
  @Insert("INSERT INTO member_wallets(user_id,balance_amount,frozen_amount) VALUES(#{memberId},#{balanceAmount},#{frozenAmount})")
  int insertWalletAccount(WalletAccountDO entity);

  /**
   * 查询WalletsBy成员Ids。
   *
   * @param memberIds 成员Ids
   * @return 查询WalletsBy成员Ids的结果
   */
  List<WalletAccountDO> selectWalletsByMemberIds(@Param("memberIds") Set<Long> memberIds);

  /**
   * 查询钱包By成员标识。
   *
   * @param memberId 成员标识
   * @return 查询钱包By成员标识的结果
   */
  WalletAccountDO selectWalletByMemberId(@Param("memberId") Long memberId);

  /**
   * 查询钱包Ledgers。
   *
   * @param memberId 成员标识
   * @return 查询钱包Ledgers的结果
   */
  List<WalletLedgerDO> selectWalletLedgers(@Param("memberId") Long memberId);
}

