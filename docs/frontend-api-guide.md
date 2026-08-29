# 前端多端接口适配文档

本文档面向家庭厨房项目前端三类入口，描述三端对接稳定版 Spring Boot 后端的真实接口约定。生产页面不得读取本地模拟业务数据。

配套文档：
- 人工阅读版后端接口：[api-spec.md](</D:/develop/Project/Kitchen/docs/api-spec.md>)
- 机器可读 OpenAPI：后端启动后访问 `/v3/api-docs`；Swagger UI 地址为 `/swagger-ui.html`。

## 0. 多端定位

| 端 | 目录 | 目标用户 | 适配重点 |
| --- | --- | --- | --- |
| 家庭前台小程序 | `frontend/` | 家庭成员 | 首页、点菜、餐篮、订单、地址、钱包、通知 |
| 商户后台小程序 | `frontend/` | 商户管理员 | 移动端轻管理：订单处理、菜品维护、系统模板菜导入、家庭菜单、采购单、通知 |
| 商户 PC 后台 | `admin-web/` | 商户管理员 | 桌面高密度管理：订单、菜品、食材、家庭、菜单、采购、通知、模板菜导入 |

适配原则：
- 三端共用同一个 Spring Boot + MySQL 后端、同一套统一响应格式。
- 家庭前台只承接成员日常点餐，不展示商户制作流程和后台管理入口。
- 商户后台小程序用于手机上快速处理订单、采购和菜品。
- PC 后台用于批量配置和高密度管理，页面统一通过真实 service 请求后端。
- 微信小程序请求层使用 `frontend/utils/api.js`，PC 后台请求层使用 `admin-web/api-client.js`。

## 1. 对接范围

当前稳定后端已覆盖家庭前台小程序、商户后台小程序和 PC 后台的主要业务闭环。三端均不得使用本地 Mock 作为生产数据回退。

| 模块 | 小程序页面 | PC 后台页面 | 后端能力 |
| --- | --- | --- | --- |
| 登录与找回密码 | `pages/auth/entry`、`pages/auth/password-recovery` | `admin-web` 登录页 | 用户登录、后台登录、微信登录、邮箱验证码找回密码 |
| 家庭首页 | `pages/home` | 不适用 | 家庭信息、动物角色人员、特色菜、预约与近期订单、首页数据卡片 |
| 家庭点菜 | `pages/menu`、`pages/dish-detail` | 不适用 | 菜单列表、菜品详情、加入餐篮 |
| 家庭餐篮 | `pages/cart` | 不适用 | 查询餐篮、增删改餐篮项、修改备注、提交订单 |
| 家庭订单 | `pages/ordering/orders`、`pages/ordering/order-detail` | 不适用 | 家庭订单列表、详情、待确认订单按当前餐篮更新、取消订单 |
| 家庭我的 | `pages/account/profile`、`pages/family/addresses`、`pages/family/address-edit`、`pages/family/wallet-ledger` | 不适用 | 地址、钱包流水 |
| 家庭关系 | `pages/family/family-management` | `admin-web` 家庭设置/平台家庭中心 | 邀请、加入审批、接受邀请、退出家庭、移交负责人、解散家庭 |
| 通知 | `pages/notifications` | `admin-web` 通知中心 | 通知列表、已读 |
| 商户工作台 | `pages/merchant`、`pages/merchant-orders`、`pages/merchant-order-detail` | `admin-web` 工作台、订单管理 | 商户订单、确认、拒单、取消、调整配送费、推进状态 |
| 菜品管理 | `pages/merchant/merchant-dishes`、`pages/merchant/dish-edit` | `admin-web` 菜品管理 | 菜品、分类、制作流程、图片上传 |
| 菜品审核跟踪 | `pages/merchant/dish-reviews` | `admin-web` 审核记录 | 商户审核历史、审核原因、撤回待审核提交 |
| 食材字典 | `pages/merchant/ingredient-edit` | `admin-web` 食材管理 | 食材列表、新增、编辑、删除、引用关系展示 |
| 家庭管理 | `pages/merchant-families`、`pages/merchant-family-detail` | `admin-web` 家庭管理 | 家庭列表、详情、基础资料、配送设置、地址簿、成员余额调整 |
| 家庭菜单配置 | `pages/family-menu`、`pages/merchant-family-detail` | `admin-web` 家庭菜单配置 | 查看/保存/复制家庭菜单 |
| 采购单 | `pages/purchase` | `admin-web` 采购清单 | 汇总采购单、家庭采购项、临时采购项、勾选、复制文本 |

当前后端暂未开放：
- 暂无阻塞家庭前台/商户后台主链路的核心接口缺口

当前家庭资料、食材字典、分类返回 ID 都已经可以直接联调。

## 2. 基础配置

### 2.1 Base URL

后端默认：

```text
http://localhost:8080
```

小程序本地调试建议：

```js
// frontend/app.js
globalData: {
  apiBaseUrl: 'http://127.0.0.1:8080'
}
```

如果用真机预览，需要换成局域网 IP，例如：

```text
http://192.168.1.20:8080
```

PC 后台本地调试建议：

```js
const api = KitchenAdminApi.createAdminApiClient({
  baseUrl: '/api',
  getSession: () => JSON.parse(localStorage.getItem('family_kitchen_admin_session') || 'null')
});
```

`admin-web` 开发环境默认通过 Vite 把 `/api` 代理到 Spring Boot，默认目标是 `http://127.0.0.1:8080`。如需切换联调环境，可在 `admin-web/.env.example` 的基础上配置 `VITE_PROXY_TARGET`。生产环境建议继续保持同源 `/api` 反向代理，避免浏览器跨域。

### 2.2 统一响应

所有接口返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

前端判断规则：

```js
if (res.data.code !== 0) {
  throw new Error(res.data.message || '请求失败');
}
```

注意：后端使用真实 HTTP 状态码表达错误，同时在响应体保留业务 `code`。前端必须同时检查 `statusCode` 和 `code`。

### 2.3 错误码

| code | 说明 | 前端建议 |
| --- | --- | --- |
| `0` | 成功 | 正常使用 `data` |
| `40001` | 参数错误 | Toast 提示并检查表单 |
| `40101` | 未登录 | 清空 session，回到入口页 |
| `40301` | 无权限 | Toast 提示无权限 |
| `40401` | 不存在 | 提示数据已变化，刷新列表 |
| `40901` | 状态冲突 | 提示订单状态已变化，刷新详情 |
| `42201` | 业务校验失败 | 展示后端 message |
| `50001` | 系统异常 | Toast 提示稍后再试 |

## 3. 登录与请求头

### 3.1 登录接口

当前稳定后端提供三种真实接入方式：

