# Merchant Multi-Featured Dishes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow a merchant to recommend up to five globally featured dishes, auto-enable them for every served family, pin them in merchant and ordering lists, and render them as a responsive family-home carousel.

**Architecture:** Store recommendation state as `dishes.featured_at datetime(6)` and serialize all recommendation/status writes with a merchant lock followed by a dish-row lock. Reuse existing dish and family menu services, keep the V9 family recommendation route as a deprecated adapter, and evolve the home response with `featuredDishes` while retaining `featuredDish` as the first-item compatibility field.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis XML, MySQL 8/Flyway, JUnit 5/Mockito, WeChat Mini Program WXML/WXSS/CommonJS, Node.js built-in test runner.

---

## File Map

### Backend data and contracts

- Create `backend/src/main/resources/db/migration/V10__add_merchant_featured_dishes.sql`: add `featured_at`, index it, deterministically migrate V9 recommendations, and enable migrated dishes for active merchant families.
- Modify `backend/src/main/java/com/familykitchen/dish/model/entity/DishEntity.java`: carry `featuredAt`.
- Modify `backend/src/main/java/com/familykitchen/dish/model/vo/DishView.java`: expose `featured` and `featuredAt`.
- Create `backend/src/main/java/com/familykitchen/dish/model/dto/DishFeaturedRequest.java`: validated boolean request.
- Create `backend/src/main/java/com/familykitchen/dish/service/MerchantDishMutationLock.java`: shared merchant-then-dish row-lock coordinator used by direct mutations and review approval.
- Modify `backend/src/main/java/com/familykitchen/dish/mapper/DishMapper.java` and `backend/src/main/resources/mapper/dish/DishMapper.xml`: recommendation queries, merchant/dish locks, deterministic list ordering, and inactive cleanup.
- Modify `backend/src/main/java/com/familykitchen/family/mapper/FamilyMapper.java` and `backend/src/main/resources/mapper/family/FamilyMapper.xml`: bulk enable family menus, featured ordering, and multi-item home query.

### Backend services and routes

- Modify `backend/src/main/java/com/familykitchen/dish/service/DishApplicationService.java` and `backend/src/main/java/com/familykitchen/dish/service/impl/DishApplicationServiceImpl.java`: global recommend/cancel transaction and unified lock ordering.
- Modify `backend/src/main/java/com/familykitchen/dish/controller/DishController.java`: `PUT /merchant/dishes/{dishId}/featured`.
- Modify `backend/src/main/java/com/familykitchen/dish/service/impl/DishReviewServiceImpl.java`: clear recommendation when an approved review makes a dish inactive, using the same lock order.
- Modify `backend/src/main/java/com/familykitchen/family/service/impl/MerchantFamilyMenuApplicationServiceImpl.java` and `backend/src/main/java/com/familykitchen/family/controller/MerchantFamilyMenuController.java`: adapt and deprecate the V9 route.
- Modify `backend/src/main/java/com/familykitchen/family/model/vo/FamilyHomeResponse.java`, `backend/src/main/java/com/familykitchen/family/service/impl/FamilyApplicationServiceImpl.java`: return carousel items plus compatibility first item.
- Modify `backend/src/main/java/com/familykitchen/family/model/vo/FamilyMenuItemView.java`: carry merchant recommendation state and `featuredAt` into family ordering `DishView`.

### Mini-program

- Modify `frontend/services/merchant.js`: recommendation request.
- Modify `frontend/utils/merchant-scenes.js`: featured fields, stable pinned ordering, full-list featured count.
- Modify `frontend/pages/merchant/merchant-dishes/index.js`, `.wxml`, `.wxss`: custom recommendation control above status, shared row lock, preserved filters, responsive card actions.
- Modify `frontend/utils/api-scenes.js`: `featuredDishes` carousel view model and menu featured flags.
- Modify `frontend/pages/family/home/index.js`, `.wxml`, `.wxss`: one/many carousel behavior.
- Modify `frontend/pages/ordering/menu/index.wxml`, `.wxss`: recommendation label; ordering comes from backend/scene sorting.
- Modify `frontend/pages/merchant/family-menu/index.js`, `.wxml`, `.wxss`: remove the superseded per-family recommendation control and source method.

