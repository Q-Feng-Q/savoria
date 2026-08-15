# 模板菜修改审核设计

## 1. 目标

商户可以针对平台模板菜提交完整修改方案，由平台管理员审核。审核通过后，方案在同一事务内覆盖系统模板主信息和全部食材；审核驳回或撤回时不修改模板。已由商户导入的菜品是独立副本，不随模板更新。

## 2. 权限与边界

- 商户接口要求已认证用户具有商户后台管理权限，`merchantId` 只能从 `CurrentUserContext` 取得，所有请求 DTO 均不接收 `merchantId`。
- 商户查询、详情和撤回 SQL 必须同时使用 `requestId + merchantId` 限定租户；未匹配时统一返回不存在，避免泄露其他商户申请。
- 平台接口要求 `hasPlatformBackendAccess()`，只有平台管理员可以查询全部申请并执行通过或驳回。
- 提交目标必须是 `dish_templates` 中的平台模板，不接受商户菜品 ID。
- 模板 `id`、`templateCode`、`createdAt` 是稳定技术身份，不允许申请覆盖。
- 可覆盖字段为 `categoryId`、`name`、`description`、`imageUrl`、`imageSourceUrl`、`imageAuthor`、`imageLicense`、`referencePrice`、`tasteTags`、`mealTags`、`sortOrder`、`enabled` 和完整 `ingredients`。
- 模板不包含制作流程，申请也不接收制作流程。
- 审核通过不更新已有 `dishes.source_template_id` 对应的商户菜品副本。
- Controller 使用 springdoc `@SecurityRequirement` 声明 Bearer Token，并使用现有统一认证拦截和中文错误响应。

## 3. 数据模型

V8 为 `dish_templates` 增加 `version bigint NOT NULL DEFAULT 0 COMMENT '模板并发版本号'`。V7 已用于家庭餐次默认值回填；任何模板业务字段或模板食材修改都必须在同一事务内递增该版本，不能只依赖 `updated_at`。

V8 新增 `dish_template_change_requests` 表，不创建物理外键：

| 字段 | MySQL 定义 | 含义 |
| --- | --- | --- |
| `id` | `bigint PRIMARY KEY AUTO_INCREMENT` | 申请 ID |
| `merchant_id` | `bigint NOT NULL` | 提交商户 ID |
| `template_id` | `bigint NOT NULL` | 目标平台模板 ID |
| `base_template_version` | `bigint NOT NULL` | 提交时模板并发版本 |
| `base_snapshot_json` | `json NOT NULL` | 提交时原模板完整业务快照 |
| `snapshot_json` | `json NOT NULL` | 申请覆盖后的完整业务快照 |
| `submit_note` | `varchar(500) NULL` | 提交说明 |
| `status` | `varchar(16) NOT NULL DEFAULT 'PENDING'` | `PENDING/APPROVED/REJECTED/WITHDRAWN` |
| `submitted_by` | `bigint NOT NULL` | 提交用户 ID |
| `reviewed_by` | `bigint NULL` | 审核平台管理员 ID |
| `review_reason` | `varchar(500) NULL` | 审核意见；驳回时必填 |
| `submitted_at` | `datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | 提交时间 |
| `reviewed_at` | `datetime(6) NULL` | 通过或驳回时间 |
| `withdrawn_by` | `bigint NULL` | 撤回用户 ID |
| `withdrawn_at` | `datetime(6) NULL` | 撤回时间 |
| `result_notification_id` | `bigint NULL` | 终态结果通知 ID |
| `pending_marker` | `tinyint GENERATED ALWAYS AS (...) STORED` | `PENDING` 时为 1，其他状态为 `NULL` |

生成列表达式为 `CASE WHEN status='PENDING' THEN 1 ELSE NULL END`；增加 `CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN'))`；唯一索引为 `(merchant_id,template_id,pending_marker)`。普通索引为 `(status,submitted_at,id)`、`(merchant_id,status,submitted_at,id)` 和 `(template_id,status,submitted_at,id)`。表及字段使用中文注释，表选项为 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`，不创建物理外键。

实体沿用项目现状使用 MyBatis-Plus `@TableName/@TableId/@TableField` 描述表映射，同时 Mapper 接口和 XML 使用 `resultMap` 执行 SQL；Service 直接调用 Mapper，保持传统 Controller-Service-Mapper 分层。

历史申请永久保留。商户失效不影响管理员查看历史；模板或分类不存在、停用时，待审核申请不可通过，只能驳回或由原商户撤回。

## 4. 快照数据契约

`base_snapshot_json` 和 `snapshot_json` 使用同一结构。提交 DTO 将 `targetSnapshot` 接收为 `JsonNode`，再由专用快照校验器使用开启 `FAIL_ON_UNKNOWN_PROPERTIES` 的独立 Jackson Reader 转为强类型快照，避免改变其他既有接口的全局 JSON 行为。所有字段必须出现且除明确说明外不允许 `null`：

