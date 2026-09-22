# 家庭厨房单容器部署 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个同时运行 MySQL、Spring Boot、Nginx 的家庭厨房单容器镜像，并将全部业务数据持久化到 `/data`。

**Architecture:** 使用多阶段构建生成 Vue 管理后台和 Spring Boot JAR，最终 Ubuntu 运行层安装 MySQL 8、Java 17、Nginx 与 tini。入口脚本串行完成数据库初始化和服务启动，并监控三个长期进程。

**Tech Stack:** Docker、Ubuntu 24.04、MySQL 8、Java 17、Spring Boot、Flyway、Nginx、POSIX shell

---

### Task 1: 固化单容器运行层

**Files:**
- Modify: `docker/single-image/Dockerfile`
- Create: `docker/single-image/mysql.cnf`
- Create: `docker/single-image/healthcheck.sh`

- [x] 将运行层改为包含 MySQL 8、Java 17、Nginx 和 tini 的 Ubuntu 镜像。
- [x] 把所有持久化目录统一到 `/data`。
- [x] 添加面向小型服务器的 MySQL 默认参数。
- [x] 添加同时检查 MySQL 与 HTTP 代理链路的健康检查。

### Task 2: 实现可靠的多进程入口

**Files:**
- Modify: `docker/single-image/entrypoint.sh`

- [x] 校验必填环境变量和安全字符。
- [x] 首次启动初始化 MySQL 数据目录。
- [x] 创建数据库与业务账号并等待 MySQL 就绪。
- [x] 启动并监控 Spring Boot 与 Nginx。
- [x] 实现信号转发和失败联动退出。

### Task 3: 提供新手启动和备份入口

**Files:**
- Create: `docker/single-image/family-kitchen.env.example`
- Create: `docker/single-image/backup.sh`
- Create: `scripts/run-single-container.ps1`
- Create: `scripts/run-single-container.sh`

- [x] 提供不依赖 `.env` 的显式配置样例。
- [x] 提供一个卷、一个端口的启动脚本。
- [x] 提供数据库与文件的一体化备份命令。

### Task 4: 更新部署文档并做静态验证

**Files:**
- Modify: `docs/deployment.md`
- Create: `scripts/verify-single-container-package.mjs`

- [x] 将单镜像章节改为单容器内置 MySQL 的真实命令。
- [x] 说明 HTTPS 接入、持久化、备份、恢复和升级步骤。
- [x] 运行静态验证脚本，检查 Dockerfile、入口脚本、挂载路径和必需文档。
- [ ] 在 Docker 可用环境执行镜像构建和首次启动冒烟测试。
