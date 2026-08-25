# Merchant Self Profile Edit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让有效商户负责人在小程序商户端安全查看和编辑商户名称、联系人、联系电话。

**Architecture:** 后端新增独立 MerchantProfile 边界，GET/PUT 均以实时 userId、merchantId 和 ACTIVE MERCHANT_ADMIN 关系约束，PUT 在单条 SQL 内再次校验关系。小程序新增独立编辑页，工作台并行加载 profile 并以其名称为权威来源；403 通过实时身份刷新失效关闭。

**Tech Stack:** Spring Boot 3、MyBatis、Jakarta Validation、JUnit 5/Mockito、微信小程序 WXML/WXSS/Node test。

---

### Task 1: 后端商户资料契约与权限测试

**Files:**
- Create: `backend/src/test/java/com/familykitchen/merchant/MerchantProfileControllerContractTest.java`
- Create: `backend/src/test/java/com/familykitchen/merchant/MerchantProfileServiceTest.java`
- Create: `backend/src/test/java/com/familykitchen/merchant/MerchantProfileMapperContractTest.java`
- Create: `backend/src/test/java/com/familykitchen/merchant/MerchantProfileMySqlTest.java`

- [ ] **Step 1: 写失败的控制器契约测试**

断言 GET/PUT `/merchant/profile`，VO/DTO 只含 `name/contactName/contactPhone`，不含 merchantId/status/ownerUserId。

- [ ] **Step 2: 写失败的服务与 Mapper 测试**

覆盖正常查询/更新、trim/空串转 null、无 merchantId、非 MERCHANT_ADMIN、商户非 active、关系撤销导致更新 0；Mapper SQL 必须包含 userId、merchantId、MERCHANT_ADMIN、ACTIVE 和 merchant status=active。

- [ ] **Step 3: 写 Testcontainers MySQL 集成测试**

使用仓库现有 `@Testcontainers(disabledWithoutDocker = true)` 模式，真实执行 Mapper：有效关系成功更新；关系撤销、非 MERCHANT_ADMIN、pending/rejected/inactive 商户均更新 0；另一商户资料不受影响。

- [ ] **Step 4: 运行测试确认 RED**

Run:
`D:\develop\apache-maven-3.9.9\bin\mvn.cmd -q -f backend/pom.xml -Dtest=MerchantProfileControllerContractTest,MerchantProfileServiceTest,MerchantProfileMapperContractTest,MerchantProfileMySqlTest test`

Expected: FAIL，因为 MerchantProfile 类型和路由尚不存在。

### Task 2: 实现后端商户资料边界

**Files:**
- Create: `backend/src/main/java/com/familykitchen/merchant/model/dto/UpdateMerchantProfileRequest.java`
- Create: `backend/src/main/java/com/familykitchen/merchant/model/vo/MerchantProfileView.java`
- Create: `backend/src/main/java/com/familykitchen/merchant/mapper/MerchantProfileMapper.java`
- Create: `backend/src/main/java/com/familykitchen/merchant/service/MerchantProfileService.java`
- Create: `backend/src/main/java/com/familykitchen/merchant/service/impl/MerchantProfileServiceImpl.java`
- Create: `backend/src/main/java/com/familykitchen/merchant/controller/MerchantProfileController.java`

- [ ] **Step 1: 建立三字段 DTO/VO**

`name` 使用 `@NotBlank @Size(max=100)`，联系人和电话使用对应长度限制；服务统一 trim，空字符串转 null。

- [ ] **Step 2: 实现严格 Mapper**

GET 通过 JOIN/EXISTS 校验当前用户的 ACTIVE MERCHANT_ADMIN 关系和 active 商户。PUT 使用：

```sql
UPDATE merchants m
SET name=#{name}, contact_name=#{contactName}, contact_phone=#{contactPhone}
WHERE m.id=#{merchantId} AND m.status='active'
AND EXISTS (
  SELECT 1 FROM merchant_user_relations mur
  WHERE mur.merchant_id=m.id AND mur.user_id=#{userId}
    AND mur.merchant_role='MERCHANT_ADMIN' AND mur.status='ACTIVE'
)
```

- [ ] **Step 3: 实现服务与控制器**

服务拒绝缺少 merchantId 的上下文，查询为空或更新 0 返回 FORBIDDEN/NOT_FOUND 的可读错误；控制器只使用 CurrentUserProvider 实时上下文。

- [ ] **Step 4: 运行聚焦测试确认 GREEN**