### Tests and docs

- Create `backend/src/test/java/com/familykitchen/database/MerchantFeaturedDishMigrationContractTest.java`.
- Create `backend/src/test/java/com/familykitchen/database/MerchantFeaturedDishMigrationMySqlTest.java`.
- Create `backend/src/test/java/com/familykitchen/dish/MerchantFeaturedDishServiceTest.java`.
- Create `backend/src/test/java/com/familykitchen/dish/DishReviewFeaturedStatusTest.java`.
- Create `backend/src/test/java/com/familykitchen/dish/MerchantFeaturedDishConcurrencyMySqlTest.java`.
- Create `backend/src/test/java/com/familykitchen/family/FamilyFeaturedDishResponseTest.java`.
- Create `backend/src/test/java/com/familykitchen/dish/DishViewSerializationTest.java`.
- Modify `backend/src/test/java/com/familykitchen/dish/DishApplicationServiceUpdateTest.java`, `backend/src/test/java/com/familykitchen/family/MerchantFamilyControllerRouteTest.java`, `backend/src/test/java/com/familykitchen/family/MerchantFamilyFeaturedDishServiceTest.java`, `backend/src/test/java/com/familykitchen/database/LogicalReferenceValidationTest.java`, `backend/src/test/java/com/familykitchen/database/FamilyFeaturedDishMapperContractTest.java`, `backend/src/test/java/com/familykitchen/database/FamilyMenuMapperContractTest.java`, `backend/src/test/java/com/familykitchen/database/FreshDatabaseMigrationTest.java`.
- Modify `frontend/tests/services.test.js`, `frontend/tests/api-scenes.test.js`, `frontend/tests/merchant-page-family-contract.test.js`, `frontend/tests/merchant-responsive-contract.test.js`, `frontend/tests/warm-animal-visual-contract.test.js`.
- Modify `docs/frontend-api-guide.md` and `docs/verification/2026-08-10-merchant-workspace-verification.md`.

## Task 1: V10 schema, entity, and deterministic query contracts

**Files:** backend migration/entity/VO/Mapper files and migration/mapper tests listed above.

- [ ] **Step 1: Write RED migration and Mapper tests**

Assert V10 follows V9, uses `datetime(6)`, adds `(merchant_id, featured_at)`, migrates at most five distinct active owned legacy dishes per merchant by family-reference count then dish ID, and enables migrated dishes for all active families without overwriting `final_price` or existing `sort_order`. Assert dish/ordering/home SQL uses `featured_at DESC, d.id DESC` and filters active dishes. Add `MerchantFeaturedDishMigrationMySqlTest` using the repo's Testcontainers pattern with `disabledWithoutDocker=true`: migrate through V9, seed two merchants, more than five legacy recommendations, inactive/cross-owned rows, existing custom prices/orders, missing menu rows, and inactive families; apply V10 and verify the exact postconditions against MySQL 8.

- [ ] **Step 2: Run RED tests**

From `backend/`:

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -s .maven-settings.xml '-Dtest=MerchantFeaturedDishMigrationContractTest,MerchantFeaturedDishMigrationMySqlTest,FreshDatabaseMigrationTest,FamilyFeaturedDishMapperContractTest,FamilyMenuMapperContractTest' test
```

Expected: FAIL because V10 and multi-feature fields/queries do not exist.

- [ ] **Step 3: Implement V10 and data contracts**

Add `featured_at datetime(6) null`, index it, deterministic bounded legacy migration, and `family_menu_items` upsert that updates only `enabled`. For missing rows assign deterministic tail positions per family and use the base price; inactive families remain untouched. Extend `DishEntity` and `DishView` with `LocalDateTime featuredAt` and `boolean featured`; extend `FamilyMenuItemView` with `LocalDateTime featuredAt` and reinterpret `featured` as the active merchant recommendation flag. Change all relevant MyBatis select columns and ordering with ID fallback. Replace `selectFeaturedDish` with `selectFeaturedDishes(familyId, limit)` while retaining a deterministic non-featured one-item fallback query.

- [ ] **Step 4: Run GREEN tests and documentation coverage**

From `backend/` run:

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -s .maven-settings.xml '-Dtest=MerchantFeaturedDishMigrationContractTest,MerchantFeaturedDishMigrationMySqlTest,FreshDatabaseMigrationTest,FamilyFeaturedDishMapperContractTest,FamilyMenuMapperContractTest,DocumentationCoverageTest' test
```

