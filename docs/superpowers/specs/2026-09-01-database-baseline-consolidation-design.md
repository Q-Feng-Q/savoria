# 数据库初始化基线收口设计

## 目标

项目尚未正式上线，不需要兼容任何生产历史库。将当前 `V1` 至 `V11` 的最终有效结构和真实初始化数据收口为四个可从空 MySQL 数据库一次执行成功的 Flyway 基线脚本，删除旧库回填、切换和迁移运行设施，确保数据库结构与当前 MyBatis 业务代码一致。

## 方案选择

采用“重写空库基线”方案：

- `V1` 负责用户、角色、商户、家庭、地址、餐次和账号安全相关表。
- `V2` 负责菜品、菜单、餐篮、订单、家庭钱包、采购和业务幂等相关表。
- `V3` 负责系统设置、通知、上传记录以及管理员、默认商户、菜品分类和食材字典等真实初始化数据。
- `V4` 负责平台菜品模板、模板分类、模板食材、模板变更审核以及全部 240 道模板数据。

删除 `V5` 至 `V11`。它们的最终业务结构和有效数据进入对应初始化脚本，历史数据更新、临时表、回填语句和迁移控制表不进入新基线。

## 最终结构归并

### V1：身份、家庭与商户

- 将 `V9` 新增的 `families.featured_dish_id` 直接写入 `families` 建表语句，并保留索引。
- 保留统一用户体系、单家庭成员关系、商户身份关系、登录会话、找回密码、邮箱和微信绑定、安全审计等当前结构。
- 不创建物理外键，关联完整性继续由 Service 逻辑校验。
- 家庭餐次不再依赖 `V7` 对历史家庭回填。创建家庭时由现有业务逻辑生成早餐、午餐、晚餐；空库初始化不插入虚构家庭。

### V2：菜品、餐篮、订单、钱包与采购

- 将 `V10` 新增的 `dishes.featured_at` 直接写入 `dishes`，保留商户推荐索引。
- 将 `V11` 对 `carts`、`orders` 和 `order_items` 的最终字段定义直接写入建表语句，包括家庭餐篮、期望用餐时间、来源餐篮和乐观锁字段。
- 将 `cart_item_selections`、`order_item_selections`、`family_wallets`、`family_wallet_ledgers`、`family_wallet_order_holds` 和 `command_idempotency` 直接创建为正式业务表。
- 删除仅用于个人钱包向家庭钱包切换的迁移追踪表、迁移批次、异常表、运行器租约、应用实例租约和切换状态表。
- `member_wallets` 和 `wallet_ledgers` 仍被当前正式业务 Mapper 使用，本次明确保留；后续如要收缩个人钱包，应另立需求并先改业务代码，不能在本次基线收口中顺带删除。
- 不执行 `V10` 的推荐菜历史回填，也不执行 `V11` 的数据迁移。新库没有历史数据，推荐菜和家庭菜单由正常业务流程维护。

### 最终表清单

`V1` 保留 20 张业务表：`users`、`merchants`、`merchant_applications`、`merchant_user_relations`、`merchant_invitation_codes`、`user_role_relations`、`families`、`family_user_relations`、`family_addresses`、`meal_slots`、`family_applications`、`family_invitations`、`family_invitation_codes`、`family_membership_requests`、`user_sessions`、`password_reset_records`、`email_verification_records`、`wechat_authorization_records`、`account_cancellation_requests`、`security_audit_logs`。

`V2` 保留 24 张业务表：`dish_categories`、`dishes`、`dish_ingredients`、`dish_cooking_steps`、`merchant_ingredients`、`family_menu_items`、`dish_review_submissions`、`carts`、`cart_items`、`cart_item_selections`、`orders`、`order_items`、`order_item_selections`、`order_delivery_snapshots`、`order_member_charges`、`member_wallets`、`wallet_ledgers`、`family_wallets`、`family_wallet_ledgers`、`family_wallet_order_holds`、`command_idempotency`、`purchase_lists`、`purchase_list_items`、`temp_purchase_items`。

`V3` 保留 4 张业务表：`system_settings`、`notifications`、`notification_dispatches`、`file_assets`。

`V4` 保留 4 张业务表：`dish_template_categories`、`dish_templates`、`dish_template_ingredients`、`dish_template_change_requests`。

明确删除 8 张只服务历史迁移的表：`family_wallet_migration_batches`、`family_wallet_migration_sources`、`family_cart_migration_sources`、`family_wallet_migration_anomalies`、`family_wallet_cutover_state`、`application_instance_leases`、`family_wallet_migration_runner_lease`、`family_wallet_migration_families`。

