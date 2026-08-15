# Template Dish Change Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add merchant-submitted, platform-admin-reviewed full replacement of system template dish information without updating already imported merchant dish copies.

**Architecture:** Keep the existing traditional MVC structure inside the `dish` module. Persist immutable base and target snapshots in a dedicated review table, use MySQL row locks plus a template version column for concurrency, and apply approved snapshots transactionally through MyBatis XML Mappers.

**Tech Stack:** Java 17 language level on a Java 21 runtime, Spring Boot 3, MyBatis XML, MyBatis-Plus annotations, MySQL 8, Flyway, Jackson, Jakarta Validation, springdoc-openapi, JUnit 5, Mockito, Testcontainers MySQL.

---

## File Structure

**Create**

- `backend/src/main/resources/db/migration/V8__add_dish_template_change_review.sql`: template version column and review table; V7 already backfills family meal slots.
- `backend/src/main/java/com/familykitchen/dish/controller/DishTemplateChangeRequestController.java`: merchant endpoints.
- `backend/src/main/java/com/familykitchen/dish/controller/AdminDishTemplateChangeRequestController.java`: platform-admin endpoints.
- `backend/src/main/java/com/familykitchen/dish/mapper/DishTemplateChangeRequestMapper.java`: review persistence contract.
- `backend/src/main/resources/mapper/dish/DishTemplateChangeRequestMapper.xml`: row locks, paging, state transitions, template replacement.
- `backend/src/main/java/com/familykitchen/dish/service/DishTemplateChangeRequestService.java`: review use-case interface.
- `backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateChangeRequestServiceImpl.java`: snapshot, validation, locking, approval and notifications.
- `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateChangeRequestDO.java`: MyBatis-Plus mapped review entity.
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateSnapshotRequest.java`: complete target snapshot and ingredient DTO.
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateChangeSubmitRequest.java`: submit note plus target snapshot.
- `backend/src/main/java/com/familykitchen/dish/model/dto/MerchantDishTemplateChangeQuery.java`: tenant-safe merchant list filters.
- `backend/src/main/java/com/familykitchen/dish/model/dto/AdminDishTemplateChangeQuery.java`: platform list filters including merchant/template IDs.
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateApproveRequest.java`: optional approval reason.
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateRejectRequest.java`: required rejection reason.
- `backend/src/main/java/com/familykitchen/dish/service/DishTemplateSnapshotValidator.java`: strict snapshot conversion, size and cross-field validation.
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateSnapshotView.java`: parsed snapshot response.
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateChangeItemView.java`: list item.
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateChangeDetailView.java`: double snapshots, current version and stale flag.
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateChangePageView.java`: paging result.
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateChangeSubmitView.java`: submission result.
- `backend/src/test/java/com/familykitchen/database/DishTemplateChangeMigrationTest.java`: V8 schema contract.
- `backend/src/test/java/com/familykitchen/dish/DishTemplateChangeRequestServiceTest.java`: service state/transaction contract.
- `backend/src/test/java/com/familykitchen/dish/DishTemplateSnapshotValidatorTest.java`: strict snapshot boundary contract.
- `backend/src/test/java/com/familykitchen/database/DishTemplateChangeMapperContractTest.java`: explicit row-lock and tenant-filter SQL contract.
- `backend/src/test/java/com/familykitchen/dish/DishTemplateChangeControllerRouteTest.java`: endpoint and springdoc contract.
- `backend/src/test/java/com/familykitchen/database/DishTemplateChangeMigrationMySqlTest.java`: MySQL JSON/CHECK/generated-column contract.
- `backend/src/test/java/com/familykitchen/dish/DishTemplateChangeTransactionMySqlTest.java`: approval transaction and rollback integration.
- `backend/src/test/java/com/familykitchen/dish/DishTemplateChangeConcurrencyMySqlTest.java`: concurrent state transition integration.

**Modify**

- `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateEntity.java`: add `version`.
- `backend/src/main/java/com/familykitchen/dish/mapper/DishTemplateMapper.java`: lock template/category and version-aware update methods.
- `backend/src/main/resources/mapper/dish/DishTemplateMapper.xml`: map version and add replacement SQL.
- `backend/src/main/java/com/familykitchen/notification/mapper/NotificationPersistenceMapper.java`: generated-key entity insert.
- `backend/src/main/java/com/familykitchen/notification/model/entity/NotificationDO.java`: add required receiver type and receiver ID.
- `backend/src/main/resources/mapper/notification/NotificationPersistenceMapper.xml`: entity notification insert.
- `backend/pom.xml`: add `org.testcontainers:junit-jupiter` and `org.testcontainers:mysql` test dependencies at version `1.19.8`, while retaining Java 17.
- `backend/src/test/java/com/familykitchen/database/FreshDatabaseMigrationTest.java`: include V8 after the existing V7 migration.
- `docs/api-spec.md`, `docs/frontend-api-guide.md`, `docs/database-design.md`, `docs/data-dictionary.md`: synchronize API and schema.

All Maven commands below run from the repository root with `-f backend/pom.xml`.

### Task 1: V8 Database Contract

**Files:** migration and database tests listed above.

- [ ] **Step 1: Write the failing migration test**

Assert V8 exists, adds `dish_templates.version`, creates the fully commented review table, uses `JSON`, `DATETIME(6)`, the pending generated column, status `CHECK`, exact indexes, InnoDB/UTF-8, and no physical foreign keys.

```java
assertTrue(sql.contains("ADD COLUMN version bigint NOT NULL DEFAULT 0"));
assertTrue(sql.contains("pending_marker tinyint GENERATED ALWAYS AS"));
assertTrue(sql.contains("CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN'))"));
assertFalse(sql.toLowerCase().contains("foreign key"));
```

- [ ] **Step 2: Run the test and verify RED**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeMigrationTest,FreshDatabaseMigrationTest test`

