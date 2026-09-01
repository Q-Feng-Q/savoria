# CookLikeHOC 全量模板菜谱同步设计

## 目标

以 CookLikeHOC 网站及其公开源码仓库为主要菜谱来源，将全部成品菜、配料组件、食材信息和制作流程同步到家庭私厨模板体系。来源数据直接进入未上线项目的初始化 SQL，不新增面向历史生产库的升级迁移，不编造图片、价格、家庭份量或缺失步骤。

本次固定来源仓库提交为 `f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed`。后续更新必须记录新的来源提交，并通过稳定来源键进行覆盖或新增。

## 来源盘点

来源包含 15 个目录：主食、凉拌、卤菜、早餐、汤、炒菜、炖菜、炸品、烤类、烫菜、煮锅、砂锅菜、蒸菜、配料和饮品。

当前提交共有 336 个菜谱 Markdown 文件、333 个唯一标题：

- 296 个成品菜文件，去重后约 294 道成品菜。
- 40 个配料文件，去重后约 39 个配料组件。
- 来源仓库共有 192 个图片文件；179 个菜谱文件声明图片，157 个没有图片。生成报告还必须列出失效引用、未使用图片和重复引用。
- 285 个文件具有标准步骤标题，51 个没有标准步骤标题。
- 292 个文件具有标准原料、配料或食材标题，44 个采用非标准结构。
- 三组重复标题为农家小炒肉鸡蛋干版本、农家小炒肉玉耳版本和小炒肉调料；每个文件都保存独立来源记录。主来源只能由版本化配置文件中的 `primarySourceByDuplicateTitle` 显式指定，生成器禁止使用文件修改时间、标题中的年份或“内容更完整”等启发式规则。新增重复组但配置未覆盖时生成失败。

最终数量由生成器产出的同步清单决定，测试不得硬编码未经解析确认的估算值。

## 合并原则

### 来源主导但不清空本地模板

- 为每个来源菜谱生成稳定 `source_key`，格式为规范化分类和相对 Markdown 路径，不以易变的数据库 ID 作为来源标识。
- 先按人工维护的别名表匹配，再按规范化标题精确匹配现有模板。
- 匹配成功时保留现有模板 ID 和 `template_code`，用来源食材、流程、分类和来源元数据覆盖相应字段。
- 来源新增菜谱创建新模板和稳定模板编码。
- 本地未匹配模板继续保留，标记为 `LOCAL_EXTENSION`，不因来源同步删除。
- 一个现有模板不得被两个来源主菜谱同时认领；重复或歧义匹配会使生成失败，必须显式补充别名规则。

### 成品菜与配料组件

- 成品菜使用 `template_type=DISH`，满足发布条件时可进入模板市场并被商户导入。
- 配料目录使用 `template_type=COMPONENT`，全部入库但不出现在点菜模板市场，也不能作为商户成品菜导入。
- 菜谱中指向配料 Markdown 的链接解析为组件逻辑引用；引用不到组件时生成失败。
- 组件可以拥有自己的食材和制作步骤，供成品菜详情与商户后台查看。

### 确定性合并矩阵

| 字段 | 匹配到现有模板 | 来源新增模板 |
| --- | --- | --- |
| 名称 | 使用规范化后的来源主标题，旧名称写入模板名称别名表 | 使用来源主标题 |
| 简介 | 根据来源事实重新归纳，来源没有可归纳信息时保留现有简介 | 为空，不生成营销描述 |
| 目标分类 | 按固定的 15 类映射表更新 | 按来源分类映射 |
| 来源分类 | 始终使用来源目录 | 始终使用来源目录 |
| 食材 | 来源食材和组件引用覆盖；旧采购量只有经过显式人工映射、复核并记录依据后才能转为 `VERIFIED`，否则按 `SOURCE_BATCH` 待完善 | 仅写来源事实，可采购数量未知时待完善 |
| 制作步骤 | 来源归纳步骤完整替换模板步骤 | 写入来源归纳步骤 |
| 参考价 | 保留现有非空已验证价格 | `NULL` |
| 图片 | 保留现有 `DECLARED` 授权图片；来源未声明图片只进内部审核区 | 公共图片字段为 `NULL` |
| 口味/餐次标签 | 保留现有人工标签 | 空 JSON 数组 |
| enabled | 仅在采购、价格和类型均可导入时保留或开启 | 默认关闭，补齐后开启 |
| 采购状态 | 按聚合规则重新计算 | 按聚合规则计算 |

