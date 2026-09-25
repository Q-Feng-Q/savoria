# 家庭厨房菜谱模板 API 规范

本文是菜谱模板市场、商户导入、模板修改审核和平台模板管理的稳定接口契约。所有路径均以 `/api` 为前缀，数据来自 MySQL，不允许前端使用模拟数据替代接口结果。

## 1. 通用约定

请求使用 UTF-8 JSON。除图片预览外，响应统一为：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

认证信息沿用登录会话。商户接口要求当前用户具备商户后台权限；平台接口仅平台管理员可访问。客户端不能提交 `merchantId` 冒充租户。

分页参数 `page` 默认 1，`pageSize` 默认 20、最大 100。分页响应统一包含 `items`、`total`、`page`、`pageSize`。

常用 HTTP 状态：

| HTTP | 含义 | 前端处理 |
| --- | --- | --- |
| 400 | 参数格式、枚举或快照结构错误 | 直接展示 `message` |
| 401 | 未登录或会话失效 | 清理会话并重新登录 |
| 403 | 缺少商户或平台权限 | 禁止进入对应后台 |
| 404 | 模板、菜品、申请或图片资源不存在 | 返回列表并刷新 |
| 409 | 版本过期、重复申请或状态已变化 | 刷新详情后重新操作 |
| 422 | 业务数据不满足导入、审核或采购要求 | 直接展示 `message` |

## 2. 公共枚举

- `templateType`: `DISH` 成品菜；`COMPONENT` 配料组件。组件不能直接导入为商户菜品。
- `sourceType`: `COOK_LIKE_HOC`、`LOCAL_EXTENSION`。
- `dataStatus`: `READY`、`NEEDS_PURCHASE_DATA`、`NEEDS_PRICE`、`NEEDS_BOTH`。
- `quantityStatus`: `VERIFIED`、`SOURCE_BATCH`、`MISSING`、`NOT_APPLICABLE`。
- `calcType`: `FIXED`、`PER_PERSON`；仅 `VERIFIED` 食材可填写。
- `mealTags`: `BREAKFAST`、`LUNCH`、`DINNER`。
- 修改申请状态：`PENDING`、`APPROVED`、`REJECTED`、`WITHDRAWN`。
- 图片资源状态：`INTERNAL_REVIEW`、`PUBLISHED`、`REJECTED`。

模板的 `referencePrice`、`description`、`imageUrl` 可为 null。`procurementReady=false` 表示该模板不能可靠生成采购清单，前端不能自行推断为可采购。商户列表和详情统一返回 `sourceType` 与服务端计算的 `importable`；只有 `importable=true` 才能勾选导入。

## 3. 商户模板市场

### 3.1 分类

```http
GET /api/merchant/dish-template-categories
```

返回启用分类的 `categoryId`、`code`、`name`、`sortOrder`。

### 3.2 模板分页

```http
GET /api/merchant/dish-templates?categoryId=1&keyword=豆角&imported=false&page=1&pageSize=20
```

`categoryId`、`keyword`、`imported` 均可省略。接口展示全部启用的成品模板，包括采购数量或价格尚未完善的来源菜谱。列表项包含模板编码、分类、菜名、简介、公共图片、参考价格、标签、`sourceType`、来源分类、完整状态、`importable`、是否缺少步骤、版本、食材数量和当前商户是否已导入。

### 3.3 模板详情

```http
GET /api/merchant/dish-templates/{templateId}
```

详情包含完整 `ingredients` 和按 `stepNo` 排序的 `cookingSteps`，不因 `importable=false` 而隐藏。图片或价格缺失时对应字段可为 null；没有步骤时 `cookingSteps` 是空数组，不是 null。

食材的数量字段遵循以下规则：

| quantityStatus | quantity/unit/calcType |
| --- | --- |
| `VERIFIED` | 必须有正数数量、单位和 `FIXED`/`PER_PERSON` |
| `SOURCE_BATCH` | 均为 null，原文保存在来源说明字段 |
| `MISSING` | 均为 null |
| `NOT_APPLICABLE` | 均为 null |

### 3.4 选择导入

```http
POST /api/merchant/dish-templates/import
Content-Type: application/json

{"templateIds":[1,2,3]}
```

去重后最大 100 个 ID。服务端只导入启用、类型为 `DISH`、价格不为 null 且 `procurementReady=true` 的模板。导入会复制菜品资料、采购食材和制作步骤；已导入模板进入 `skippedIds`，不会覆盖商户已经修改的副本。

```json
{
  "importedIds": [1, 2],
  "skippedIds": [3],
  "importedCount": 2,
  "skippedCount": 1
}
```