- 普通用户使用 `POST /api/auth/login`。
- 拥有后台权限的商户或平台账号使用 `POST /api/auth/admin/login`。
- 已绑定微信的用户可使用 `wx.login` 获取的临时 `code` 调用 `POST /api/auth/wechat/login`。

#### 方式一：后台账号登录

```http
POST /api/auth/admin/login
```

请求：

```json
{
  "username": "admin",
  "password": "123456"
}
```

#### 方式二：普通用户账号登录

```http
POST /api/auth/login
```

请求：

```json
{
  "username": "已注册用户名或已验证邮箱",
  "password": "用户密码"
}
```

#### 方式三：真实微信登录

```http
POST /api/auth/wechat/login
```

```json
{
  "code": "wx.login 返回的临时登录码"
}
```

后端通过微信 `code2Session` 换取 OpenID。该微信必须已绑定现有用户，且后端必须配置真实 `app-id` 和 `app-secret`；不存在任何 `member-*`、`merchant-*` 或 `dev-token-*` 开发登录约定。

三种登录方式成功后，返回结构一致：

```json
{
  "accessToken": "后端签发的 JWT",
  "userId": 1,
  "merchantId": 1,
  "familyId": 2,
  "memberId": 10,
  "roleTemplate": "member",
  "backendRoles": [],
  "merchantAdminScopes": []
}
```

### 3.2 session 存储建议

登录后建议存完整 session：

```js
{
  accessToken: '后端签发的 JWT',
  userId: 1,
  merchantId: 1,
  familyId: 2,
  memberId: 10,
  roleTemplate: 'member',
  backendRoles: [],
  merchantAdminScopes: []
}
```

所有业务 ID 均以后端返回的数字 ID 为准。

### 3.3 必带请求头

当前稳定后端以 `Authorization: Bearer <accessToken>` 作为主鉴权方式，`X-*` 身份头保留为联调辅助信息。前端可以继续透传这些字段，但不能只依赖它们而省略 `Authorization`。

家庭端接口请求头：

```js
{
  Authorization: `Bearer ${session.accessToken}`,
  'X-User-Id': session.userId,
  'X-Merchant-Id': session.merchantId,
  'X-Family-Id': session.familyId,
  'X-Member-Id': session.memberId,
  'X-Role-Template': session.roleTemplate
}
```

商户端接口请求头：

```js
{
  Authorization: `Bearer ${session.accessToken}`,
  'X-User-Id': session.userId,
  'X-Merchant-Id': session.merchantId,
  'X-Family-Id': session.familyId,
  'X-Member-Id': session.memberId,
  'X-Role-Template': session.roleTemplate,
  'X-Backend-Roles': (session.backendRoles || []).join(','),
  'X-Merchant-Admin-Scopes': (session.merchantAdminScopes || []).join(',')
}
```

商户权限判断：

```js
const isMerchant =
  session.roleTemplate === 'merchant_admin' ||
  (session.backendRoles || []).includes('merchant_admin') ||
  (session.merchantAdminScopes || []).includes('merchant');
```

### 3.4 小程序请求层 `frontend/utils/api.js`

小程序请求层已经支持 `getSession`，会自动注入后端需要的身份 header。家庭前台和商户后台小程序共用这一层，请通过 session 判断进入家庭页面还是商户页面。

使用形态：

```js
const request = createApiClient({
  baseUrl: getApp().globalData.apiBaseUrl,
  getToken: () => sessionStore.getToken(),
  getSession: () => sessionStore.getSession()
});
```

请求层使用登录返回的顶层数字 ID 和 JWT。`session.actor` 仅为历史兼容字段，不得由新代码写入。

### 3.5 PC 后台请求层 `admin-web/api-client.js`

PC 后台已提供独立请求层，所有业务页面均通过该请求层访问真实后端。

```js
const api = KitchenAdminApi.createAdminApiClient({
  baseUrl: '/api',
  getSession: () => JSON.parse(localStorage.getItem('family_kitchen_admin_session') || 'null')
});

const orders = await api('/api/merchant/orders', { method: 'GET' });
```

PC 后台 session 至少需要：

```js
{
  accessToken: '后端签发的 JWT',
  userId: 1,
  merchantId: 1,
  roleTemplate: 'merchant_admin',
  backendRoles: ['merchant_admin'],
  merchantAdminScopes: ['merchant']
}
```

注意：PC 后台不走 `wx.request`，使用浏览器 `fetch`。当前 `admin-web` 已默认走真实接口，请保持 `/api` 同源代理；如果切成独立域名，请同步配置反向代理或后端 CORS。

## 4. 接口模块适配建议

小程序建议新建：

```text
frontend/services/
  auth.js
  family.js
  cart.js
  orders.js
  merchant.js
  purchase.js
  notifications.js
  files.js
```

每个 service 只返回后端 `data`：

```js
async function getFamilyHome() {
  const res = await request('/api/family/home', { method: 'GET' });
  return res.data;
}
```

PC 后台建议后续按模块拆分：

```text
admin-web/services/
  dashboard.js
  orders.js
  dishes.js
  families.js
  purchase.js
  notifications.js
```

当前 PC 后台已经拆出 `dashboard.js`、`orders.js`、`dishes.js`、`families.js`、`ingredients.js`、`purchases.js`、`notifications.js` 等 service，统一经由 `admin-web/src/services/http.js` 发请求。新增页面时优先复用现有 service 约定，不再直接散落写 `fetch`。

## 5. 家庭端页面接口

### 5.1 `pages/entry` 登录页

#### 用户账号登录

```http
POST /api/auth/login
```

#### 已绑定用户微信登录

```http
POST /api/auth/wechat/login
```

请求：

```json
{
  "code": "wx.login 返回的临时登录码"
}
```

成功后：
- 保存 session
- 如果 `roleTemplate === 'merchant_admin'`，跳转 `pages/merchant/index`
- 否则跳转 `pages/home/index`

### 5.2 `pages/home` 首页

#### 获取首页数据

```http
GET /api/family/home
```

关键返回：

```js
{
  family: {
    familyId,
    familyName,
    merchantName
  },
  member: {
    memberId,
    name,
    roleTemplate
  },
  crew: {
    chefName,   // 商户负责人；联系人为空时回退负责人账号名称
    helperName, // 家庭管理员；不存在时为“无帮厨”
    tasterName  // 普通成员；不存在时回退管理员，再不存在为“无试吃员”
  },
  serviceDate,
  featuredDishes: [
    {
      dishId,
      name,
      description,
      price,
      imageUrl
    }
  ],
  featuredDish: {
    dishId,
    name,
    description,
    price,
    imageUrl
  },
  dashboardCards: [
    { key, label, value }
  ],
  recentOrders: [
    { orderId, expectedMealTime, mealSlotName, status, totalAmount }
  ]
}
```

