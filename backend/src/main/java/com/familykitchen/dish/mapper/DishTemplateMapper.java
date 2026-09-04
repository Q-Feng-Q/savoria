package com.familykitchen.dish.mapper;

import com.familykitchen.dish.model.entity.DishCategoryEntity;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishTemplateCategoryEntity;
import com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity;
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateImageAssetEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.entity.DishTemplateNameAliasEntity;
import com.familykitchen.dish.model.entity.DishTemplateSourceRecordEntity;
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
   * 查询平台管理端可见的全部模板，不应用商户市场资格过滤。
   * @return 包含待完善模板和组件的模板列表
   */
  List<DishTemplateEntity> selectAdminTemplates();
  /**
   * 锁定并读取平台模板，供提交与审核事务使用。
   * @param templateId 模板 ID
   * @return 被锁定模板，不存在时为空
   */
  DishTemplateEntity selectTemplateForUpdate(@Param("templateId") Long templateId);
  /**
   * 锁定并读取仍满足商户市场资格的成品模板。
   * @param templateId 模板ID
   * @return 满足资格的锁定模板；资格已变化时为空
   */
  DishTemplateEntity selectEligibleTemplateForUpdate(@Param("templateId") Long templateId);
  /**
   * 锁定并读取目标模板分类，防止事务内被并发停用。
   * @param categoryId 分类 ID
   * @return 被锁定分类，不存在时为空
   */
  DishTemplateCategoryEntity selectCategoryForUpdate(@Param("categoryId") Long categoryId);
  /**
   * 查询模板食材。
   * @param templateId 模板 ID
   * @return 食材明细
   */
  List<DishTemplateIngredientEntity> selectTemplateIngredients(@Param("templateId") Long templateId);
  /**
   * 查询模板制作步骤。
   * @param templateId 模板ID
   * @return 按步骤序号排序的模板制作步骤
   */
  List<DishTemplateCookingStepEntity> selectTemplateCookingSteps(@Param("templateId") Long templateId);
  /**
   * 查询模板来源记录。
   * @param templateId 模板ID
   * @return 模板来源记录
   */
  List<DishTemplateSourceRecordEntity> selectTemplateSourceRecords(@Param("templateId") Long templateId);
  /**
   * 查询模板名称别名。
   * @param templateId 模板ID
   * @return 模板名称别名
   */
  List<DishTemplateNameAliasEntity> selectTemplateNameAliases(@Param("templateId") Long templateId);
  /**
   * 查询模板内部图片审核资产。
   * @param templateId 模板ID
   * @return 内部图片审核资产
   */
  List<DishTemplateImageAssetEntity> selectTemplateImageAssets(@Param("templateId") Long templateId);
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
  /**
   * 按目标完整快照覆盖模板主信息，并以基础版本作并发保护。
   * @param template 目标模板实体
   * @param baseVersion 基础版本
   * @return 更新行数
   */
  int replaceTemplate(@Param("template") DishTemplateEntity template,
      @Param("baseVersion") Long baseVersion);
  /**
   * 删除模板全部旧食材。
   * @param templateId 模板 ID
   * @return 删除行数
   */
  int deleteTemplateIngredients(@Param("templateId") Long templateId);
  /**
   * 插入审核通过后的单项模板食材。
   * @param ingredient 模板食材实体
   * @return 新增行数
   */
  int insertTemplateIngredient(DishTemplateIngredientEntity ingredient);
  /**
   * 删除模板的全部制作步骤。
   * @param templateId 模板ID
   * @return 删除步骤数
   */
  int deleteTemplateCookingSteps(@Param("templateId") Long templateId);
  /**
   * 新增模板制作步骤。
   * @param step 模板制作步骤
   * @return 新增行数
   */
  int insertTemplateCookingStep(DishTemplateCookingStepEntity step);
  /**
   * 新增模板来源记录。
   * @param record 来源记录
   * @return 新增行数
   */
  int insertTemplateSourceRecord(DishTemplateSourceRecordEntity record);
  /**
   * 删除模板全部来源记录。
   * @param templateId 模板ID
   * @return 删除来源记录数
   */
  int deleteTemplateSourceRecords(@Param("templateId") Long templateId);
  /**
   * 新增模板名称别名。
   * @param alias 名称别名
   * @return 新增行数
   */
  int insertTemplateNameAlias(DishTemplateNameAliasEntity alias);
  /**
   * 删除模板全部名称别名。
   * @param templateId 模板ID
   * @return 删除别名数
   */
  int deleteTemplateNameAliases(@Param("templateId") Long templateId);
  /**
   * 新增内部图片审核资产。
   * @param asset 内部图片资产
   * @return 新增行数
   */
  int insertTemplateImageAsset(DishTemplateImageAssetEntity asset);
  /**
   * 更新内部图片审核资产状态和审核字段。
   * @param asset 内部图片资产
   * @return 更新行数
   */
  int updateTemplateImageAsset(DishTemplateImageAssetEntity asset);
}