Expected: failure because V8 does not exist and the migration list ends at the existing V7 meal-slot backfill.

- [ ] **Step 3: Add MySQL integration dependencies and write the failing MySQL migration test**

Add Testcontainers `junit-jupiter` and `mysql` version `1.19.8` in test scope. Write `DishTemplateChangeMigrationMySqlTest` so a clean MySQL 8 container migrates through V8 and exercises JSON, status `CHECK`, the generated marker and pending uniqueness.

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeMigrationMySqlTest test`

Expected: FAIL because the asserted V8 table and template version column do not exist.

- [ ] **Step 4: Add V8 and update migration order**

Use a generated `pending_marker` and unique index `(merchant_id,template_id,pending_marker)`. Add all Chinese column/table comments and no foreign keys.

- [ ] **Step 5: Run migration tests and verify GREEN**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeMigrationTest,FreshDatabaseMigrationTest test`

Expected: zero failures.

- [ ] **Step 6: Verify the migration on MySQL 8 with Testcontainers**

`DishTemplateChangeMigrationMySqlTest` migrates a clean MySQL 8 database through V8 and asserts JSON values are accepted, invalid statuses fail the `CHECK`, and concurrent pending inserts for one merchant/template are rejected by the generated-column unique index.

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeMigrationMySqlTest test`

Expected: PASS against an actual MySQL 8 container; a missing Docker runtime is an explicit environment failure, not an H2 substitute.

- [ ] **Step 7: Commit**

`git commit -m "feat: add template dish change review schema"`

### Task 2: Snapshot DTOs and Validation

**Files:** snapshot DTO, submit DTO, query DTO, review DTO, snapshot VO, entity.

- [ ] **Step 1: Write failing validator tests for snapshot boundaries**

Cover schema version, required full fields, local image path, price scale/range, tag limits/enums, ingredient limits, duplicate normalized names, calc type quantity rules, unknown JSON properties and unsupported snapshots.

```java
assertThrows(BusinessException.class,
    () -> validator.parseAndValidate(snapshotWithSchemaVersion(2)));
```

- [ ] **Step 2: Run the test and verify RED**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateSnapshotValidatorTest test`

Expected: compilation failure because `DishTemplateSnapshotValidator` does not exist.

- [ ] **Step 3: Implement annotated DTOs and normalization**

