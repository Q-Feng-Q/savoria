package com.familykitchen.cart.mapper;

import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 餐篮 MyBatis Mapper。
 *
 * <p>负责餐篮主表、餐篮项和可售菜品快照的真实数据库读写。</p>
 */
@Mapper
public interface CartMapper {

  /**
   * 新增餐篮并回填数据库生成的餐篮 ID。
   *
   * @param entity 餐篮持久化实体
   * @return 受影响行数
   */
  int insertCart(CartEntity entity);

  /**
   * 查询Active购物车。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param mealSlotId mealSlot标识
   * @param serviceDate serviceDate
   * @return 查询Active购物车的结果
   */
  CartEntity selectActiveCart(
      @Param("familyId") Long familyId,
      @Param("memberId") Long memberId,
      @Param("mealSlotId") Long mealSlotId,
      @Param("serviceDate") LocalDate serviceDate
  );

  /**
   * 查询Available菜品。
   *
   * @param familyId 家庭标识
   * @param dishId 菜品标识
   * @return 查询Available菜品的结果
   */
  CartDishSnapshot selectAvailableDish(@Param("familyId") Long familyId, @Param("dishId") Long dishId);

  /**
   * 校验餐次存在、启用且属于当前家庭。
   *
   * @param familyId 当前家庭ID
   * @param mealSlotId 待校验餐次ID
   * @return 匹配记录数量
   */
  int countAvailableMealSlot(@Param("familyId") Long familyId, @Param("mealSlotId") Long mealSlotId);

  /**
   * 查询购物车项目列表。
   *
   * @param cartId 购物车标识
   * @return 查询购物车项目列表的结果
   */
  List<CartItemEntity> selectCartItems(@Param("cartId") Long cartId);

  /**
   * 查询购物车项目。
   *
   * @param cartId 购物车标识
   * @param dishId 菜品标识
   * @return 查询购物车项目的结果
   */
  CartItemEntity selectCartItem(@Param("cartId") Long cartId, @Param("dishId") Long dishId);

  /**
   * 查询购物车项目By标识。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param itemId 项目标识
   * @return 查询购物车项目By标识的结果
   */
  CartItemEntity selectCartItemById(@Param("familyId") Long familyId, @Param("memberId") Long memberId, @Param("itemId") Long itemId);

  /**
   * 新增购物车项目。
   *
   * @param entity 实体
   * @return 新增购物车项目的结果
   */
  int insertCartItem(CartItemEntity entity);

  /**
   * 更新购物车项目。
   *
   * @param entity 实体
   * @return 更新购物车项目的结果
   */
  int updateCartItem(CartItemEntity entity);

  /**
   * 删除购物车项目。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param itemId 项目标识
   * @return 删除购物车项目的结果
   */
  int deleteCartItem(@Param("familyId") Long familyId, @Param("memberId") Long memberId, @Param("itemId") Long itemId);

  /**
   * 更新购物车备注。
   *
   * @param cartId 购物车标识
   * @param remark 备注
   * @return 更新购物车备注的结果
   */
  int updateCartRemark(
      @Param("cartId") Long cartId,
      @Param("remark") String remark
  );

  /**
   * 提交Active购物车。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param mealSlotId mealSlot标识
   * @param serviceDate serviceDate
   * @return 提交Active购物车的结果
   */
  int submitActiveCart(
      @Param("familyId") Long familyId,
      @Param("memberId") Long memberId,
      @Param("mealSlotId") Long mealSlotId,
      @Param("serviceDate") LocalDate serviceDate
  );
}
