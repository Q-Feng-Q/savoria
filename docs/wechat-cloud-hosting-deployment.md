# 微信云托管部署

## 当前服务

- 云环境：`prod-d5g0vleyp9ed264bf`
- 云托管服务：`springboot-6dl0`
- 容器入口：Nginx 监听 `0.0.0.0:8080`
- 应用端口：Spring Boot 监听 `0.0.0.0:8081`
- 数据库：云开发 MySQL 或可由云托管访问的腾讯云 MySQL

云托管容器只运行 Nginx 和 Spring Boot，不内置 MySQL。服务允许缩容到零并扩容到多个实例，容器本地磁盘不能作为生产数据库持久化层。

## 部署前配置

先在云开发控制台创建数据库 `family_kitchen`，再进入“云托管 > springboot-6dl0 > 服务配置 > 环境变量”，配置以下变量：

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `MYSQL_ADDRESS` | 是 | 云开发 MySQL 连接地址，格式为 `host:port` |
| `MYSQL_USERNAME` | 是 | 数据库账号 |
| `MYSQL_PASSWORD` | 是 | 数据库密码 |
| `FAMILY_KITCHEN_JWT_SECRET` | 是 | 至少 32 个字符的随机密钥 |
| `FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD` | 是 | 管理员初始密码，至少 6 位 |
| `FAMILY_KITCHEN_WECHAT_APP_SECRET` | 是 | 当前小程序 AppSecret |

以下非敏感变量已经由 `container.config.json` 提供：

- `PORT=8080`
- `SERVER_ADDRESS=0.0.0.0`
- `SERVER_PORT=8081`
- `MYSQL_DATABASE=family_kitchen`
- `FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_USERNAME=admin`
- `FAMILY_KITCHEN_WECHAT_APP_ID=wx092b0184d87bf822`

不要把密码、JWT 密钥或 AppSecret 写入 Git、`application.yml`、Dockerfile 或 `container.config.json`。

如果使用云开发 MySQL，控制台能够直接查看 `MYSQL_ADDRESS`、账号和密码。使用腾讯云 MySQL 时，数据库与云托管需要位于同一地域，并为云托管开启对应 VPC 私有网络连接。

## 重新部署

环境变量保存后重新触发 Git 仓库部署。正常启动日志顺序应包含：

```text
[cloudrun-entrypoint] database=<地址>/family_kitchen user=<账号> gateway=8080 app=0.0.0.0:8081
[cloudrun-entrypoint] starting Spring Boot on 0.0.0.0:8081
[cloudrun-entrypoint] starting Nginx on 0.0.0.0:8080
Tomcat started on port(s): 8081
Started FamilyKitchenApplication
```

首次连接空数据库时 Flyway 会执行初始化脚本，因此探针延迟设置为 300 秒。若缺少配置，容器会直接输出 `missing required environment variable: ...`；若数据库网络不可达，继续检查 MySQL 地址、私有网络和安全组。

## 小程序调用

小程序已通过 `wx.cloud.callContainer` 调用云托管：

- 环境 ID：`prod-d5g0vleyp9ed264bf`
- 请求头 `X-WX-SERVICE`：`springboot-6dl0`

小程序必须与目标云环境完成关联。普通 JSON 接口不需要配置 request 合法域名；当前图片上传和下载仍通过云托管 HTTPS 域名，因此云托管服务需要开启公网访问，并在微信公众平台配置对应的 `uploadFile`、`downloadFile` 合法域名。

## 凭据处理

旧配置曾出现过明文 AppSecret、管理员密码和数据库密码。重新部署前应在微信公众平台和数据库控制台轮换这些凭据，再把新值写入云托管环境变量。
