# 食光知味项目部署文档

本文档基于当前仓库的实际代码与配置编写，适用于：

- 后端：Java 17、Spring Boot 3.0.5、MyBatis、Flyway、MySQL 8
- 管理后台：Vue 3、Vite 5
- 用户端：微信原生小程序

推荐的生产拓扑如下：

```text
微信小程序 ───────────────┐
                         │ HTTPS
浏览器 ──> Nginx ── /api ┼──> Spring Boot :8080 ──> MySQL 8
             │           │              │
             └─ 管理后台静态文件         └─ 本地上传目录
```

## 1. 部署前检查

### 当前初始化基线（2026-09-23）

按重新建库方案，所有结构与基础数据已归并至以下三份脚本，位于 `backend/src/main/resources/db/migration/`：

1. `V1__init_schema.sql`：完整表结构，已包含多尺寸 Logo 配置和制作步骤图片字段。
2. `V2__init_system_and_admin.sql`：系统配置、管理员、默认商户和字典数据。
3. `V3__init_recipe_catalog.sql`：完整内置菜谱目录。

原 V4、V5 增量脚本已移除。发布前必须执行 Maven `clean` 构建（例如 `mvn clean package`），避免 `target/classes` 或旧 JAR 仍携带已删除脚本。选择全新空数据库并由 Flyway 顺序初始化，不要先手动执行三份 SQL 再让 Flyway 重复执行。

**该基线不能直接升级已有数据库。** 不要靠删除旧库的 Flyway 历史或执行 `repair` 来绕过结构差异；需要保留旧数据时先备份，并另行制定迁移方案。本次代码修改没有执行数据库删除、重建或初始化。

### 1.1 环境要求

| 组件 | 建议版本 | 用途 |
| --- | --- | --- |
| Linux | Ubuntu 22.04 LTS 或同等级发行版 | 生产服务器 |
| JDK | 17 | 构建、运行后端 |
| Maven | 3.8+ | 构建后端 |
| MySQL | 8.0 | 业务数据库 |
| Node.js | 18+ | 构建管理后台 |
| npm | 随 Node.js 安装 | 安装前端依赖 |
| Nginx | 1.20+ | HTTPS、静态文件、接口反向代理 |
| 微信开发者工具 | 当前稳定版 | 构建并上传小程序 |

服务器至少应放行 `80`、`443` 端口。后端的 `8080` 端口建议只监听内网或仅允许 Nginx 访问，不直接暴露到公网。

### 1.2 上线前必须修改

`backend/src/main/resources/application.yml` 不再提供数据库密码、JWT 密钥和初始管理员密码默认值。本地运行和容器启动前都必须提供这些环境变量。

至少应修改：

- 数据库地址、用户名、强密码
- JWT 密钥，建议使用 32 字节以上的随机值
- 初始管理员用户名和强密码
- 上传文件的持久化目录
- 微信小程序 `AppID`、`AppSecret`
- 小程序中的生产 API 地址
- HTTPS 证书和微信小程序服务器域名白名单

> 如果数据库中已经存在同名管理员，修改“初始管理员”环境变量不会重置该账号的密码。

## 2. 本地联调部署

### 2.1 初始化 MySQL

登录 MySQL 后执行：

```sql
CREATE DATABASE family_kitchen
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'family_kitchen'@'%' IDENTIFIED BY '请替换为强密码';
GRANT ALL PRIVILEGES ON family_kitchen.* TO 'family_kitchen'@'%';
FLUSH PRIVILEGES;
```

后端启动时，Flyway 会自动执行 `backend/src/main/resources/db/migration` 下的数据库迁移。不要手工重复执行迁移 SQL，也不要修改已经在生产环境执行过的迁移文件；需要变更数据库时应新增更高版本的迁移。

### 2.2 启动后端

PowerShell 示例：

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/family_kitchen?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
$env:SPRING_DATASOURCE_USERNAME='family_kitchen'
$env:SPRING_DATASOURCE_PASSWORD='请替换为数据库密码'
$env:FAMILY_KITCHEN_JWT_SECRET='请替换为足够长的随机密钥'
$env:FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD='请替换为管理员强密码'
mvn spring-boot:run
```

启动成功后：

- 后端地址：`http://127.0.0.1:8080`
- Swagger：`http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI：`http://127.0.0.1:8080/v3/api-docs`

