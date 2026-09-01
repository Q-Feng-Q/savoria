# Database Baseline Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the development-only V1-V11 migration chain with a clean V1-V4 empty-database baseline that preserves every final business table and approved seed row while removing legacy wallet/cart migration infrastructure.

**Architecture:** First execute the untouched V1-V11 chain in an owned disposable MySQL container and freeze its final metadata and seed data as a canonical manifest. Then atomically rewrite V1-V4 and delete V5-V11, compare the new empty-database result to that oracle, and finally remove runtime migration code and all residual configuration references.

**Tech Stack:** Java 17, Spring Boot, Flyway, MySQL 8, MyBatis/MyBatis-Plus, JUnit 5, Testcontainers, Jackson, Maven 3.9.9

---

## Safety Rules

- Do not modify or stage the pre-existing dirty cart files or `AGENTS.md`.
- Before every commit, run `git status --short` and `git diff --cached --name-only`; stop if any pre-existing dirty path is staged.
- Do not drop, recreate, repair, baseline, or edit Flyway history in the user's current database during implementation.
- Every MySQL test uses `SafeTestFlyway.configure(...)`, existing ownership guards, and an owned disposable schema.
- Oracle generation cannot skip when Docker is unavailable. Failure to start owned MySQL blocks generation and must be reported.

## Canonical Manifest Shape

`legacy-v11-baseline-manifest.json` uses sorted arrays and stable JSON serialization:

- `tables`: only the 52 retained business tables, with table name, engine, charset, collation and comment. Migration-only metadata is never serialized.
- `columns`: table/name/ordinal, exact type, precision/scale, datetime precision, nullability, default, generated expression, `extra`, charset, collation and comment.
- `indexes`: table/name/sequence, uniqueness, type, ordered column or expression, prefix length, collation and visibility.
- `checks`: table/name, normalized expression and enforcement state.
- `seeds`: sorted complete rows for the ten approved seeded tables; volatile timestamps are excluded and all other columns included.
- `emptyBusinessTables`: every other retained business table, each with zero rows.
- `foreignKeyCount`: exactly zero.

### Task 1: Freeze the untouched V1-V11 final state

**Files:**
- Create: `backend/src/test/java/com/familykitchen/testsupport/DatabaseBaselineManifest.java`
- Create: `backend/src/test/java/com/familykitchen/database/LegacyV11BaselineManifestCaptureIT.java`
- Create: `backend/src/test/resources/database/legacy-v11-baseline-manifest.json`

- [ ] **Step 1: Add the manifest serializer and metadata reader**

Read MySQL `information_schema.tables`, `columns`, `statistics`, `table_constraints`, and `check_constraints`; query approved seed rows; normalize expressions; sort every collection; serialize deterministically with Jackson.

- [ ] **Step 2: Add an explicit capture test**

Require `-DfamilyKitchen.writeBaselineManifest=true`, start owned MySQL 8, run untouched V1-V11 with `SafeTestFlyway`, assert 52 retained plus 8 migration-only tables, and write only the 52 retained tables to the fixture. Construct migration-only names from fragments inside the capture IT so forbidden literals are not retained in test source. Missing opt-in fails clearly rather than silently writing.

- [ ] **Step 3: Generate the oracle**

```powershell
cd backend
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -DfamilyKitchen.writeBaselineManifest=true -Dtest=LegacyV11BaselineManifestCaptureIT test
```

Expected: PASS, zero skipped tests, and exact counts of 10 dish categories, 73 merchant ingredients, 9 template categories, 240 templates and 403 template ingredients.

The `*IT` suffix keeps this opt-in writer out of ordinary Surefire discovery; it only runs through the explicit `-Dtest` command above.

- [ ] **Step 4: Prove deterministic regeneration**

Run the same command again and verify the manifest has no diff.

- [ ] **Step 5: Commit only the three oracle paths**

Inspect staged names and commit `test: freeze final database baseline`.

