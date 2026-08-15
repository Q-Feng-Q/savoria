package com.familykitchen.dish.mapper;

import com.familykitchen.dish.model.entity.DishCategoryEntity;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishIngredientEntity;
import com.familykitchen.dish.model.entity.IngredientDictionaryEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 菜品与食材字典 MyBatis Mapper。
 */
@Mapper
public interface DishMapper {

  /**
   * 新增菜品并回填数据库生成的菜品 ID。
   *
   * @param entity 菜品持久化实体
   * @return 受影响行数
   */
  int insertDish(DishEntity entity);

  /**
   * 查询Dishes。
   *
   * @param merchantId 商户标识
   * @return 查询Dishes的结果
   */
  List<DishEntity> selectDishes(@Param("merchantId") Long merchantId);

  /**
   * 查询菜品。
   *
   * @param merchantId 商户标识
   * @param dishId 菜品标识
   * @return 查询菜品的结果
   */
  DishEntity selectDish(@Param("merchantId") Long merchantId, @Param("dishId") Long dishId);

  /**
   * 校验菜品分类存在且属于当前商户。
   *
   * @param merchantId 当前商户ID
   * @param categoryId 待校验分类ID
   * @return 匹配记录数量
   */
  int countCategoryOwnership(@Param("merchantId") Long merchantId, @Param("categoryId") Long categoryId);

  /**
   * 更新菜品。
   *
   * @param entity 实体
   * @return 更新菜品的结果
   */
  int updateDish(DishEntity entity);

  /** Updates only the owned dish status column.
   * @param merchantId owning merchant identifier
   * @param dishId dish identifier
   * @param status normalized persisted status
   * @return affected row count
   */
  int updateDishStatus(@Param("merchantId") Long merchantId, @Param("dishId") Long dishId,
                       @Param("status") String status);

  /**
   * 删除菜品Ingredients。
   *
   * @param dishId 菜品标识
   * @return 删除菜品Ingredients的结果
   */
  int deleteDishIngredients(@Param("dishId") Long dishId);

  /**
   * 新增菜品食材。
   *
   * @param entity 实体
   * @return 新增菜品食材的结果
   */
  int insertDishIngredient(DishIngredientEntity entity);

  /**
   * 查询菜品Ingredients。
   *
   * @param dishId 菜品标识
   * @return 查询菜品Ingredients的结果
   */
  List<DishIngredientEntity> selectDishIngredients(@Param("dishId") Long dishId);

  /**
   * 删除CookingSteps。
   *
   * @param dishId 菜品标识
   * @return 删除CookingSteps的结果
   */
  int deleteCookingSteps(@Param("dishId") Long dishId);

  /**
   * 新增CookingStep。
   *
   * @param entity 实体
   * @return 新增CookingStep的结果
   */
  int insertCookingStep(DishCookingStepEntity entity);

  /**
   * 查询CookingSteps。
   *
   * @param dishId 菜品标识
   * @return 查询CookingSteps的结果
   */
  List<DishCookingStepEntity> selectCookingSteps(@Param("dishId") Long dishId);

  /**
   * 查询Categories。
   *
   * @param merchantId 商户标识
   * @return 查询Categories的结果
   */
  List<DishCategoryEntity> selectCategories(@Param("merchantId") Long merchantId);

  /**
   * 新增Category。
   *
   * @param entity 实体
   * @return 新增Category的结果
   */
  int insertCategory(DishCategoryEntity entity);

  /**
   * 更新Category。
   *
   * @param entity 实体
   * @return 更新Category的结果
   */
  int updateCategory(DishCategoryEntity entity);

  /**
   * 删除Category。
   *
   * @param merchantId 商户标识
   * @param categoryId category标识
   * @return 删除Category的结果
   */
  int deleteCategory(@Param("merchantId") Long merchantId, @Param("categoryId") Long categoryId);

  /**
   * 统计DishesInCategory。
   *
   * @param merchantId 商户标识
   * @param categoryId category标识
   * @return 统计DishesInCategory的结果
   */
  int countDishesInCategory(@Param("merchantId") Long merchantId, @Param("categoryId") Long categoryId);

  /**
   * 查询食材Dictionary。
   *
   * @param merchantId 商户标识
   * @return 查询食材Dictionary的结果
   */
  List<IngredientDictionaryEntity> selectIngredientDictionary(@Param("merchantId") Long merchantId);

  /**
   * 查询食材。
   *
   * @param merchantId 商户标识
   * @param ingredientId 食材标识
   * @return 查询食材的结果
   */
  IngredientDictionaryEntity selectIngredient(@Param("merchantId") Long merchantId, @Param("ingredientId") Long ingredientId);

  /**
   * 新增食材。
   *
   * @param entity 实体
   * @return 新增食材的结果
   */
  int insertIngredient(IngredientDictionaryEntity entity);

  /**
   * 更新食材。
   *
   * @param entity 实体
   * @return 更新食材的结果
   */
  int updateIngredient(IngredientDictionaryEntity entity);

  /**
   * 删除食材。
   *
   * @param merchantId 商户标识
   * @param ingredientId 食材标识
   * @return 删除食材的结果
   */
  int deleteIngredient(@Param("merchantId") Long merchantId, @Param("ingredientId") Long ingredientId);

  /**
   * 统计食材References。
   *
   * @param merchantId 商户标识
   * @param ingredientName 食材名称
   * @return 统计食材References的结果
   */
  int countIngredientReferences(@Param("merchantId") Long merchantId, @Param("ingredientName") String ingredientName);
}