Expected: all pass (the MySQL test may be explicitly skipped only when Docker is unavailable).

- [ ] **Step 5: Commit exact files**

Stage only V10, the named entity/VO/Mapper files, and named database tests. Commit `feat: add merchant featured dish persistence`.

## Task 2: Recommendation service, locks, auto-enable, and route compatibility

**Files:** dish DTO/service/controller, family adapter service/controller, DishReview service, and focused tests.

- [ ] **Step 1: Write RED service and route tests**

Cover:

- exact merchant context passed by the new route; no backend access or null merchant ID returns `FORBIDDEN` without service invocation; `{}` and `{"featured":null}` return `BAD_REQUEST` because `DishFeaturedRequest` uses `@NotNull Boolean featured`;
- lock order is merchant then target dish, followed by active/ownership revalidation;
- first through fifth recommendations succeed, sixth fails; duplicate recommendation is idempotent and keeps timestamp;
- cancel clears timestamp only; two consecutive `featured=false` requests both succeed, leave `featured_at=NULL`, and never count recommendations or touch family menus;
- the count query includes only `featured_at IS NOT NULL AND status='active'`, so a stale inactive row cannot consume one of five slots;
- auto-enable updates existing rows without price/order writes and inserts missing rows at each family's tail;
- `updateDishStatus`, direct full update, and approved review to inactive clear recommendation in the same transaction;
- Mockito `InOrder` proves merchant lock then dish `FOR UPDATE`, locked-state revalidation, then atomic inactive write/featured cleanup for review-disabled full `updateDish`, `updateDishStatus`, and existing-dish `DishReviewServiceImpl.approve`;
- `MerchantFeaturedDishConcurrencyMySqlTest` uses real MySQL transactions and barriers to prove six simultaneous recommendation attempts never commit more than five, and recommend-versus-inactive update never ends with `status=inactive` and non-null `featured_at`;
- old family route validates family ownership, calls the global recommend service, never updates `families.featured_dish_id`, and carries an OpenAPI deprecated marker.
- existing `MerchantFamilyFeaturedDishServiceTest` and `LogicalReferenceValidationTest` constructor fixtures compile against the adapter's new dependency and assert legacy family-column writes are gone.

- [ ] **Step 2: Run RED tests**

