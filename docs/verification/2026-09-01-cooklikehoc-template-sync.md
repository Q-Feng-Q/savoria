# CookLikeHOC 模板菜谱同步验证记录

## 同步基线

- 来源站点：`https://cooklikehoc.soilzhu.su/`
- 来源仓库提交：`f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed`
- 来源 Markdown：336 个
- 去重后菜谱标题：333 个
- 来源分类：15 个
- 初始化模板总数：549 个（既有 240 个，新增 309 个）

## 数据完整性

- 模板食材总记录：1987 条，其中 CookLikeHOC 同步食材 1618 条
- 制作步骤：795 条
- 仓库图片：192 张
- 声明图片：179 张
- 私有审核区已暂存图片：179 张
- 未解决问题：0 个
- 组件引用自检：64 个引用，0 个悬空引用

来源未声明权利的图片只进入运行时私有审核区，不写入公共图片地址。没有来源图片的模板保持公共图片字段为空。数量无法可靠结构化的食材保留原始文本，并标记为待补采购数据。

## 可重复生成

使用同一来源提交和同一版本化配置生成确定性同步产物。本次上线前基线合并后，菜谱目录写入 `V3__init_recipe_catalog.sql`；后续 release 使用 `--migration-file` 生成新的前向迁移，不覆盖已经应用的初始化脚本。

- `V1__init_schema.sql`：最终 64 张业务表结构，无物理外键和增量 `ALTER TABLE`。
- `V2__init_system_and_admin.sql`：88 条系统基础种子。
- `V3__init_recipe_catalog.sql`：549 个模板、1987 条模板食材、336 条来源记录、795 个步骤和 179 个图片审核资产。
- `V1__init_schema.sql` SHA-256：`CDF6C1468C843F555F133C98592D889943A3549990B0C09CF7528EE27C827437`
- `V2__init_system_and_admin.sql` SHA-256：`7DD505335EB493BA75C29FAF7546F09288AEB5178921122BC1343CC00B93752A`
- `V3__init_recipe_catalog.sql` SHA-256：`35C6505EE823F9582013A179D8215C3B3006222B70FFCD85C8846B12F1130B23`
- `cooklikehoc-sync-manifest.json`: `F2A63E7C84931F64705C3FCA5F060A424717428983F16F9E2452E8AD6E108C98`
- `cooklikehoc-quality-report.json`: `B1B2356723CE0DCEBC50B8A65AC6682E4A5747FD5C5B1BEE43C8C1071E2BDBAB`

## 验证结果

- 同步工具：33 项通过，0 失败。
- 后端迁移定向套件：35 项通过，0 失败。
- 后端完整套件：361 项通过，0 失败，13 项 Docker/MySQL 并发测试因当前环境无 Docker 而跳过（共 374 项）。
- 小程序：498 项通过，0 失败。
- PC 管理端：50 项通过，0 失败。
- PC 管理端生产构建：通过。

`FreshDatabaseMigrationTest` 已验证迁移目录只包含 V1/V2/V3。独立空 MySQL 验证库和真实 `family_kitchen` 数据库均已成功执行三段初始化，Flyway 成功记录为 3、失败记录为 0。重建后的真实库包含 64 张业务表、1 个管理员、1 个默认商户、10 个菜品分类、73 个商户食材、549 个菜品模板、1987 条模板食材、336 条来源记录、795 个制作步骤和 179 个图片审核资产；家庭、餐篮、订单、采购单、通知和会话均为 0。重建前完整数据保留在 `family_kitchen_pre_reset_20260913_0953`，其 65 张表及每表行数均已与重建前源库核对一致。
