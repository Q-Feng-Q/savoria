# Family Management Button Contrast Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复家庭管理页按钮与承载背景过于接近的问题，并统一所有可点击控件的状态和触控高度。

**Architecture:** 只在家庭管理页根节点声明 `--fm-*` 颜色变量，替换未定义的旧主题变量；WXML 为 busy 状态显式附加禁用类。通过选择器级契约测试锁定主、次、正向、普通、危险、按压和禁用状态。

**Tech Stack:** 微信小程序 WXML/WXSS、Node.js test。

---

### Task 1: 建立对比度回归契约

**Files:**
- Modify: `frontend/tests/family-management-page-contract.test.js`

- [ ] **Step 1: 写失败测试**

断言页面不含 `--sk-coral`/`--sk-ink`；存在 `--fm-primary/secondary/positive/danger/ink`；主、次、正向、拒绝、危险按钮声明块具有明确背景/文字/边框；按压态不降低 opacity；禁用态 opacity=.58。

- [ ] **Step 2: 对每类控件逐个断言 88rpx**

覆盖 section-quiet-action、profile-action、section-link、transfer-action、block-action、invitation-code、mini-action、danger-action。

- [ ] **Step 3: 断言 WXML busy 控件带动态 is-disabled**

覆盖保存、取消、移交、生成邀请码、发送邀请、退出/解散。邀请接受/拒绝和申请通过/拒绝没有 busy 状态，本次不改变其业务行为。

- [ ] **Step 4: 运行测试确认 RED**

Run: `node --test frontend/tests/family-management-page-contract.test.js`

Expected: FAIL，指出未定义变量和状态色不足。

### Task 2: 实现页面级高对比度按钮体系

**Files:**
- Modify: `frontend/pages/family/family-management/index.wxml`
- Modify: `frontend/pages/family/family-management/index.wxss`

- [ ] **Step 1: 添加页面级颜色变量并替换旧变量**

主色 `#9f4f3d`、深咖啡 `#4f392f`、正向 `#55705c`、危险 `#a63f35`、次按钮边框 `#9b765f`。

- [ ] **Step 2: 分别实现按钮状态**

主按钮深珊瑚白字；次按钮暖白深咖啡；正向按钮深绿白字；拒绝按钮暖白深咖啡；危险按钮浅红深红。

- [ ] **Step 3: 显式实现 disabled/pressed 和 88rpx**

按压只缩放并增加内阴影，不降低透明度；禁用保留颜色并使用 `opacity:.58`。删除文件末尾的补丁式覆盖，将 88rpx 合并进各选择器声明。

- [ ] **Step 4: 为所有 busy 控件绑定 is-disabled**

不使用属性选择器或微信原生 button。

- [ ] **Step 5: 运行聚焦测试确认 GREEN**

Run: `node --test frontend/tests/family-management-page-contract.test.js frontend/tests/native-button-free-contract.test.js frontend/tests/responsive-warm-animal-contract.test.js`

### Task 3: 完整验证

- [ ] **Step 1: 运行完整小程序测试**

Run: `D:\develop\WebDev\node\npm.cmd --prefix frontend test`

- [ ] **Step 2: 运行 WXSS/WXML 编译兼容契约与差异检查**

Run: `node --test frontend/tests/wxml-expression-contract.test.js frontend/tests/wxss-compiler-compat.test.js`

Run: `node --check frontend/tests/family-management-page-contract.test.js`

Run: `git diff --check`

- [ ] **Step 3: 记录视觉检查**

在微信开发者工具可用时检查 320px、360px、大字体和安全区；不可用时在交付说明中明确未执行。
