# 家庭厨房菜谱模板数据库设计

本文说明菜谱模板子系统在 MySQL 8 中的实际持久化结构。Flyway 迁移位于 `backend/src/main/resources/db/migration/`：V4 创建模板市场，V5/V6 扩充既有模板，V8 创建修改审核表，V13 增量同步完整 CookLikeHOC 菜谱目录。

## 1. 设计原则

- 数据库不使用外键，关联完整性由 Service 事务、行锁、唯一索引和业务校验维护。
- 平台模板与商户导入副本分离。导入后商户菜品可以独立修改，模板更新不会静默覆盖副本。
- 来源身份、图片授权和采购就绪状态由服务端维护，不能由商户审核快照覆盖。
- 模板主表使用 `version` 乐观锁；审批事务同时替换主信息、食材和步骤。
- CookLikeHOC 同步结果通过前向迁移写入初始化链路；来源缺图时公共图片字段保持 null。

## 2. 核心表

### `dish_templates`

平台模板主表。关键字段包括稳定 `template_code`、逻辑分类 `category_id`、菜名和简介、可空公共图片与参考价格、标签、来源类型/键/版本/分类、`template_type`、`data_status`、`procurement_ready`、`image_rights_status`、排序、启用状态和 `version`。

约束：`template_code`、`source_key` 唯一。业务代码校验分类存在且启用；`COMPONENT` 不能直接导入；`DISH` 只有价格和采购数据满足条件时可导入。

### `dish_template_ingredients`

模板食材与组件引用表。通过 `template_id` 逻辑关联主表，使用 `source_line_key` 保证生成数据稳定。普通食材保存名称、分类、数量状态、可空采购数量、单位、计算方式和来源原文；组件行保存 `component_template_id` 与 `component_multiplier`。

`quantity_status=VERIFIED` 时采购数量、单位和计算方式必须完整；其他状态保持采购字段为 null。组件存在性、循环和单位兼容性由统一采购就绪判定器检查。

### `dish_template_cooking_steps`

模板制作步骤表。通过 `template_id` 逻辑关联模板，`item_key` 全局唯一，`template_id + step_no` 唯一。步骤保存标题、完整内容、时长、温度、火候及可选组件引用。业务层保证 `step_no` 从 1 连续递增。

### `dish_template_source_records`

来源证据表。保存原仓库文件、分类、标题、份量说明、原料段、制作流程及固定来源版本。`source_key` 唯一。平台查询可返回业务来源信息，但内部文件系统路径不进入公开接口。

### `dish_template_image_assets`

私有图片审核表。保存内容哈希、媒体类型、大小、内部存储键、来源声明和审核状态。`internal_storage_key` 仅后端使用，任何 API 都不得直接返回它。

状态流转为 `INTERNAL_REVIEW -> PUBLISHED` 或 `INTERNAL_REVIEW -> REJECTED`。发布必须填写作者、来源页和许可证；驳回后不可预览或再次发布。公共 URL 只在发布后写入模板主表。

### `dish_template_change_requests`

商户模板修改申请表。保存商户、模板、基础版本、v2 基础快照、v2 目标快照、状态、提交/审核/撤回人员和时间以及结果通知 ID。

同一商户和模板同一时间只允许一个 `PENDING` 申请。审批时按申请 ID 和模板 ID 加行锁，比较 `base_template_version` 与当前 `version`，不一致即拒绝覆盖。

## 3. 关联表

- `dish_template_categories`: 平台分类字典，模板通过 `category_id` 逻辑关联。
- `dish_template_name_aliases`: 来源名称或别名映射，用于稳定合并与检索。
- `dishes`: 商户菜品副本，`source_template_id` 记录导入来源；不构成自动同步关系。
- `dish_ingredients`: 商户菜品采购食材，导入时由模板食材图展开后复制。
- `dish_cooking_steps`: 商户制作步骤，导入时复制模板步骤，并记录可选来源步骤 ID。

## 4. 事务边界

选择导入在单个事务内校验全部模板，随后写入商户分类、菜品、食材和步骤。任一模板不满足启用、成品类型、价格或采购就绪条件时返回业务错误，不写入半份数据。

修改审批在单个事务内完成：锁申请、锁模板并校验版本、锁目标分类和组件、更新主表、删除并重建食材、删除并重建步骤、更新申请状态、创建商户通知。任何写入失败均由 Spring 事务回滚。

图片发布在单个事务内校验资源状态、SHA-256 内容、来源声明和模板版本，复制到公共目录后更新模板及资源状态。客户端永远不能提供或读取服务器绝对路径。

## 5. 初始化与同步

CookLikeHOC 同步固定到已审核的来源提交，生成清单、质量报告和确定性 SQL 数据段。生产发布使用 `V13__sync_complete_cooklikehoc_recipe_catalog.sql` 作为前向迁移，避免修改已执行的 `V4/V5/V6` 及其 Flyway 校验和。相同来源版本重复生成必须得到相同数据；缺少步骤的菜谱保留空步骤集合，缺少图片或授权声明的菜谱不发布公共图片。
