# CookLikeHOC 模板菜谱同步验证记录

## 同步基线

- 来源站点：`https://cooklikehoc.soilzhu.su/`
- 来源仓库提交：`f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed`
- 来源 Markdown：336 个
- 去重后菜谱标题：333 个
- 来源分类：15 个
- 初始化模板总数：549 个（既有 240 个，新增 309 个）

## 数据完整性

- 食材记录：1618 条
- 制作步骤：795 条
- 仓库图片：192 张
- 声明图片：179 张
- 私有审核区已暂存图片：179 张
- 未解决问题：0 个
- 组件引用自检：64 个引用，0 个悬空引用

来源未声明权利的图片只进入运行时私有审核区，不写入公共图片地址。没有来源图片的模板保持公共图片字段为空。数量无法可靠结构化的食材保留原始文本，并标记为待补采购数据。

## 可重复生成

使用同一来源提交和同一版本化配置连续执行两次 release，三个产物逐字节一致：

- `V13__sync_complete_cooklikehoc_recipe_catalog.sql`: `30ABF55BE2247919E265E122CCBF098A212C2CF49E6F8A428011F207190D6ECD`。它作为已部署数据库的前向迁移，保留既有 `V4/V5/V6` 校验和，并增量补齐完整目录。
- `cooklikehoc-sync-manifest.json`: `F2A63E7C84931F64705C3FCA5F060A424717428983F16F9E2452E8AD6E108C98`
- `cooklikehoc-quality-report.json`: `B1B2356723CE0DCEBC50B8A65AC6682E4A5747FD5C5B1BEE43C8C1071E2BDBAB`

## 验证结果

- 同步工具：34 项通过，0 失败。
- 后端相关迁移与菜品链路：23 项通过，0 失败。
- 后端完整套件：352 项通过，0 失败，12 项 Docker/MySQL 并发测试因当前环境无 Docker 而跳过。
- 小程序：498 项通过，0 失败。
- PC 管理端：50 项通过，0 失败。
- PC 管理端生产构建：通过。

`FreshDatabaseMigrationTest` 已验证初始化 SQL 可按空库顺序执行。现有数据库由 Flyway 顺序执行 `V13`，不会改写已经应用的历史迁移；发布后还需核对 `flyway_schema_history` 以及模板、来源、食材和步骤数量。
