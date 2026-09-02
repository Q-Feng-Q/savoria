# CookLikeHOC Template Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将固定提交的 CookLikeHOC 全部菜谱、食材和制作流程确定性同步进初始化 SQL，并补齐平台维护、商户导入、审核以及小程序和 PC 展示闭环。

**Architecture:** 新增独立 Java 17 离线生成工具读取外部固定提交，不在应用运行时联网；工具输出规范化清单、质量报告、私有图片清单和可直接合入 `V4` 的 SQL 数据段。Spring Boot 继续采用传统 MVC，Service 直接调用 MyBatis Mapper；模板市场只暴露采购与价格均就绪的成品菜，平台端可维护全部菜谱和组件。

**Tech Stack:** Java 17、Maven、CommonMark、Jackson、Spring Boot 3、MyBatis/MyBatis-Plus annotations、MySQL 8/Flyway、JUnit 5/Testcontainers、WeChat Mini Program、Vue 3/Vite、Node test runner。

---

## File Map

- `tools/cooklikehoc-sync/`: 独立离线导入器，只负责解析、规范化、校验、生成 SQL/报告和复制私有审核图片。
- `backend/src/main/resources/db/migration/V4__init_dish_template_market.sql`: 本功能所有新表、字段和最终初始化数据；折叠并替代原 `V5/V6` 模板数据。
- `backend/src/main/java/com/familykitchen/dish/`: 模板实体、DTO/VO、Controller、Service、Mapper，保持现有传统 MVC 分层。
- `frontend/pages/merchant/dish-template-*`: 小程序商户模板列表、详情和同步审核。
- `admin-web/src/views/merchant/`、`admin-web/src/views/platform/`: PC 商户模板市场和平台模板维护。
- `docs/frontend-api-guide.md`、`docs/api-spec.md`、`docs/database-design.md`: 前后端契约、管理接口和数据库规则。

### Task 1: Scaffold The Deterministic Importer

**Files:**
- Create: `tools/cooklikehoc-sync/pom.xml`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/RecipeSyncMain.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/config/SyncConfig.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/model/SourceRecipe.java`
- Create: `tools/cooklikehoc-sync/src/test/java/com/familykitchen/recipesync/RecipeSyncMainTest.java`
- Create: `tools/cooklikehoc-sync/src/test/resources/fixtures/炒菜/标准菜谱.md`

- [ ] **Step 1: Write the failing CLI contract test**

```java
@Test
void rejectsCheckoutAtUnexpectedRevision() {
  var error = assertThrows(SyncFailure.class,
      () -> runner.run(fixtureRepo(), configWithRevision("bad-revision")));
  assertEquals("SOURCE_REVISION_MISMATCH", error.code());
}
```

- [ ] **Step 2: Run the focused test and verify failure**

Run: `cd tools/cooklikehoc-sync; mvn test -Dtest=RecipeSyncMainTest`

Expected: FAIL because the CLI/config/model classes do not exist.

- [ ] **Step 3: Implement the smallest CLI boundary**

Support required arguments `--mode draft|release`, `--source-dir`, `--config-dir`, `--output-dir`, plus release-only `--v4-file/--manifest-file/--quality-report-file`; verify `git rev-parse HEAD`, require UTF-8 strict decoding, and refuse missing/unexpected revisions. Both modes load `config/local-template-baseline.json` as the complete existing-template input closure. Draft mode may emit deterministic proposals/review queues but never writes V4 or backend resources; release mode requires every versioned allocation and review disposition and atomically writes all three committed outputs. Add CommonMark, Jackson and JUnit dependencies only to this tool module. Configure Maven Shade Plugin with `finalName=cooklikehoc-sync`, `RecipeSyncMain` as the manifest entry point and runtime dependencies in the executable JAR.

- [ ] **Step 4: Re-run the focused tests**

Expected: PASS and no generated file contains a current timestamp.

- [ ] **Step 5: Commit**

```bash
git add tools/cooklikehoc-sync
git commit -m "feat: scaffold deterministic recipe sync tool"
```

### Task 2: Parse All Markdown Structures And Components

**Files:**
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/parser/RecipeMarkdownParser.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/parser/RecipeSectionClassifier.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/model/SourceIngredient.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/model/SourceStep.java`
- Test: `tools/cooklikehoc-sync/src/test/java/com/familykitchen/recipesync/parser/RecipeMarkdownParserTest.java`
- Create: `tools/cooklikehoc-sync/src/test/resources/fixtures/配料/组件菜谱.md`
- Create: `tools/cooklikehoc-sync/src/test/resources/fixtures/非标准/无标准标题.md`

