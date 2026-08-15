package com.familykitchen.dish.mapper;

import com.familykitchen.dish.model.entity.DishCategoryEntity;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishTemplateCategoryEntity;
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.entity.IngredientDictionaryEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 平台菜品模板查询和商户导入 MyBatis Mapper。 */
@Mapper
public interface DishTemplateMapper {
  /**
   * 查询平台当前启用的模板分类。
   * @return 平台当前启用的模板分类
   */
  List<DishTemplateCategoryEntity> selectTemplateCategories();
  /**
   * 统计满足筛选条件的模板数量。
   * @param merchantId 当前商户 ID
   * @param categoryId 模板分类 ID
   * @param keyword 菜名关键词
   * @param imported 导入状态
   * @return 匹配数量
   */
  long countTemplates(@Param("merchantId") Long merchantId, @Param("categoryId") Long categoryId,
      @Param("keyword") String keyword, @Param("imported") Boolean imported);
  /**
   * 分页查询模板列表。
   * @param merchantId 当前商户 ID
   * @param categoryId 模板分类 ID
   * @param keyword 菜名关键词
   * @param imported 导入状态
   * @param offset 分页偏移量
   * @param pageSize 每页数量
   * @return 模板列表
   */
  List<DishTemplateEntity> selectTemplates(@Param("merchantId") Long merchantId,
      @Param("categoryId") Long categoryId, @Param("keyword") String keyword,
      @Param("imported") Boolean imported, @Param("offset") int offset, @Param("pageSize") int pageSize);
  /**
   * 查询单个启用模板及当前商户导入状态。
   * @param merchantId 当前商户 ID
   * @param templateId 模板 ID
   * @return 模板实体，不存在时为空
   */
  DishTemplateEntity selectTemplate(@Param("merchantId") Long merchantId, @Param("templateId") Long templateId);
  /**
   * 查询模板食材。
   * @param templateId 模板 ID
   * @return 食材明细
   */
  List<DishTemplateIngredientEntity> selectTemplateIngredients(@Param("templateId") Long templateId);
  /**
   * 批量查询启用模板。
   * @param templateIds 模板 ID 列表
   * @return 启用模板列表
   */
  List<DishTemplateEntity> selectTemplatesByIds(@Param("templateIds") List<Long> templateIds);
  /**
   * 查询全部启用模板，供商户一键导入。
   * @return 按分类与模板排序的全部启用模板
   */
  List<DishTemplateEntity> selectAllEnabledTemplates();
  /**
   * 批量查询模板食材。
   * @param templateIds 模板 ID 列表
   * @return 食材明细
   */
  List<DishTemplateIngredientEntity> selectTemplateIngredientsByIds(@Param("templateIds") List<Long> templateIds);
  /**
   * 查询当前商户已经导入的模板 ID。
   * @param merchantId 当前商户 ID
   * @param templateIds 待检查模板 ID
   * @return 已导入模板 ID
   */
  List<Long> selectImportedTemplateIds(@Param("merchantId") Long merchantId,
      @Param("templateIds") List<Long> templateIds);
  /**
   * 按名称查询商户分类。
   * @param merchantId 当前商户 ID
   * @param name 分类名称
   * @return 商户分类，不存在时为空
   */
  DishCategoryEntity selectMerchantCategoryByName(@Param("merchantId") Long merchantId,
      @Param("name") String name);
  /**
   * 幂等创建商户分类。
   * @param category 商户分类实体
   * @return 新增行数
   */
  int insertMerchantCategoryIgnore(DishCategoryEntity category);
  /**
   * 幂等写入模板导入菜品。
   * @param dish 商户菜品实体
   * @return 新增行数，重复模板为零
   */
  int insertImportedDishIgnore(DishEntity dish);
  /**
   * 查询模板导入后对应的商户菜品 ID。
   * @param merchantId 当前商户 ID
   * @param templateId 来源模板 ID
   * @return 商户菜品 ID
   */
  Long selectImportedDishId(@Param("merchantId") Long merchantId, @Param("templateId") Long templateId);
  /**
   * 幂等补齐商户食材字典。
   * @param ingredient 商户食材字典实体
   * @return 新增行数
   */
  int insertMerchantIngredientIgnore(IngredientDictionaryEntity ingredient);
}
