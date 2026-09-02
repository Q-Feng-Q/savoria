# Merchant Dish Bulk Delete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add merchant-scoped bulk logical deletion, an auditable recycle bin, and batch restore while keeping deleted dishes out of family menus and new cart/order activity.

**Architecture:** Extend `dishes` with deletion audit fields and treat `deleted` as a terminal hidden state until explicit restore to `inactive`. Implement transactional batch commands under the existing merchant/dish locking model, disable dependent family-menu rows, expose explicit available/deleted query scopes, and add a Mini Program recycle-bin workflow using one page-wide mutation guard.

**Tech Stack:** Java 17, Spring Boot, MyBatis XML, Flyway/MySQL, JUnit 5/Mockito/Testcontainers, WeChat Mini Program WXML/WXSS/CommonJS, Node `node:test`.

---

### Task 1: Add the logical-deletion schema and persistence contract

**Files:**
- Create: `backend/src/main/resources/db/migration/V12__add_dish_logical_deletion.sql`
- Create: `backend/src/test/java/com/familykitchen/database/DishLogicalDeletionMigrationContractTest.java`
- Create: `backend/src/test/java/com/familykitchen/database/DishLogicalDeletionMigrationMySqlTest.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/entity/DishEntity.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/vo/DishView.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/mapper/DishMapper.java`
- Modify: `backend/src/main/resources/mapper/dish/DishMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/DishApplicationService.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/controller/DishController.java`
- Modify: `backend/src/main/java/com/familykitchen/family/service/impl/FamilyApplicationServiceImpl.java`
- Modify: `backend/src/test/java/com/familykitchen/dish/DishViewSerializationTest.java`
- Modify: `backend/src/test/java/com/familykitchen/dish/DishApplicationServiceUpdateTest.java`
- Modify: `backend/src/test/java/com/familykitchen/family/FamilyFeaturedDishResponseTest.java`
- Create: `backend/src/test/java/com/familykitchen/dish/DishListScopeControllerTest.java`
- Create: `backend/src/test/java/com/familykitchen/family/FamilyDishDetailDeletionTest.java`

- [ ] **Step 1: Write failing migration contract tests**

Require V12 to add nullable `deleted_at datetime(6)`, nullable `deleted_by bigint`, and `idx_dishes_merchant_status_deleted (merchant_id,status,deleted_at,id)` without modifying V1–V11.

- [ ] **Step 2: Write a failing full-chain MySQL migration test**

Follow `MerchantFeaturedDishMigrationMySqlTest`: start MySQL Testcontainers, migrate only through V11, seed legacy active/inactive rows, then apply V12. Verify column nullability, `datetime(6)` precision, and exact ordered index columns `merchant_id,status,deleted_at,id`; verify legacy rows remain unchanged with null audit fields.

- [ ] **Step 3: Run migration tests and verify RED**

`cd backend; D:\develop\apache-maven-3.9.9\bin\mvn.cmd -Dtest=DishLogicalDeletionMigrationContractTest,DishLogicalDeletionMigrationMySqlTest test`

Expected: FAIL because V12 does not exist.

- [ ] **Step 4: Add V12**

Use:

```sql
ALTER TABLE dishes
  ADD COLUMN deleted_at datetime(6) NULL COMMENT '逻辑删除时间' AFTER status,
  ADD COLUMN deleted_by bigint NULL COMMENT '执行逻辑删除的用户ID' AFTER deleted_at,
  ADD KEY idx_dishes_merchant_status_deleted (merchant_id,status,deleted_at,id);
```

- [ ] **Step 5: Add failing mapper/entity/view and scope tests**

Require all dish select column lists/result maps to expose `deleted_at`, `deleted_by`, and the recycle-bin operator display. Require `DishView` JSON to include `deletedAt` and `deletedByName`. In `DishListScopeControllerTest`, cover omitted scope, `available`, `deleted`, invalid scope, and nickname→username→“用户 {id}” fallback. In `FamilyDishDetailDeletionTest`, prove merchant and family detail reject deleted dishes.