- [ ] **Step 1: Add failing parser tests**

Cover H1 title, 15 source directories, image/link nodes, ingredient lines, ordered/unordered steps, time/temperature/heat facts, missing sections and component links. Assert source ordering and source-line keys are stable.

- [ ] **Step 2: Run parser tests and verify failure**

Run: `cd tools/cooklikehoc-sync; mvn test -Dtest=RecipeMarkdownParserTest`

- [ ] **Step 3: Implement AST-based parsing**

Use CommonMark nodes rather than whole-file regular expressions. Unknown structures produce typed issues; no field may be silently dropped. Preserve raw batch quantity text and component occurrence position.

- [ ] **Step 4: Run parser tests and inspect UTF-8 output**

Expected: PASS; Chinese titles round-trip without replacement characters.

- [ ] **Step 5: Commit**

```bash
git add tools/cooklikehoc-sync
git commit -m "feat: parse CookLikeHOC recipe structures"
```

### Task 3: Normalize, Merge And Gate Procurement Deterministically

**Files:**
- Create: `tools/cooklikehoc-sync/config/source-sync.json`
- Create: `tools/cooklikehoc-sync/config/template-name-mappings.json`
- Create: `tools/cooklikehoc-sync/config/procurement-mappings.json`
- Create: `tools/cooklikehoc-sync/config/rewrite-reviews.json`
- Create: `tools/cooklikehoc-sync/config/template-id-allocations.json`
- Create: `tools/cooklikehoc-sync/config/local-template-baseline.json`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/normalize/RecipeNormalizer.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/merge/TemplateMergePlanner.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/procurement/ProcurementReadinessEvaluator.java`
- Test: `tools/cooklikehoc-sync/src/test/java/com/familykitchen/recipesync/merge/TemplateMergePlannerTest.java`
- Test: `tools/cooklikehoc-sync/src/test/java/com/familykitchen/recipesync/procurement/ProcurementReadinessEvaluatorTest.java`

- [ ] **Step 1: Add failing deterministic merge tests**

First encode all current V4 plus V5/V6 local templates, ingredients, prices, tags and licensed image attribution in `local-template-baseline.json`; this version-controlled file becomes the sole machine-readable local merge baseline. Add a one-time contract test that compares its IDs/template codes/counts with the pre-consolidation migrations before V5/V6 are deleted. Assert explicit primary files for the three duplicate title groups, alias collision failure, stable IDs, source-led overwrite, unmatched local retention and no mtime/content-length heuristic. `template-id-allocations.json` permanently maps every stable source/local key to `id/templateCode`; deleting or renumbering an existing allocation fails generation, while new keys receive IDs above the recorded high-water mark.

- [ ] **Step 2: Add failing procurement graph tests**

```java
@Test
void repeatedComponentPathsKeepBothMultipliers() { /* 2x sauce + 1x sauce = 3x leaves */ }

