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

模板的 `referencePrice`、`description`、`imageUrl` 可为 null。`procurementReady=false` 表示该模板不能可靠生成采购清单，前端不能自行推断为可采购。

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

`categoryId`、`keyword`、`imported` 均可省略。列表项包含模板编码、分类、菜名、简介、公共图片、参考价格、标签、来源分类、完整状态、是否缺少步骤、版本、食材数量和当前商户是否已导入。

### 3.3 模板详情

```http
GET /api/merchant/dish-templates/{templateId}
```

详情包含完整 `ingredients` 和按 `stepNo` 排序的 `cookingSteps`。图片或价格缺失时对应字段可为 null；没有步骤时 `cookingSteps` 是空数组，不是 null。

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

以下字段不能出现在 v2 快照：`imageUrl`、`imageSourceUrl`、`imageAuthor`、`imageLicense`、图片资源 ID、`sourceType`、`sourceKey`、`sourceUrl`、`sourceRevision`、`sourceCategory`、`templateType`、`dataStatus`、`procurementReady`、`imageRightsStatus`。图片、来源、类型和派生状态由服务端维护。

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