标题规范化依次执行：UTF-8 严格解码、Unicode NFC、去除 Markdown 链接但保留可见文字、全角括号统一、首尾空白清理和连续空白压缩。路径统一使用 `/`、NFC 和 UTF-8，不做大小写折叠。

分类映射和别名配置保存在受版本控制的 UTF-8 JSON 文件中。输入文件按规范化 `source_key` 字典序处理。新模板 ID 和 `template_code` 由受版本控制的稳定分配清单决定，首次分配后永不因新增文件排序而改变；键、ID、编码或别名发生碰撞时生成立即失败。

## 数据模型

### dish_templates

在现有模板表中增加：

- `template_type`：`DISH/COMPONENT`。
- `source_type`：`COOK_LIKE_HOC/LOCAL_EXTENSION`。
- `source_key`：稳定来源路径，来源模板唯一。
- `source_url`：对应网站菜谱页面。
- `source_revision`：来源 Git 提交。
- `source_category`：原始目录名。
- `source_yield_text`：来源中可识别的份数或批次原文，可空。
- `data_status`：`READY/NEEDS_PURCHASE_DATA/NEEDS_PRICE/NEEDS_BOTH`。
- `procurement_ready`：采购用量是否已完成家庭化校验。
- `image_rights_status`：`DECLARED/UNDECLARED/NONE`。

`image_url`、`image_source_url`、`image_author`、`image_license` 和 `reference_price` 改为可空。已有本地模板的真实价格和已声明授权图片继续保留；来源新增模板没有价格时保持空值。

只有 `template_type=DISH`、`data_status=READY`、`procurement_ready=1`、参考价非空且 `enabled=1` 的模板可以被商户导入。待完善模板仍可被平台管理员查询和维护。

### dish_template_source_records

新增来源记录表，保证 336 个来源文件都可追踪：

- `id`、`template_id`。
- `source_key`，全局唯一，使用 NFC 规范化后的分类和相对路径。
- `source_title`、`source_category`、`source_path`、`source_url`、`source_revision`。
- `record_type`：`PRIMARY/SOURCE_ALIAS/PATH_RENAME`，只表达来源文件之间的主从关系或来源路径改名，不承载本地模板名称历史。
- `alias_reason`，别名或历史重命名原因，可空。
- `content_sha256`，规范化 Markdown 内容摘要。

`dish_templates.source_key` 保存主来源键；其他重复文件和未来改名路径保存在来源记录表。任一来源文件必须且只能映射到一个模板，任一模板最多有一个 `PRIMARY` 来源记录。

### dish_template_name_aliases

新增模板名称别名表，独立保存本地旧名称和来源标题变体：

- `id`、`template_id`、`alias_name`、`normalized_alias_name`、`created_at`。
- `alias_type`：`LOCAL_PREVIOUS_NAME/SOURCE_TITLE_VARIANT/MANUAL`。
- `normalized_alias_name` 全局唯一；同一规范化别名命中多个模板时生成失败，必须在映射配置中人工消歧。

模板匹配顺序固定为稳定来源键、显式映射、规范化主名称、规范化名称别名。来源记录与名称别名分别维护，避免把“文件是同一来源的另一个版本”和“菜名曾经不同”混为一谈。

### dish_template_ingredients

扩展模板食材表：

- `source_text`：来源原料行的归一化文本。
- `source_quantity_text`：步骤或原文中识别出的批量用量文本，可空。
- `quantity_status`：`VERIFIED/SOURCE_BATCH/MISSING/NOT_APPLICABLE`。
- `component_template_id`：指向配料组件模板的逻辑 ID，可空。
- `source_line_key`：来源文件与原料行位置组成的稳定键，用于追踪同名但用途不同的原料行。
- `component_occurrence_key`：组件在当前菜谱中的引用出现键，可空；同一组件被引用两次时必须生成两个不同出现键。
- `component_multiplier`：组件引用倍数，可空；仅在来源明确给出且可验证时写入。

现有可用于采购的数量保持 `quantity + unit + calc_type`。来源用量只有在份数和单位可可靠换算时才写入采购字段；无法换算时数量为空或为非采购状态，绝不把餐饮批量数直接用于家庭采购清单。

