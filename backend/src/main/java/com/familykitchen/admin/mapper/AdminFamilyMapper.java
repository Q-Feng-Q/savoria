package com.familykitchen.admin.mapper;

import com.familykitchen.admin.model.dto.AdminFamilyMemberUpdateRequest;
import com.familykitchen.admin.model.dto.AdminFamilyUpdateRequest;
import com.familykitchen.admin.model.vo.AdminFamilyMemberView;
import com.familykitchen.admin.model.vo.AdminFamilyOptionView;
import com.familykitchen.family.model.entity.AddressEntity;
import com.familykitchen.family.model.vo.AddressView;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 负责平台管理家庭相关数据的数据库访问与持久化更新。
 */
@Mapper
public interface AdminFamilyMapper {

  /**
   * 查询家庭选项。
   *
   * @return 查询家庭选项后的结果
   */
  @Select("""
      select f.id family_id, f.name family_name, f.merchant_id, m.name merchant_name,
             f.status, (select count(*) from family_user_relations fr where fr.family_id=f.id and fr.status='ACTIVE') member_count
      from families f join merchants m on m.id = f.merchant_id
      order by case when f.status = 'active' then 0 else 1 end, m.name, f.name
      """)
  List<AdminFamilyOptionView> selectFamilyOptions();

  /**
   * 查询家庭选项。
   *
   * @param familyId 家庭标识
   * @return 查询家庭选项后的结果
   */
  @Select("""
      select f.id family_id, f.name family_name, f.merchant_id, m.name merchant_name,
             f.status, (select count(*) from family_user_relations fr where fr.family_id=f.id and fr.status='ACTIVE') member_count
      from families f join merchants m on m.id = f.merchant_id where f.id = #{familyId}
      """)
  AdminFamilyOptionView selectFamilyOption(@Param("familyId") Long familyId);

  /**
   * 查询家庭Note。
   *
   * @param familyId 家庭标识
   * @return 查询家庭Note后的结果
   */
  @Select("select note from families where id = #{familyId}")
  String selectFamilyNote(@Param("familyId") Long familyId);

  /**
   * 查询配送是否启用。
   *
   * @param familyId 家庭标识
   * @return 是否满足对应条件
   */
  @Select("select delivery_enabled from families where id = #{familyId}")
  Boolean selectDeliveryEnabled(@Param("familyId") Long familyId);

  /**
   * 查询配送费用。
   *
   * @param familyId 家庭标识
   * @return 查询配送费用后的结果
   */
  @Select("select delivery_fee_default from families where id = #{familyId}")
  java.math.BigDecimal selectDeliveryFee(@Param("familyId") Long familyId);

  /**
   * 查询配送Free。
   *
   * @param familyId 家庭标识
   * @return 是否满足对应条件
   */
  @Select("select delivery_fee_free from families where id = #{familyId}")
  Boolean selectDeliveryFree(@Param("familyId") Long familyId);

  /**
   * 查询Members。
   *
   * @param familyId 家庭标识
   * @return 查询Members后的结果
   */
  @Select("""
      select u.id member_id, u.username, u.nickname name, u.mobile, u.status activation_status, fr.family_role role_template,
             coalesce(w.balance_amount, 0) available_balance, coalesce(w.frozen_amount, 0) frozen_balance
      from family_user_relations fr join users u on u.id=fr.user_id left join member_wallets w on w.user_id=u.id
      where fr.family_id=#{familyId} and fr.status='ACTIVE' order by u.status='FROZEN',u.nickname
      """)
  List<AdminFamilyMemberView> selectMembers(@Param("familyId") Long familyId);

  /**
   * 查询Addresses。
   *
   * @param familyId 家庭标识
   * @return 查询Addresses后的结果
   */
  @Select("""
      select id address_id, contact_name, contact_phone, address_text, is_default default_address
      from family_addresses where family_id = #{familyId} order by is_default desc, id
      """)
  List<AddressView> selectAddresses(@Param("familyId") Long familyId);

  /**
   * 更新家庭。
   *
   * @param familyId 家庭标识
   * @param body 请求体
   * @return 更新家庭后的结果
   */
  @Update("""
      update families set name = #{body.familyName}, note = #{body.note}, status = #{body.status},
        delivery_enabled = #{body.deliveryEnabled}, delivery_fee_default = #{body.deliveryFeeDefault},
        delivery_fee_free = #{body.deliveryFree} where id = #{familyId}
      """)
  int updateFamily(@Param("familyId") Long familyId, @Param("body") AdminFamilyUpdateRequest body);

  /**
   * 停用家庭。
   *
   * @param familyId 家庭标识
   * @return 停用家庭后的结果
   */
  @Update("update families set status = 'inactive' where id = #{familyId}")
  int disableFamily(@Param("familyId") Long familyId);

  /**
   * 更新成员。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param body 请求体
   * @return 更新成员后的结果
   */
  @Update("""
      update users u join family_user_relations fr on fr.user_id=u.id and fr.family_id=#{familyId} and fr.status='ACTIVE'
      set u.nickname=#{body.name},u.mobile=#{body.mobile},u.status=#{body.activationStatus},fr.family_role=#{body.roleTemplate}
      where u.id=#{memberId}
      """)
  int updateMember(@Param("familyId") Long familyId, @Param("memberId") Long memberId,
                   @Param("body") AdminFamilyMemberUpdateRequest body);

  /**
   * 停用成员。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @return 停用成员后的结果
   */
  @Update("update family_user_relations set status='REMOVED',ended_at=now() where user_id=#{memberId} and family_id=#{familyId} and status='ACTIVE' and family_role<>'OWNER'")
  int disableMember(@Param("familyId") Long familyId, @Param("memberId") Long memberId);

  /**
   * 统计成员。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @return 统计成员后的结果
   */
  @Select("select count(*) from family_user_relations where user_id=#{memberId} and family_id=#{familyId} and status='ACTIVE'")
  int countMember(@Param("familyId") Long familyId, @Param("memberId") Long memberId);

  /**
   * 新增地址。
   *
   * @param address 地址
   * @return 新增地址后的结果
   */
  @Insert("""
      insert into family_addresses(family_id, contact_name, contact_phone, address_text, is_default)
      values(#{familyId}, #{contactName}, #{contactPhone}, #{addressText}, #{defaultAddress})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertAddress(AddressEntity address);

  /**
   * 更新地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @param address 地址
   * @return 更新地址后的结果
   */
  @Update("""
      update family_addresses set contact_name = #{address.contactName}, contact_phone = #{address.contactPhone},
        address_text = #{address.addressText} where id = #{addressId} and family_id = #{familyId}
      """)
  int updateAddress(@Param("familyId") Long familyId, @Param("addressId") Long addressId,
                    @Param("address") AddressEntity address);

  /**
   * 删除地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @return 删除地址后的结果
   */
  @Delete("delete from family_addresses where id = #{addressId} and family_id = #{familyId}")
  int deleteAddress(@Param("familyId") Long familyId, @Param("addressId") Long addressId);

  /**
   * 处理Default地址。
   *
   * @param familyId 家庭标识
   */
  @Update("update family_addresses set is_default = 0 where family_id = #{familyId}")
  void clearDefaultAddress(@Param("familyId") Long familyId);

  /**
   * 设置Default地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @return 设置Default地址后的结果
   */
  @Update("update family_addresses set is_default = 1 where id = #{addressId} and family_id = #{familyId}")
  int setDefaultAddress(@Param("familyId") Long familyId, @Param("addressId") Long addressId);
}