@Test
void nullComponentMultiplierBlocksProcurement() { /* never defaults to one */ }
```

Also cover cycles, unresolved components, incompatible units, `SOURCE_BATCH/MISSING`, and audited legacy quantity reuse.

- [ ] **Step 3: Implement normalizer, merge planner and shared evaluator**

Normalize with UTF-8, NFC, visible Markdown text, full-width bracket conversion and whitespace compression. Match in this fixed order: source key, explicit mapping, normalized primary name, normalized name alias.

- [ ] **Step 4: Run merge and procurement tests**

Expected: PASS with zero nondeterministic ordering.

- [ ] **Step 5: Commit**

```bash
git add tools/cooklikehoc-sync
git commit -m "feat: add deterministic recipe merge and procurement gates"
```

### Task 4: Generate Reviewed Steps, Reports, SQL And Private Assets

**Files:**
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/rewrite/StepFactRewriter.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/rewrite/SimilarityGate.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/output/SyncArtifactWriter.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/output/SqlRenderer.java`
- Create: `tools/cooklikehoc-sync/src/main/java/com/familykitchen/recipesync/output/V4GeneratedSectionUpdater.java`
- Create: `tools/cooklikehoc-sync/src/test/java/com/familykitchen/recipesync/output/SyncArtifactWriterTest.java`
- Modify: `.gitignore`

- [ ] **Step 1: Add failing release-gate and byte-stability tests**

Require zero unresolved parser issues, links, aliases, component references and rewrite warnings. Same versioned inputs must produce byte-identical JSON/SQL SHA-256. `REWRITE_REQUIRED` and missing reviewer disposition must block final output.

- [ ] **Step 2: Implement factual step rewriting and similarity review input**

Retain action order, quantities, time, temperature, heat and completion condition; do not copy long source prose. Store only short source locators in SQL.

- [ ] **Step 3: Implement output and private image copy policy**

Add `tools/cooklikehoc-sync/.source/` and `backend/runtime-data/private/dish-template-review-assets/` to `.gitignore`. Copy source-declared images to the private directory using stable storage keys. Generate asset rows with `UNDECLARED/INTERNAL_REVIEW`, but keep public `image_url` null. Do not create assets for recipes without images. `V4GeneratedSectionUpdater` replaces only the text between the unique markers `-- BEGIN GENERATED COOKLIKEHOC DATA` and `-- END GENERATED COOKLIKEHOC DATA`; missing, duplicate or reversed markers fail without writing, and replacement uses an atomic temporary-file move.

- [ ] **Step 4: Run generator tests twice and compare hashes**

Expected: identical manifest, report and SQL fragment; a fixture V4 receives byte-identical marker replacement with no stale generated row; no current timestamp and no absolute filesystem path.

- [ ] **Step 5: Commit**

```bash
git add .gitignore tools/cooklikehoc-sync
git commit -m "feat: generate reviewed recipe sync artifacts"
```

### Task 5: Consolidate The Initialization Schema Into V4

**Files:**
- Modify: `backend/src/main/resources/db/migration/V4__init_dish_template_market.sql`
- Delete: `backend/src/main/resources/db/migration/V5__expand_regional_dish_templates.sql`
- Delete: `backend/src/main/resources/db/migration/V6__finalize_regional_dish_template_images.sql`
- Modify: `backend/src/test/java/com/familykitchen/database/DishTemplateMigrationTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/DishTemplateRegionalExpansionTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/DishTemplateImageAssetTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/FreshDatabaseMigrationTest.java`
- Create: `backend/src/test/java/com/familykitchen/database/CookLikeHocTemplateMigrationTest.java`

- [ ] **Step 1: Write failing schema and data contract tests**

Assert the V4-only schema includes source records, name aliases, nullable template prices/images, ingredient quantity states, template steps, image assets, exactly one generated-data marker pair and V4 `ALTER TABLE dish_cooking_steps`. Assert no physical foreign keys and every table/column has comments.

- [ ] **Step 2: Run database contract tests and verify failure**

Run: `cd backend; mvn test -Dtest=DishTemplateMigrationTest,CookLikeHocTemplateMigrationTest,FreshDatabaseMigrationTest`

- [ ] **Step 3: Rewrite the V4 schema and fold V5/V6 legacy data**