`V4` 将模板食材的 `quantity`、`unit` 和 `calc_type` 调整为可空，并由数据库约束和服务校验共同维护状态不变量：`VERIFIED` 必须数量大于 0、单位和计算方式非空；`SOURCE_BATCH/MISSING` 的采购数量、单位和计算方式必须为空，只保留来源用量原文；`NOT_APPLICABLE` 不参与采购计算。任何从旧数量转为 `VERIFIED` 的人工映射都必须进入版本化映射清单，包含来源键、来源行键、沿用模板项、审核人和审核理由。

采购就绪采用唯一的共享判定器：从成品菜开始遍历全部直接食材和组件引用。所有需要采购的叶子食材必须为 `VERIFIED`，且 `quantity > 0`、单位非空、`calc_type` 为受支持值；`NOT_APPLICABLE` 不进入采购计算；任一 `SOURCE_BATCH` 或 `MISSING` 都使整道菜 `procurement_ready=0`。组件引用通过有向图遍历，检测到环或未解析组件时生成失败，但同一组件经不同合法引用路径出现时必须按 `component_occurrence_key` 和倍数完整展开，不能因“重复路径”去重。`component_multiplier=NULL` 不得默认为 1，必须使所属成品菜 `procurement_ready=0`。聚合只合并规范化食材相同、单位可兼容、`calc_type` 相同且均为 `VERIFIED` 的叶子项；单位或计算方式不兼容的项分行保留并阻止错误求和。直接食材与组件食材即使同名也保留来源行和引用路径的乘数，确保采购量不被低估。

商户市场列表、模板详情、选择导入和全部导入共用同一 eligibility 查询条件。导入事务内再次锁定并校验模板类型、数据状态、价格、版本和采购就绪状态，任何条件变化返回明确冲突错误，不生成半成品商户菜。

### dish_template_cooking_steps

新增模板制作步骤表：

- `id`、`template_id`、`step_no`。
- `title`，可空。
- `content`，保存归纳后的完整操作。
- `source_text`，仅保存必要的结构化来源依据，不保存冗长重复段落。
- `duration_seconds`、`temperature_text`、`heat_level`，能可靠识别时填写。
- `component_template_id`，当前步骤使用配料组件时填写逻辑引用。
- 唯一索引为 `template_id + step_no`，不创建物理外键。

流程归纳必须保留原有顺序、关键用量、时间、火候、预处理和出锅条件。没有流程的菜谱保存空步骤列表，不生成占位步骤。

商户 `dish_cooking_steps` 同步增加 `duration_seconds`、`temperature_text`、`heat_level`、`source_template_step_id`、`component_template_id` 和 `source_note`，因此导入不会把结构化流程压扁成不可追踪文本。成品菜引用组件时，导入器先复制组件准备步骤并使用组件名称作为阶段标题，再复制成品菜步骤；`component_template_id` 保留来源关系，步骤序号在商户菜内重新连续编号。

### dish_template_image_assets

新增内部图片资产表，为管理员预览和受控发布提供持久化依据：

- `id`、`template_id`、`source_record_id`、`internal_storage_key`、`content_sha256`、`mime_type`、`file_size`。
- `source_image_path`、`source_url`、`source_revision`，用于定位来源，不保存可被客户端拼接的本机绝对路径。
- `asset_status`：`INTERNAL_REVIEW/PUBLISHED/REJECTED`。
- `public_image_url`、`image_author`、`image_license`、`reviewed_by`、`reviewed_at`，未发布时均为空。
- `source_revision + source_image_path + content_sha256` 唯一；同一内容可关联多个模板，但每个模板必须有独立关联记录。

来源没有图片时不创建资产记录。发布操作锁定资产与模板，只有 `INTERNAL_REVIEW` 可转为 `PUBLISHED`；成功后资产授权元数据与模板公共图片字段在同一事务内更新。拒绝资产转为 `REJECTED` 且不能预览给非平台管理员；已发布资产不得通过普通模板编辑接口替换或回退。

## 来源解析与生成

新增离线生成器，输入为固定提交的 CookLikeHOC 仓库，输出为：

1. 规范化菜谱清单 JSON，便于审查和测试。
2. 初始化 SQL 中的分类、模板、食材、组件引用和制作步骤语句。
3. 图片资源清单及来源元数据。
4. 数据质量报告，包括重复标题、别名匹配、缺图、缺步骤、缺食材标题、未解析链接和采购待完善原因。