兼容接口 `POST /api/merchant/dish-templates/import-all` 导入全部符合条件且尚未导入的成品模板。新页面优先使用选择导入。

## 4. 商户模板修改申请

### 4.1 直接编辑模板快照

```http
POST /api/merchant/dish-templates/{templateId}/change-requests
```

请求体：

```json
{
  "submitNote": "补充实测步骤和用量",
  "targetSnapshot": {
    "schemaVersion": 2,
    "categoryId": 3,
    "name": "豆角焖面",
    "description": "北方家常做法",
    "imageUrl": "/uploads/images/4c7f.jpg",
    "imageAssetId": 108,
    "removeImage": false,
    "imageRightsConfirmed": true,
    "referencePrice": 18.00,
    "tasteTags": ["家常"],
    "mealTags": ["LUNCH", "DINNER"],
    "sortOrder": 10,
    "enabled": true,
    "ingredients": [{
      "itemId": "ingredient-1",
      "ingredientName": "豆角",
      "ingredientCategory": "蔬菜",
      "quantityStatus": "VERIFIED",
      "quantity": 300.0000,
      "unit": "g",
      "calcType": "FIXED",
      "sourceText": null,
      "sourceQuantityText": null,
      "componentTemplateId": null,
      "componentMultiplier": null,
      "sortOrder": 1
    }],
    "cookingSteps": [{
      "itemId": "step-1",
      "stepNo": 1,
      "title": "焖制",
      "content": "加入面条，小火焖至成熟。",
      "durationSeconds": 600,
      "temperatureText": null,
      "heatLevel": "小火",
      "componentTemplateId": null
    }]
  }
}
```

`schemaVersion` 固定为 2。`ingredients` 至少一项、最多 100 项；`cookingSteps` 最多 100 项并从 1 连续编号。`itemId` 在同一数组内唯一且在前后快照间稳定。快照 UTF-8 编码后最大 1 MB。

`referencePrice` 可为 null。非 `VERIFIED` 食材的 `quantity`、`unit`、`calcType` 必须为 null。组件引用必须存在；填写 `componentTemplateId` 时 `componentMultiplier` 必须大于 0。

图片更换流程：先调用 `POST /api/files/images`，再把返回的相对地址 `url` 和 `fileId` 分别填入 `imageUrl`、`imageAssetId`，并将 `imageRightsConfirmed` 设为 `true`。服务端会校验图片资产属于当前登录账号，审核通过后才会替换模板图片。申请删除图片时设置 `removeImage=true`，且 `imageUrl`、`imageAssetId` 必须为 null。旧客户端省略全部图片字段时保留模板原图。

以下字段不能出现在 v2 快照：`imageSourceUrl`、`imageAuthor`、`imageLicense`、`sourceType`、`sourceKey`、`sourceUrl`、`sourceRevision`、`sourceCategory`、`templateType`、`dataStatus`、`procurementReady`、`imageRightsStatus`。图片来源说明、来源身份、类型和派生状态由服务端维护。

### 4.2 从已导入菜品申请同步

```http
POST /api/merchant/dishes/{dishId}/template-change-requests
Content-Type: application/json

{"submitNote":"采用商户实测数据"}
```

仅 `templateImported=true` 的菜品可调用。服务端从真实菜品、食材和制作步骤生成 v2 快照，不接受客户端覆盖模板 ID。手工菜品、空食材、来源模板停用或已有待审核申请会返回明确错误。

### 4.3 申请列表、详情与撤回

```http
GET /api/merchant/dish-template-change-requests?status=PENDING&keyword=豆角&page=1&pageSize=20
GET /api/merchant/dish-template-change-requests/{requestId}
POST /api/merchant/dish-template-change-requests/{requestId}/withdraw
```

详情返回 `baseSnapshot`、`targetSnapshot`、`baseTemplateVersion`、`currentTemplateVersion` 和 `stale`。仅当前商户可见；仅 `PENDING` 可撤回。

## 5. 平台模板管理

### 5.1 列表与详情

```http
GET /api/admin/dish-templates?page=1&pageSize=20&sourceType=COOK_LIKE_HOC&templateType=DISH&dataStatus=READY&missingImage=false&missingSteps=false
GET /api/admin/dish-templates/{templateId}
GET /api/admin/dish-templates/{templateId}/source-records
```

平台列表可按 `keyword`、`sourceType`、`templateType`、`dataStatus`、`sourceCategory`、`missingImage`、`missingSteps` 筛选。详情包含来源记录、名称别名、完整食材图、步骤和内部图片审核摘要，但不能把内部文件路径返回给客户端。

