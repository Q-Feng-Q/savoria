# 微信云托管小程序请求接入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让现有小程序服务层透明地访问微信云托管 Spring Boot 服务。

**Architecture:** 运行时在统一 API 入口选择云托管适配器或 HTTP 适配器。普通接口使用 `callContainer`，上传下载保留微信原生文件 API并注入云托管服务头。

**Tech Stack:** 微信原生小程序、Node.js `node:test`、Spring Boot REST API

---

### Task 1: 云托管适配器

**Files:**
- Create: `frontend/utils/cloud-hosting.js`
- Create: `frontend/tests/cloud-hosting.test.js`

- [ ] 写路径、请求头和回退行为的失败测试。
- [ ] 运行测试并确认因模块缺失而失败。
- [ ] 实现最小适配器并让测试通过。

### Task 2: 接入应用运行时

**Files:**
- Modify: `frontend/app.js`
- Modify: `frontend/utils/api-runtime.js`
- Modify: `frontend/tests/api-runtime.test.js`
- Modify: `frontend/tests/proxy-contract.test.js`

- [ ] 写云配置自动选择 `callContainer` 的失败测试。
- [ ] 初始化云能力并把适配器注入现有 service。
- [ ] 验证 HTTP 显式注入仍可用于测试和本地开发。

### Task 3: 全量验证

- [ ] 运行 `node --test tests/cloud-hosting.test.js tests/api-runtime.test.js tests/proxy-contract.test.js`。
- [ ] 运行 `npm test`。
- [ ] 检查 Git diff，确认未加入凭据或运行数据。
