# 小程序品牌文案系统配置设计

## 目标

将小程序内用于营造品牌氛围的固定文案收口到平台级系统配置中，使平台管理员无需修改代码即可调整首页、餐篮和个人中心的品牌表达。

本功能只配置品牌与氛围文案。按钮名称、字段标签、状态名称、错误提示和业务说明继续由代码维护，避免配置缺失影响核心操作。

## 配置字段

沿用唯一记录 `system_settings.id = 1`，新增以下显式字段：

| 接口字段 | 数据库字段 | 默认文案 | 最大长度 | 使用位置 |
| --- | --- | --- | ---: | --- |
| `brandTagline` | `brand_tagline` | `好好吃饭，就是幸福` | 120 | 登录、注册等品牌入口 |
| `homeHeroTagline` | `home_hero_tagline` | `让家常菜 · 温暖每一餐\n就是最好的时光` | 120 | 家庭首页顶部 |
| `homeFooterMessage` | `home_footer_message` | `好好吃饭\n就是一家人在一起` | 120 | 家庭首页及订单页底部 |
| `cartHeroTagline` | `cart_hero_tagline` | `让家常菜 · 温暖每一餐` | 120 | 餐篮顶部 |
| `deliveryMessage` | `delivery_message` | `美味正在路上，用食物，把温暖送到家` | 80 | 餐篮配送提示 |
| `pickupMessage` | `pickup_message` | `先在一起，好好吃饭，期待您的到来` | 80 | 餐篮到店自取提示 |
| `cartFooterMessage` | `cart_footer_message` | `把平凡的日子，过成温暖的诗` | 120 | 餐篮底部；站点名称继续由页面单独展示 |
| `profileWelcomeMessage` | `profile_welcome_message` | `好好吃饭，就是幸福` | 120 | 已绑定家庭的个人中心欢迎语 |

字段允许换行，但不支持 HTML、富文本或模板表达式。数据库字段使用 `VARCHAR(500)` 并允许为空，以兼容后续中英文长度差异。

未绑定家庭时的“从这里，认识新家”属于状态提示，不纳入品牌文案配置。

## 数据模型与接口

通过新的前向 Flyway 迁移为 `system_settings` 增加八个可空字段，不修改已经存在的迁移文件。

以下模型同步增加对应属性：

- `SystemSettingDO`
- `SystemSettingRequest`
- `SystemSettingView`
- `PublicSystemSettingView`
- `SystemSettingMapper.xml`

管理员接口沿用：

- `GET /admin/system-settings`
- `PUT /admin/system-settings`

小程序公开读取沿用：

- `GET /public/system-settings`

公开接口只返回最终有效文案，不暴露管理信息。服务层读取字段时先去除首尾空白；值为空、全空格或不存在时返回表格中的默认文案。管理员提交空字符串表示恢复默认值，持久化为 `NULL`。

## 后台交互

平台后台“系统配置”页面新增“品牌文案”区域，包含八个多行输入框：

- 显示字段用途和默认文案。
- 显示当前字符数与最大字符数。
- 允许换行。
- 清空输入并保存表示恢复默认文案。
- 与系统配置统一保存，失败时保留用户输入并显示错误。

品牌文案编辑区域不与 Logo 尺寸配置耦合，避免修改文案时覆盖品牌图片字段。

## 小程序运行时

扩展现有 `frontend/utils/branding.js`：

1. 定义八个内置默认值。
2. `normalizeBranding` 兼容旧接口缺少字段、空字符串、非字符串和超长异常值。
3. `brandStore` 继续负责公共配置缓存，不增加页面级网络请求。
4. `withBranding` 向页面 data 注入统一的 `brandCopy` 对象。
5. 页面 WXML 从 `brandCopy` 读取文案。

需要替换的首批页面：

- 登录与注册页：`brandTagline`
- 家庭首页：`homeHeroTagline`、`homeFooterMessage`
- 订单页底部：`homeFooterMessage`
- 餐篮：`cartHeroTagline`、`deliveryMessage`、`pickupMessage`、`cartFooterMessage`
- 个人中心：`profileWelcomeMessage`

页面首次渲染使用内置默认值；公共配置返回后由现有订阅机制更新。接口失败、超时、旧后端没有新字段或本地缓存损坏时，页面保持默认文案，不展示空白。

## 校验与安全

- 后端 DTO 对普通文案使用 `@Size(max = 120)`，配送和自取提示使用 `@Size(max = 80)`。
- 前端后台同步限制输入长度，但以后端校验为准。
- 文案按纯文本渲染，不使用 `rich-text`。
- 公共接口不返回 SMTP、审计账号等私有字段。
- 保存继续记录现有系统配置审计信息。

## 测试

后端测试覆盖：

- 数据库字段、MyBatis 映射和更新语句完整。
- 空值与空字符串返回默认文案。
- 自定义值通过管理员及公开接口返回。
- 字符长度超限被拒绝。
- 公开配置不泄露私有配置。

后台测试覆盖：

- 八个字段可以加载、修改并随保存请求提交。
- 清空字段可恢复默认值。
- 保存失败保留输入。

小程序测试覆盖：

- 旧接口响应缺少字段时使用默认值。
- 自定义文案更新缓存并同步到页面。
- 空值、非字符串和失败响应不会产生空白文案。
- 指定页面不再保留对应硬编码氛围文案。

## 不在本次范围

- 按钮、标签、状态、错误提示和业务帮助文案。
- 商户或家庭级别的独立文案。
- 多语言、富文本、排期发布和文案版本历史。
- 字体替换；字体方案作为独立需求继续处理。