### Task 2: Atomically consolidate V1-V4 and remove V5-V11

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_identity_family_and_merchant.sql`
- Modify: `backend/src/main/resources/db/migration/V2__init_menu_order_wallet_and_purchase.sql`
- Modify: `backend/src/main/resources/db/migration/V3__init_system_notification_and_defaults.sql`
- Modify: `backend/src/main/resources/db/migration/V4__init_dish_template_market.sql`
- Delete: `backend/src/main/resources/db/migration/V5__expand_regional_dish_templates.sql`
- Delete: `backend/src/main/resources/db/migration/V6__finalize_regional_dish_template_images.sql`
- Delete: `backend/src/main/resources/db/migration/V7__backfill_default_family_meal_slots.sql`
- Delete: `backend/src/main/resources/db/migration/V8__add_dish_template_change_review.sql`
- Delete: `backend/src/main/resources/db/migration/V9__add_family_featured_dish.sql`
- Delete: `backend/src/main/resources/db/migration/V10__add_merchant_featured_dishes.sql`
- Delete: `backend/src/main/resources/db/migration/V11__add_family_cart_time_and_wallet.sql`
- Create: `backend/src/test/java/com/familykitchen/database/ConsolidatedBaselineMySqlTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/FreshDatabaseMigrationTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/DishTemplateMigrationTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/DishTemplateRegionalExpansionTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/DishTemplateChangeMigrationTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/MerchantFeaturedDishMigrationContractTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/MerchantFeaturedDishMigrationMySqlTest.java`

- [ ] **Step 1: Write failing four-file, strict-encoding and oracle-equivalence contracts**

Require exactly V1-V4. Decode with a `CharsetDecoder` using `CodingErrorAction.REPORT`; reject BOM, CRLF, malformed bytes and missing final LF. Require zero physical foreign-key syntax and migration-only table names. Before changing SQL, create `ConsolidatedBaselineMySqlTest`: run only the old V1-V4 in owned MySQL with `SafeTestFlyway`, generate the canonical 52-table manifest, and require exact equality with the oracle plus absent migration tables, empty unseeded tables, BCrypt `123456`, and no unapproved seed rows.

- [ ] **Step 2: Confirm the expected failure**

```powershell
cd backend
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=FreshDatabaseMigrationTest,ConsolidatedBaselineMySqlTest test
```

Expected: FAIL because V5-V11 still exist and old V1-V4 do not yet equal the final V11 oracle. Confirm from Surefire output that `ConsolidatedBaselineMySqlTest` executed and was not skipped.

- [ ] **Step 3: Rewrite V1 and V2 to final schema**

Put `families.featured_dish_id` in V1. Put `dishes.featured_at`, final cart/order/order-item nullability, `expected_meal_time`, `source_cart_id`, cart `version`, and the six permanent V11 tables in V2. Retain `member_wallets` and `wallet_ledgers`; exclude migration-only tables and backfill DML.

- [ ] **Step 4: Rewrite V4 to final template state**

Put template `version` and `dish_template_change_requests` in V4, append the 42 V5 templates, preserve 403 total ingredients, and fold every V6 attribution correction directly into inserts.

- [ ] **Step 5: Delete V5-V11 in the same working change**

Use `apply_patch`. Do not run Flyway between Steps 3 and 5 because duplicate DDL is expected until deletion completes.

- [ ] **Step 6: Adapt template and featured-dish tests**

Point regional/change-review checks at V4. Convert V10 tests from historical backfill checks to final `featured_at` schema and Mapper/business behavior.

- [ ] **Step 7: Run the atomic baseline set**

```powershell
cd backend
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=FreshDatabaseMigrationTest,ConsolidatedBaselineMySqlTest,DishTemplateMigrationTest,DishTemplateRegionalExpansionTest,DishTemplateChangeMigrationTest,MerchantFeaturedDishMigrationContractTest,MerchantFeaturedDishMigrationMySqlTest test
```

Expected: PASS. Inspect Surefire XML and require both MySQL tests to report zero skipped tests.

Run this exact report guard after Maven:

```powershell
$reports = @('ConsolidatedBaselineMySqlTest','MerchantFeaturedDishMigrationMySqlTest')
foreach ($name in $reports) {
  [xml]$xml = Get-Content -LiteralPath "target/surefire-reports/TEST-com.familykitchen.database.$name.xml"
  if ([int]$xml.testsuite.skipped -ne 0 -or [int]$xml.testsuite.failures -ne 0 -or [int]$xml.testsuite.errors -ne 0 -or [int]$xml.testsuite.tests -lt 1) { exit 1 }
}
```

- [ ] **Step 8: Commit with exact staging**

Stage only the 11 migration paths and seven named test paths; inspect staged names; commit `refactor: consolidate database initialization baseline`.

### Task 3: Remove migration runtime and compatibility coupling

**Delete production:**
- `backend/src/main/java/com/familykitchen/migration/ApplicationInstanceLeaseService.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletDdlExecutor.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletFamilyExecutor.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationMapper.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationRunner.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationService.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyWalletBusinessTransactionBarrierAspect.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyWalletBusinessWriteBarrierFilter.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyWalletMigrationBarrierService.java`
- `backend/src/main/java/com/familykitchen/migration/FamilyWalletMigrationLeaseService.java`
- `backend/src/main/resources/mapper/migration/FamilyCartWalletMigrationMapper.xml`
- `backend/src/main/java/com/familykitchen/wallet/service/PersonalWalletCutoverGuard.java`

**Delete tests:**
- `backend/src/test/java/com/familykitchen/FamilyKitchenApplicationMigrationBootstrapTest.java`
- `backend/src/test/java/com/familykitchen/migration/ApplicationInstanceLeaseServiceTest.java`
- `backend/src/test/java/com/familykitchen/migration/FamilyWalletBusinessTransactionBarrierAspectTest.java`
- `backend/src/test/java/com/familykitchen/migration/TransactionAdvisorOrderingContractTest.java`
- `backend/src/test/java/com/familykitchen/wallet/PersonalWalletCutoverGuardTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyCartWalletDdlExecutorSafetyTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationContractTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationMySqlTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationRecoveryMySqlTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationRunnerTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationSafetyContractTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationServiceSafetyTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyWalletBusinessWriteBarrierFilterTest.java`
- `backend/src/test/java/com/familykitchen/database/FamilyWalletPermanentFenceSqlContractTest.java`

**Modify production/config:**
- `backend/src/main/java/com/familykitchen/FamilyKitchenApplication.java`
- `backend/src/main/java/com/familykitchen/common/config/MybatisConfiguration.java`
- `backend/src/main/java/com/familykitchen/config/NormalSchedulingConfiguration.java`
- `backend/src/main/java/com/familykitchen/auth/config/AdminAccountInitializer.java`
- `backend/src/main/java/com/familykitchen/order/service/impl/FamilyOrderApplicationServiceImpl.java`
- `backend/src/main/resources/application.yml`

**Modify tests/inventory:**
- `backend/src/test/java/com/familykitchen/ApplicationContextTest.java`
- `backend/src/test/java/com/familykitchen/cart/SharedCartConcurrencyMySqlTest.java`
- `backend/src/test/java/com/familykitchen/family/FamilyMemberCartCleanupConcurrencyMySqlTest.java`
- `backend/src/test/java/com/familykitchen/order/FamilyOrderSubmissionMySqlTest.java`
- `backend/src/test/java/com/familykitchen/order/FamilyWalletOrderLifecycleMySqlTest.java`
- `backend/src/test/java/com/familykitchen/wallet/FamilyWalletConcurrencyMySqlTest.java`
- `backend/src/test/java/com/familykitchen/order/FamilyOrderApplicationServiceTest.java`
- `backend/src/test/java/com/familykitchen/contract/MiniProgramServiceAuthorizationCases.java`
- `frontend/tests/fixtures/delivery-baseline.json`

- [ ] **Step 1: Add a failing zero-reference contract**

In `ApplicationContextTest`, scan `backend/src/main`, `backend/src/test`, and `frontend/tests/fixtures/delivery-baseline.json` for deleted package/table/property/guard names and verify ordinary context startup without migration settings. Build each forbidden term from string fragments inside the test so the scan cannot match its own source literals.

- [ ] **Step 2: Confirm the expected failure**

```powershell
cd backend
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=ApplicationContextTest test
```

- [ ] **Step 3: Delete every exact migration-only file listed above**

Use `apply_patch`; keep business wallet/cart/order/idempotency code.

- [ ] **Step 4: Simplify coupled production files**

Remove migration argument parsing, mapper scan, conditions, lease settings and order cutover guard injection while keeping normal scheduling, admin initialization and order submission.

- [ ] **Step 5: Adapt coupled tests and frontend inventory**

Remove guard mocks and obsolete properties while preserving business assertions. Remove deleted backend test identities from `delivery-baseline.json`.

- [ ] **Step 6: Run startup, order and authorization tests**

```powershell
cd backend
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=ApplicationContextTest,FamilyOrderApplicationServiceTest,MiniProgramResourceAuthorizationTest test
```

Expected: PASS with no migration beans or properties.

- [ ] **Step 7: Run frontend tests**

Run: `cd frontend; npm test`

- [ ] **Step 8: Commit with explicit staging**

Stage every named path individually, inspect staged names, and commit `refactor: remove legacy database migration runtime`. Never use broad `git add backend/src/main backend/src/test`.

### Task 4: Document reset procedure and verify

**Files:**
- Modify: `backend/README.md`
- Modify: `docs/database-design.md`
- Modify: `backend/src/test/java/com/familykitchen/database/FreshDatabaseMigrationTest.java`

- [ ] **Step 1: Document local reset safety**

Require `SELECT DATABASE()`, backup, typed confirmation of the exact disposable schema name, and separate human-confirmed drop/create commands. State tests/app never delete the database and `flyway repair`, `baselineOnMigrate`, history editing and in-place production upgrades are unsupported.

- [ ] **Step 2: Run baseline, encoding and startup verification**

```powershell
cd backend
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=FreshDatabaseMigrationTest,ConsolidatedBaselineMySqlTest,ApplicationContextTest test
```

Inspect Surefire XML and require `ConsolidatedBaselineMySqlTest` to have zero skipped tests.

Run this exact report guard:

```powershell
[xml]$xml = Get-Content -LiteralPath 'target/surefire-reports/TEST-com.familykitchen.database.ConsolidatedBaselineMySqlTest.xml'
if ([int]$xml.testsuite.skipped -ne 0 -or [int]$xml.testsuite.failures -ne 0 -or [int]$xml.testsuite.errors -ne 0 -or [int]$xml.testsuite.tests -lt 1) { exit 1 }
```

- [ ] **Step 3: Run the full backend suite**

```powershell
cd backend
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' test
```

Expected: BUILD SUCCESS; report other Docker-backed skipped tests separately.

- [ ] **Step 4: Run clean package**

```powershell
cd backend
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' clean package
```

- [ ] **Step 5: Final repository checks**

Run `git diff --check`, `git status --short`, migration listing and zero-reference `rg`. Confirm only V1-V4, strict UTF-8/LF, 52 retained tables equal to oracle, and pre-existing cart changes plus `AGENTS.md` untouched and unstaged.

- [ ] **Step 6: Commit exact documentation/test paths**

Stage only the three task paths, inspect staged names, and commit `docs: document clean database initialization`.