解析器按 Markdown 语法读取标题、图片、章节、列表和链接，不依赖脆弱的整文件正则替换。非标准文件采用可测试的兼容规则；无法确定时输出待人工处理错误，不静默丢字段。

步骤 `content` 必须重新组织语言，只保留烹饪事实，包括原料用量、动作顺序、时间、温度、火候和完成条件；运行时 SQL 不保存来源中的长段说明。`source_text` 只保留简短来源定位和必要事实摘要。生成配置必须固定 `normalizationVersion`、`tokenizerVersion` 和 `similarityThreshold`；超过阈值的步骤写入复核清单，记录 `sourceKey`、`stepNo`、分数、审核人、审核时间和 `APPROVED/REWRITE_REQUIRED` 处置结果。只要存在未处置或 `REWRITE_REQUIRED` 项，生成器就拒绝输出最终 SQL。相似度只是审查辅助，不代表版权或授权判断。

生成器必须幂等。完整输入集合包括固定来源提交、主来源配置、名称/路径映射、人工采购映射、稳定 ID 分配清单、归一化与分词版本、相似度阈值和人工复核结果；这些版本化输入完全相同时，重复运行必须得到字节一致的清单与 SQL。生成产物不得写入当前时间，需审计的时间使用版本化输入中的固定值。生成器不在应用启动时联网，生产运行只读取 MySQL 中已经初始化的数据。

## 初始化 SQL

所有新增模板字段、来源记录表、名称别名表、步骤表、来源分类、来源菜谱、配料组件、食材和流程都直接写入 `V4__init_dish_template_market.sql`。商户 `dish_cooking_steps` 已由 `V2` 建表，本次需要的扩展字段也在 `V4` 通过 `ALTER TABLE` 增加；不得再修改 `V2`。项目未上线，本次不创建 `V4` 之后的补丁迁移，全部本次变更集中在 `V4`。

现有 198 道基础模板、`V5` 的 42 道地域模板和 `V6` 的最终图片归属信息先完整折叠进 `V4`，随后删除冲突的 `V5/V6`，使完整 Flyway 链从空库只执行一次最终模板数据。修改已执行迁移会改变 checksum，因此现有开发数据库必须在明确核对库名并备份后重建空 schema；不允许使用 `flyway repair` 或修改历史表绕过。验收必须执行完整迁移目录，而不是单独运行 `V4`。

SQL 继续遵守：

- UTF-8、LF、末尾换行。
- 所有表和字段有中文注释。
- 每条初始化数据独立一行。
- 不创建物理外键。
- JSON、引号、换行和反斜杠正确转义。
- 数据 ID 和模板编码稳定、可重复生成。

## 图片规则

- 来源菜谱声明且仓库中真实存在的图片只下载到不对外映射的内部审核目录，使用稳定文件名并保存内容摘要。
- 保存来源页面和来源仓库路径，`image_rights_status=UNDECLARED`。未声明授权的审核文件绝不写入公共 `image_url`，商户和家庭 API 也不得返回内部路径。
- 原有已声明授权图片继续保留其作者和许可证字段。
- 来源没有图片时所有图片字段为 `NULL`，不生成占位图、不复用无关菜图。
- 数据库和服务层强制状态不变量：`UNDECLARED/NONE` 时公共 `image_url` 必须为 `NULL`；`DECLARED` 时公共资源、作者、来源地址和许可证均必须非空。
- 平台管理员只能通过独立图片发布操作选择内部审核资源、填写作者、来源地址和许可证，服务校验后复制到公共静态目录并原子更新状态；普通模板 `PUT` 不接受公共图片路径或版权状态字段。小程序和 PC 对空图片使用纯布局占位状态，不发起空 URL 请求。

## 后端流程

### 查询

- 商户模板市场只返回可导入的 `DISH + READY` 模板。
- 平台管理端可按来源、类型、数据状态、分类、缺图和缺流程筛选全部模板与组件。
- 模板详情返回来源信息、食材采购状态、组件引用和有序 `cookingSteps`。

平台管理员接口：