### 2.3 启动管理后台

```powershell
cd admin-web
npm install
npm run dev
```

默认访问地址为 `http://127.0.0.1:5174`。Vite 会把 `/api/*` 转发到 `http://127.0.0.1:8080/*`。

如后端不在本机，可先复制并修改环境配置：

```powershell
Copy-Item .env.example .env.local
```

`.env.local` 示例：

```dotenv
VITE_PROXY_TARGET=http://192.168.1.10:8080
VITE_API_BASE_URL=/api
```

### 2.4 启动小程序联调代理

当前小程序的 `frontend/app.js` 默认请求 `http://127.0.0.1:8080`，后端在本机时可直接用微信开发者工具打开 `frontend` 目录。

如果需要通过本地代理联调：

```powershell
cd frontend
$env:MINI_PROXY_TARGET='http://127.0.0.1:8080'
npm run proxy
```

代理默认监听 `http://127.0.0.1:3001/api/*`，并把请求转发到后端。使用代理时需要同步调整 `frontend/app.js` 中的 `apiBaseUrl`。

## 3. 生产环境部署

以下以 Linux 服务器上的 `/opt/family-kitchen` 为程序目录、`/var/lib/family-kitchen/uploads` 为上传文件目录进行说明。

### 3.1 构建后端

在项目根目录执行：

```bash
cd backend
mvn clean test
mvn clean package -DskipTests
```

构建产物：

```text
backend/target/family-kitchen-backend-0.1.0-SNAPSHOT.jar
```

将 JAR 上传到服务器：

```text
/opt/family-kitchen/backend/family-kitchen-backend.jar
```

### 3.2 配置后端环境变量

创建 `/etc/family-kitchen/backend.env`：

```dotenv
SERVER_PORT=8080

SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/family_kitchen?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=true
SPRING_DATASOURCE_USERNAME=family_kitchen
SPRING_DATASOURCE_PASSWORD=请替换为数据库强密码

FAMILY_KITCHEN_FILE_STORAGE_LOCAL_ROOT=/var/lib/family-kitchen/uploads
FAMILY_KITCHEN_JWT_SECRET=请替换为至少32字节的随机密钥
FAMILY_KITCHEN_JWT_EXPIRES_IN_SECONDS=7200

FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_USERNAME=admin
FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_DISPLAY_NAME=系统管理员
FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD=请替换为管理员强密码

FAMILY_KITCHEN_WECHAT_APP_ID=请替换为小程序AppID
FAMILY_KITCHEN_WECHAT_APP_SECRET=请替换为小程序AppSecret
```

如果 MySQL 服务器没有正确配置 TLS，先完成数据库 TLS 配置，再启用 `useSSL=true`。不要通过关闭证书校验来掩盖生产环境的证书问题。

限制配置文件权限：

```bash
sudo chown root:family-kitchen /etc/family-kitchen/backend.env
sudo chmod 640 /etc/family-kitchen/backend.env
```

### 3.3 使用 systemd 运行后端

创建专用用户和目录：

```bash
sudo useradd --system --home /opt/family-kitchen --shell /usr/sbin/nologin family-kitchen
sudo mkdir -p /opt/family-kitchen/backend /var/lib/family-kitchen/uploads
sudo chown -R family-kitchen:family-kitchen /opt/family-kitchen/backend /var/lib/family-kitchen
```

创建 `/etc/systemd/system/family-kitchen.service`：

```ini
[Unit]
Description=Family Kitchen Backend
After=network-online.target mysql.service
Wants=network-online.target

[Service]
Type=simple
User=family-kitchen
Group=family-kitchen
WorkingDirectory=/opt/family-kitchen/backend
EnvironmentFile=/etc/family-kitchen/backend.env
ExecStart=/usr/bin/java -jar /opt/family-kitchen/backend/family-kitchen-backend.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143
NoNewPrivileges=true
PrivateTmp=true
UMask=0027

[Install]
WantedBy=multi-user.target
```

