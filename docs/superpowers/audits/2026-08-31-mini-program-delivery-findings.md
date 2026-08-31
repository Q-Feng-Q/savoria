# 小程序交付审查问题记录

基线提交：`38bf3d2`

| ID | 严重度 | 业务域 | 复现条件 | 失败测试 | 根因 | 修改文件 | 验证 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| MP-001 | P2 | 商户修改申请 | 从详情跳转后返回，或身份切换后再次显示页面 | `delivery-page-behavior.test.js` / `pages/merchant/dish-template-change-detail/index` | 页面仅在 `onLoad` 请求，`onShow` 不刷新 | `frontend/pages/merchant/dish-template-change-detail/index.js` | 页面行为、模板修改申请与模板市场共 21 项测试通过 | FIXED |
| MP-002 | P2 | 商户修改申请列表 | 审核/撤回后返回列表，或切换商户身份后再次显示页面 | `delivery-page-behavior.test.js` / `pages/merchant/dish-template-changes/index` | 列表仅在 `onLoad` 请求，返回页面不重新拉取 | `frontend/pages/merchant/dish-template-changes/index.js` | 页面行为、模板修改申请与模板市场共 21 项测试通过 | FIXED |
| MP-003 | P2 | 系统菜品导入 | 导入后返回列表，或切换商户身份后再次显示页面 | `delivery-page-behavior.test.js` / `pages/merchant/dish-templates/index` | 列表仅在 `onLoad` 请求，可能保留旧导入状态和旧身份数据 | `frontend/pages/merchant/dish-templates/index.js` | 页面行为、模板修改申请与模板市场共 21 项测试通过 | FIXED |
| MP-004 | P1 | 通知与家庭钱包流水 | 通知或流水超过 100 条 | `delivery-page-behavior.test.js` / `notifications and wallet ledger load every backend page` | 页面固定请求第 1 页、100 条，后续数据静默丢失 | `frontend/utils/pagination.js`、通知页、家庭钱包流水页 | 相关 35 项与全量前端 484 项测试通过 | FIXED |
| MP-005 | P2 | 商户修改申请列表 | 翻页期间后台新增或调整记录，后一页与已加载页重叠 | `delivery-page-behavior.test.js` / `change request pagination deduplicates rows by request id` | 直接拼接分页结果，没有按申请 ID 去重 | 商户修改申请列表、分页工具 | 相关 35 项与全量前端 484 项测试通过 | FIXED |
| MP-006 | P1 | 账号/身份切换 | 账号 A 的请求较慢，切换到账号 B 后 A 的响应才返回 | `delivery-page-behavior.test.js`、`account-switching.test.js` | 16 个登录后刷新页面缺少请求代次与身份校验，旧响应可覆盖新页面；身份同步本身也会覆盖用户后来选择的账号 | `frontend/utils/identity-load.js`、`account-switching.js` 及账号、家庭、点菜、订单、商户审核相关页面 | 身份相邻域 45 项与全量前端 487 项测试通过 | FIXED |

## 状态说明

- `OPEN`：已复现，尚未修复。
- `FIXED`：目标测试与相邻业务域测试通过。
- `GATED`：代码检查完成，但仍等待 Docker/MySQL、微信开发者工具或真机环境验证。
