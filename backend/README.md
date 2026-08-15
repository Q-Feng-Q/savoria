# Family Kitchen Backend

家庭厨房小程序后端，当前固定使用 `Java 17 + Spring Boot 3 + MyBatis + MySQL 8`。

## 当前定位

- 运行时只保留数据库方案。
- 持久化方案为 MyBatis / MyBatis-Plus 注解 / MySQL。
- 数据库初始化由 Flyway 管理。
- 接口文档使用 SpringDoc / Swagger。
- 文件统一使用 UTF-8 无 BOM。

## 目录结构

```text
backend/
  src/main/java/com/familykitchen/
    auth/           登录、鉴权、管理员账号初始化
    cart/           餐篮业务
    common/         通用配置、异常、响应、用户上下文
    dish/           菜品、分类、食材字典
    family/         家庭资料、首页、地址、家庭菜单
    file/           图片上传
    notification/   通知中心
    order/          订单、状态机、订单持久化
    purchase/       采购清单与临时采购项
    wallet/         钱包与流水
  src/main/resources/
    application.yml
    db/migration/
    mapper/
  src/test/java/
```

## 本地运行

本版本数据库迁移已经重建为新的 V1-V3 基线。旧数据库与旧的
`flyway_schema_history` 不兼容；确认旧数据无需保留后，删除并重新创建数据库：

```sql
DROP DATABASE IF EXISTS family_kitchen;
CREATE DATABASE family_kitchen
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

三份迁移脚本不包含物理外键。跨表存在性、商户/家庭归属和状态合法性由 Service
事务校验；同表业务唯一性继续由唯一索引和生成列唯一索引保证。

项目不读取 `.env`，本地数据库、JWT、管理员、邮件和微信参数统一配置在
`src/main/resources/application.yml`。首次启动前请确认 MySQL 已启动，并按本机实际情况修改
`spring.datasource.username` 和 `spring.datasource.password`。

默认本地数据库连接为：

```text
jdbc:mysql://127.0.0.1:3306/family_kitchen
用户名：root
密码：123456
```

Flyway V3 会初始化平台管理员，应用启动器会再次确认账号和角色存在：

```text
用户名：admin
密码：Admin@123456
```

初始密码的 BCrypt 摘要直接保存在初始化 SQL 中。首次登录后必须立即修改管理员密码；
生产部署前还必须修改 JWT 密钥及数据库密码。

启动服务：

```bash
mvn spring-boot:run
```

Swagger 地址：

```text
http://localhost:8080/swagger-ui.html
```

## 测试

```bash
mvn test
```

当前重点覆盖：

- `AdminAuthApplicationServiceTest`
- `OrderStateMachineTest`
- `OrderSubmissionServiceTest`
- `FamilyOrderApplicationServiceTest`
- `PurchaseApplicationServiceImplTest`
- `WalletAccountTest`

## 说明

- JDBC 聚合存储实现已经删除。
- 所有业务 Service 按传统 MVC 结构直接调用 Mapper，不保留 port、store 或 repository 中间层。
- 采购清单聚合逻辑由 `PurchaseApplicationServiceImpl` 直接通过 `PurchaseMapper` 查询真实数据库。
- 数据库初始化只写入固定平台管理员和系统基础目录，不写入模拟商户、家庭、订单或钱包数据。