页面映射建议：
- 顶部家庭/商户文案：`family.familyName`、`family.merchantName`
- 当前成员：`member.name`
- 首页动物角色：`crew.chefName`、`crew.helperName`、`crew.tasterName`
- 今日推荐/特色菜：优先使用 `featuredDishes`，最多 5 项，按 `featuredAt` 对应的推荐时间降序、菜品 ID 降序返回；其中只包含当前有效家庭菜单中已启用、已上架的商户推荐菜。`featuredDish` 是 `featuredDishes[0]` 的兼容字段；没有有效商户推荐时，两者都回退为家庭菜单中 `sortOrder` 升序、菜品 ID 降序的第一道有效菜。
- 首页推荐区使用 `swiper`：多于 1 项时开启自动播放、循环、指示点和下一项露出，轮播间隔 4 秒；只有 1 项时关闭自动播放、循环和指示点，也不保留下一项边距；显式空 `featuredDishes` 不再读取旧单项字段。推荐卡片说明使用菜品 `description`，为空时显示“今日家庭推荐”。
- 新订单预约时间：`recentOrders[].expectedMealTime`；旧订单才允许回退历史餐次名称
- 数据卡片：`dashboardCards`
- 近期订单入口：`recentOrders`

### 5.3 `pages/menu` 点菜页

#### 预计用餐时间