### 5.2 更新模板业务数据

```http
PUT /api/admin/dish-templates/{templateId}
```

请求使用 `schemaVersion=2` 和 `expectedVersion`，完整提交名称、简介、分类、可空参考价、标签、餐次、启用状态、食材及步骤。平台接口同样拒绝来源、图片、类型和派生状态字段。成功后重新计算 `dataStatus`、`procurementReady` 并递增版本；版本不一致返回 409。

### 5.3 内部图片审核

```http
GET /api/admin/dish-template-assets/{assetId}/preview
POST /api/admin/dish-templates/{templateId}/image-promotion
POST /api/admin/dish-templates/{templateId}/image-assets/{assetId}/reject
```

这些接口仅平台管理员可调用。`preview` 只接受受控 `assetId`，不接受文件路径；只有 `INTERNAL_REVIEW` 或 `PUBLISHED` 资源可按服务端规则访问，`REJECTED` 不可预览。

发布请求：

```json
{
  "internalAssetId": 71,
  "author": "图片作者",
  "sourceUrl": "https://example.com/source-page",
  "license": "明确授权说明",
  "expectedVersion": 4
}
```

只有具备作者、来源页和许可证声明的内部图片可转为 `PUBLISHED`，成功后生成公共图片地址并递增模板版本。驳回请求必须填写原因，状态变为 `REJECTED`。

## 6. 平台修改审核

```http
GET /api/admin/dish-template-change-requests?status=PENDING&merchantId=21&templateId=5&page=1&pageSize=20
GET /api/admin/dish-template-change-requests/{requestId}
POST /api/admin/dish-template-change-requests/{requestId}/approve
POST /api/admin/dish-template-change-requests/{requestId}/reject
```

通过请求的 `reason` 可选；驳回原因必填。审批会锁定申请和模板版本，保留图片、来源、模板类型等服务端字段，原子替换可编辑主信息、食材和步骤，重新计算采购就绪状态，并向商户通知中心写入结果。申请过期或状态已变化返回 409。

## 品牌标识配置（2026-09-16）

`GET /api/public/system-settings` 与管理员配置响应增加以下公开品牌字段（公开响应仍不包含 SMTP、密码或审计信息）：

| 字段 | 说明 | 默认与限制 |
| --- | --- | --- |
| siteName | 站点名称 | 默认食光栀味，最长 100 字符 |
| siteLogoUrl | 标准 Logo，兼容原字段 | 最长 500 字符 |
| siteLogoSmallUrl | 小号 Logo | 最长 500 字符；为空回退标准 Logo，再回退内置图 |
| siteLogoLargeUrl | 大号 Logo | 最长 500 字符；为空回退标准 Logo，再回退内置图 |
| siteFaviconUrl | 浏览器图标 | 最长 500 字符；为空使用内置 favicon.ico |
| siteLogoSmallSize | 小号设计像素边长 | 整数 16～64，数据库空值返回 32 |
| siteLogoSize | 标准设计像素边长 | 整数 24～120，数据库空值返回 56 |
| siteLogoLargeSize | 大号设计像素边长 | 整数 48～160，数据库空值返回 96 |

```http
PATCH /api/admin/system-settings/branding
Content-Type: application/json

{"siteName":"食光栀味","siteLogoSmallUrl":"/uploads/brand-small.png","siteLogoSmallSize":32}
```

此入口继续仅允许平台管理员，仅更新非 null 的品牌字段；未提交或 null 表示保留，空 URL 字符串表示清除。尺寸恢复默认需显式提交 32、56、96，不接受小数或字符串。`siteName` 若提交不得为空白。返回更新后的完整管理员配置（SMTP 密码仍脱敏），保留系统配置审计并清除缓存。不会读出旧配置再整体覆盖邮件、维护或绑定开关。

现有 `PUT /api/admin/system-settings` 同样支持这些字段；旧客户端不提交新增字段或提交 null 时保留数据库值。原有标准 Logo 字段继续沿用全量 PUT 的原语义。

四种资源 URL 仅接受具有有效主机的 HTTPS URL 或以单斜杠开头的后端站点相对路径；拒绝协议相对 `//`、控制字符、空格、反斜杠、userinfo、javascript/data/file/http URL。后端不下载资源。客户端使用 API 基址解析相对路径，小程序域名限制由部署配置保障。

迁移文件 `V4__responsive_branding.sql` 新增可空字段，只将旧默认名“食光知味”改为“食光栀味”，不覆盖其他自定义名称。该文件需另行授权部署，本次不执行业务库迁移；应在部署读取新列的后端版本前应用迁移。