Receive `targetSnapshot` as `JsonNode`. Use records with `@Schema` and Bean Validation for the documented shape, then use a validator-owned `ObjectReader` configured with `FAIL_ON_UNKNOWN_PROPERTIES` to parse the node. Reject serialized target snapshots larger than 1 MiB in UTF-8 before conversion; normalize and enforce all cross-field rules. Keep snapshot schema at version 1 without changing the global application `ObjectMapper`.

- [ ] **Step 4: Add review entity and view records**

Use `@TableName`, `@TableId`, `@TableField`, detailed UTF-8 comments and `@Schema` descriptions.

- [ ] **Step 5: Run the validator test and verify GREEN**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateSnapshotValidatorTest test`

- [ ] **Step 6: Commit**

`git commit -m "feat: define template change snapshot contract"`

### Task 3: Mapper and Merchant Submission Flow

**Files:** change Mapper/XML, template Mapper/XML/entity, service interface/implementation.

- [ ] **Step 1: Extend failing tests for submission behavior**

Assert merchant ID comes from context, template is locked, base version and base snapshot are captured together, pending precheck is friendly, unique-key conflicts become Chinese 409 errors, and merchant list/detail are tenant-filtered.

- [ ] **Step 2: Verify RED**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeRequestServiceTest,DishTemplateChangeMapperContractTest test`

Expected: FAIL because the submission Service and four lock statements do not exist.

- [ ] **Step 3: Implement explicit Mapper locks and submission SQL**

Required SQL includes:

```sql
SELECT ... FROM dish_templates WHERE id=#{templateId} FOR UPDATE;
SELECT ... FROM dish_template_change_requests
WHERE id=#{requestId} AND merchant_id=#{merchantId} FOR UPDATE;
```

Expose and test the exact methods `selectForUpdate(requestId)`, `selectMerchantForUpdate(requestId, merchantId)`, `selectTemplateForUpdate(templateId)` and `selectCategoryForUpdate(categoryId)`. Add `DishTemplateChangeMapperContractTest` to assert every statement contains `FOR UPDATE` and merchant-scoped SQL includes `merchant_id`.

Map the new template `version` everywhere it is selected.

- [ ] **Step 4: Implement transactional submit/list/detail/withdraw**

`submit` locks the template, then locks and validates the target category as existing/enabled before reading ingredients and inserting the application. `withdraw` locks the tenant-scoped application and only changes `PENDING` to `WITHDRAWN`.

- [ ] **Step 5: Run service and Mapper contract tests**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeRequestServiceTest,DishTemplateChangeMapperContractTest test`

- [ ] **Step 6: Commit**

`git commit -m "feat: add merchant template change submissions"`

### Task 4: Platform Review and Atomic Replacement

**Files:** service implementation, both Mappers/XML, notification Mapper/XML.

- [ ] **Step 1: Add failing approval/reject/concurrency tests**

Cover request lock, template lock, category lock, stale version 409, full field replacement, delete/reinsert ingredients, version increment, rejection reason and processed-state conflict. Approval must lock application, template and target category in that order and reject a missing/disabled template or category without template writes or notification; rejection locks only the application. The real MySQL transaction test verifies an existing merchant `dishes` copy remains byte-for-byte unchanged.

```java
verify(templateMapper).deleteTemplateIngredients(templateId);
```

- [ ] **Step 2: Verify RED**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeRequestServiceTest test`

Expected: FAIL on missing approval/rejection implementation and atomic notification behavior.

- [ ] **Step 3: Implement generated-key notification insert**

Add a distinct `insertNotificationEntity(NotificationDO notification)` method with `useGeneratedKeys=true`; keep the existing parameter-based `insertNotification(...)` method unchanged for compatibility. Add `receiverType` and `receiverId` to `NotificationDO` and its result map so the generated-key entity method is complete.

- [ ] **Step 4: Implement approve/reject transactions**

Approval lock order is application, template, target category. Confirm the locked template and category still exist and are enabled, compare `version`, validate the parsed snapshot again, replace the main row and ingredients, increment version, transition the application, create the merchant result notification and save its ID in one transaction. Rejection locks only the application. Notification tests assert `receiverType=merchant`, the correct merchant receiver ID, and content containing request ID, template name, review result, review reason and a detail entry.

