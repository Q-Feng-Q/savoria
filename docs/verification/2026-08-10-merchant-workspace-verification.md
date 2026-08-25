# 商户工作台最终验证记录

验证时间：2026-08-10（Asia/Shanghai）

## 自动化测试

- 小程序端：`cd frontend; npm test`，259 项通过，0 项失败。
- 后端：`cd backend; mvn test`，83 项通过，0 项失败，构建成功。
- 覆盖范围包括商户响应式断点、页面状态、权限路由、自定义按钮、WXML 表达式与 WXSS 编译兼容约束。

## 数据库与身份上下文

- Flyway：成功校验 4 个迁移，当前数据库版本为 4，无待执行迁移。
- 重新建立专用开发验证数据：用户 2、商户 2、家庭 1。
- `GET /users/me/context`：用户 2 返回 `availableModes=[family,merchant]`，权限为 `FAMILY_MEMBER`、`FAMILY_ADMIN`、`MERCHANT_ADMIN`。

## 真实接口冒烟测试

以下接口均以用户 2 的 Bearer 会话调用并返回 HTTP 200：

- `GET /merchant/families`：1 条家庭记录。
- `GET /merchant/families/1`：家庭详情对象。
- `GET /merchant/families/1/menu`：空数组。
- `GET /merchant/orders`：空数组。
- `GET /merchant/dishes`：空数组。
- `GET /merchant/dish-categories`：10 条分类。
- `GET /merchant/ingredients`：73 条食材。
- `GET /merchant/purchases/summary?date=2026-08-10&includePending=true`：空数组。
- `GET /merchant/purchases/by-family?familyId=1&date=2026-08-10&includePending=true`：空数组。
- `GET /merchant/purchases/temp-items?date=2026-08-10`：空数组。
- `GET /merchant/purchases/copy-text?date=2026-08-10`：空文本。

## 未执行项

- 未执行菜品、食材、菜单和订单的破坏性变更链路；本轮真实接口验证保留为只读冒烟测试。
- 微信开发者工具 CLI 的 `auto`/`preview` 命令在本机启动服务时持续无输出并超时，未取得 IDE 编译截图。代码侧 WXSS/WXML 兼容测试已通过，但不得将其等同于 IDE 编译成功。

## 2026-08-15 系统菜单导入增量验证

- 小程序完整回归：`cd frontend; npm test`，274 项通过，0 项失败。
- 后端完整回归：`cd backend; & 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -s .maven-settings.xml test`，93 项通过，0 项失败，`BUILD SUCCESS`。
- 页面脚本语法：`dish-templates/index.js` 与 `merchant-dishes/index.js` 的 `node --check` 均以退出码 0 完成。
- 新增接口契约：`POST /merchant/dish-templates/import-all` 无请求正文，只从实时登录上下文取得商户身份。
- 全量导入测试覆盖精确 198 道启用模板、已导入跳过且不覆盖、并发重复键跳过、空模板集合，以及选择导入原有非空/去重/100 道/不可用 ID 边界。
- 小程序测试覆盖系统菜库入口、选择导入、确认后全量导入、成功/跳过统计、双向忙碌锁、确认框竞争窗口、失败状态保留、自定义按钮和紧凑/横屏/安全区适配。
- 模板分页测试覆盖第一页替换、触底追加至 `total`、到达末页停止请求、加载中防重、刷新与旧分页请求隔离、筛选世代隔离，以及导入成功后的勾选清理与静默刷新。
- 家庭菜单查询以商户已上架菜品为候选集，已有家庭配置通过左连接保留；未配置菜品默认停用并使用基础价，确保新上架菜品可立即配置。
- V7 迁移为现有家庭补齐早餐、午餐和晚餐；新家庭审批创建时同步初始化三种餐次，且不会覆盖同名已有配置。

## 2026-08-15 家庭首页推荐菜增量验证

- 小程序完整回归：`cd frontend; npm test`，275 项通过，0 项失败。
- 后端完整回归：`cd backend; & 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -s .maven-settings.xml test`，122 项通过，0 项失败，1 项环境跳过，`BUILD SUCCESS`。
- 页面脚本语法：家庭菜单页、商户接口服务和场景映射脚本的 `node --check` 均以退出码 0 完成。
- V9 为家庭增加显式推荐菜字段；家庭首页仅在推荐菜仍属于该商户、家庭菜单已启用且菜品已上架时优先展示，否则按菜单排序稳定回退。
- 新接口 `PUT /merchant/families/{familyId}/menu/featured` 只接受当前登录商户上下文和 `dishId`，覆盖越权家庭、未配置、已停用、未上架及跨商户菜品拒绝。
- 商户家庭菜单使用自定义 `view` 操作控件展示“设为首页推荐”与“首页推荐”状态，具备行级防重复、失败解锁、静默刷新、88rpx 触控尺寸、紧凑屏换行和安全区兼容。
- 家庭首页推荐卡片改为展示菜品真实简介；无简介时显示“今日家庭推荐”。
- 开发数据库由 Flyway 从 V8 成功迁移到 V9；当前后端已从本项目 `target/classes` 重启，OpenAPI 已暴露推荐菜路由，未登录调用返回 HTTP 401，确认权限拦截生效。

## 2026-08-15 商户多推荐菜增量验证

- 后端完整回归：`cd backend; & 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -s .maven-settings.xml test`，共 164 项，其中 161 项通过、0 项失败、0 项错误、3 项跳过，`BUILD SUCCESS`。
- 跳过项为 `DishTemplateChangeMigrationMySqlTest`、`MerchantFeaturedDishMigrationMySqlTest` 和 `MerchantFeaturedDishConcurrencyMySqlTest`；原因是当前环境未发现可用 Docker，Testcontainers 无法启动 MySQL。契约、服务和 H2/非容器测试正常通过，但不把这 3 项记作已执行通过。
- 小程序完整回归：`cd frontend; npm test`，300 项通过、0 项失败、0 项跳过。首次完整执行发现 `merchant-state-coverage.test.js` 仍期望已移除的家庭级推荐操作产生第 3 次静默刷新；核对页面仅剩保存与复制两条刷新路径后，将该测试期望恢复为 2，目标测试 9/9 通过，随后完整 300/300 通过。
- 页面脚本语法：`pages/merchant/merchant-dishes/index.js`、`pages/family/home/index.js`、`utils/api-scenes.js`、`utils/merchant-scenes.js` 的 `node --check` 均以退出码 0 完成。
- 自动测试覆盖商户级推荐 boxed Boolean 请求、最多 5 道和并发上限、推荐时自动启用有效家庭并保留已有家庭价/排序、缺失菜单项以基础价追加到尾部、商户与点菜列表推荐置顶、三条下架路径清除推荐、旧家庭路由 deprecated adapter、家庭首页多推荐与单项兼容、swiper 多项/单项行为。
- V10/Flyway 运行时迁移与 OpenAPI 路由：本轮未执行。重启前的 8080 端口所有权检查需要沙箱外权限，但工具审批额度已用尽并拒绝执行；为避免停止来源不明的进程，没有绕过该安全检查，也没有修改共享开发数据库。