新页面不再请求早餐/午餐/晚餐列表，也不得调用已退役的
`GET /api/family/meal-slots`。预计用餐时间来自共享餐篮，
完整读写契约以 [21. 家庭共享餐篮、预计用餐时间与家庭钱包](#21-家庭共享餐篮预计用餐时间与家庭钱包当前权威契约) 为准。

#### 菜单列表

```http
GET /api/family/menu-items
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `categoryId` | number | 否 | 分类过滤 |
| `keyword` | string | 否 | 菜名搜索 |
| `serviceDate` | string | 否 | 预留，格式 `yyyy-MM-dd` |
| `mealSlotId` | number | 否 | 预留 |

返回项：

```js
{
  dishId,
  categoryId,
  name,
  description,
  imageUrl,
  price,
  status
}
```

#### 查询餐篮

```http
GET /api/family/cart?mealSlotId=20&date=2026-07-02
```

用于显示底部餐篮数量和每道菜已选数量。

#### 加入餐篮

```http
POST /api/family/cart/items
```

请求：

```json
{
  "mealSlotId": 20,
  "date": "2026-07-02",
  "dishId": 100,
  "quantity": 1,
  "itemRemark": "少盐"
}
```

说明：
- 同一成员、同一菜品重复加入，后端会累加数量
- `quantity` 最小为 `1`

### 5.4 `pages/dish-detail` 菜品详情页

```http
GET /api/family/menu-items/{dishId}
```

返回：

```js
{
  dishId,
  categoryId,
  name,
  description,
  imageUrl,
  price,
  status,
  ingredients: [
    { ingredientName, quantity, unit, calcType }
  ],
  cookingSteps: [
    { stepNo, title, content }
  ]
}
```

前端展示建议：
- 家庭端可以展示原材料
- 如果不想给家庭端看制作流程，前端暂时隐藏 `cookingSteps`
- 商户端菜品详情可以展示 `cookingSteps`

### 5.5 `pages/cart` 餐篮页

#### 查询餐篮

```http
GET /api/family/cart?mealSlotId=20&date=2026-07-02
```

返回：

```js
{
  familyId,
  mealSlotId,
  date,
  remark,
  totalQuantity,
  totalAmount,
  items: [
    {
      itemId,
      dishId,
      dishName,
      price,
      quantity,
      ownerMemberId,
      ownerMemberName,
      editable,
      itemRemark
    }
  ]
}
```

#### 修改餐篮项

```http
PUT /api/family/cart/items/{itemId}
```

请求体同新增餐篮项。

#### 删除餐篮项

```http
DELETE /api/family/cart/items/{itemId}
```

#### 修改点餐备注

```http
PUT /api/family/cart/remark
```

请求：

```json
{
  "mealSlotId": 20,
  "date": "2026-07-02",
  "remark": "不要香菜，送到门口"
}
```

#### 提交订单

```http
POST /api/family/orders
```

请求：

```json
{
  "date": "2026-07-02",
  "mealSlotId": 20,
  "deliveryMode": "DELIVERY",
  "addressId": 1,
  "remark": "不要香菜"
}
```

枚举：
- `deliveryMode`: `PICKUP` / `DELIVERY`

说明：
- 配送订单建议传 `addressId`
- 自提订单可不传 `addressId`
- 下单会按默认配送费冻结，商户确认前可调整，多退少补

### 5.6 `pages/orders` 订单列表

```http
GET /api/family/orders
```

返回：`OrderView[]`

订单状态：

| 后端值 | 前端文案 |
| --- | --- |
| `PENDING` | 待确认 |
| `CONFIRMED` | 已确认 |
| `PREPARING` | 备菜中 |
| `READY` | 待取餐/待配送 |
| `DONE` | 已完成 |
| `CANCELLED` | 已取消 |
| `REJECTED` | 已拒单 |

### 5.7 `pages/order-detail` 订单详情

#### 查询详情

```http
GET /api/family/orders/{orderId}
```

#### 取消订单

```http
POST /api/family/orders/{orderId}/cancel
```

请求：

```json
{
  "reason": "临时不吃了"
}
```

### 5.8 地址页面

#### 地址列表

```http
GET /api/family/addresses
```

#### 新增地址

```http
POST /api/family/addresses
```

请求：

```json
{
  "contactName": "陈梅",
  "contactPhone": "13800000000",
  "addressText": "上海市浦东新区 xx 路 1 号",
  "defaultAddress": true
}
```

#### 修改地址

```http
PUT /api/family/addresses/{addressId}
```

#### 设为默认地址

```http
POST /api/family/addresses/{addressId}/default
```

#### 删除地址

```http
DELETE /api/family/addresses/{addressId}
```

说明：
- 删除默认地址后，如果还有其他地址，后端会自动补一个新的默认地址

### 5.9 家庭资料与负责人移交

#### 查询家庭业务资料

```http
GET /api/family/info
```

返回仅包含家庭端可展示的业务资料：

```json
{
  "familyName": "林家小院",
  "note": "晚餐少辣",
  "merchantName": "老祁私厨",
  "deliveryEnabled": true,
  "deliveryFeeDefault": 6.00,
  "deliveryFree": false
}
```

家庭 ID、商户 ID、状态、余额和审计字段不在此响应中。默认联系人、电话和配送地址继续从 `GET /api/family/addresses` 选择 `defaultAddress=true` 的地址；编辑仍跳转既有地址簿页面。

#### 修改家庭业务资料

```http
PUT /api/family/info
```

仅 `OWNER` 或 `ADMIN` 可操作，请求白名单固定为：

```json
{
  "familyName": "林家新桌",
  "note": "周末聚餐"
}
```

- `familyName` trim 后必填，最长 100 字；
- `note` 可为空，最长 255 字；
- 不接受联系人数组、家庭 ID、商户、余额、状态、配送规则或审计字段。

#### 查询负责人候选

```http
GET /api/family/owner-candidates
```

仅当前有效 `OWNER` 可调用。返回当前家庭中关系和账号均为 `ACTIVE`、非当前负责人的成员，按成员标识稳定排序：

```json
[
  {
    "memberId": 12,
    "displayName": "小林",
    "phoneSuffix": "2318"
  }
]
```

接口不返回完整手机号；`memberId` 仅供 picker 选中后提交，不在界面显示。

#### 移交负责人

```http
PUT /api/family/owner
```

```json
{
  "targetMemberId": 12
}
```

仅当前有效 `OWNER` 可操作。服务端会锁定家庭并重新校验候选；成功后旧负责人降为 `MEMBER`。小程序先本地移除 `FAMILY_ADMIN` 并把 `roleTemplate` 降为 `member`，再通过 `GET /api/users/me/context` 刷新权威身份；刷新失败时保持降权状态。

### 5.10 钱包流水

```http
GET /api/family/me/wallet/ledgers
```

返回项：

```js
{
  ledgerId,
  memberId,
  type,
  amount,
  balanceBefore,
  balanceAfter,
  frozenBefore,
  frozenAfter,
  remark,
  createdAt
}
```

## 6. 通知中心接口

### 6.1 通知列表

```http
GET /api/notifications
```

查询参数：

| 参数 | 类型 | 必填 | 默认 |
| --- | --- | --- | --- |
| `receiverScope` | string | 否 | 无 |
| `readStatus` | string | 否 | `all` |
| `category` | string | 否 | 无 |
| `page` | number | 否 | `1` |
| `pageSize` | number | 否 | `20` |

返回：

```js
{
  page,
  pageSize,
  total,
  items: [
    {
      notificationId,
      receiverScope,
      category,
      title,
      content,
      read,
      createdAt
    }
  ]
}
```

### 6.2 单条已读

```http
POST /api/notifications/{notificationId}/read
```

### 6.3 全部已读

```http
POST /api/notifications/read-all
```

请求：

```json
{
  "receiverScope": "family"
}
```

## 7. 商户端接口

商户端接口都要求：

```js
{
  'X-Backend-Roles': 'merchant_admin'
}
```

或者：

```js
{
  'X-Merchant-Admin-Scopes': 'merchant'
}
```

### 7.0 商户负责人维护自身商户资料

小程序商户工作台使用以下接口读取和修改当前负责人的商户业务资料：

```http
GET /api/merchant/profile
PUT /api/merchant/profile
```

查询返回及更新请求都只包含以下白名单字段：

```json
{
  "name": "暖炉小馆",
  "contactName": "林女士",
  "contactPhone": "13800000000"
}
```

- `name` 必填，最长 100 个字符；`contactName` 最长 50 个字符；`contactPhone` 最长 30 个字符。
- 系统 ID、所属关系、商户状态、余额和审计字段不能通过这组接口修改。
- 后端会实时校验当前用户与当前商户之间处于 `ACTIVE` 状态的 `MERCHANT_ADMIN` 关系，同时要求商户状态为 `active`。
- 权限被撤销时返回 `40301`。小程序必须先关闭本地商户能力，再刷新账号身份；刷新失败时保持关闭状态并进入账号管理页。

### 7.1 商户菜品列表

```http
GET /api/merchant/dishes
```

列表项 `DishView` 的推荐相关字段：

```js
{
  dishId,
  categoryId,
  name,
  description,
  imageUrl,
  price,
  status,
  sourceTemplateId,
  templateImported,
  featuredAt, // ISO-8601 日期时间；未推荐时为 null
  featured    // boolean；由 featuredAt 是否存在得出
}
```

商户菜品列表把推荐菜置顶，推荐菜内部按 `featuredAt` 降序、菜品 ID 降序确定性排列。家庭点菜接口返回的菜单也按同一规则将推荐菜置顶；前端应保留服务端顺序，只展示“主厨推荐”标记，不再自行二次排序。

### 7.2 商户菜品详情

```http
GET /api/merchant/dishes/{dishId}
```

### 7.3 新增菜品

```http
POST /api/merchant/dishes
```

请求：

```json
{
  "name": "土豆烧牛腩",
  "categoryId": 3,
  "description": "软糯下饭",
  "imageUrl": "/uploads/images/1751350000000.png",
  "basePrice": 32.00,
  "ingredients": [
    {
      "ingredientName": "牛腩",
      "quantity": 500,
      "unit": "g",
      "calcType": "FIXED"
    }
  ],
  "cookingSteps": [
    {
      "stepNo": 1,
      "title": "焯水",
      "content": "牛腩冷水下锅焯水"
    }
  ],
  "status": "active"
}
```

### 7.4 修改菜品

```http
PUT /api/merchant/dishes/{dishId}
```

请求体同新增菜品。

当完整修改把 `status` 设为 `INACTIVE` 时，会在同一条更新中清除推荐状态。

### 7.5 设置商户推荐菜

```http
PUT /api/merchant/dishes/{dishId}/featured
Content-Type: application/json
```

请求体：

```json
{
  "featured": true
}
```

`featured` 是必填的 boxed Boolean；标准请求应发送 JSON 布尔值 `true` 或 `false`。字段缺失或为 `null` 会返回 HTTP 400（`BAD_REQUEST`）。`true` 表示推荐，`false` 表示取消推荐；重复设置为当前状态按幂等成功处理。

业务规则与错误：

- 菜品必须属于当前登录商户；不存在或跨商户菜品返回 HTTP 404（`NOT_FOUND`）。
- 只能推荐 `active` 菜品；推荐已下架菜品返回 HTTP 422（`BUSINESS_INVALID`，消息“只能推荐已上架菜品”）。
- 每个商户最多同时推荐 5 道菜；推荐第 6 道返回 HTTP 422（`BUSINESS_INVALID`，消息“商户最多可推荐5道菜”）。并发请求也受同一上限约束。
- 推荐成功会自动把该菜启用到商户所有 `active` 家庭。家庭已有菜单项时只把 `enabled` 改为 `true`，保留原 `sortOrder` 和家庭价；缺失菜单项时以菜品基础价作为家庭价，并追加到该家庭现有排序尾部。
- 取消推荐只清除商户推荐状态，不删除或停用家庭菜单项。
- 菜品通过完整修改、独立状态接口或审核批准三条路径变成 `inactive` 时，都会同步清除推荐状态。

### 7.6 单独更新制作流程

```http
PUT /api/merchant/dishes/{dishId}/cooking-steps
```

请求：

```json
[
  {
    "stepNo": 1,
    "title": "焯水",
    "content": "牛腩冷水下锅"
  }
]
```

### 7.7 菜品分类

```http
GET /api/merchant/dish-categories
POST /api/merchant/dish-categories
PUT /api/merchant/dish-categories/{categoryId}
DELETE /api/merchant/dish-categories/{categoryId}
```

分类请求体：

```json
{
  "name": "家常热菜",
  "sortOrder": 10,
  "enabled": true
}
```

分类返回项：

```json
{
  "categoryId": 1,
  "name": "家常热菜",
  "sortOrder": 10,
  "enabled": true
}
```

### 7.7 图片上传

```http
POST /api/files/images
Content-Type: multipart/form-data
```

小程序调用：

```js
wx.uploadFile({
  url: `${baseUrl}/api/files/images`,
  filePath,
  name: 'file',
  header: buildAuthHeaders(session),
  success(res) {
    const payload = JSON.parse(res.data);
    const imageUrl = payload.data.url;
  }
});
```

限制：
- `jpeg` / `png` / `webp`
- 最大 `4MB`

### 7.8 商户订单

#### 列表与详情

```http
GET /api/merchant/orders
GET /api/merchant/orders/{orderId}
```

#### 确认订单

```http
POST /api/merchant/orders/{orderId}/confirm
```

#### 拒绝订单

```http
POST /api/merchant/orders/{orderId}/reject
```

#### 商户取消订单

```http
POST /api/merchant/orders/{orderId}/cancel
```

请求：

```json
{
  "reason": "食材异常，无法出餐"
}
```

说明：商户在备菜中也能取消，但必须填写原因。

#### 调整配送费

```http
POST /api/merchant/orders/{orderId}/delivery-fee
```

请求：

```json
{
  "deliveryFee": 8.00
}
```

#### 推进订单状态

```http
POST /api/merchant/orders/{orderId}/status
```

请求：

```json
{
  "status": "PREPARING",
  "reason": "开始备菜"
}
```

### 7.9 商户家庭列表

```http
GET /api/merchant/families
```

返回项：

```js
{
  familyId,
  familyName,
  merchantName,
  note,
  contactNames,
  deliveryEnabled,
  deliveryFeeDefault,
  deliveryFree,
  deliverySummary,
  defaultAddressText,
  addressCount,
  memberCount,
  activeMenuCount,
  lowBalanceMemberCount,
  frozenBalanceTotal
}
```

### 7.10 商户家庭详情

```http
GET /api/merchant/families/{familyId}
```

返回项：

```js
{
  familyId,
  familyName,
  merchantId,
  merchantName,
  note,
  contactNames,
  deliveryEnabled,
  deliveryFeeDefault,
  deliveryFree,
  deliverySummary,
  addresses: [{ addressId, contactName, contactPhone, addressText, defaultAddress }],
  members: [{ memberId, name, availableBalance, frozenBalance, lowBalance }],
  activeMenuCount
}
```

### 7.11 更新家庭基础资料

```http
PUT /api/merchant/families/{familyId}/profile
```

请求：

```json
{
  "familyName": "陈家晚饭",
  "note": "工作日晚饭优先，口味偏清淡",
  "contactNames": ["陈梅", "陈浩"]
}
```

前端适配建议：
- 商户小程序详情页可做成“基础资料卡片 + 编辑弹层”。
- PC 后台可直接复用现有家庭详情抽屉里的“地址与备注”区域，再加一个联系人摘要输入框。
- `contactNames` 是摘要字段，不替代成员列表本身；成员名仍以成员接口/详情返回为准。

### 7.12 更新家庭配送设置

```http
PUT /api/merchant/families/{familyId}/delivery-policy
```

请求：

```json
{
  "deliveryEnabled": true,
  "deliveryFeeDefault": 6.00,
  "deliveryFree": false
}
```

### 7.13 食材字典

#### 食材列表

```http
GET /api/merchant/ingredients
```

返回项：

```js
{
  ingredientId,
  name,
  category,
  unit,
  referencedDishCount,
  referencedDishNames,
  removable
}
```

#### 新增食材

```http
POST /api/merchant/ingredients
```

```json
{
  "name": "白胡椒",
  "category": "调料",
  "unit": "g"
}
```

#### 编辑食材

```http
PUT /api/merchant/ingredients/{ingredientId}
```

说明：
- 如果食材改名，后端会同步更新当前商户名下菜品原材料名称。

#### 删除食材

```http
DELETE /api/merchant/ingredients/{ingredientId}
```

说明：
- 只有 `removable=true` 时才能删。
- PC 后台“已被引用，不可删”的 UI 可以直接绑定 `removable`。

### 7.14 家庭菜单配置

#### 查看家庭菜单

```http
GET /api/merchant/families/{familyId}/menu
```

返回项包含 `featured` 与 `featuredAt`，表示商户级推荐状态及推荐时间。推荐菜会置顶；家庭菜单页只负责启停、价格和排序配置，不再提供家庭级推荐按钮。

#### 保存家庭菜单

```http
PUT /api/merchant/families/{familyId}/menu
```

请求：

```json
{
  "items": [
    {
      "dishId": 100,
      "enabled": true,
      "sortOrder": 10,
      "familyFinalPrice": 16.00
    }
  ]
}
```

#### 复制家庭菜单

```http
POST /api/merchant/families/{familyId}/menu/copy
```

请求：

```json
{
  "sourceFamilyId": 3
}
```

#### 设置首页推荐菜（旧兼容接口，已弃用）

```http
PUT /api/merchant/families/{familyId}/menu/featured
```

请求：

```json
{
  "dishId": 100
}
```

说明：

- 家庭必须属于当前登录商户。
- 该路由是 deprecated adapter：验证家庭归属后，等价调用 `PUT /api/merchant/dishes/{dishId}/featured` 并传 `{ "featured": true }`，推荐范围是商户全局而非单个家庭。
- 新代码必须使用商户菜品推荐接口；旧路由不支持取消推荐，也不再写入家庭的 `featuredDishId`。
- 菜品不存在、跨商户、已下架或商户已有 5 道推荐时，沿用商户菜品推荐接口的错误语义。
- 没有有效推荐菜时，家庭首页仍按 `sortOrder` 升序、菜品 ID 降序选择一个有效菜单项回退。

### 7.13 采购单

#### 汇总采购单

```http
GET /api/merchant/purchases/summary?date=2026-07-02&includePending=true
```

返回项：

```js
{
  ingredientName,
  quantity,
  unit,
  sourceStatus, // ESTIMATED / CONFIRMED
  sources: [
    { familyId, mealSlotId, orderId, dishId, serviceDate }
  ]
}
```

#### 按家庭查看采购项

```http
GET /api/merchant/purchases/by-family?familyId=2&date=2026-07-02&includePending=true
```

#### 勾选采购项

```http
POST /api/merchant/purchases/items/{itemId}/checked
```

请求：

```json
{
  "checked": true
}
```

#### 新增临时采购项

```http
POST /api/merchant/purchases/temp-items
```

请求：

```json
{
  "date": "2026-07-02",
  "mealSlotId": 20,
  "ingredientName": "番茄酱",
  "quantity": 2,
  "unit": "瓶",
  "remark": "临时补货"
}
```

#### 删除临时采购项

```http
DELETE /api/merchant/purchases/temp-items/{itemId}
```

#### 复制采购文本

```http
GET /api/merchant/purchases/copy-text?date=2026-07-02&mealSlotId=20
```

### 7.14 商户查看成员钱包流水

```http
GET /api/merchant/members/{memberId}/wallet/ledgers
```

### 7.15 商户调整成员余额

```http
POST /api/merchant/members/{memberId}/wallet/adjust
```

请求：

```json
{
  "type": "MANUAL_CREDIT",
  "amount": 20.00,
  "remark": "线下补记"
}
```

说明：
- 仅支持 `MANUAL_CREDIT`、`MANUAL_DEBIT`、`MERCHANT_RECHARGE`
- 调整后返回新增的那条钱包流水

### 7.16 PC 后台页面适配表

| PC 页面 | 已接入的稳定接口 | 可选增强项 |
| --- | --- | --- |
| 工作台 | `/api/merchant/orders`、`/api/merchant/purchases/summary`、`/api/notifications` | 独立经营概览统计接口 |
| 订单管理 | `/api/merchant/orders`、订单确认/拒绝/取消/配送费/状态推进 | 批量操作接口 |
| 菜品管理 | `/api/merchant/dishes`、`/api/merchant/dish-categories`、`/api/files/images` | 无 |
| 食材管理 | `/api/merchant/ingredients` 全套增删改查 | 无 |
| 家庭管理 | `/api/merchant/families`、详情、基础资料、配送设置、成员钱包流水、余额调整 | 无 |
| 家庭菜单配置 | `/api/merchant/families/{familyId}/menu`、保存、复制 | 无 |
| 采购清单 | `/api/merchant/purchases/summary`、`by-family`、临时项、勾选、复制文本 | 批量打印/导出接口 |
| 通知中心 | `/api/notifications`、已读、全部已读 | 按业务对象跳转需要前端路由映射 |

PC 后台优先真实接口接入顺序：
1. 订单管理
2. 菜品管理
3. 采购清单
4. 通知中心
5. 家庭管理
6. 家庭菜单配置
7. 食材管理

## 8. 前端联调顺序

### 8.1 家庭端最小闭环

1. `POST /api/auth/admin/login`
2. 保存 session，并补齐请求头
3. `GET /api/family/home`
4. `GET /api/family/meal-slots`
5. `GET /api/family/menu-items`
6. `GET /api/family/cart`
7. `POST /api/family/cart/items`
8. `PUT /api/family/cart/remark`
9. `POST /api/family/orders`
10. `GET /api/family/orders`
11. `GET /api/family/orders/{orderId}`

### 8.2 商户端最小闭环

1. `POST /api/auth/admin/login`
2. 保存 session，并补齐商户权限请求头
3. `GET /api/merchant/families`
4. `GET /api/merchant/families/{familyId}`
5. `GET /api/merchant/dish-categories`
6. `GET /api/merchant/dishes`
7. `POST /api/files/images`

## 19. 账号、通知与审核对接补充

### 19.1 登录会话与账号生命周期

| 场景 | 方法与路径 | 前端处理 |
| --- | --- | --- |
| 当前权限上下文 | `GET /api/users/me/context` | 用返回的家庭、商户、平台字段决定入口显示 |
| 修改用户名 | `PUT /api/users/me/username` | 每个账号仅一次，冲突时展示服务端消息 |
| 当前设备退出 | `POST /api/auth/logout` | 成功后清除本地 token 并回登录页 |
| 全部设备退出 | `POST /api/auth/logout-all` | 同上，其他设备后续请求会收到 401 |
| 申请注销 | `POST /api/users/me/cancellation` | 请求体为 `{ "password": "..." }` |
| 撤销注销 | `DELETE /api/users/me/cancellation` | 冷静期内恢复账号为 ACTIVE |

### 19.2 通知范围

普通用户查询通知时不传 `receiverScope`，后端默认使用 `user`。家庭通知传 `family`，商户后台通知传 `merchant`；若当前账号不具备对应关系，接口返回 HTTP 403。

### 19.3 菜品审核跟踪

商户端使用 `GET /api/merchant/dish-reviews` 展示审核历史，使用 `GET /api/merchant/dish-reviews/{reviewId}` 展示快照与审核原因，待审核记录可调用 `POST /api/merchant/dish-reviews/{reviewId}/withdraw` 撤回。

### 19.4 错误处理

不要再假设所有响应都是 HTTP 200。网络层应先读取 HTTP 状态，再解析统一响应体：401 清理登录态，403 展示权限不足，409 展示状态冲突，422 展示业务校验消息，503 进入维护页。
8. `POST /api/merchant/dishes`
9. `GET /api/merchant/orders`
10. `POST /api/merchant/orders/{orderId}/confirm`
11. `GET /api/merchant/purchases/summary`
12. `POST /api/merchant/members/{memberId}/wallet/adjust`
13. `GET /api/notifications`

### 8.3 PC 后台最小闭环

1. 调 `POST /api/auth/admin/login`
2. 将 session 保存到 `localStorage.family_kitchen_admin_session`
3. 用 `admin-web/api-client.js` 创建请求实例
4. `GET /api/merchant/orders` 加载订单列表
5. 接入订单确认、拒绝、取消、配送费调整、状态推进
6. `GET /api/merchant/dishes` 和分类接口加载菜品管理数据

## 21. 平台菜品模板市场

小程序商户端页面：`pages/merchant/dish-templates/index`、`pages/merchant/dish-template-detail/index`。PC 商户后台页面：`/dish-templates`。两端都只能使用以下真实接口，不从本地 Mock 或基础商户复制模板。

当前模板市场共 240 道菜，包括 198 道基础家常菜和 42 道地域特色菜。列表中的 `data.total` 是系统模板总数；商户菜品接口显示的是当前商户已经导入的副本数量，两者不能混用。

### 21.1 模板分类

```http
GET /api/merchant/dish-template-categories
```

返回 `categoryId`、`code`、`name`、`sortOrder`。

### 21.2 分页查询模板

```http
GET /api/merchant/dish-templates?categoryId=1&keyword=鸡&imported=false&page=1&pageSize=20
```

- `categoryId`、`keyword`、`imported` 可不传。
- `page` 默认 `1`；`pageSize` 默认 `20`、最大 `100`。
- `data.items` 包含 `templateId`、`templateCode`、分类、名称、简介、后端本地图片 URL、参考价、口味标签、推荐餐次、食材数量和 `imported`。
- 图片是后端相对路径，例如 `/images/dish-templates/dish-001.jpg`；小程序需要使用当前 API `baseUrl` 拼接，不能打入小程序代码包。

### 21.3 模板详情

```http
GET /api/merchant/dish-templates/{templateId}
```

详情额外返回 `imageSourceUrl`、`imageAuthor`、`imageLicense` 和 `ingredients`。模板没有 `cookingSteps`；导入后商户可在普通菜品编辑页自行补录。

### 21.4 选择性批量导入

```http
POST /api/merchant/dish-templates/import
Content-Type: application/json

{
  "templateIds": [1, 2, 3]
}
```

单次最多 100 道。服务端只从登录上下文读取商户 ID，模板首次导入后菜品状态为 `active`。同一商户重复导入同一模板时不会覆盖已有副本，而是返回到 `skippedIds`。

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "importedIds": [1, 2],
    "skippedIds": [3],
    "importedCount": 2,
    "skippedCount": 1
  }
}
```

常见错误：`请选择要导入的模板菜品`、`单次最多导入100道模板菜品`、`模板菜品不存在或已停用：[ID]`、`无商户后台访问权限`。

### 21.5 一键导入全部启用模板

```http
POST /api/merchant/dish-templates/import-all
```

- 请求不携带正文，也不允许前端传入商户 ID；商户身份只取自当前登录上下文。
- 后端读取全部启用模板并在一个事务内导入，不受选择导入单次 100 道上限影响。
- 同一商户已经导入的模板会计入 `skippedIds`/`skippedCount`，不会覆盖商户修改后的名称、价格、图片、食材、制作流程或上下架状态。
- 停用模板不会进入全量导入集合。
- 响应结构与 21.4 选择性批量导入一致。

小程序商户端从“菜品管理 → 系统菜库”进入模板市场。页面同时提供勾选导入和“导入全部”，两个操作互斥；全量导入必须先确认，成功后显示导入与跳过数量并刷新模板状态。
模板列表按接口分页加载：首次展示第一页，页面触底后继续请求下一页并追加，直到已展示数量达到 `data.total`。筛选、搜索、下拉刷新和导入成功后都从第一页重新加载。
7. `GET /api/merchant/families`、详情、基础资料、配送设置、余额调整加载家庭管理数据
8. `GET /api/merchant/purchases/summary` 加载采购清单
9. `GET /api/notifications` 加载通知中心
10. `GET /api/merchant/ingredients` 及增删改接口维护食材数据

## 9. 数据适配提醒

### 9.1 ID 类型

后端真实 ID 是数字：

```js
dishId: 100
familyId: 2
memberId: 10
```

页面跳转参数和 `dataset` 传递后会变成字符串，调用接口前应按接口要求转为数字并校验有效性。

### 9.2 状态值大小写

后端订单状态是大写：

```js
PENDING
CONFIRMED
PREPARING
READY
DONE
CANCELLED
REJECTED
```

如果当前前端页面使用小写状态，需要加一层映射。

### 9.3 金额字段

金额字段后端返回 number：

```js
price
basePrice
familyFinalPrice
deliveryFee
totalAmount
amount
balanceBefore
balanceAfter
```

前端展示统一用：

```js
`¥${Number(value || 0).toFixed(2)}`
```

### 9.4 日期字段

请求日期使用：

```text
yyyy-MM-dd
```

例如：

```js
2026-07-02
```

时间字段如 `createdAt` 是：

```text
yyyy-MM-ddTHH:mm:ss
```

### 9.5 图片地址

后端返回可能是相对路径：

```text
/uploads/images/xxx.png
```

前端展示前需要拼接：

```js
function toImageUrl(url) {
  if (!url) return '';
  if (/^https?:\/\//.test(url)) return url;
  return `${getApp().globalData.apiBaseUrl}${url}`;
}
```

## 10. 当前接口缺口

为了避免前端误接不存在的接口，当前还剩的主要缺口集中在“增强能力”，不是主链路阻塞项：

| 前端需求 | 当前状态 | 建议 |
| --- | --- | --- |
| PC 后台批量操作 | 后端未开放批量接口 | 先按单条操作完成第一版 |
| 经营统计看板 | 仍缺独立统计聚合接口 | 工作台先复用订单/采购/通知现有聚合数据 |
# 统一用户上下文适配

前端不得再把登录响应中的 `familyId` 或角色当作长期权限依据。访问令牌只标识用户，后端每次请求都会从关系表读取当前家庭及权限。

- 用户登录成功后可以处于“尚未加入家庭”状态。
- 账号资料、修改密码、绑定邮箱、绑定微信等页面不依赖家庭。
- 点餐、餐篮、家庭地址、家庭订单和采购清单需要有效家庭关系。
- 收到 `FAMILY_NOT_JOINED` 时进入加入或创建家庭页面，不要退出登录。
- 用户退出家庭后应清理本地家庭缓存和未提交餐篮，但保留登录状态。
- 后台小程序和 PC 端使用同一用户登录接口；只有存在平台或商户角色关系的用户才能进入后台。

## 20. 模板菜品修改审核三端适配

商户后台小程序和商户 PC 后台都可以提交模板修改；平台 PC 后台负责审核。家庭前台小程序只消费平台模板或商户菜品，不展示审核入口。

### 商户后台小程序与 PC

1. 在模板详情增加“申请修改”入口，先加载 `GET /api/merchant/dish-templates/{templateId}`；响应包含 `sortOrder/enabled/version`，可直接组装完整快照，再由用户编辑。
2. 图片先调用真实上传接口，提交时只传 `/uploads/` 或 `/images/` 相对路径。
3. 调用 `POST /api/merchant/dish-templates/{templateId}/change-requests`；不要传 `merchantId`。
4. 申请列表调用 `GET /api/merchant/dish-template-change-requests`，状态映射为待审核、已通过、已驳回、已撤回。
5. 详情同时展示 `baseSnapshot` 和 `targetSnapshot`；`stale=true` 时提示模板已变化，商户可撤回后重提。
6. 仅 `PENDING` 显示撤回按钮。409 错误直接展示后端中文 `message`。

### 平台 PC 后台

待审列表使用 `GET /api/admin/dish-template-change-requests?status=PENDING&page=1&pageSize=20`。详情页按字段和食材列表对比双快照；`stale=true`、模板停用或分类停用时禁用“通过”，仍允许驳回。通过请求体为 `{ "reason": "可选意见" }`，驳回请求体为 `{ "reason": "必填原因" }`。

审核完成后商户通知中心以 `receiverScope=merchant` 查询结果通知。已经导入的商户菜品不得在前端标记为“自动同步”，它们与平台模板相互独立。

### 20.1 从商户菜品申请同步到模板

商户菜品列表和详情现在返回只读字段 `sourceTemplateId`、`templateImported`。小程序商户端和 PC 商户后台仅在 `templateImported=true` 时显示“同步模板”，手工菜品不得显示入口。

```http
POST /api/merchant/dishes/{dishId}/template-change-requests
Content-Type: application/json

{
  "submitNote": "采用商户实测后的食材用量"
}
```

前端接入约束：

1. 请求体只发送可选 `submitNote`，不读取模板详情拼装快照，也不传 `merchantId`、`templateId`。
2. 点击前明确提示“提交后进入平台审核，不会立即修改系统菜库，制作步骤不会同步”。
3. 提交期间按 `dishId` 禁止重复点击。
4. 成功后使用响应 `requestId` 进入现有模板修改申请详情；申请状态仍使用 `PENDING/APPROVED/REJECTED/WITHDRAWN`。
5. 400、403、404、409、422 的后端中文 `message` 直接展示，尤其是手工菜品、来源模板停用、空食材和重复待审核申请。
6. 审核通过只更新平台模板，不把新模板自动覆盖到当前商户或其他商户已有菜品。

服务端自动覆盖菜名、简介、图片、价格和食材；模板分类、标签、餐次、图片授权、排序和启用状态保持来源模板值。因此前端没有这些字段的选择或编辑步骤。

## 21. 家庭共享餐篮、预计用餐时间与家庭钱包（当前权威契约）

本节自 2026-08-25 起取代本文旧版按 `date + mealSlotId` 划分餐篮、提交订单选择付款成员、个人钱包结算的说明。旧字段只用于读取历史订单，不得用于新点餐流程。

### 21.1 单一活动餐篮

```http
GET /api/family/cart
```

查询不接收日期或餐次参数。响应必须包含服务器权威可用性字段：

```json
{
  "serverNow": "2026-08-28T17:02:00",
  "serverDate": "2026-08-28",
  "minimumExpectedMealTime": "2026-08-28T17:15:00",
  "timeStepMinutes": 15,
  "bookingEnded": false,
  "cartId": 7,
  "version": 3,
  "expectedMealTime": "2026-08-28T18:30:00",
  "remark": "少盐",
  "totalQuantity": 3,
  "totalAmount": 114.00,
  "items": [{
    "itemId": 21,
    "dishId": 9,
    "dishName": "番茄牛腩",
    "price": 38.00,
    "quantity": 3,
    "currentMemberQuantity": 1,
    "currentMemberRemark": "少盐",
    "selections": [
      { "memberId": 10, "memberName": "小林", "quantity": 1, "itemRemark": "少盐" },
      { "memberId": 11, "memberName": "阿禾", "quantity": 2, "itemRemark": null }
    ]
  }]
}
```

`quantity` 是全家汇总数量；当前成员只能修改自己的绝对数量 `currentMemberQuantity`。主列表只显示汇总，`selections` 仅在用户主动展开明细时展示。

所有餐篮写操作必须带 `cartId`、当前 `cartVersion` 和同一次用户动作内稳定的 `requestId`：

```http
PUT /api/family/cart/items
PUT /api/family/cart/expected-meal-time
PUT /api/family/cart/remark
```

菜品更新请求使用绝对数量而不是增量；数量 `0` 表示当前成员移除自己的选择。预计用餐时间只能从 `minimumExpectedMealTime` 开始，按 `timeStepMinutes` 生成，并且必须位于 `serverDate` 当天。客户端本地时钟不得扩大服务器允许范围；`bookingEnded=true` 时禁用提交。

`requestId` 用于幂等：相同请求号与相同负载返回同一结果；同一请求号若被不同负载复用则返回冲突。`CART_CHANGED`（40931）表示版本过期，前端刷新并要求用户重新操作；`CART_SUBMITTED`（40932）表示原餐篮已提交，前端加载新的空餐篮，不重放旧动作。

### 21.2 提交与订单快照

```http
POST /api/family/orders
Content-Type: application/json

{
  "cartId": 7,
  "cartVersion": 4,
  "requestId": "submit-...",
  "deliveryMode": "PICKUP",
  "addressId": null,
  "remark": "少盐"
}
```

新提交不得发送 `date`、`mealSlotId`、`payerMemberId` 或个人钱包字段。提交成功后，原餐篮成为不可变订单快照，并立即为家庭生成一个新的活动餐篮；用户可继续点菜并提交新的预约。

家庭和商户订单列表返回聚合菜品，不携带成员 `selections`；详情接口返回提交时冻结的成员昵称、数量和备注快照。新订单显示 `expectedMealTime`。只有该字段为空的历史订单才回退显示 `serviceDate + mealSlotName`，不得虚构餐次。

### 21.3 家庭钱包

```http
GET /api/family/wallet
GET /api/family/wallet/ledgers?page=1&pageSize=20

GET /api/merchant/families/{familyId}/wallet
GET /api/merchant/families/{familyId}/wallet/ledgers?page=1&pageSize=20
POST /api/merchant/families/{familyId}/wallet/adjust
```

商户调整请求体：

```json
{
  "requestId": "family-wallet-...",
  "type": "MANUAL_CREDIT",
  "amount": 20.00,
  "remark": "商户手动充值"
}
```

订单提交冻结、拒单/取消释放、完成扣减均操作家庭钱包。成员选择归属仅用于订单明细与审计，不参与拆分扣款。旧 `/api/family/me/wallet/ledgers`、`/api/merchant/members/{memberId}/wallet/*` 和 `/api/family/meal-slots` 为退役契约，新页面不得调用；个人钱包余额切换由受控迁移流程完成，禁止在当前业务数据库中以页面操作或普通启动流程执行清空。
