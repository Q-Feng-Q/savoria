package com.familykitchen.cart.mapper;

import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.entity.CartItemSelectionEntity;
import java.time.LocalDateTime;
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
   * Locks the owning family row while an initial active cart is created.
   *
   * @param familyId family identifier
   * @return locked family identifier, or null when missing
   */
  Long lockFamilyForCart(@Param("familyId") Long familyId);

  /**
   * Selects the one active shared cart.
   *
   * @param familyId family identifier
   * @return active cart or null
   */
  CartEntity selectFamilyActiveCart(@Param("familyId") Long familyId);

  /**
   * Current-reads and locks the active family cart.
   *
   * @param familyId family identifier
   * @return active cart or null
   */
  CartEntity selectFamilyActiveCartForUpdate(@Param("familyId") Long familyId);

  /**
   * Selects a cart by identifier.
   *
   * @param cartId cart identifier
   * @param familyId family identifier
   * @return cart or null
   */
  CartEntity selectFamilyCart(@Param("cartId") Long cartId, @Param("familyId") Long familyId);

  /**
   * Current-reads and locks a family cart for conflict classification.
   *
   * @param cartId cart identifier
   * @param familyId family identifier
   * @return cart or null
   */
  CartEntity selectFamilyCartForUpdate(
      @Param("cartId") Long cartId, @Param("familyId") Long familyId);

  /**
   * Atomically advances an active cart version.
   *
   * @param cartId cart identifier
   * @param familyId family identifier
   * @param version expected version
   * @return affected rows
   */
  int bumpVersion(@Param("cartId") Long cartId, @Param("familyId") Long familyId,
      @Param("version") long version);

  /** Atomically marks the versioned shared cart submitted.
   * @param cartId cart identifier
   * @param familyId family identifier
   * @param version expected version
   * @return affected rows
   */
  int submitFamilyCart(@Param("cartId") Long cartId,@Param("familyId") Long familyId,
      @Param("version") long version);

  /**
   * Updates expected meal time after a successful version bump.
   *
   * @param cartId cart identifier
   * @param expectedMealTime validated expected time
   * @return affected rows
   */
  int updateExpectedMealTime(@Param("cartId") Long cartId,
      @Param("expectedMealTime") LocalDateTime expectedMealTime);

  /**
   * Selects one member selection.
   *
   * @param itemId aggregate item identifier
   * @param userId member identifier
   * @return selection or null
   */
  CartItemSelectionEntity selectSelection(@Param("itemId") Long itemId,
      @Param("userId") Long userId);

  /**
   * Current-reads and locks one member attribution row.
   *
   * @param itemId aggregate item identifier
   * @param userId member identifier
   * @return locked selection or null
   */
  CartItemSelectionEntity selectSelectionForUpdate(
      @Param("itemId") Long itemId, @Param("userId") Long userId);

  /**
   * Lists member attribution rows.
   *
   * @param itemId aggregate item identifier
   * @return selections
   */
  List<CartItemSelectionEntity> selectSelections(@Param("itemId") Long itemId);

  /**
   * Current-reads and locks all attribution rows for immutable order snapshotting.
   *
   * @param itemId aggregate item identifier
   * @return locked selections
   */
  List<CartItemSelectionEntity> selectSelectionsForUpdate(@Param("itemId") Long itemId);

  /**
   * Upserts a member absolute quantity.
   *
   * @param itemId aggregate item identifier
   * @param userId member identifier
   * @param quantity absolute quantity
   * @param remark member remark
   * @return affected rows
   */
  int upsertSelection(@Param("itemId") Long itemId, @Param("userId") Long userId,
      @Param("quantity") int quantity, @Param("remark") String remark);

  /**
   * Deletes only one member selection.
   *
   * @param itemId aggregate item identifier
   * @param userId member identifier
   * @return affected rows
   */
  int deleteSelection(@Param("itemId") Long itemId, @Param("userId") Long userId);

  /**
   * Sums positive selection quantities.
   *
   * @param itemId aggregate item identifier
   * @return aggregate quantity
   */
  int sumSelections(@Param("itemId") Long itemId);

  /**
   * Deletes an empty aggregate item.
   *
   * @param itemId item identifier
   * @return affected rows
   */
  int deleteAggregateItem(@Param("itemId") Long itemId);

  /**
   * Deletes all attribution rows from one cart.
   *
   * @param cartId cart identifier
   * @return affected rows
   */
  int deleteAllSelections(@Param("cartId") Long cartId);

  /**
   * Deletes all aggregate rows from one cart.
   *
   * @param cartId cart identifier
   * @return affected rows
   */
  int deleteAllAggregateItems(@Param("cartId") Long cartId);

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

  /** Locks the currently enabled family-menu price used for immutable order snapshotting.
   * @param familyId family identifier
   * @param dishId dish identifier
   * @return current sale snapshot, or null
   */
  CartDishSnapshot selectAvailableDishForUpdate(
      @Param("familyId") Long familyId,@Param("dishId") Long dishId);

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
   * Current-reads and locks aggregate items for membership cleanup or submission.
   *
   * @param cartId cart identifier
   * @return locked cart items
   */
  List<CartItemEntity> selectCartItemsForUpdate(@Param("cartId") Long cartId);

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