Create the final schema, preserve existing local template rows as the pre-generation merge input, fold the 42 regional rows and licensed image metadata from V5/V6 into V4, and add an initially empty generated-data marker section. Full source override/addition is performed deterministically in Task 13 after all generator release gates pass. Add checks equivalent to:

```sql
CONSTRAINT chk_template_image_rights CHECK (
  (image_rights_status IN ('UNDECLARED','NONE') AND image_url IS NULL)
  OR (image_rights_status='DECLARED' AND image_url IS NOT NULL
      AND image_source_url IS NOT NULL AND image_author IS NOT NULL AND image_license IS NOT NULL)
)
```

The image asset table includes nullable `rejection_reason varchar(500)` and a state check requiring a nonblank reason for `REJECTED` while other states keep it null. Each initialization row stays on one line; use UTF-8/LF and logical references only. V4 must remain executable with the marker section empty so schema tests do not depend on network/source provisioning.

- [ ] **Step 4: Delete V5/V6 and adapt legacy fixed-count/image tests**

Licensed legacy images remain tested as a subset. Full source counts and undeclared/no-image assertions are deferred to Task 13, after the real generated data has replaced the empty marker section.

- [ ] **Step 5: Run migration tests**

Expected: H2 contracts pass; when Docker is available, MySQL 8 full Flyway chain also passes.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration backend/src/test/java/com/familykitchen/database
git commit -m "feat: initialize complete CookLikeHOC template catalog"
```

### Task 6: Extend Template Entities, DTOs, VOs And Mapper Queries

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateEntity.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateIngredientEntity.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/entity/DishCookingStepEntity.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateCookingStepEntity.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateSourceRecordEntity.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateNameAliasEntity.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateImageAssetEntity.java`
- Create: `backend/src/main/java/com/familykitchen/dish/service/DishTemplateProcurementReadinessEvaluator.java`
- Create: `backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateProcurementReadinessEvaluatorImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateView.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateDetailView.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/mapper/DishTemplateMapper.java`
- Modify: `backend/src/main/resources/mapper/dish/DishTemplateMapper.xml`
- Modify: `backend/src/main/resources/mapper/dish/DishMapper.xml`
- Modify: `backend/src/test/java/com/familykitchen/database/DishTemplateMapperContractTest.java`
- Create: `backend/src/test/java/com/familykitchen/dish/DishTemplateProcurementReadinessEvaluatorTest.java`

- [ ] **Step 1: Add failing mapper result-map and eligibility tests**

Require merchant queries to filter exactly `DISH + READY + procurement_ready + price + enabled`; admin queries must not apply that filter. Verify nullable mappings and ordered steps.

- [ ] **Step 2: Implement annotated entities and documented DTO/VO fields**

Use `@TableName`, `@TableId`, explicit `@TableField` where names are nontrivial, `@Schema` on API models and UTF-8 Chinese JavaDoc. `DishTemplateImageAssetEntity` includes `rejectionReason`; V4 defines nullable `rejection_reason varchar(500)` and requires it when `asset_status='REJECTED'`. Implement the backend evaluator with the same graph, multiplicity, quantity-state and compatible-unit rules as the importer; it accepts template/ingredient/component projections loaded by Mapper and returns readiness plus explicit blocking reasons. Admin update and review approval must call this runtime evaluator instead of trusting client/generated flags. Do not expose internal storage keys or rejection notes in merchant VOs.

- [ ] **Step 3: Implement MyBatis XML**

Add result maps and direct CRUD/select statements for source records, aliases, ingredients, steps and image assets. Keep Service-to-Mapper MVC flow without repository/port adapters.

- [ ] **Step 4: Run focused mapper tests**

