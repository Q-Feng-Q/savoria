package com.familykitchen.family.mapper;

import com.familykitchen.family.model.entity.AddressEntity;
import com.familykitchen.family.model.entity.FamilyMemberRecord;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.model.entity.MealSlotRecord;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.family.model.vo.FamilyHomeResponse;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 家庭端与商户家庭管理 MyBatis Mapper。
 */
@Mapper
public interface FamilyMapper {

  /**
   * 查询家庭。
   *
   * @param merchantId 商户标识
   * @param familyId 家庭标识
   * @return 查询家庭的结果
   */
  FamilyRecord selectFamily(@Param("merchantId") Long merchantId, @Param("familyId") Long familyId);

  /**
   * 查询成员。
   *
   * @param memberId 成员标识
   * @return 查询成员的结果
   */
  FamilyMemberRecord selectMember(@Param("memberId") Long memberId);

  /**
   * 查询RecentOrders。
   *
   * @param familyId 家庭标识
   * @return 查询RecentOrders的结果
   */
  List<FamilyHomeResponse.OrderSummary> selectRecentOrders(@Param("familyId") Long familyId);

  /**
   * 查询Featured菜品。
   *
   * @param familyId 家庭标识
   * @return 查询Featured菜品的结果
   */
  FamilyHomeResponse.FeaturedDish selectFeaturedDish(@Param("familyId") Long familyId);

  /**
   * 查询Addresses。
   *
   * @param familyId 家庭标识
   * @return 查询Addresses的结果
   */
  List<AddressEntity> selectAddresses(@Param("familyId") Long familyId);

  /**
   * 处理Default地址。
   *
   * @param familyId 家庭标识
   * @return 处理Default地址的结果
   */
  int clearDefaultAddress(@Param("familyId") Long familyId);

  /**
   * 新增地址。
   *
   * @param entity 实体
   * @return 新增地址的结果
   */
  int insertAddress(AddressEntity entity);

  /**
   * 更新地址。
   *
   * @param entity 实体
   * @return 更新地址的结果
   */
  int updateAddress(AddressEntity entity);

  /**
   * 设置Default地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @return 设置Default地址的结果
   */
  int setDefaultAddress(@Param("familyId") Long familyId, @Param("addressId") Long addressId);

  /**
   * 删除地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @return 删除地址的结果
   */
  int deleteAddress(@Param("familyId") Long familyId, @Param("addressId") Long addressId);

  /**
   * 查询家庭菜单项目列表。
   *
   * @param merchantId 商户标识
   * @param familyId 家庭标识
   * @return 查询家庭菜单项目列表的结果
   */
  List<FamilyMenuItemView> selectFamilyMenuItems(@Param("merchantId") Long merchantId, @Param("familyId") Long familyId);

  /**
   * 查询MealSlots。
   *
   * @param familyId 家庭标识
   * @return 查询MealSlots的结果
   */
  List<MealSlotRecord> selectMealSlots(@Param("familyId") Long familyId);

  /**
   * 查询商户Families。
   *
   * @param merchantId 商户标识
   * @return 查询商户Families的结果
   */
  List<FamilyRecord> selectMerchantFamilies(@Param("merchantId") Long merchantId);

  /**
   * 查询Members。
   *
   * @param familyId 家庭标识
   * @return 查询Members的结果
   */
  List<FamilyMemberRecord> selectMembers(@Param("familyId") Long familyId);

  /**
   * 更新家庭资料。
   *
   * @param merchantId 商户标识
   * @param familyId 家庭标识
   * @param familyName 家庭名称
   * @param note note
   * @param contactNamesJson 联系人NamesJson
   * @return 更新家庭资料的结果
   */
  int updateFamilyProfile(
      @Param("merchantId") Long merchantId,
      @Param("familyId") Long familyId,
      @Param("familyName") String familyName,
      @Param("note") String note,
      @Param("contactNamesJson") String contactNamesJson
  );

  /**
   * 更新家庭配送Policy。
   *
   * @param merchantId 商户标识
   * @param familyId 家庭标识
   * @param deliveryEnabled 配送是否启用
   * @param deliveryFeeDefault 配送费用Default
   * @param deliveryFree 配送Free
   * @return 更新家庭配送Policy的结果
   */
  int updateFamilyDeliveryPolicy(
      @Param("merchantId") Long merchantId,
      @Param("familyId") Long familyId,
      @Param("deliveryEnabled") boolean deliveryEnabled,
      @Param("deliveryFeeDefault") BigDecimal deliveryFeeDefault,
      @Param("deliveryFree") boolean deliveryFree
  );

  /**
   * 删除家庭菜单。
   *
   * @param familyId 家庭标识
   * @return 删除家庭菜单的结果
   */
  int deleteFamilyMenu(@Param("familyId") Long familyId);

  /**
   * 新增家庭菜单项目。
   *
   * @param familyId 家庭标识
   * @param dishId 菜品标识
   * @param enabled 是否启用
   * @param sortOrder sort订单
   * @param finalPrice finalPrice
   * @return 新增家庭菜单项目的结果
   */
  int insertFamilyMenuItem(
      @Param("familyId") Long familyId,
      @Param("dishId") Long dishId,
      @Param("enabled") boolean enabled,
      @Param("sortOrder") int sortOrder,
      @Param("finalPrice") BigDecimal finalPrice
  );

  /**
   * 复制家庭菜单。
   *
   * @param targetFamilyId 目标家庭标识
   * @param sourceFamilyId 来源家庭标识
   * @return 复制家庭菜单的结果
   */
  int copyFamilyMenu(@Param("targetFamilyId") Long targetFamilyId, @Param("sourceFamilyId") Long sourceFamilyId);

  /**
   * 校验家庭存在且属于当前商户。
   *
   * @param merchantId 当前商户ID
   * @param familyId 待校验家庭ID
   * @return 匹配记录数量
   */
  int countFamilyOwnership(@Param("merchantId") Long merchantId, @Param("familyId") Long familyId);

  /**
   * 校验菜品存在且属于当前商户。
   *
   * @param merchantId 当前商户ID
   * @param dishId 待校验菜品ID
   * @return 匹配记录数量
   */
  int countDishOwnership(@Param("merchantId") Long merchantId, @Param("dishId") Long dishId);
}