- `GET /api/admin/dish-templates`：需要平台管理员权限，支持 `page/pageSize/keyword/sourceType/templateType/dataStatus/sourceCategory/missingImage/missingSteps`，返回全部模板和组件。枚举筛选值非法返回 400，不静默忽略。
- `GET /api/admin/dish-templates/{templateId}`：返回来源记录、名称别名、内部图片审核元数据、完整食材图和有序步骤；内部审核文件只对平台管理员返回受控资源 ID，不返回文件系统路径。
- `PUT /api/admin/dish-templates/{templateId}`：提交完整 `schemaVersion=2` 可编辑快照和 `expectedVersion`，维护名称、简介、分类、价格、标签、采购数量、步骤及启用状态；不允许提交服务端来源与图片版权字段。
- `GET /api/admin/dish-templates/{templateId}/source-records`：返回主来源、别名和内容摘要。
- `GET /api/admin/dish-template-assets/{assetId}/preview`：仅平台管理员可按受控资源 ID 预览内部审核图片，不暴露文件系统路径。
- `POST /api/admin/dish-templates/{templateId}/image-promotion`：提交 `internalAssetId/author/sourceUrl/license/expectedVersion`，完成授权校验、公共资源复制和版本递增。

管理接口契约固定如下：

- 列表项包含 `templateId/templateCode/name/sourceCategory/templateType/dataStatus/procurementReady/imageRightsStatus/imageUrl/referencePrice/missingSteps/sourceRevision/version`；其中图片和价格可空。
- 详情在列表项基础上增加 `description/categoryId/tasteTags/mealTags/enabled/sourceRecords/nameAliases/ingredients/cookingSteps`，以及管理员专用的 `internalAssetReviews: [{ assetId, assetStatus, previewAvailable }]`。
- `PUT` 请求包含 `schemaVersion=2`、`expectedVersion`、`name`、可空 `description`、`categoryId`、可空 `referencePrice`、`tasteTags`、`mealTags`、`enabled`、`ingredients` 和 `cookingSteps`。食材项必须携带稳定项 ID、食材 ID、数量状态，以及按该状态约束的可空数量、单位和计算方式；步骤项必须携带稳定项 ID、连续序号、标题、内容、时间、温度、火候和可空组件引用。
- `PUT` 与图片发布成功统一返回 `{ templateId, version, dataStatus, procurementReady }`。`dataStatus` 和 `procurementReady` 由服务端根据保存后的价格、采购字段与模板类型重新计算，客户端值即使出现也必须拒绝。

商户接口沿用 `/api/merchant/dish-template-categories`、`/api/merchant/dish-templates`、详情和导入接口，但只暴露 eligible 成品菜。商户详情可查看公开来源链接、食材和步骤，不返回内部图片、版权审核备注或配料组件内部管理字段。家庭端不新增模板市场接口。

不存在返回 404；组件或待完善模板导入返回 422 并说明具体缺失项；版本变化返回 409；非平台管理员访问管理接口返回 403。所有列表分页上限与现有后台列表一致，步骤始终按 `stepNo` 升序。

### 商户导入

- 导入时复制模板菜品、家庭化采购食材和全部制作步骤。
- 组件不生成商户菜品；被菜品引用的组件流程作为来源说明保留在导入快照中。
- 待完善、缺价格或采购未就绪模板返回明确业务错误，不允许以零价或错误采购量导入。
- 已导入菜品不因后续来源同步而自动覆盖。

### 模板变更审核

- 商户申请同步菜品到来源模板时，食材和制作步骤一并进入审核快照。
- 管理员审核通过后替换模板食材和步骤并递增版本。
- 审核快照升级为 `schemaVersion=2`，包含可空价格、完整食材状态、组件引用和 `cookingSteps`，不包含 `imageUrl`、图片资产 ID 或图片版权字段。商户菜品图片不能借模板同步审核改变；模板图片只能由平台管理员执行独立受控发布。新增步骤快照 DTO，验证连续序号、字段长度、组件逻辑存在性和可空边界。
- 来源身份、来源提交、来源记录、模板类型、图片版权状态、`dataStatus` 和 `procurementReady` 均为服务端所有字段，商户提交不得修改。审核通过时服务端保留来源与版权字段，并根据最终价格、食材、组件和步骤重新计算派生状态。
- 审核通过在一个事务中乐观锁定模板版本，原子替换模板字段、食材和步骤，再递增版本；任一步失败全部回滚。
- 本设计只负责未上线环境的初始化数据生成。未来来源版本更新必须另建向前迁移或管理端同步任务：执行前查询待处理审核、校验模板版本和来源摘要，存在冲突时在写入前整体终止，不允许离线初始化生成器直接连接运行库。