- [ ] **Step 6: Implement model, callers, and mapper mapping as one compile-safe change**

Add `LocalDateTime deletedAt`, `Long deletedBy`, and display-name projection. Change `selectDishes` to accept `scope`; available selects `status <> 'deleted'`, deleted selects `status = 'deleted'` and left-joins `users` with `COALESCE(NULLIF(u.nickname,''),NULLIF(u.username,''),CONCAT('用户 ',d.deleted_by))`. In the same step update `DishApplicationService.dishes(user,scope)`, `DishApplicationServiceImpl`, `DishController @RequestParam(defaultValue="available")`, every `DishView` constructor/converter including `FamilyApplicationServiceImpl`, and all direct mocks/tests so `mvn testCompile` remains green. Ordinary merchant/family detail must reject `deleted`.

- [ ] **Step 7: Run focused tests and verify GREEN**

`cd backend; D:\develop\apache-maven-3.9.9\bin\mvn.cmd -Dtest=DishLogicalDeletionMigrationContractTest,DishLogicalDeletionMigrationMySqlTest,DishViewSerializationTest,DishApplicationServiceUpdateTest,DishListScopeControllerTest,FamilyFeaturedDishResponseTest,FamilyDishDetailDeletionTest test`

Expected: PASS.

- [ ] **Step 8: Commit**

`git add backend/src/main/resources/db/migration/V12__add_dish_logical_deletion.sql backend/src/main/java/com/familykitchen/dish/model/entity/DishEntity.java backend/src/main/java/com/familykitchen/dish/model/vo/DishView.java backend/src/main/java/com/familykitchen/dish/mapper/DishMapper.java backend/src/main/resources/mapper/dish/DishMapper.xml backend/src/main/java/com/familykitchen/dish/service/DishApplicationService.java backend/src/main/java/com/familykitchen/dish/service/impl/DishApplicationServiceImpl.java backend/src/main/java/com/familykitchen/dish/controller/DishController.java backend/src/main/java/com/familykitchen/family/service/impl/FamilyApplicationServiceImpl.java backend/src/test/java/com/familykitchen/database/DishLogicalDeletionMigrationContractTest.java backend/src/test/java/com/familykitchen/database/DishLogicalDeletionMigrationMySqlTest.java backend/src/test/java/com/familykitchen/dish/DishViewSerializationTest.java backend/src/test/java/com/familykitchen/dish/DishApplicationServiceUpdateTest.java backend/src/test/java/com/familykitchen/dish/DishListScopeControllerTest.java backend/src/test/java/com/familykitchen/family/FamilyFeaturedDishResponseTest.java backend/src/test/java/com/familykitchen/family/FamilyDishDetailDeletionTest.java && git commit -m "feat: add dish logical deletion schema"`

### Task 2: Implement exact batch-delete and restore API contracts

**Files:**
- Create: `backend/src/main/java/com/familykitchen/dish/model/dto/BatchDishMutationRequest.java`
- Create: `backend/src/main/java/com/familykitchen/dish/model/vo/BatchDishMutationResult.java`
- Create: `backend/src/test/java/com/familykitchen/dish/DishBulkMutationServiceTest.java`
- Create: `backend/src/test/java/com/familykitchen/dish/DishBulkMutationControllerRouteTest.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/DishApplicationService.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/controller/DishController.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/mapper/DishMapper.java`
- Modify: `backend/src/main/resources/mapper/dish/DishMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/family/mapper/FamilyMapper.java`
- Modify: `backend/src/main/resources/mapper/family/FamilyMapper.xml`

- [ ] **Step 1: Write failing request-validation tests**

Define `BatchDishMutationRequest(List<Long> dishIds)` with raw list size 1–100 and positive non-null elements. Test empty, 101 raw IDs, null, non-positive, duplicates, cross-merchant, and nonexistent IDs.

- [ ] **Step 2: Write failing batch service tests**