- [ ] **Step 5: Run service tests and verify GREEN**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeRequestServiceTest test`

- [ ] **Step 6: Verify transaction and concurrency behavior on MySQL 8**

`DishTemplateChangeTransactionMySqlTest` proves approval replaces all template fields and ingredients atomically, increments `version`, writes one correctly addressed and fully populated notification, rolls back every write when notification insertion fails, and leaves a pre-existing imported merchant dish unchanged. It also proves an ingredient-only intervening template change increments the version, a stale approval returns conflict while keeping the request `PENDING` with no notification, unavailable/disabled templates or categories cannot be approved and produce no writes or notification, rejection creates exactly one notification with the required content and persists its ID, and withdrawal creates no notification. `DishTemplateChangeConcurrencyMySqlTest` races approve/reject/withdraw and two approvals based on the same template version, asserting one valid terminal transition and no duplicate notifications.

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeTransactionMySqlTest,DishTemplateChangeConcurrencyMySqlTest test`

- [ ] **Step 7: Commit**

`git commit -m "feat: review and apply template dish changes"`

### Task 5: Merchant and Admin Controllers

**Files:** two controllers and route tests.

- [ ] **Step 1: Write failing route and annotation tests**

Assert all eight routes, merchant/platform permission guards, `@Tag`, `@Operation`, `@SecurityRequirement`, `@ApiResponse`, `@Parameter`, `@Valid`, and explicit Chinese responses. Annotate the `JsonNode targetSnapshot` component with `@Schema(implementation = DishTemplateSnapshotRequest.class)` and assert `/v3/api-docs` exposes every nested snapshot and ingredient field rather than a free-form object.

- [ ] **Step 2: Verify RED**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeControllerRouteTest test`

- [ ] **Step 3: Implement merchant controller**

Expose submit, paged list, detail and withdraw under `/merchant` and always derive merchant ID from `CurrentUserProvider`.

- [ ] **Step 4: Implement platform controller**

Expose paged list, detail, approve and reject under `/admin`; require platform backend access.

- [ ] **Step 5: Verify controller tests GREEN**

Run: `mvn -f backend/pom.xml -q -Dtest=DishTemplateChangeControllerRouteTest test`

- [ ] **Step 6: Commit**

`git commit -m "feat: expose template dish review APIs"`

### Task 6: API and Database Documentation

**Files:** four documentation files.

- [ ] **Step 1: Add complete API examples**

Document submit payload, full snapshot, page/detail responses, approve/reject payloads, status machine, all HTTP statuses and Chinese error messages.

- [ ] **Step 2: Add multi-client integration notes**

Explain that mini-program merchant admin and PC admin can submit; platform PC admin reviews; imported copies never receive template updates.

- [ ] **Step 3: Update database design and dictionary**

Describe V8, version semantics, both JSON snapshots and logical references.

- [ ] **Step 4: Run documentation coverage tests**

Run: `mvn -f backend/pom.xml -q -Dtest=DocumentationCoverageTest test`

- [ ] **Step 5: Commit**

`git commit -m "docs: document template dish change review APIs"`

### Task 7: Full Verification and Real MySQL

- [ ] **Step 1: Run the complete backend suite**

Run: `mvn -f backend/pom.xml clean test`

Expected: all tests pass with zero failures and errors.

- [ ] **Step 2: Build the production JAR**

Run: `mvn -f backend/pom.xml -q -DskipTests package`

Expected: exit code 0.

- [ ] **Step 3: Start against configured real MySQL**

Start `backend/target/family-kitchen-backend-0.1.0-SNAPSHOT.jar` hidden, allow Flyway to apply V8, and confirm `Successfully validated 8 migrations` and schema version 8.

- [ ] **Step 4: Verify the real API flow**

Login as a merchant administrator, submit a full change for a template, verify pending state and unchanged template; login as platform admin, approve it, verify all fields/ingredients changed and template version incremented; verify an existing imported merchant dish remains unchanged.

- [ ] **Step 5: Verify OpenAPI**

Fetch `/v3/api-docs`, confirm all eight routes and Chinese schemas, then leave the backend running on an available documented port.

- [ ] **Step 6: Final status check**

Inspect changed files only, ensure UTF-8, no mock data, no temporary scripts, no physical foreign keys and no unrelated cleanup.