## 普通菜品与滋补品（2026-09-19）

菜品及成品菜模板新增以下四个平级字段，贯通商户菜品列表/详情/编辑、家庭菜单/详情、商户模板市场、平台模板管理及模板修改申请的基线/目标快照：

| 字段 | 约束 |
| --- | --- |
| `productType` | `NORMAL`（普通菜品）或 `NOURISHMENT`（滋补品）；新增默认 `NORMAL`，历史空值读取为 `NORMAL`；其他值及空字符串拒绝 |
| `nourishmentDescription` | 可选滋补介绍，纯文本，最多 1000 字符 |
| `servingAdvice` | 可选食用建议，纯文本，最多 1000 字符 |
| `precautions` | 可选注意事项，纯文本，最多 1000 字符 |

新增菜品时未提供文字为空；更新时缺失或 `null` 保留持久化原值，显式空字符串或纯空白清空对应文字。文字去首尾空白、保留内部换行。切回 `NORMAL` 保留滋补文字，客户端仅在 `NOURISHMENT` 且内容非空时展示；不能按 HTML 渲染。类型和既有分类独立，价格、上下架、推荐、家庭启用、身份与商户隔离规则不变。配料组件 `COMPONENT` 不接受滋补类型或滋补介绍，不新增点餐资格。

以下 GET 接口可选查询参数 `productType=NORMAL|NOURISHMENT`，省略表示全部，非法值返回 400：

- `/api/merchant/dishes`：与 `scope` 共同在 SQL 查询阶段过滤。
- `/api/merchant/dish-templates`：与分类、关键词、导入状态共同过滤；先过滤再分页，`total` 为完整匹配数量。
- `/api/admin/dish-templates`：与已有筛选共同过滤；设置商品类型时只返回 `DISH` 成品模板，分页与总数使用相同类型条件。
- `/api/family/menu`：与关键词、分类共同过滤该家庭全部已启用且上架的菜单，不局限推荐菜；原推荐排序仍保留。该接口非分页。

模板首次导入复制四字段，从已导入商户菜品申请更新模板时同样包含四字段；不新增自动覆盖商户修改的同步任务。模板快照仍使用 `schemaVersion=2`。新申请将缺失/`null` 的新增字段从当前模板补齐；空内容在目标快照中以空字符串表示显式清空。历史申请读取不改写已存基线和目标，批准时缺失/`null` 的新增字段保留当前值；原模板 `version` 乐观并发校验保持不变。普通菜品审核也保留旧客户端未提供的新增字段。新增文字不进入历史订单快照。

本项目尚未部署：经用户明确要求，四列直接加入 `V1__init_schema.sql` 中 `dishes` 与 `dish_templates` 的建表定义；没有新增滋补 ALTER 迁移，不修改 V2～V4 的既有内容。本次未对业务数据库执行建表或迁移。

## 用户 BUG / 建议反馈

以下地址沿用部署层 `/api` 前缀，控制器路径自身不重复添加 `/api`。除图片字节响应外均返回 `{ code, message, data }`。所有接口使用 `Authorization: Bearer ...`，未加入家庭的账号也能使用用户接口；反馈属于账号，不属于家庭或商户。

| 方法与路径 | 请求 | data / 权限 |
| --- | --- | --- |
| `POST /api/feedback/images` | multipart 字段 `file` | `{ imageId: "UUID" }`；任意已登录账号 |
| `GET /api/feedback/images/{imageId}` | 无 | JPEG/PNG 字节；附件上传者，或已绑定反馈附件的平台管理员 |
| `POST /api/feedback` | `{ requestId, type, content, imageIds }` | 完整用户详情 |
| `GET /api/feedback` | `page=1&pageSize=20` | 当前账号 `{ items, total, page, pageSize }` |
| `GET /api/feedback/{feedbackId}` | 无 | 当前账号详情，其他账号返回 404 |
| `GET /api/admin/feedback` | `page=1&pageSize=20&type=BUG&status=OPEN`，筛选可省略 | 平台管理员 `{ items, total, page, pageSize }` |
| `GET /api/admin/feedback/{feedbackId}` | 无 | 平台详情及 `history` |
| `PUT /api/admin/feedback/{feedbackId}` | `{ status, reply, version }` | 更新后的平台详情及 `history` |