Assert IDs are deduplicated in first-seen order after raw-size validation; merchant row is locked once; dish rows are locked in ascending numeric order; any missing/foreign ID aborts before writes.

- [ ] **Step 3: Write failing mutation semantics tests**

Delete must set `status='deleted'`, `deleted_at=CURRENT_TIMESTAMP(6)`, `deleted_by=current user`, clear `featured_at`, disable matching family-menu rows without changing price/sort, and preserve first audit values for already-deleted rows. Disable family-menu rows for every validated unique delete ID, including already-deleted dishes, so stale enabled rows cannot revive later. Restore must set `inactive` and clear deletion/featured fields without enabling menus.

- [ ] **Step 4: Write failing route/response tests**

Require:

```java
@PostMapping("/dishes/batch-delete")
ApiResponse<BatchDishMutationResult> batchDelete(...)

@PostMapping("/dishes/batch-restore")
ApiResponse<BatchDishMutationResult> batchRestore(...)
```

Assert `requestedCount`, `uniqueCount`, `changedCount`, and `unchangedCount`.

- [ ] **Step 5: Run focused tests and verify RED**

`cd backend; D:\develop\apache-maven-3.9.9\bin\mvn.cmd -Dtest=DishBulkMutationServiceTest,DishBulkMutationControllerRouteTest test`

Expected: FAIL because DTOs, service methods, mapper writes, and routes are absent.

- [ ] **Step 6: Implement minimal transactional commands**

Add `bulkDelete(user, request)` and `bulkRestore(user, request)`. Use one transaction, lock merchant then sorted dish IDs, calculate changed/unchanged counts, issue guarded updates, and disable family-menu rows for all validated unique delete targets. Preserve first deletion audit values and keep counts state-based. Pending-review closure is added atomically to this command in Task 3 after its service API exists.

- [ ] **Step 7: Run focused tests and verify GREEN**

Run step 5 command. Expected: PASS.

- [ ] **Step 8: Commit**

`git add backend/src/main/java/com/familykitchen/dish/model/dto/BatchDishMutationRequest.java backend/src/main/java/com/familykitchen/dish/model/vo/BatchDishMutationResult.java backend/src/main/java/com/familykitchen/dish/service/DishApplicationService.java backend/src/main/java/com/familykitchen/dish/service/impl/DishApplicationServiceImpl.java backend/src/main/java/com/familykitchen/dish/controller/DishController.java backend/src/main/java/com/familykitchen/dish/mapper/DishMapper.java backend/src/main/resources/mapper/dish/DishMapper.xml backend/src/main/java/com/familykitchen/family/mapper/FamilyMapper.java backend/src/main/resources/mapper/family/FamilyMapper.xml backend/src/test/java/com/familykitchen/dish/DishBulkMutationServiceTest.java backend/src/test/java/com/familykitchen/dish/DishBulkMutationControllerRouteTest.java && git commit -m "feat: add merchant dish bulk deletion API"`