Expected: PASS; importer golden cases and backend evaluator golden cases produce identical readiness/reason results, and `DocumentationCoverageTest` still passes.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/familykitchen/dish backend/src/main/resources/mapper/dish backend/src/test/java/com/familykitchen/database
git commit -m "feat: model complete recipe template data"
```

### Task 7: Import Eligible Templates With Ingredients And Steps

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/dish/service/DishTemplateService.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/controller/DishTemplateController.java`
- Modify: `backend/src/test/java/com/familykitchen/dish/DishTemplateServiceTest.java`
- Modify: `backend/src/test/java/com/familykitchen/dish/DishTemplateControllerRouteTest.java`

- [ ] **Step 1: Add failing service tests**

Cover list/detail eligibility, null image handling, ordered cooking steps, component preparation-step expansion, selective import, import-all, transactional recheck and 422 errors for price/procurement/component failures.

- [ ] **Step 2: Run focused tests and verify failure**

Run: `cd backend; mvn test -Dtest=DishTemplateServiceTest,DishTemplateControllerRouteTest`

- [ ] **Step 3: Implement a single shared eligibility policy**

Use the same Mapper predicate for list, detail, selected import and import-all. Re-lock and revalidate each template in the import transaction; copy verified leaf ingredients and all expanded steps, then renumber merchant steps continuously.

- [ ] **Step 4: Run focused and dish module tests**

Expected: PASS; imported merchant dishes retain non-null `basePrice` and valid procurement rows.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/familykitchen/dish backend/src/test/java/com/familykitchen/dish
git commit -m "feat: import eligible recipe templates with steps"
```

### Task 8: Add Platform Template And Controlled Image Administration

**Files:**
- Create: `backend/src/main/java/com/familykitchen/dish/controller/AdminDishTemplateController.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/dto/AdminDishTemplateQuery.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/dto/AdminDishTemplateUpdateRequest.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateImagePromotionRequest.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateImageRejectionRequest.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/vo/AdminDishTemplateDetailView.java`
- Create: `backend/src/main/java/com/familykitchen/dish/config/DishTemplateAssetProperties.java`
- Create: `backend/src/main/java/com/familykitchen/dish/service/AdminDishTemplateService.java`
- Create: `backend/src/main/java/com/familykitchen/dish/service/DishTemplateImageStorageService.java`
- Create: `backend/src/main/java/com/familykitchen/dish/service/impl/AdminDishTemplateServiceImpl.java`
- Create: `backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateImageStorageServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/mapper/DishTemplateMapper.java`
- Modify: `backend/src/main/resources/mapper/dish/DishTemplateMapper.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application-test-mvc.yml`
- Create: `backend/src/test/java/com/familykitchen/dish/AdminDishTemplateServiceTest.java`
- Create: `backend/src/test/java/com/familykitchen/dish/AdminDishTemplateControllerContractTest.java`

- [ ] **Step 1: Add failing admin authorization and contract tests**

Cover filters, illegal enums (400), 404, expected-version conflict (409), server-owned field rejection, derived-status recomputation, asset preview authorization, promotion metadata validation, atomic publish, rejection from `INTERNAL_REVIEW`, rejected preview denial and rejected-asset promotion denial.

- [ ] **Step 2: Implement documented Springdoc controllers**

Add the exact routes from the design plus `POST /api/admin/dish-templates/{templateId}/image-assets/{assetId}/reject` with `reason`; it updates only `INTERNAL_REVIEW` and returns `{ assetId, assetStatus }`. The preview response streams only an `INTERNAL_REVIEW/PUBLISHED` controlled asset resolved beneath the configured private root after canonical-path validation; rejected assets return 404 and no route accepts a filesystem path from the client. Configure private/public roots in `application.yml`, never `.env`.

- [ ] **Step 3: Implement Service-to-Mapper transactions**

Template `PUT` atomically replaces editable main data, ingredients and steps, then invokes `DishTemplateProcurementReadinessEvaluator` inside the transaction to persist server-derived `dataStatus/procurementReady`. Image promotion locks template and asset, writes a content-named file beneath the configured public root before making it reachable, then writes complete rights metadata and increments version in one database transaction; rollback removes a newly created unreferenced file. Generic update cannot mutate image fields. Rejection locks the asset, stores reviewer/reason, transitions it permanently to `REJECTED`, and both preview and promotion reject that state.

- [ ] **Step 4: Run focused tests**

Expected: PASS with platform permission enforcement and no private key in merchant/family serialization.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/familykitchen/dish backend/src/main/resources/mapper/dish backend/src/main/resources/application.yml backend/src/test/resources/application-test-mvc.yml backend/src/test/java/com/familykitchen/dish
git commit -m "feat: add platform recipe template administration"
```