From `backend/`:

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -s .maven-settings.xml '-Dtest=MerchantFeaturedDishServiceTest,MerchantFeaturedDishConcurrencyMySqlTest,DishApplicationServiceUpdateTest,DishReviewFeaturedStatusTest,MerchantFamilyControllerRouteTest,MerchantFamilyFeaturedDishServiceTest,LogicalReferenceValidationTest' test
```

Expected: FAIL on missing method/route/locking behavior.

- [ ] **Step 3: Implement the service with one locking boundary**

Add `setFeaturedDish(CurrentUserContext, dishId, featured)`. Create injectable `MerchantDishMutationLock` backed by `DishMapper`; its single operation calls `lockMerchant(merchantId)` then `selectDishForUpdate(merchantId,dishId)` and returns the locked entity or throws `NOT_FOUND`. Both `DishApplicationServiceImpl` and `DishReviewServiceImpl` must use this collaborator before actual dish-state writes, then revalidate the locked state inside their transaction. The recommendation path counts after locking, updates a microsecond timestamp, and bulk enables families. Do not trust frontend counts.

- [ ] **Step 4: Implement both routes**

Add the new body `{ featured }` route with boxed `@NotNull Boolean`. Adapt the V9 family route to validate the family then call global recommendation with `true`; annotate it deprecated and remove its old family-column write. Update constructor-based tests named in Step 1.

- [ ] **Step 5: Run GREEN tests**

Run the Step 2 command. Expected: all focused tests pass.

- [ ] **Step 6: Commit exact files**

Stage only named DTO/service/controller/Mapper changes and focused tests. Commit `feat: add merchant featured dish workflow`.

## Task 3: Multi-item home response and pinned family ordering

**Files:** `FamilyHomeResponse`, `FamilyMenuItemView`, `FamilyApplicationServiceImpl`, family Mapper/XML, `FamilyFeaturedDishResponseTest`, `DishViewSerializationTest`, `FamilyFeaturedDishMapperContractTest`, and `FamilyMenuMapperContractTest`.

- [ ] **Step 1: Write RED response and ordering tests**

Create `FamilyFeaturedDishResponseTest` to assert home returns 0–5 `featuredDishes` ordered by `featured_at DESC, dish_id DESC`; `featuredDish` equals the first item; no recommended items falls back to one valid menu dish; no valid dishes returns `[]` and `null`; and only one list query plus at most one fallback query executes. Create `DishViewSerializationTest` with Jackson and springdoc/route reflection assertions for `featured` and `featuredAt`. Update both Mapper contract tests so `FamilyMenuItemView` selects `d.featured_at AS featuredAt`, maps effective merchant recommendation, and `FamilyApplicationServiceImpl.menuItems()` passes both fields into `DishView`.

- [ ] **Step 2: Run RED tests**

From `backend/`:

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -s .maven-settings.xml '-Dtest=FamilyFeaturedDishResponseTest,DishViewSerializationTest,FamilyFeaturedDishMapperContractTest,FamilyMenuMapperContractTest' test
```

Expected: response-constructor, serialization, and query-contract failures.

- [ ] **Step 3: Implement response composition**

Construct the list once, cap at five in SQL, and derive compatibility field with `featuredDishes.isEmpty() ? null : featuredDishes.get(0)`. Do not execute one query per carousel item.

- [ ] **Step 4: Run GREEN tests**

Run the exact Step 2 command; expect all pass.

- [ ] **Step 5: Commit exact files**

Commit `feat: return featured dish carousel data` with only response/service/query/tests.

## Task 4: Merchant dish card recommendation control

**Files:** merchant service/scene/page files and frontend service/page/responsive tests.

- [ ] **Step 1: Write RED frontend behavior and contract tests**

Assert service sends exact `PUT /api/merchant/dishes/{id}/featured` with `{featured}`. Scene sorts featured first with timestamp and ID fallback, computes `featuredCount` from the unfiltered full list, and exposes `recommendLabel` (`推荐`, `已推荐`, `推荐已满`). WXML must place recommendation `<view>` before status `<view>`, expose aria role/label/disabled/busy, hover feedback, and 88rpx minimum. Behavior tests prove recommendation and status share `busyDishMap`, duplicate taps issue one request, success silently reloads while preserving query/filter, and failure releases only the row.

- [ ] **Step 2: Run RED tests**

From `frontend/`:

```powershell
node --test tests/services.test.js tests/api-scenes.test.js tests/merchant-page-family-contract.test.js tests/merchant-responsive-contract.test.js tests/native-button-free-contract.test.js
```

Expected: new assertions fail.

- [ ] **Step 3: Implement service, scene, and page**

Keep full `dishRows` as the count source. Add the custom recommendation control directly above the status control. For an inactive dish, recommendation is disabled; at count five only non-featured rows show `推荐已满`; featured rows can always cancel. Reuse the same row busy map for both mutations.

- [ ] **Step 4: Run GREEN and compiler contracts**

Run Step 2 plus:

```powershell
node --test tests/wxml-expression-contract.test.js tests/wxss-compiler-compat.test.js tests/responsive-warm-animal-contract.test.js
```