### Task 3: Prevent deleted-state resurrection and conflicting writers

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/dish/service/MerchantDishMutationLock.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishReviewServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/DishReviewService.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateChangeRequestServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/mapper/DishReviewMapper.java`
- Modify: `backend/src/main/resources/mapper/dish/DishReviewMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/family/service/impl/MerchantFamilyMenuApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/family/mapper/FamilyMapper.java`
- Modify: `backend/src/main/resources/mapper/family/FamilyMapper.xml`
- Test: `backend/src/test/java/com/familykitchen/dish/DishApplicationServiceUpdateTest.java`
- Test: `backend/src/test/java/com/familykitchen/dish/DishReviewFeaturedStatusTest.java`
- Test: `backend/src/test/java/com/familykitchen/dish/DishTemplateChangeRequestServiceTest.java`
- Test: `backend/src/test/java/com/familykitchen/family/MerchantFamilyFeaturedDishServiceTest.java`
- Test: `backend/src/test/java/com/familykitchen/family/FamilyFeaturedDishResponseTest.java`
- Test: `backend/src/test/java/com/familykitchen/dish/DishBulkMutationServiceTest.java`

- [ ] **Step 1: Add failing deleted-state guard tests**

For merchant/family detail, edit, status, featured, cooking steps, review submission/approval, and template sync, return `BUSINESS_INVALID` after locking/checking a `deleted` dish and verify no mutation occurs.

- [ ] **Step 2: Add failing pending-review closure tests**

Add `DishReviewService.rejectPendingForDeletedDishes(merchantId,dishIds,actorId)` so `DishApplicationServiceImpl` reuses its existing dependency and no new constructor argument is introduced. Batch deletion rejects pending submissions for every validated ID with reason “菜品已删除”, `reviewed_by=actorId`, and `reviewed_at`. Review submission locks merchant+dish before checking pending/snapshot state; approval re-locks and rejects deleted targets.

- [ ] **Step 3: Add failing family-menu locking tests**

Save/copy must lock the merchant before validating/writing full menu drafts, enumerate source and target dish IDs, sort-lock and validate them, then write family-menu rows. Family copy may not rely on blind `copyFamilyMenu` SQL before validation. Add merchant/dish lock methods to the already-injected `FamilyMapper` so the service constructor does not change.

- [ ] **Step 4: Add the delete→restore→activate regression while still RED**

Verify a previously enabled family-menu row is forced disabled by delete; restore then activation leaves it disabled and the family client menu excludes it.

- [ ] **Step 5: Run focused tests and verify RED**

`cd backend; D:\develop\apache-maven-3.9.9\bin\mvn.cmd -Dtest=DishBulkMutationServiceTest,DishApplicationServiceUpdateTest,DishReviewFeaturedStatusTest,DishTemplateChangeRequestServiceTest,MerchantFamilyFeaturedDishServiceTest,FamilyFeaturedDishResponseTest test`

Expected: FAIL on missing deleted-state checks/locks.

- [ ] **Step 6: Implement shared lock guards**

Extend `MerchantDishMutationLock` with merchant-only and sorted-many locking for dish services. In the family-menu service, add equivalent canonical lock queries to its already-injected `FamilyMapper` rather than adding a constructor dependency. Explicitly defer all cart/order mapper and service edits until Task 4 has captured the dirty baseline. Ensure Task 3 dish/family writers validate after locks, add transactional pending-review rejection, and guard updates with `status <> 'deleted'` or `status='deleted'` so audit-field invariants cannot be mixed.

- [ ] **Step 7: Run focused tests and verify GREEN**

Run the Step 5 command. Expected: PASS.

- [ ] **Step 8: Commit**

`git add backend/src/main/java/com/familykitchen/dish/service/DishReviewService.java backend/src/main/java/com/familykitchen/dish/service/MerchantDishMutationLock.java backend/src/main/java/com/familykitchen/dish/service/impl/DishApplicationServiceImpl.java backend/src/main/java/com/familykitchen/dish/service/impl/DishReviewServiceImpl.java backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateChangeRequestServiceImpl.java backend/src/main/java/com/familykitchen/dish/mapper/DishReviewMapper.java backend/src/main/resources/mapper/dish/DishReviewMapper.xml backend/src/main/java/com/familykitchen/family/service/impl/MerchantFamilyMenuApplicationServiceImpl.java backend/src/main/java/com/familykitchen/family/mapper/FamilyMapper.java backend/src/main/resources/mapper/family/FamilyMapper.xml backend/src/test/java/com/familykitchen/dish/DishBulkMutationServiceTest.java backend/src/test/java/com/familykitchen/dish/DishApplicationServiceUpdateTest.java backend/src/test/java/com/familykitchen/dish/DishReviewFeaturedStatusTest.java backend/src/test/java/com/familykitchen/dish/DishTemplateChangeRequestServiceTest.java backend/src/test/java/com/familykitchen/family/MerchantFamilyFeaturedDishServiceTest.java backend/src/test/java/com/familykitchen/family/FamilyFeaturedDishResponseTest.java && git commit -m "fix: guard deleted dishes across merchant writes"`

### Task 4: Make existing carts deterministic and non-submittable

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/cart/model/entity/CartItemEntity.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/model/vo/CartView.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/mapper/CartMapper.java`
- Modify: `backend/src/main/resources/mapper/cart/CartMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/cart/service/impl/CartApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/order/service/impl/FamilyOrderApplicationServiceImpl.java`
- Modify: `backend/src/test/java/com/familykitchen/cart/CartApplicationServiceTest.java`
- Modify: `backend/src/test/java/com/familykitchen/order/FamilyOrderApplicationServiceTest.java`
- Modify: `backend/src/test/java/com/familykitchen/cart/SharedCartConcurrencyMySqlTest.java`
- Modify: `frontend/utils/api-scenes.js`
- Modify: `frontend/pages/ordering/cart/index.js`
- Modify: `frontend/pages/ordering/cart/index.wxml`
- Modify: `frontend/pages/ordering/cart/index.wxss`
- Test: `frontend/tests/api-scenes.test.js`
- Test: `frontend/tests/shared-cart-pages.test.js`