### Task 9: Upgrade Template Change Review To Schema Version 2

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateSnapshotRequest.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateIngredientSnapshotRequest.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateCookingStepSnapshotRequest.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/DishTemplateSnapshotValidator.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateChangeRequestServiceImpl.java`
- Modify: `backend/src/main/resources/mapper/dish/DishTemplateChangeRequestMapper.xml`
- Modify: `backend/src/test/java/com/familykitchen/dish/DishTemplateSnapshotValidatorTest.java`
- Modify: `backend/src/test/java/com/familykitchen/dish/DishTemplateChangeRequestServiceTest.java`

- [ ] **Step 1: Replace image-edit expectations with failing v2 snapshot tests**

Assert merchant snapshots contain price, editable presentation, ingredients and steps, but reject `imageUrl`, asset IDs, source identity/type/revision, rights fields and derived statuses.

- [ ] **Step 2: Implement v2 parsing and validation**

Validate contiguous step numbers, stable item IDs, nullable quantity fields by status, component existence and field lengths. Build imported-dish sync snapshots with cooking steps.

- [ ] **Step 3: Implement approval transaction**

Lock by expected version, preserve source/image/server fields, atomically replace editable template data, ingredients and steps, call the same runtime `DishTemplateProcurementReadinessEvaluator` to recompute `dataStatus/procurementReady`, increment version and notify.

- [ ] **Step 4: Run review tests**

Expected: PASS including stale version, rollback and forbidden-field cases.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/familykitchen/dish backend/src/main/resources/mapper/dish backend/src/test/java/com/familykitchen/dish
git commit -m "feat: review complete recipe template snapshots"
```

### Task 10: Publish Stable API And Database Documentation

**Files:**
- Create: `docs/api-spec.md`
- Modify: `docs/frontend-api-guide.md`
- Create: `docs/database-design.md`
- Modify: `backend/src/test/java/com/familykitchen/DocumentationCoverageTest.java`

- [ ] **Step 1: Add failing documentation coverage assertions**

Require every new controller route, enum, nullable field, error code, pagination rule and image security rule to appear in docs.

- [ ] **Step 2: Write request/response examples**

Document merchant eligibility, template detail steps, selective import, admin list/detail/update, asset preview/promotion/rejection and schema-v2 review. Explicitly distinguish template nullable price/image from merchant non-null price/procurement contracts.

- [ ] **Step 3: Run documentation tests**

Run: `cd backend; mvn test -Dtest=DocumentationCoverageTest`

Expected: PASS and Springdoc annotations match the written contract.

- [ ] **Step 4: Commit**

```bash
git add docs backend/src/test/java/com/familykitchen/DocumentationCoverageTest.java
git commit -m "docs: publish recipe template API contracts"
```

### Task 11: Adapt The WeChat Merchant Frontend

**Files:**
- Modify: `frontend/services/merchant.js`
- Modify: `frontend/utils/dish-template-selection.js`
- Modify: `frontend/utils/dish-template-change.js`
- Modify: `frontend/pages/merchant/dish-templates/index.js`
- Modify: `frontend/pages/merchant/dish-templates/index.wxml`
- Modify: `frontend/pages/merchant/dish-templates/index.wxss`
- Modify: `frontend/pages/merchant/dish-template-detail/index.js`
- Modify: `frontend/pages/merchant/dish-template-detail/index.wxml`
- Modify: `frontend/pages/merchant/dish-template-detail/index.wxss`
- Modify: `frontend/pages/merchant/dish-template-change-edit/index.js`
- Modify: `frontend/pages/merchant/dish-template-change-edit/index.wxml`
- Modify: `frontend/pages/merchant/dish-template-change-detail/index.wxml`
- Modify: `frontend/pages/merchant/dish-edit/index.js`
- Modify: `frontend/pages/merchant/dish-edit/index.wxml`
- Modify: `frontend/tests/dish-template-market.test.js`
- Modify: `frontend/tests/dish-template-change-requests.test.js`