## 前端适配

- 小程序与 PC 模板详情展示制作步骤、来源、采购就绪状态和无图状态。
- 商户模板市场不展示组件和待完善模板。
- 平台管理端增加全部来源数据列表、待完善筛选、组件详情、步骤编辑和来源对比。
- 所有图片组件兼容 `null`，不显示破图。
- 导入完成后的商户菜品编辑页可以看到复制后的制作流程。

具体前端改动范围：

- 小程序：`pages/merchant/dish-templates`、`dish-template-detail`、`dish-template-change-edit`、`dish-template-change-detail`、`dish-edit`，以及 `services/merchant.js`、`utils/dish-template-change.js`。
- PC 商户端：`admin-web/src/views/merchant/DishTemplatesView.vue`、`DishTemplateChangesView.vue`、`api/dishes.js`、`api/dish-template-changes.js` 和模板变更工具。
- PC 平台端：新增平台模板管理列表/详情编辑视图和 `api/admin-dish-templates.js`，在 `router/index.js`、`layouts/AdminLayout.vue` 注册平台菜单；审核差异页增加步骤和采购状态比较。

模板市场、平台管理和审核 DTO 显式允许 `imageUrl/referencePrice=null`。商户正式菜品请求和响应继续要求 `basePrice` 非空，采购清单正式项继续要求可计算数量，不能因模板待完善状态放宽运行中业务契约。待完善价格显示“待完善”而非 `¥0`；空图片渲染固定比例的无图区域；步骤编辑器保持稳定序号并支持时间、火候、温度和组件来源字段。

## 测试与验收

- 固定来源提交必须精确得到 336 个 Markdown 文件、333 个唯一标题、15 个分类、192 个仓库图片文件和 179 个图片声明，并分别报告缺失引用、未使用资源和重复引用。
- 每个来源文件必须落入主模板或持久化别名来源记录；发布门禁要求未解析文件、链接、组件和歧义匹配全部为 0，生成阶段的显式错误不能带入最终 SQL。
- 所有来源成品菜和配料组件均有稳定来源键，不重复。
- 步骤序号连续，关键时间、温度、火候和组件链接不丢失。
- 组件不出现在商户市场，待完善模板不能导入。
- 无图片模板的数据库字段和 API 字段为 `null`。
- 未声明授权的内部图片不会出现在商户或家庭 API。
- 内部图片资产只能由平台管理员按受控 ID 预览；状态不变量、发布事务、拒绝后不可发布及普通 `PUT` 无法越权修改图片均有集成测试。
- 商户导入复制步骤；已有商户菜不被自动覆盖。
- 初始化 SQL 可从空 MySQL 8 数据库执行，无外键，编码和注释测试通过。
- 保存规范化菜谱黄金清单及 SHA-256，完整版本化输入相同时重复生成必须字节一致；测试覆盖非标准 Markdown、三组重复标题、路径重命名、组件环、组件多路径及倍数展开、空倍数阻断采购和失效图片引用。
- 食材状态约束测试覆盖 `VERIFIED` 完整数量、`SOURCE_BATCH/MISSING` 空采购字段、人工沿用审核依据以及不兼容单位不求和。
- 改写发布门禁测试覆盖未审核、要求重写、通过复核和禁止当前时间进入生成产物。
- MySQL 集成测试覆盖完整 Flyway 链、nullable 字段、来源唯一约束、导入 eligibility 和步骤复制；审核集成测试覆盖 schema v2、步骤原子替换和版本冲突。
- 小程序和 PC 测试覆盖 null 图片、null 价格、待完善状态、步骤展示/编辑和平台管理权限。
- 更新 `docs/api-spec.md`、`docs/frontend-api-guide.md`、`docs/database-design.md` 和需求文档中的模板数量、步骤和图片约束。

## 非目标

- 不自动推断来源没有提供的家庭份量、售价、口味标签或餐次标签。
- 不把配料组件作为可点餐菜品。
- 不对已经导入的商户菜品做批量覆盖。
- 不在应用运行时抓取网站。
- 不宣称来源图片具有未声明的授权。