Note: `CartMapper.java`, `CartApplicationServiceImpl.java`, `CartMapper.xml`, and `CartApplicationServiceTest.java` already contain overlapping user-owned uncommitted changes.

- [ ] **Step 1: Capture and protect the dirty-cart baseline**

Before editing, save the exact unstaged diff of the four dirty cart files as a review artifact outside Git staging and record their hashes. Do not revert it. After feature edits, compare against that baseline and inspect `git diff --cached` plus `git diff --cached --name-only`. If a required feature hunk contains inseparable user-owned lines, pause for user direction instead of committing Task 4.

- [ ] **Step 2: Add failing cart-view tests**

Expose `available` and `unavailableReason` on `CartItemView`. Existing cart rows for deleted dishes remain visible from snapshots, can be reduced/removed, and are marked unavailable.

- [ ] **Step 3: Add failing mutation algorithm tests**

Use an initial unlocked snapshot only to discover the target/current selection, then acquire canonical locks and re-read the current member quantity. If `newQuantity < lockedCurrentQuantity` (including zero), allow reduction/removal without current dish availability. If `newQuantity > lockedCurrentQuantity`, require current availability. Equal quantity/note-only updates retain quantity and may update the member remark without availability. Test positive reduction, removal, increase, equal/note-only, and a concurrent quantity change between snapshot and lock.

- [ ] **Step 4: Add failing ordered checkout and race tests**

Refactor checkout explicitly: resolve family→merchant without locks; lock merchant; collect distinct dish IDs and lock dish rows in ascending order; lock corresponding family-menu rows; only then lock cart/items/selections and re-read the checkout snapshot. Do not rely on join optimizer lock order. Add ordered Mockito tests plus MySQL delete-vs-add and delete-vs-checkout races. Assert either the cart/order commits before deletion or deletion commits first and mutation/submission is rejected; never allow success after committed deletion.

- [ ] **Step 5: Run focused backend tests and verify RED**

`cd backend; D:\develop\apache-maven-3.9.9\bin\mvn.cmd -Dtest=CartApplicationServiceTest,FamilyOrderApplicationServiceTest,SharedCartConcurrencyMySqlTest test`

Expected: FAIL until availability is exposed and lock order is unified.

- [ ] **Step 6: Implement availability and locks**

Add lock queries to the already-injected `CartMapper` so constructors do not change. Implement the exact reduction comparison and checkout order from steps 3–4. Cart reads left-join current dish/menu availability but retain snapshot rows.

- [ ] **Step 7: Add failing frontend scene/markup test**

Require “菜品已删除，不可提交” on unavailable rows and disable only increment/add actions; decrement/remove remain active.

- [ ] **Step 8: Implement cart warning UI**