| JSON 字段 | Java 类型 | 规则 |
| --- | --- | --- |
| `schemaVersion` | `Integer` | 必填，固定为 `1` |
| `categoryId` | `Long` | 必填，正整数，分类必须存在且启用 |
| `name` | `String` | 去首尾空白后 1 至 100 字符 |
| `description` | `String` | 去首尾空白后 1 至 255 字符 |
| `imageUrl` | `String` | 1 至 500 字符，只允许 `/uploads/` 或 `/images/` 本地路径 |
| `imageSourceUrl` | `String` | 1 至 1000 字符 |
| `imageAuthor` | `String` | 1 至 255 字符 |
| `imageLicense` | `String` | 1 至 255 字符 |
| `referencePrice` | `BigDecimal` | `0.00` 至 `99999999.99`，最多 2 位小数 |
| `tasteTags` | `List<String>` | 0 至 10 项，每项 1 至 20 字符，去空去重 |
| `mealTags` | `List<String>` | 0 至 3 项，只允许 `BREAKFAST/LUNCH/DINNER`，去重 |
| `sortOrder` | `Integer` | `-1000000` 至 `1000000` |
| `enabled` | `Boolean` | 必填 |
| `ingredients` | `List<IngredientSnapshot>` | 1 至 100 项，按 `sortOrder` 和数组顺序保存 |

`IngredientSnapshot` 的完整字段为：`ingredientName`（1 至 100 字符）、`ingredientCategory`（1 至 50 字符）、`quantity`（`0.00` 至 `99999999.99`，最多 2 位小数）、`unit`（1 至 20 字符）、`calcType`（`FIXED/PER_PERSON/NO_PURCHASE`）和 `sortOrder`（`-1000000` 至 `1000000`）。`FIXED/PER_PERSON` 数量必须大于 0；`NO_PURCHASE` 数量必须为 0，单位仍必填用于展示。食材名称按去首尾空白后的不区分大小写值判重。

完整替换请求缺少任一字段、包含未知字段、目标快照 UTF-8 JSON 超过 1 MB 或使用不支持的 `schemaVersion` 时返回具体中文 `400` 错误。模板和食材技术 ID、创建时间不进入快照。

## 5. 状态机与并发

```text
PENDING -> APPROVED
PENDING -> REJECTED
PENDING -> WITHDRAWN
```

- 只有 `PENDING` 可处理，重复审核或撤回返回 `409 模板菜品修改申请已处理`。
- `approve`、`reject`、`withdraw` 分别使用独立 `@Transactional` Service 方法，均先 `SELECT ... FOR UPDATE` 锁定申请，再在锁内校验权限和状态。
- 统一锁顺序为“申请 → 模板 → 目标分类”；通过时继续用 `SELECT ... FOR UPDATE` 锁定模板和目标分类，分类变更流程不得采用相反锁顺序。驳回和撤回不锁模板及分类。
- 商户只能撤回自己商户的申请，SQL 使用 `id + merchant_id` 限定。
- 驳回必须填写非空原因；通过时审核备注可空。
- 状态变更后保留完整快照，不删除审核记录。
- 只有成功取得状态转换权的事务可以创建通知，其他并发请求直接返回 `409`，不会重复通知。

## 6. 提交与校验

提交采用完整替换语义，不支持部分字段省略：

提交使用单一 `@Transactional` Service 方法：

1. 校验商户后台权限和从认证上下文取得的 `merchantId`。
2. 按第 4 节规则规范化并校验完整目标快照。
3. 使用 `SELECT ... FOR UPDATE` 锁定平台模板，校验模板存在；校验目标分类存在且启用。
4. 在保持模板行锁期间读取全部当前食材，将模板和食材序列化为 `baseSnapshotJson`，并从同一锁定记录读取 `baseTemplateVersion`，保证原快照与版本来自同一数据时点。
5. 查询相同商户和模板是否已有待审核申请，仅用于友好预检。
6. 在同一事务内插入 `PENDING` 申请；数据库唯一索引作为并发最终防线。捕获该唯一索引冲突并返回 `409 本商户对该模板已有待审核修改申请`，不得暴露 SQL 异常。

审批时重新校验 `snapshot_json`；`base_snapshot_json` 仅用于审核差异对比和历史展示，不作为覆盖来源。

图片继续通过现有上传接口获取本地 URL，提交接口不接收文件流。

## 7. 审核事务与通知

审核通过使用单一 `@Transactional` Service 方法：

1. `SELECT ... FOR UPDATE` 锁定申请，确认状态为 `PENDING`。
2. 锁定目标模板并读取当前 `version`。
3. 若当前版本不等于 `baseTemplateVersion`，返回 `409 模板菜品已发生变化，请重新提交`，申请保持待审核，便于管理员驳回或商户撤回重提。
4. 解析并重新校验快照；使用 `SELECT ... FOR UPDATE` 锁定目标分类并确认仍存在且启用，防止校验后并发停用或删除。
5. 更新模板全部可变字段。
6. 删除模板现有食材并按快照顺序重新写入。
7. 将模板 `version` 原子递增 1。
8. 将申请更新为 `APPROVED`，记录管理员、备注和审核时间。
9. 向申请所属 `merchantId` 写入审核结果通知，正文包含申请 ID、模板名称、结果、审核原因和详情入口，并将通知 ID 写回 `result_notification_id`。