Expected: all pass.

- [ ] **Step 5: Commit exact files**

Commit `feat: add merchant dish recommendation controls`.

## Task 5: Ordering pin/tag, home swiper, and removal of old family control

**Files:** API scene, family home, ordering menu, merchant family-menu, and visual/behavior tests.

- [ ] **Step 1: Write RED carousel and ordering tests**

Assert `buildApiHomeScene` maps `featuredDishes` and falls back from old `featuredDish`. For one item the scene/WXML must set autoplay, circular, and indicator dots false and `nextMargin=0`; for multiple items all three booleans are true, interval is about 4000ms, and `nextMargin` is bounded and nonzero. WXML binds each item ID to detail navigation and avoids fixed recommendation copy. WXML/WXSS must use a non-overflowing card/slide width so the next card is visibly but slightly exposed on normal phones; the 320px media rule uses a smaller safe margin. Ordering rows display `主厨推荐` and remain first. Family-menu page contains no `setFeaturedDish` method, old service call, or recommendation control.

- [ ] **Step 2: Run RED tests**

From `frontend/`:

```powershell
node --test tests/api-scenes.test.js tests/merchant-page-family-contract.test.js tests/warm-animal-visual-contract.test.js tests/merchant-responsive-contract.test.js
```

Expected: carousel/tag/removal assertions fail.

- [ ] **Step 3: Implement carousel and list presentation**

Use `swiper` only as the carousel container; card taps remain custom views. One item uses `next-margin="0"` with all motion/indicators off. Multiple items use about 4000ms, circular/indicator dots on, and responsive nonzero `next-margin`/slide sizing for the approved peek affordance without page-level overflow; 320px uses the smaller tested margin. Keep recommendation label restrained. Remove V9 family-page recommendation UI and code.

- [ ] **Step 4: Run GREEN and full frontend regression**

Run Step 2, then `npm test` from `frontend/`. Expected: all tests pass.

- [ ] **Step 5: Commit exact files**

Commit `feat: show featured dish carousel and pinned menus`.

## Task 6: Documentation, full verification, migration, and runtime smoke test

**Files:** API guide and verification record only, after code tests pass.

- [ ] **Step 1: Update API documentation**

Document new route/body/errors, `DishView.featured/featuredAt`, `FamilyHomeResponse.featuredDishes`, old route deprecation, max-five rule, automatic family enablement, pinned ordering, and downstatus cleanup.

- [ ] **Step 2: Run complete automated verification**

From `backend/`:

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -s .maven-settings.xml test
```

From `frontend/`:

```powershell
npm test
node --check pages/merchant/merchant-dishes/index.js
node --check pages/family/home/index.js
node --check utils/api-scenes.js
node --check utils/merchant-scenes.js
```

Expected: both complete suites and all syntax checks pass.

- [ ] **Step 3: Inspect runtime ownership and apply V10**

Resolve the process listening on 8080. Stop it only if its executable command line contains this workspace's `backend/target/classes` and `com.familykitchen.FamilyKitchenApplication`. Start the current project hidden, confirm Flyway reports V10 and OpenAPI exposes the new route. If MySQL access is sandbox-blocked, request escalation; do not change DB credentials.

- [ ] **Step 4: Run read-only runtime smoke tests**

Verify Flyway V10, OpenAPI route/body schema, an unauthenticated 401, and authenticated GET responses only. Do not run a mutating recommendation smoke test against the shared development database: cancellation intentionally does not undo auto-enabled or inserted `family_menu_items`, and this repository has no documented cascade-safe disposable cleanup workflow. Mutation correctness is verified by the isolated Testcontainers migration/concurrency/service suites.

- [ ] **Step 5: Record actual evidence**

Append exact test totals, V10 result, route presence, response examples, and any skipped runtime checks to the verification record.

- [ ] **Step 6: Commit docs only**

Stage only `docs/frontend-api-guide.md` and the verification record. Commit `docs: document merchant featured dishes`.