Map availability fields in `api-scenes.js`, render a warm warning badge, and add handler-side increment guards in `pages/ordering/cart/index.js`; decrement/remove remain available.

- [ ] **Step 9: Run focused backend/frontend tests and verify GREEN**

`cd backend; D:\develop\apache-maven-3.9.9\bin\mvn.cmd -Dtest=CartApplicationServiceTest,FamilyOrderApplicationServiceTest,SharedCartConcurrencyMySqlTest test`

`cd frontend; node --test tests/api-scenes.test.js tests/shared-cart-pages.test.js`

Expected: PASS.

- [ ] **Step 10: Commit only intended cart hunks**

Review the feature delta against the captured baseline. Stage exact files/hunks only, inspect cached content, preserve all pre-existing user changes, and do not commit if ownership cannot be separated safely.

### Task 5: Add Mini Program service contracts

**Files:**
- Modify: `frontend/services/merchant.js`
- Modify: `frontend/tests/services.test.js`
- Modify: `frontend/scripts/generate-delivery-service-contracts.js`
- Modify: `frontend/tests/fixtures/delivery-service-contracts.json`
- Modify: `frontend/tests/delivery-api-contract.test.js`
- Modify: `backend/src/test/resources/contracts/mini-program-api-contract.json`
- Modify: `docs/api-spec.md`

- [ ] **Step 1: Add failing service tests**

Require `getDishes({ scope: 'deleted' })`, `batchDeleteDishes({ dishIds })`, and `batchRestoreDishes({ dishIds })` to use the exact query/body/routes.

- [ ] **Step 2: Run service tests and verify RED**

`cd frontend; node --test tests/services.test.js tests/delivery-api-contract.test.js`

Expected: FAIL because operations are absent from service and fixtures.

- [ ] **Step 3: Implement service functions and regenerate fixtures**

Encode query values and call the two POST endpoints. Extend `scripts/generate-delivery-service-contracts.js` with a deterministic `getDishes({scope:'deleted'})` sample plus both batch service operations, then run the generator for service contracts; do not declare merchant-page operations before Task 6 page source calls them, and never hand-edit generated JSON. Document request/response/scope/error behavior in `docs/api-spec.md`.

Run:

`cd frontend; node scripts/generate-delivery-service-contracts.js`

- [ ] **Step 4: Run service tests and verify GREEN**

Run step 2 command. Expected: PASS.

- [ ] **Step 5: Commit**

`git add frontend/services/merchant.js frontend/tests/services.test.js frontend/scripts/generate-delivery-service-contracts.js frontend/tests/fixtures/delivery-service-contracts.json frontend/tests/delivery-api-contract.test.js backend/src/test/resources/contracts/mini-program-api-contract.json docs/api-spec.md && git commit -m "docs: publish dish recycle bin API"`

### Task 6: Implement merchant selection, delete confirmation, and recycle bin

**Files:**
- Create: `frontend/tests/merchant-dish-bulk-delete.test.js`
- Modify: `frontend/pages/merchant/merchant-dishes/index.js`
- Modify: `frontend/pages/merchant/merchant-dishes/index.wxml`
- Modify: `frontend/pages/merchant/merchant-dishes/index.wxss`
- Modify: `frontend/tests/merchant-page-family-contract.test.js`
- Modify: `frontend/tests/merchant-responsive-contract.test.js`
- Modify: `frontend/tests/native-button-free-contract.test.js`
- Modify: `frontend/scripts/generate-delivery-service-contracts.js`
- Modify: `frontend/tests/fixtures/delivery-page-operations.json`
- Modify: `frontend/tests/fixtures/delivery-service-contracts.json`
- Modify: `frontend/tests/delivery-audit-inventory.test.js`
- Modify: `backend/src/test/resources/contracts/mini-program-api-contract.json`

- [ ] **Step 1: Add failing selection/scope tests**

Test per-row selection, all visible filtered rows, selection clearing on search/status/scope/reload, and ordinary vs recycle-bin loading.

