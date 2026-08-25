# 小程序商户资料自助编辑设计

## 目标

允许当前商户负责人在小程序商户端查看并编辑自己的商户名称、联系人和联系电话。家庭端仍只读所属商户信息；平台管理端继续负责商户状态、负责人关系和审计信息。

## 权限边界

- 当前数据模型以有效的 `merchant_user_relations.merchant_role = MERCHANT_ADMIN` 关系表示商户负责人。
- GET/PUT 不能只依赖宽泛的 `hasMerchantBackendAccess()`。服务和 SQL 都必须校验当前 `userId + merchantId` 对应一条 `merchant_role='MERCHANT_ADMIN' AND status='ACTIVE'` 的实时关系。
- 服务端始终使用实时上下文中的 `userId` 和 `merchantId`，请求体不接收商户 ID。
- 只允许修改 `name`、`contactName`、`contactPhone`。
- 商户 ID、负责人、状态、所属关系、创建/更新时间和审计字段不在响应编辑模型和更新 DTO 中。
- 不复用平台管理员商户接口，避免小程序获得负责人和状态修改能力。

## 后端接口

### GET /api/merchant/profile

返回当前商户：

```json
{
  "name": "暖炉小馆",
  "contactName": "林女士",
  "contactPhone": "13800000000"
}
```

### PUT /api/merchant/profile

请求：

```json
{
  "name": "暖炉小馆",
  "contactName": "林女士",
  "contactPhone": "13800000000"
}
```

校验规则：

- `name` 必填、去除首尾空白、最长 100 字。
- `contactName` 可空、去除首尾空白、最长 50 字；空字符串持久化为 `null`。
- `contactPhone` 可空、去除首尾空白、最长 30 字；空字符串持久化为 `null`。
- 仅 `status='active'` 的商户可查询和更新；`pending`、`rejected`、`inactive` 或不存在均拒绝请求。

查询和更新使用独立 MerchantProfileMapper。PUT 的单条 UPDATE 同时限定 `merchantId`、`status='active'`，并通过 `EXISTS` 重新校验 `userId + merchantId + MERCHANT_ADMIN + ACTIVE` 关系；即使平台在请求上下文生成后撤销关系，更新行数也必须为 0。GET 使用相同的关系和商户状态条件。

响应 VO 只包含 `name`、`contactName`、`contactPhone`；更新 DTO 也只包含这三个字段。额外的 `merchantId`、`status`、`ownerUserId` 不参与绑定和更新。

## 小程序页面

- 在商户工作台顶部商户名称旁增加自定义“编辑商家信息”控件。
- 新增独立页面 `pages/merchant/merchant-profile-edit/index`，进入时请求 GET profile。
- 页面包含商户名称、联系人、联系电话三个字段，以及保存、取消两个自定义按钮。
- 保存时进行前端同等长度校验，使用 busy 锁阻止重复提交。
- 保存失败保留草稿并显示接口错误；成功后提示并返回工作台。工作台 `onShow` 重新加载，展示最新商户名称。
- 商户工作台 `load()` 同时请求 GET profile，profile.name 是工作台商户名称的权威来源，即使商户尚未服务任何家庭也能显示和刷新真实名称。普通 profile 网络失败以局部错误降级，可暂时使用既有场景名称，不阻断订单、家庭和采购区域。
- 401 继续交给现有统一认证恢复。GET/PUT 返回 403 或关系/商户状态失效时，页面调用现有 `refreshAccountIdentity` 重新读取 `/users/me/context` 并写回 session；若 merchant 模式已消失，立即离开编辑页/工作台并跳转到刷新后身份的目标页面或账号管理页，不能保留可编辑状态。

## 视觉与响应式

- 延续暖动物私厨的暖纸张背景、深咖啡文字和深珊瑚主按钮。
- 不使用微信原生 button；全部操作控件具备 aria-role、aria-label、按压反馈和至少 88rpx 触控高度。
- 表单宽度使用 `min-width: 0`、`box-sizing: border-box`，覆盖 320px、360px、常规手机、大字体与底部安全区，无横向滚动或标签截断。

## 测试

- 后端控制器契约：路由、GET/PUT VO/DTO 三字段白名单和严格负责人权限入口；额外 `merchantId/status/ownerUserId` 不进入更新模型。
- 后端服务与 Mapper 测试：正常查询/修改、字段清洗、非负责人拒绝、`pending/rejected/inactive`/不存在拒绝，且只按上下文 merchantId 更新；模拟上下文生成后关系撤销，断言 UPDATE 为 0。
- 小程序服务测试：精确 GET/PUT 路径与请求体。
- 页面测试：加载、校验、取消、保存失败保留草稿、双击只发一次、保存成功返回、403 刷新身份后退出编辑态。
- 工作台测试：profile 名称优先、无家庭仍显示 profile 名称、普通 profile 失败局部降级、403 后失效关闭。
- 家庭端契约测试明确不存在商户资料编辑入口。
- WXML/WXSS 契约：注册页面、自定义按钮、88rpx、小屏、安全区、无原生 button。
- 运行完整后端与小程序测试；在线后端可确认归属时再做只读/已授权接口烟测。