### V3：系统与真实初始化数据

- 保留系统配置、通知中心、上传文件等最终表结构。
- 保留固定管理员账号、管理员角色、默认商户和商户管理员关系。
- 保留真实菜品分类和食材字典，每条初始化数据维持一条独立 `INSERT`。
- 不插入测试家庭、测试订单、测试钱包、测试菜品或其他演示数据。
- 管理员密码继续使用 BCrypt 摘要，默认密码仍为项目当前约定的 `123456`，部署前可通过正式修改密码流程更换。

### V4：模板市场

- 将 `V5` 的 42 道地域菜品和食材追加到 `V4`，与原有 198 道模板组成 240 道模板。
- 将 `V6` 的最终图片来源、作者和授权信息直接修正到对应模板的插入语句，不保留后置 `UPDATE`。
- 将 `V8` 的 `dish_templates.version` 和 `dish_template_change_requests` 直接写入模板基线。
- 模板数据继续使用本地图片路径及真实来源和授权信息，不新增模拟模板。

## 迁移代码清理

删除仅为已存在数据库升级服务的 `com.familykitchen.migration` 运行器、Mapper XML、切换屏障、实例租约和对应测试。同步清理启动配置、切面、过滤器及钱包代码中对迁移状态的依赖。

正式业务需要的家庭钱包、幂等、餐篮和订单服务保留。清理后，应用启动只依赖 Flyway 从空库执行 `V1-V4`，不再存在第二套 Java 数据迁移流程。

清理验收要求为：代码、Mapper XML、配置和测试中不再出现 `com.familykitchen.migration`、上述 8 张迁移表、迁移屏障、迁移过滤器、迁移切面、`family-kitchen.migration.*` 和 `family-kitchen.instance.*`。`FamilyKitchenApplication`、`NormalSchedulingConfiguration`、`AdminAccountInitializer` 及订单服务中与迁移模式耦合的条件或依赖同步改为普通启动逻辑。应用上下文必须能在未提供任何迁移模式或实例租约配置时正常启动。

## 开发数据库重建边界

重写已存在的 `V1-V4` 会改变 Flyway checksum，删除 `V5-V11` 也无法对已经执行过旧迁移的数据库做原地升级。因此本次基线只支持空库初始化：现有本地开发数据库必须由开发者明确确认后删除并重新创建，再由 Flyway 执行新的 `V1-V4`。

本次不自动删除任何数据库，不对名称不明确的数据库执行清理，也不支持用 `flyway repair`、`baselineOnMigrate` 或手工修改 `flyway_schema_history` 绕过校验。文档需给出先核对数据库名称、再备份需要的数据、最后重建空 schema 的操作步骤。

## 测试与错误边界

- 更新 `FreshDatabaseMigrationTest`，只接受 `V1-V4`，并检查无物理外键、所有表和字段有中文注释、索引字段真实存在。
- 将原先针对 `V5-V11` 文件文本的测试改为针对最终基线结构和数据的测试；纯历史回填测试删除。
- 重构前从旧 `V1-V11` 的空库执行结果生成受版本控制的最终结构清单和初始化数据摘要。重构后在 MySQL/Testcontainers 中执行新 `V1-V4`，逐表比较全部保留表的字段名、类型、精度、可空性、默认值、生成属性、字段注释、表注释、主键、唯一键、普通索引、检查约束、字符集和排序规则，防止折叠时静默丢失最终结构。
- 初始化数据按完整行或确定性摘要校验：1 条系统设置、1 个管理员用户、1 条管理员角色关系、1 个默认商户、1 条商户管理员关系、10 个商户菜品分类、73 条商户食材、9 个模板分类、240 道模板、403 条模板食材，以及 `V6` 修正过的全部图片来源、作者和授权字段。管理员 BCrypt 摘要必须可由 `123456` 验证通过，同时断言不存在清单之外的初始化业务数据。
- MySQL 验证通过 `information_schema` 检查所有业务表均为 `utf8mb4/utf8mb4_0900_ai_ci`、物理引用约束数量为 0，并精确核对关键检查约束和索引。
- SQL 文件使用严格 UTF-8 解码，拒绝 BOM、CRLF 和无末尾 LF 的文件。
- 运行后端完整测试；Docker 不可用时明确记录 Testcontainers 未执行，不能把跳过当作通过。

## 安全边界

- 不修改用户当前未提交的餐篮相关代码。
- 不保留任何生产密码或密钥。
- 不创建物理外键。
- 不使用模拟数据补齐初始化内容。
- 本次只收口当前已经存在的数据库结构和数据，不引入新的菜谱或业务功能。