- [ ] **Step 2: Add failing delete/restore action tests**

Assert delete confirmation contains count and impact text; cancel sends no request. Valid delete/restore sends one request with unique selected IDs, reloads on success, keeps list/selection on failure, and blocks empty/>100 selections.

- [ ] **Step 3: Add failing page-wide lock tests**

Hold delete on a deferred Promise; verify restore, status, featured, template sync, and edit handlers issue no second write/navigation while busy.

- [ ] **Step 4: Add failing markup/accessibility/responsive tests**

Require custom `view` checkboxes with checkbox ARIA, ≥88rpx touch area, ordinary/recycle filters, wrapping toolbar, deletion operator/time, and no native `button`.

- [ ] **Step 5: Run focused tests and verify RED**

`cd frontend; node --test tests/merchant-dish-bulk-delete.test.js tests/merchant-page-family-contract.test.js tests/merchant-responsive-contract.test.js tests/native-button-free-contract.test.js`

Expected: FAIL before implementation.

- [ ] **Step 6: Implement page logic**

Add `scope`, selection map/count/all-selected, and one `mutationBusy` guard. Load `available` or `deleted`; use non-optimistic delete/restore; clear selection only on accepted reload/filter/search/scope changes or successful mutation.

- [ ] **Step 7: Implement WXML/WXSS**

Add accessible custom checkbox controls, bulk toolbar, recycle-bin filter, confirmation flow, deletion audit text, warm-theme disabled/busy states, and narrow-screen wrapping.

- [ ] **Step 8: Run focused tests and verify GREEN**

Run `cd frontend; node scripts/generate-delivery-service-contracts.js` after page source includes the new calls, then run:

`cd frontend; node --test tests/merchant-dish-bulk-delete.test.js tests/merchant-page-family-contract.test.js tests/merchant-responsive-contract.test.js tests/native-button-free-contract.test.js tests/delivery-audit-inventory.test.js tests/delivery-api-contract.test.js`

Expected: PASS and generated page-operation inventory matches source.

- [ ] **Step 9: Commit**

`git add frontend/pages/merchant/merchant-dishes/index.js frontend/pages/merchant/merchant-dishes/index.wxml frontend/pages/merchant/merchant-dishes/index.wxss frontend/scripts/generate-delivery-service-contracts.js frontend/tests/fixtures/delivery-page-operations.json frontend/tests/fixtures/delivery-service-contracts.json frontend/tests/merchant-dish-bulk-delete.test.js frontend/tests/merchant-page-family-contract.test.js frontend/tests/merchant-responsive-contract.test.js frontend/tests/native-button-free-contract.test.js frontend/tests/delivery-audit-inventory.test.js frontend/tests/delivery-api-contract.test.js backend/src/test/resources/contracts/mini-program-api-contract.json && git commit -m "feat: add merchant dish recycle bin"`

### Task 7: Full verification and delivery review

**Files:**
- Review: all files changed by Tasks 1–6

- [ ] **Step 1: Run backend suite**

`cd backend; D:\develop\apache-maven-3.9.9\bin\mvn.cmd test`

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run frontend suite**

`cd frontend; npm test`

Expected: zero failures.

- [ ] **Step 3: Run admin build regression**

`cd admin-web; npm run build`

Expected: successful production bundle.

- [ ] **Step 4: Inspect database safety**

Confirm only V12 was added, no existing migration changed, and no command connected to the configured business database.

- [ ] **Step 5: Inspect working-tree ownership**

`git status --short` and `git diff --stat`; verify pre-existing cart changes and untracked `AGENTS.md` were not overwritten or accidentally staged.

- [ ] **Step 6: Request code review**

Use `superpowers:requesting-code-review` against both specs and this plan. Fix P0/P1 findings and rerun affected tests.

- [ ] **Step 7: Run final fresh verification**

Use `superpowers:verification-before-completion`, rerun the commands from steps 1–3, and report exact results.