- [ ] **Step 1: Add failing mini-program contract tests**

Cover null image/price, source labels, step display, eligible-only list, selection import, v2 snapshot without image fields and imported steps in dish editing.

- [ ] **Step 2: Run focused tests and verify failure**

Run: `cd frontend; node --test tests/dish-template-market.test.js tests/dish-template-change-requests.test.js`

- [ ] **Step 3: Implement service and view-model adaptations**

Use real backend fields only. Render fixed-aspect no-image state, “待完善” only where admin/review data can surface, ordered step sections and clear import errors. Preserve existing warm cute visual tokens and 88rpx touch targets.

- [ ] **Step 4: Run frontend tests**

Run: `cd frontend; npm test`

Expected: all tests pass with no native button, unsupported WXSS selector or unresolved module regression.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat: show complete recipe templates in mini program"
```

### Task 12: Adapt The PC Merchant And Platform Consoles

**Files:**
- Modify: `admin-web/src/api/dishes.js`
- Modify: `admin-web/src/api/dish-template-changes.js`
- Create: `admin-web/src/api/admin-dish-templates.js`
- Modify: `admin-web/src/views/merchant/DishTemplatesView.vue`
- Modify: `admin-web/src/views/merchant/DishTemplateChangesView.vue`
- Modify: `admin-web/src/views/platform/DishTemplateChangeReviewsView.vue`
- Create: `admin-web/src/views/platform/DishTemplatesView.vue`
- Create: `admin-web/src/views/platform/DishTemplateDetailView.vue`
- Modify: `admin-web/src/router/index.js`
- Modify: `admin-web/src/layouts/AdminLayout.vue`
- Modify: `admin-web/tests/dish-template-market.test.js`
- Modify: `admin-web/tests/dish-template-change-requests.test.js`
- Create: `admin-web/tests/platform-dish-template-management.test.js`

- [ ] **Step 1: Add failing API, route and UI contract tests**

Cover merchant steps/null image, platform menu permissions, all admin filters, optimistic updates, derived status display, private preview, image promotion/rejection and review-step comparison.

- [ ] **Step 2: Implement API modules and routes**

Keep HTTP calls in `src/api/`; platform routes require platform admin identity and merchant routes remain merchant-scoped.

- [ ] **Step 3: Implement operational views**

Use dense tables and unframed detail sections, status pills, explicit empty/error/loading states, step editors and guarded dialogs. Do not expose storage paths or let generic form state submit server-owned fields.

- [ ] **Step 4: Run tests and production build**

Run: `cd admin-web; npm test; npm run build`

Expected: all tests pass and Vite production build succeeds.

- [ ] **Step 5: Commit**

```bash
git add admin-web
git commit -m "feat: manage complete recipe templates on PC"
```

### Task 13: Generate The Full Source Dataset And Close Verification

**Files:**
- Modify: `tools/cooklikehoc-sync/config/*.json`
- Create: `backend/src/main/resources/db/data/cooklikehoc-sync-manifest.json`
- Create: `backend/src/main/resources/db/data/cooklikehoc-quality-report.json`
- Modify: `backend/src/main/resources/db/migration/V4__init_dish_template_market.sql`
- Create: `docs/verification/2026-09-01-cooklikehoc-template-sync.md`

- [ ] **Step 1: Provision and verify the canonical source checkout**

Run:

```bash
git clone https://github.com/Gar-b-age/CookLikeHOC.git tools/cooklikehoc-sync/.source/CookLikeHOC
git -C tools/cooklikehoc-sync/.source/CookLikeHOC checkout --detach f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed
git -C tools/cooklikehoc-sync/.source/CookLikeHOC rev-parse HEAD
```

If the directory already exists, run `git -C tools/cooklikehoc-sync/.source/CookLikeHOC fetch origin` before the detached checkout instead of cloning again. `.source/` is gitignored. Expected revision: `f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed`.

- [ ] **Step 2: Run draft mode to bootstrap review inputs**

Run: `cd tools/cooklikehoc-sync; mvn package; java -jar target/cooklikehoc-sync.jar --mode draft --source-dir .source/CookLikeHOC --config-dir config --output-dir ../../backend/target/cooklikehoc-sync`

Draft mode never writes V4. It emits deterministic `template-id-allocation-proposals.json`, `rewrite-review-queue.json` and parser/merge issues. Allocation proposals sort new stable keys and allocate above the committed high-water mark; existing allocations can never be proposed for renumbering.

- [ ] **Step 3: Resolve and commit all draft review inputs**

Review every allocation proposal, duplicate-primary mapping, procurement carry-over and high-similarity step. Merge accepted stable IDs into `config/template-id-allocations.json` and record each rewrite disposition with reviewer, fixed review time and `APPROVED` or corrected rewritten content in `config/rewrite-reviews.json`. Re-run draft mode until parser/merge issues are zero and no unallocated source key or unresolved review item remains; do not fabricate procurement quantities to clear warnings.

- [ ] **Step 4: Run release mode against all 336 Markdown files**

Run: `cd tools/cooklikehoc-sync; java -jar target/cooklikehoc-sync.jar --mode release --source-dir .source/CookLikeHOC --config-dir config --output-dir ../../backend/target/cooklikehoc-sync --v4-file ../../backend/src/main/resources/db/migration/V4__init_dish_template_market.sql --manifest-file ../../backend/src/main/resources/db/data/cooklikehoc-sync-manifest.json --quality-report-file ../../backend/src/main/resources/db/data/cooklikehoc-quality-report.json`

Expected report: 336 source files, 333 unique titles, 15 categories, 192 repository images, 179 image declarations, zero unresolved files/links/components/aliases/allocations/rewrite warnings. Release mode stages and atomically replaces the real manifest, quality report and V4 marker section; failure before all validation leaves all three committed targets unchanged.

- [ ] **Step 5: Verify deterministic replacement and real source assertions**

Confirm the updater replaced the entire previous marker section, left all schema/legacy text outside the markers byte-identical, and left no stale generated source key. Confirm all source processes are present where provided; recipes without steps keep an empty step list; undeclared images remain private; missing-image templates have null public fields; components are not importable. Save `backend/target/cooklikehoc-sync/artifact-sha256.json` as `first-run-sha256.json`, run the exact release command again, and compare the two hash manifests byte-for-byte. Do not use `git diff --exit-code` for this repeatability check because the first release intentionally creates uncommitted artifacts.

- [ ] **Step 6: Run complete verification**

Run:

```bash
cd backend && mvn test
cd ../frontend && npm test
cd ../admin-web && npm test && npm run build
```

Expected: backend build success, mini-program tests pass, admin tests/build pass. Docker-backed MySQL tests must run and pass when Docker is available; otherwise record the exact skipped tests and run a real empty MySQL 8 Flyway startup before release.

- [ ] **Step 7: Write verification evidence**

Record source commit, artifact hashes, counts, unresolved issue count, test totals, MySQL execution result and the private image directory location without listing filesystem paths in public API examples.

- [ ] **Step 8: Commit**

```bash
git add tools/cooklikehoc-sync/config backend/src/main/resources/db/data backend/src/main/resources/db/migration/V4__init_dish_template_market.sql docs/verification
git commit -m "feat: sync full CookLikeHOC recipe catalog"
```