加载并启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now family-kitchen
sudo systemctl status family-kitchen
```

查看日志：

```bash
sudo journalctl -u family-kitchen -f
```

### 3.4 构建管理后台

`admin-web/public/backend.config.json` 的生产推荐配置为：

```json
{
  "apiBaseUrl": "/api",
  "devProxyTarget": "",
  "preferProxyInDev": false
}
```

构建：

```bash
cd admin-web
npm ci
npm run build
```

将 `admin-web/dist` 中的全部文件部署到：

```text
/var/www/family-kitchen-admin
```

### 3.5 配置 Nginx

下面配置同时处理 Vue History 路由、API 反向代理和上传文件访问。把域名及证书路径替换为真实值：

```nginx
server {
    listen 80;
    server_name admin.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name admin.example.com;

    ssl_certificate     /etc/letsencrypt/live/admin.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/admin.example.com/privkey.pem;

    root /var/www/family-kitchen-admin;
    index index.html;
    client_max_body_size 4m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080/uploads/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

检查并重载：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

生产环境推荐让管理后台与 API 使用同一域名，避免额外的跨域配置。不要把 Swagger 暴露到公网；如确需访问，应通过 VPN、内网或 Nginx 访问控制进行保护。

### 3.6 Docker 单镜像部署

该方案面向单机、小规模和低部署成本场景。一个镜像、一个容器同时包含 MySQL、Spring Boot 和 Nginx：

- Nginx：监听容器 `8080`，托管管理后台并代理 `/api`、`/uploads`
- Spring Boot：只监听容器回环地址 `127.0.0.1:8081`
- MySQL 8：只监听容器回环地址 `127.0.0.1:3306`
- Java 17 JRE：默认最大堆为 `512m`
- `tini`：负责 PID 1 信号转发与子进程回收

对外只需映射容器 `8080`。HTTPS 由宿主机上的宝塔、1Panel、Nginx、Caddy 或云负载均衡器处理，不要直接向公网暴露 MySQL。

镜像中的“前端”是 `admin-web` PC 管理后台。微信小程序 `frontend` 仍需通过微信开发者工具上传并发布，它不作为网页运行在容器中，只通过配置好的 HTTPS 域名访问容器 API。

所有需要保留的数据统一位于 `/data`：

| 目录 | 内容 |
| --- | --- |
| `/data/mysql` | MySQL 数据文件 |
| `/data/uploads` | 公开上传文件与已发布模板图片 |
| `/data/feedback-private` | 意见反馈私有附件 |
| `/data/dish-template-assets` | 模板菜品待审核私有素材 |
| `/data/backups` | 一键备份生成的文件 |
| `/data/config` | 容器自动生成的数据库内部凭据，禁止手工删除或外传 |

部署时必须把整个 `/data` 挂载到 Docker 命名卷或宿主机目录。重建容器时继续挂载同一个卷即可保留数据库和文件。

#### PowerShell 构建

在项目根目录执行：

```powershell
.\scripts\build-single-image.ps1 `
  -ImageName family-kitchen `
  -Tag 0.1.0
```

构建指定平台的无缓存镜像：

```powershell
.\scripts\build-single-image.ps1 `
  -ImageName registry.example.com/family-kitchen `
  -Tag 0.1.0 `
  -Platform linux/amd64 `
  -NoCache
```

#### Bash 构建

```bash
chmod +x scripts/build-single-image.sh
./scripts/build-single-image.sh \
  --image-name family-kitchen \
  --tag 0.1.0
```

构建指定平台的无缓存镜像：

```bash
./scripts/build-single-image.sh \
  --image-name registry.example.com/family-kitchen \
  --tag 0.1.0 \
  --platform linux/amd64 \
  --no-cache
```

两个脚本默认在 Docker 构建阶段执行后端和管理后台测试。需要快速构建时可以使用 PowerShell 的 `-SkipTests` 或 Bash 的 `--skip-tests`；正式发布不建议跳过测试。

| 功能 | PowerShell | Bash |
| --- | --- | --- |
| 镜像名称 | `-ImageName NAME` | `--image-name NAME` |
| 镜像标签 | `-Tag TAG` | `--tag TAG` |
| 跳过测试 | `-SkipTests` | `--skip-tests` |
| 禁用缓存 | `-NoCache` | `--no-cache` |
| 目标平台 | `-Platform linux/amd64` | `--platform linux/amd64` |

#### 容器环境变量

复制配置模板并修改：

```bash
cp docker/single-image/family-kitchen.env.example /opt/family-kitchen/family-kitchen.env
chmod 600 /opt/family-kitchen/family-kitchen.env
```

关键配置如下：

```dotenv
MYSQL_DATABASE=family_kitchen
MYSQL_APP_USERNAME=family_kitchen
MYSQL_APP_PASSWORD=请设置至少12位数据库密码

FAMILY_KITCHEN_JWT_SECRET=请替换为至少32字节的随机密钥
FAMILY_KITCHEN_JWT_EXPIRES_IN_SECONDS=7200

FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_USERNAME=admin
FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_DISPLAY_NAME=系统管理员
FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD=请替换为管理员强密码

FAMILY_KITCHEN_WECHAT_APP_ID=请替换为小程序AppID
FAMILY_KITCHEN_WECHAT_APP_SECRET=请替换为小程序AppSecret
```

容器会自动生成内部 JDBC 地址，不要再设置 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME` 或 `SPRING_DATASOURCE_PASSWORD`。业务数据库密码只接受字母、数字和 `._~:@%+=,-`，用于避免首次初始化时产生 SQL 歧义。

这里的 `family-kitchen.env` 是 Docker `--env-file` 显式读取的服务器配置文件，不是 Spring Boot 自动读取的 `.env`。应用本身不依赖 `.env` 文件。配置文件应限制访问权限，且不能提交到版本库。

#### 启动容器

Linux 服务器执行：

```bash
chmod +x scripts/run-single-container.sh
./scripts/run-single-container.sh \
  --image family-kitchen:0.1.0 \
  --env-file /opt/family-kitchen/family-kitchen.env
```

Windows PowerShell 执行：

```powershell
.\scripts\run-single-container.ps1 `
  -Image family-kitchen:0.1.0 `
  -EnvFile C:\family-kitchen\family-kitchen.env
```

两个脚本默认只绑定宿主机 `127.0.0.1:8080`，并创建或复用命名卷 `family-kitchen-data:/data`。如果 HTTPS 反向代理位于另一台服务器，可通过 `--bind 0.0.0.0` 或 `-BindAddress 0.0.0.0` 调整，但应同时设置防火墙白名单。

访问：

```text
管理后台：http://服务器地址:8080/
后端接口：http://服务器地址:8080/api/*
上传文件：http://服务器地址:8080/uploads/*
```

宿主机 HTTPS 反向代理的上游地址设置为 `http://127.0.0.1:8080`。公网只开放 `80/443`，微信小程序接口域名使用最终的 HTTPS 域名。

#### 检查运行状态

```bash
docker logs -f family-kitchen
docker inspect --format '{{json .State.Health}}' family-kitchen
curl -i http://127.0.0.1:8080/api/public/system-settings
```

Docker `HEALTHCHECK` 会同时检查 MySQL、Spring Boot、Nginx 和代理链路。MySQL、Java 或 Nginx 任一进程退出时，入口脚本都会结束容器，`--restart unless-stopped` 随后重新启动整个服务。

首次启动需要初始化 MySQL 并执行 Flyway，大型菜谱初始化可能持续数分钟。日志出现 `starting Nginx` 后才表示全部服务已经就绪。

#### 备份与恢复

执行一键备份：

```bash
docker exec family-kitchen family-kitchen-backup
docker volume inspect family-kitchen-data
```

命令会在 `/data/backups` 下生成数据库 SQL 压缩包和业务文件压缩包。命名卷仍应定期复制到另一台服务器或对象存储。

恢复到全新数据卷时，先启动一次容器完成 MySQL 初始化，再停止容器。把文件压缩包解压到 `/data`，然后执行：

```bash
gzip -dc family-kitchen-db-YYYYMMDD-HHMMSS.sql.gz | \
  docker exec -i family-kitchen mysql \
    --defaults-extra-file=/run/mysql-root-client.cnf family_kitchen
```

恢复完成后重启容器。正式恢复前应保留当前卷的完整副本。

#### 升级镜像

升级前先执行备份，然后停止并删除旧容器。不要删除 `family-kitchen-data` 卷，再用相同环境文件和数据卷启动新镜像：

```bash
docker stop family-kitchen
docker rm family-kitchen
./scripts/run-single-container.sh \
  --image family-kitchen:0.2.0 \
  --env-file /opt/family-kitchen/family-kitchen.env
```

Flyway 会在新版本首次启动时自动升级数据库。

#### 单镜像限制

该模式以部署简单为优先，MySQL、Java 和 Nginx 不能独立扩容或滚动更新，任一服务重启都会影响整站。建议服务器至少 2 GB 内存，4 GB 更稳妥。业务量增长、多实例部署或需要数据库高可用时，应把 MySQL 独立迁出容器。

## 4. 微信云托管部署后端

仓库根目录的 `Dockerfile` 是微信云托管专用镜像，只构建并运行 Spring Boot 后端，不包含管理后台或 MySQL。云托管服务端口为 `8080`，健康检查地址为 `/public/system-settings`。

### 4.1 首次绑定 GitHub 流水线

在微信云托管控制台打开现有 Spring Boot 服务并创建流水线：

1. 代码源选择 GitHub 仓库 `Q-Feng-Q/savoria`。
2. 发布分支选择 `dev`。
3. 构建目录选择仓库根目录 `.`。
4. Dockerfile 路径填写 `Dockerfile`。
5. 容器端口填写 `8080`，启动探针延迟建议不少于 `120` 秒。
6. 开启“代码推送后自动构建/发布”（控制台用词可能是自动发布或自动部署）。

`container.config.json` 可用于从模板首次创建服务；已有服务仍以控制台中的实例、扩缩容、环境变量和发布设置为准。

### 4.2 云托管环境变量

Spring Boot 模板通常已提供前三项 MySQL 连接变量。数据库名不是 `family_kitchen` 时，以云数据库中的真实名称覆盖：

```dotenv
MYSQL_ADDRESS=云数据库内网地址:3306
MYSQL_USERNAME=云数据库用户名
MYSQL_PASSWORD=云数据库密码
MYSQL_DATABASE=family_kitchen

FAMILY_KITCHEN_JWT_SECRET=至少32字节的随机密钥
FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD=初始管理员强密码
FAMILY_KITCHEN_WECHAT_APP_ID=小程序AppID
FAMILY_KITCHEN_WECHAT_APP_SECRET=小程序AppSecret
```

不要把这些值写进仓库、Dockerfile 或 `container.config.json`。首次连接空数据库时，Flyway 会自动执行 V1-V3 初始化脚本；不要再手工重复执行 SQL。

### 4.3 上传文件说明

镜像把上传文件写到 `/data` 下，但云托管容器本地磁盘会随实例重建或扩缩容而丢失，也不能在多个实例间共享。当前配置只适合部署验证。正式保存菜品图片、步骤图片和反馈附件前，应接入云存储/对象存储，或使用云托管支持的持久化存储；不能仅依赖容器目录。

### 4.4 发布验证

流水线发布成功后，用云托管分配的 HTTPS 域名验证：

```text
GET https://云托管域名/public/system-settings
```

接口返回 `200` 后，再把小程序生产环境的 API 地址切换为该 HTTPS 域名，并真机验证注册登录、微信绑定、图片上传、点菜、餐篮、下单和商户接单流程。

## 5. 微信小程序发布

1. 在 `frontend/app.js` 中把 `apiBaseUrl` 改为生产 HTTPS API 地址，例如：

   ```js
   apiBaseUrl: 'https://api.example.com'
   ```

2. 在微信公众平台配置以下合法域名：

   - `request` 合法域名：后端 HTTPS 域名
   - `uploadFile` 合法域名：后端 HTTPS 域名
   - `downloadFile` 合法域名：后端 HTTPS 域名

3. 确保域名具有受信任的 HTTPS 证书，不能使用 IP 地址、自签名证书或 `localhost`。
4. 用微信开发者工具打开 `frontend` 目录。
5. 核对 `frontend/project.config.json` 中的 `appid` 是否为目标小程序。
6. 在开发者工具中完成真机调试、上传代码、提交审核和发布。

当前 `project.config.json` 的 `urlCheck` 为 `false`，这只适合开发联调。提交前应开启域名校验，并在真机上验证登录、图片上传、下单和通知等主要流程。

## 6. 部署验证

### 6.1 后端与数据库

```bash
curl -i http://127.0.0.1:8080/public/system-settings
curl -I http://127.0.0.1:8080/swagger-ui.html
```

再检查启动日志中是否出现 Flyway 迁移失败、数据库连接失败或端口占用。

> 当前项目没有引入 Spring Boot Actuator 依赖，因此 `/actuator/health` 不能作为现成的健康检查接口。

### 6.2 管理后台

```bash
curl -I https://admin.example.com/
curl -i https://admin.example.com/api/public/system-settings
```

浏览器验收：

- 刷新 `/login`、`/dashboard` 等路由不会出现 Nginx 404
- 管理员可以登录
- 菜品图片能正常上传和访问
- API 请求地址为同域 `/api/*`，没有混合内容或跨域错误

### 6.3 小程序

至少完成以下真机回归：

- 注册、登录、退出
- 微信登录或账号绑定
- 家庭菜单、购物车、下单
- 商户接单和订单状态推进
- 图片加载与上传
- 通知中心

## 7. 更新发布与回滚

### 7.1 后端更新

发布前备份数据库和上传目录。推荐保留带版本号的 JAR：

```text
/opt/family-kitchen/releases/family-kitchen-backend-0.1.0.jar
/opt/family-kitchen/releases/family-kitchen-backend-0.2.0.jar
```

发布步骤：

1. 执行测试并构建新 JAR。
2. 备份数据库和 `/var/lib/family-kitchen/uploads`。
3. 上传新 JAR，更新当前版本软链接或 systemd 的 JAR 路径。
4. 重启服务并检查日志。
5. 执行接口和核心业务冒烟测试。

Flyway 迁移应优先采用向后兼容设计。数据库迁移执行后，仅回滚 JAR 可能无法恢复旧版本，因此数据库结构变更必须准备单独的回滚方案或恢复备份方案。

### 7.2 管理后台更新

把每次构建产物放入独立版本目录，通过软链接切换：

```text
/var/www/family-kitchen-admin-releases/20260726-120000
/var/www/family-kitchen-admin -> 上述版本目录
```

回滚时将软链接切回上一版本并重载 Nginx。

## 8. 备份建议

建议至少每天执行：

```bash
mysqldump --single-transaction --routines --triggers \
  -u family_kitchen -p family_kitchen \
  > family_kitchen_$(date +%F_%H%M%S).sql

tar -czf uploads_$(date +%F_%H%M%S).tar.gz \
  /var/lib/family-kitchen/uploads
```

备份文件应复制到另一台服务器或对象存储，并定期进行恢复演练。数据库备份和上传目录备份应尽量来自同一时间窗口。

## 9. 常见问题

### 后端启动时报数据库连接失败

- 检查 MySQL 是否监听目标地址和端口。
- 检查数据库用户的来源地址授权。
- 检查 JDBC URL、用户名、密码和服务器时区。
- 检查防火墙和云安全组。

### 后端启动时报 Flyway 校验失败

不要修改已经执行过的迁移文件。恢复原迁移文件，并通过新增迁移修正结构或数据。

### 管理后台接口返回 404

- 检查 `backend.config.json` 的 `apiBaseUrl` 是否为 `/api`。
- 检查 Nginx `location /api/` 和 `proxy_pass` 是否都带末尾 `/`。
- 检查后端是否监听 `127.0.0.1:8080`。

### 刷新管理后台页面返回 404

确认 Nginx 使用：

```nginx
try_files $uri $uri/ /index.html;
```

### 上传成功但图片无法显示

- 检查 `FAMILY_KITCHEN_FILE_STORAGE_LOCAL_ROOT` 是否为持久化绝对路径。
- 检查运行用户对上传目录是否有读写权限。
- 检查 Nginx 是否转发 `/uploads/`。
- 检查 HTTPS 页面是否引用了 HTTP 图片地址。

### 小程序请求失败

- 生产环境不能请求 `127.0.0.1`。
- 检查微信公众平台合法域名。
- 检查 HTTPS 证书链和域名备案要求。
- 检查 `frontend/app.js` 中的 `apiBaseUrl`。
- 使用真机调试查看具体请求错误。

## 10. 上线验收清单

- [ ] 后端测试通过，JAR 构建成功
- [ ] 管理后台测试和构建通过
- [ ] MySQL 8 数据库和专用账号已创建
- [ ] Flyway 迁移成功
- [ ] 数据库密码、JWT 密钥、管理员密码已更换
- [ ] 上传目录已持久化并纳入备份
- [ ] systemd 服务可启动、停止和自动重启
- [ ] Nginx HTTPS、History 路由、`/api`、`/uploads` 均正常
- [ ] Swagger 未直接暴露公网
- [ ] 管理后台核心流程验收通过
- [ ] 小程序生产 API 地址和合法域名配置正确
- [ ] 小程序真机核心流程验收通过
- [ ] 数据库和上传文件备份、恢复方案已验证