运行 Task 1 命令，Expected: PASS。

### Task 3: 小程序服务和身份撤销辅助逻辑

**Files:**
- Modify: `frontend/services/merchant.js`
- Modify: `frontend/tests/services.test.js`
- Create: `frontend/utils/merchant-profile.js`
- Create: `frontend/tests/merchant-profile.test.js`

- [ ] **Step 1: 写失败的服务与辅助函数测试**

断言 `getProfile()` 精确 GET `/api/merchant/profile`，`updateProfile(payload)` 精确 PUT；覆盖表单 snapshot、校验、403 识别和刷新身份后的目标地址。

- [ ] **Step 2: 运行测试确认 RED**

Run: `node --test frontend/tests/services.test.js frontend/tests/merchant-profile.test.js`

- [ ] **Step 3: 实现最小服务与纯函数**

辅助模块负责 trim/长度校验、错误类型判断和 `failClosedMerchantSession`。403 后页面先同步移除本地 MERCHANT_ADMIN、merchantId、merchant scopes 并锁定编辑能力，再调用现有 `refreshAccountIdentity`；刷新失败仍保持失效关闭并跳转账号管理页。

- [ ] **Step 4: 重跑确认 GREEN**

### Task 4: 商户资料编辑页

**Files:**
- Create: `frontend/pages/merchant/merchant-profile-edit/index.js`
- Create: `frontend/pages/merchant/merchant-profile-edit/index.json`
- Create: `frontend/pages/merchant/merchant-profile-edit/index.wxml`
- Create: `frontend/pages/merchant/merchant-profile-edit/index.wxss`
- Modify: `frontend/app.json`
- Create: `frontend/tests/merchant-profile-edit-page.test.js`

- [ ] **Step 1: 写失败的页面行为与模板测试**

覆盖加载/重试、字段校验、取消、保存失败保留草稿、双击只请求一次、保存成功 navigateBack、403 先失效关闭再刷新身份、身份刷新网络失败仍离开编辑页；模板禁止原生 button，逐个控件检查 aria、88rpx、小屏和安全区。并读取家庭管理页 WXML，断言不存在商户资料编辑入口。

- [ ] **Step 2: 运行测试确认 RED**

Run: `node --test frontend/tests/merchant-profile-edit-page.test.js frontend/tests/native-button-free-contract.test.js`

- [ ] **Step 3: 实现页面**

使用可注入依赖的 page factory 便于测试；自定义 view 按钮，loading/error/ready 三态，busy 锁和草稿保留。

- [ ] **Step 4: 重跑确认 GREEN**

### Task 5: 工作台 profile 权威名称与入口

**Files:**
- Modify: `frontend/pages/merchant/index.js`
- Modify: `frontend/pages/merchant/index.wxml`
- Modify: `frontend/pages/merchant/index.wxss`
- Modify: `frontend/utils/merchant-scenes.js`
- Modify: `frontend/tests/merchant-page-family-contract.test.js`
- Modify: `frontend/tests/merchant-responsive-contract.test.js`

- [ ] **Step 1: 写失败测试**

覆盖 profile 名称优先、无家庭仍显示 profile 名称、普通 profile 失败只显示局部错误、编辑入口、403 先失效关闭再刷新身份；身份刷新失败仍跳转账号管理页且本地不保留商户权限。

- [ ] **Step 2: 运行聚焦测试确认 RED**

Run: `node --test frontend/tests/merchant-page-family-contract.test.js frontend/tests/merchant-responsive-contract.test.js`

- [ ] **Step 3: 实现并行 profile 加载与入口**

在工作台 region state 增加 profile，buildApiMerchantScene 优先使用 `merchantProfile.name`；顶部放高对比度自定义编辑入口。

- [ ] **Step 4: 重跑确认 GREEN**

### Task 6: 文档与完整验证

**Files:**
- Modify: `docs/frontend-api-guide.md`

- [ ] **Step 1: 补充 GET/PUT 商户资料接口、权限、字段白名单和 403 行为**
- [ ] **Step 2: 运行完整后端测试**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -q -f backend/pom.xml test`

- [ ] **Step 3: 运行完整小程序测试**

Run: `D:\develop\WebDev\node\npm.cmd --prefix frontend test`

- [ ] **Step 4: 运行语法与差异检查**

Run: `node --check frontend/pages/merchant/merchant-profile-edit/index.js`

Run: `git diff --check`