用户详情/列表项为 `{ feedbackId, type, content, status, reply, version, createdAt, updatedAt, images: [{ imageId }] }`。平台列表和详情额外含 `ownerUserId`、`ownerName`，平台详情额外含 `history: [{ historyId, adminId, fromStatus, toStatus, reply, createdAt }]`。初始 `status=OPEN`、`reply=""`、`version=0`。列表按创建时间、ID 倒序；页码从 1 开始，每页默认 20、最多 100。用户响应不包含附件磁盘位置、处理人或处理历史。

`type` 为 `BUG | SUGGESTION`；描述去首尾空白后 1～2000 字符；`imageIds` 可省略，最多 6 个、不允许重复，必须均为本账号未绑定且未过期的反馈图片。`requestId` 为 1～80 位字母、数字、下划线或连字符，客户端为一次逻辑提交生成并在网络重试时复用。同账号同 requestId 的相同规范化内容及相同图片顺序返回原反馈；内容变化返回 409。不同请求不能复用已绑定附件。

状态为 `OPEN | PROCESSING | RESOLVED | CLOSED`。管理员可重新打开；回复为去首尾空白的纯文本，最多 2000 字符，已解决/已关闭时必填。提交的 `version` 必须等于读取版本，否则返回 409 并要求刷新。成功更新递增版本并写审计历史。普通商户不能访问平台端点。

图片只接受经 MIME、签名、尺寸及解码验证的 JPEG/PNG；单文件最多 4 MiB、总像素不超过 2000 万。multipart 请求总上限为 5 MiB，留出边界开销。按账号串行检查成功创建记录：每小时最多 10 条反馈、30 张图片；超限返回 409 和可读提示，幂等重试不消耗配额。上传配额在解码与落盘前检查。

在 multipart 解析阶段触发的大小超限返回 HTTP 413、业务码 `41301` 及明确大小提示，不再归为系统 500 错误。

私有根配置 `family-kitchen.feedback.private-root`（环境变量 `FAMILY_KITCHEN_FEEDBACK_PRIVATE_ROOT`），默认 `./data/feedback-private`，不能与公开上传目录互相包含；目录使用真实路径校验。图片无公开 URL，响应含 `Cache-Control: no-store` 与 `X-Content-Type-Options: nosniff`。未绑定附件 24 小时过期；清理默认每小时运行，使用附件行锁重新检查未绑定状态，失败保留记录以便下次重试。登记失败补偿删除文件，补偿失败的随机对象文件在 24 小时后由专用孤儿扫描重试，扫描不处理非本模块命名文件。

初始化表为 `user_feedback`、`feedback_images`、`feedback_history`，按尚未部署要求直接加入 V1 建表文件；本功能不新增 ALTER，不对业务数据库执行初始化。

## 制作步骤图片（2026-09-23）

菜品、模板详情、家庭菜品详情及模板/菜品审核快照的每个 `cookingSteps[]` 新增有序 `imageUrls`：

```json
{"stepNo":1,"title":"处理食材","content":"清洗后切块。","imageUrls":["/uploads/images/a.jpg","/uploads/images/b.jpg"]}
```

- 每个步骤允许 0～5 张。现有 `POST /api/files/images` 上传返回 `url`，保存原始资源路径，不能保存客户端临时路径。上传支持 JPEG/PNG/WebP，单文件最多 4 MiB；沿用原有鉴权。
- 后端在创建、编辑、模板申请和审批应用时校验数量与 URL。支持 `/uploads/images/`、`/uploads/dish-template-assets/`、`/images/` 或无凭据的 HTTPS 地址，最长 2048 字符；拒绝控制字符、路径穿越及危险协议。超量/非法地址返回业务参数错误。
- 显式 `imageUrls: []` 清空该步骤图片。旧数据在展示响应中返回空数组；旧写入请求和历史快照保留“未提供”语义，不自动当作清空。
- 模板缺失字段按稳定步骤键匹配保留；兼容旧客户端基于数据库步骤 ID 生成的键。商户菜品步骤有图但旧请求缺失图片字段时，仅允许步骤数量、顺序及元数据未变化的写入；歧义请求拒绝并提示刷新。缺失整个 `cookingSteps` 保留当前步骤；显式 `cookingSteps: []` 表示删除所有步骤。
- 新审核快照保存前解析缺失字段，历史快照批准前完成兼容检查；模板导入、组件展开及状态调整保留对应步骤图片。移除引用不删除原图片文件。
- 数据库前置条件（2026-09-23 空库基线合并）：`V1__init_schema.sql` 已在 `dish_cooking_steps` 与 `dish_template_cooking_steps` 中直接定义可空 JSON 列 `image_urls`；原 V5 已移除。仅供全新空库初始化，不可直接套用已有 Flyway 历史库。未执行当前业务库重建；部署需同步发布后端和客户端。
