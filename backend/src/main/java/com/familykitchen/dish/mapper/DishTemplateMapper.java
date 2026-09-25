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
  long countTemplatesByProductType(@Param("merchantId") Long merchantId, @Param("categoryId") Long categoryId,
      @Param("keyword") String keyword, @Param("imported") Boolean imported, @Param("productType") String productType);
  List<DishTemplateEntity> selectTemplatesByProductType(@Param("merchantId") Long merchantId,
      @Param("categoryId") Long categoryId, @Param("keyword") String keyword,
      @Param("imported") Boolean imported, @Param("offset") int offset, @Param("pageSize") int pageSize, @Param("productType") String productType);
  long countAdminTemplatesByProductType(@Param("keyword") String keyword, @Param("sourceType") String sourceType,
      @Param("templateType") String templateType, @Param("dataStatus") String dataStatus,
      @Param("sourceCategory") String sourceCategory, @Param("missingImage") Boolean missingImage,
      @Param("missingSteps") Boolean missingSteps, @Param("productType") String productType);
  List<DishTemplateEntity> selectAdminTemplatesPageByProductType(@Param("keyword") String keyword,
      @Param("sourceType") String sourceType, @Param("templateType") String templateType,
      @Param("dataStatus") String dataStatus, @Param("sourceCategory") String sourceCategory,
      @Param("missingImage") Boolean missingImage, @Param("missingSteps") Boolean missingSteps,
      @Param("offset") int offset, @Param("pageSize") int pageSize, @Param("productType") String productType);
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
   * 统计平台管理端筛选后的模板数量。
   * @param keyword 菜名关键词
   * @param sourceType 来源类型
   * @param templateType 模板类型
   * @param dataStatus 数据完整状态
   * @param sourceCategory 来源分类
   * @param missingImage 是否缺少公开图片
   * @param missingSteps 是否缺少制作步骤
   * @return 匹配模板数量
   */
  long countAdminTemplates(@Param("keyword") String keyword, @Param("sourceType") String sourceType,
      @Param("templateType") String templateType, @Param("dataStatus") String dataStatus,
      @Param("sourceCategory") String sourceCategory, @Param("missingImage") Boolean missingImage,
      @Param("missingSteps") Boolean missingSteps);
  /**
   * 分页查询平台管理端可见的全部模板。
   * @param keyword 菜名关键词
   * @param sourceType 来源类型
   * @param templateType 模板类型
   * @param dataStatus 数据完整状态
   * @param sourceCategory 来源分类
   * @param missingImage 是否缺少公开图片
   * @param missingSteps 是否缺少制作步骤
   * @param offset 分页偏移量
   * @param pageSize 每页数量
   * @return 当前页模板实体
   */
  List<DishTemplateEntity> selectAdminTemplatesPage(@Param("keyword") String keyword,
      @Param("sourceType") String sourceType, @Param("templateType") String templateType,
      @Param("dataStatus") String dataStatus, @Param("sourceCategory") String sourceCategory,
      @Param("missingImage") Boolean missingImage, @Param("missingSteps") Boolean missingSteps,
      @Param("offset") int offset, @Param("pageSize") int pageSize);
  /**
   * 查询平台管理端单个模板，不应用市场资格过滤。
   * @param templateId 模板ID
   * @return 模板实体，不存在时为空
   */
  DishTemplateEntity selectAdminTemplate(@Param("templateId") Long templateId);
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
   * 按受控资源ID查询内部图片。
   * @param assetId 受控图片资源ID
   * @return 内部图片资产，不存在时为空
   */
  DishTemplateImageAssetEntity selectTemplateImageAsset(@Param("assetId") Long assetId);
  /**
   * 锁定内部图片审核记录。
   * @param assetId 受控图片资源ID
   * @return 被锁定图片资产，不存在时为空
   */
  DishTemplateImageAssetEntity selectTemplateImageAssetForUpdate(@Param("assetId") Long assetId);
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
   * 保存平台管理员可编辑字段和服务端派生状态。
   * @param template 待保存模板
   * @param expectedVersion 期望并发版本
   * @return 更新行数
   */
  int updateAdminTemplate(@Param("template") DishTemplateEntity template,
      @Param("expectedVersion") Long expectedVersion);
  /**
   * 保存公共模板图片授权并递增模板版本。
   * @param templateId 模板ID
   * @param publicImageUrl 公共图片地址
   * @param sourceUrl 图片来源页面
   * @param author 图片作者
   * @param license 图片许可证
   * @param expectedVersion 期望并发版本
   * @return 更新行数
   */
  int publishTemplateImage(@Param("templateId") Long templateId,
      @Param("publicImageUrl") String publicImageUrl, @Param("sourceUrl") String sourceUrl,
      @Param("author") String author, @Param("license") String license,
      @Param("expectedVersion") Long expectedVersion);
  /**
   * 将已锁定的待审核图片标记为已发布。
   * @param assetId 受控图片资源ID
   * @param publicImageUrl 公共图片地址
   * @param author 图片作者
   * @param license 图片许可证
   * @param reviewedBy 审核管理员ID
   * @return 更新行数
   */
  int markTemplateImageAssetPublished(@Param("assetId") Long assetId,
      @Param("publicImageUrl") String publicImageUrl, @Param("author") String author,
      @Param("license") String license, @Param("reviewedBy") Long reviewedBy);
  /**
   * 将内部图片永久驳回，仅允许从待审核状态流转。
   * @param assetId 受控图片资源ID
   * @param reviewedBy 审核管理员ID
   * @param reason 驳回原因
   * @return 更新行数
   */
  int rejectTemplateImageAsset(@Param("assetId") Long assetId, @Param("reviewedBy") Long reviewedBy,
      @Param("reason") String reason);
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