通知记录与状态转换处于同一数据库事务，申请行本身是通知幂等边界；通知失败则审核事务回滚，版本冲突和状态冲突不产生通知。当前一期只有站内通知，不调用外部推送；未来增加外部推送时使用同事务 outbox。

驳回事务锁定申请、校验 `PENDING`、写入 `REJECTED` 和必填原因并创建结果通知。撤回事务锁定申请后写入 `WITHDRAWN`、`withdrawnBy/withdrawnAt`；撤回不创建商户结果通知。提交申请不额外创建平台通知，平台后台通过待审核列表形成待办。

## 8. 接口

### 7.1 商户端

```http
POST /merchant/dish-templates/{templateId}/change-requests
GET  /merchant/dish-template-change-requests?status=PENDING&keyword=&page=1&pageSize=20
GET  /merchant/dish-template-change-requests/{requestId}
POST /merchant/dish-template-change-requests/{requestId}/withdraw
```

提交 DTO 明确定义为 `{ "submitNote": String|null, "targetSnapshot": JsonNode }`，通过 springdoc `@Schema(implementation = DishTemplateSnapshotRequest.class)` 向前端展示强类型结构；`submitNote` 最长 500 字符。完整字段、缺失字段、未知字段和 1 MB 规则只应用于 `targetSnapshot`；持久化时只将校验后的强类型快照重新序列化到 `snapshot_json`，提交说明单独写入 `submit_note`。响应返回申请 ID、状态和提交时间。列表支持状态、模板名称、页码和每页数量筛选。详情返回 `baseSnapshot`、`targetSnapshot`、当前模板版本、`stale` 标志及审核信息，不能将当前模板描述为提交时原模板。

### 7.2 平台管理端

```http
GET  /admin/dish-template-change-requests?status=PENDING&merchantId=&templateId=&keyword=&page=1&pageSize=20
GET  /admin/dish-template-change-requests/{requestId}
POST /admin/dish-template-change-requests/{requestId}/approve
POST /admin/dish-template-change-requests/{requestId}/reject
```

通过 DTO 为 `{ "reason": String|null }`，`reason` 去首尾空白后为空则规范化为 `null`，非空时最长 500 字符；驳回 DTO 为 `{ "reason": String }`，去首尾空白后必须为 1 至 500 字符。商户和平台列表的 `keyword` 均按模板名称模糊匹配。列表参数要求 `page >= 1`、`1 <= pageSize <= 100`，默认按 `submitted_at DESC,id DESC`。所有 Controller 使用 springdoc `@Tag`、`@Operation`、`@Parameter`、`@SecurityRequirement`、`@ApiResponse` 和 DTO `@Schema`。

所有接口使用统一 `ApiResponse`。参数或 JSON 错误返回 `400`，未认证返回 `401`，越权返回 `403`，资源不存在返回 `404`，重复申请、已处理和版本冲突返回 `409`。全局异常处理器负责把 Bean Validation、JSON 解析、枚举转换、唯一键和未知异常转换为 UTF-8 中文消息。

## 9. 错误消息

- `模板菜品不存在`
- `模板菜品分类不存在或已停用`
- `本商户对该模板已有待审核修改申请`
- `模板菜品修改申请不存在`
- `模板菜品修改申请已处理`
- `无权操作该模板菜品修改申请`
- `驳回原因不能为空`
- `模板菜品已发生变化，请重新提交`
- `模板菜品快照版本不支持`
- `模板菜品修改内容不完整`
- 字段和食材校验返回具体中文错误，不暴露 SQL 或框架异常。

## 10. 测试与文档

- 数据库迁移测试：表注释、字段注释、索引字段、无物理外键、迁移顺序、非法状态被 CHECK 拒绝及生成列待审核唯一约束。
- Service 测试：提交双快照、重复待审核、越权查询/撤回、状态机、驳回原因、旧快照版本和全部字段边界。
- 审核通过测试：全部主字段覆盖、旧食材删除、新食材完整写入、模板版本递增、只有食材变化也触发版本冲突、事务异常回滚、已有商户菜品不更新。
- 并发测试：双提交唯一索引竞争、通过/驳回/撤回竞争、两个商户申请同一模板后的审批竞争、通知失败回滚和终态通知唯一性。
- 使用真实 MySQL 测试环境验证生成列、JSON、唯一索引、`SELECT FOR UPDATE` 和事务行为，不以 H2 替代这些数据库契约。
- Controller 路由和权限测试：租户过滤、商户端与平台管理端权限、中文错误、springdoc 注解和 `/v3/api-docs` 契约。
- 更新 `api-spec.md`、`frontend-api-guide.md`、`database-design.md` 和 `data-dictionary.md`。
