# Imported Dish Template Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow a merchant to submit the current data of an imported dish as a reviewed change request for its source platform template.

**Architecture:** Add one server-side submission endpoint under the existing merchant dish resource. The backend loads the owned dish and its source template, merges merchant-owned fields with platform-owned fields into the existing schema-v1 snapshot, and delegates to the current template change request persistence and review workflow. Mini-program and PC clients only send an optional note and never assemble privileged template fields.

**Tech Stack:** Java 21, Spring Boot 3, MyBatis/MyBatis-Plus, MySQL, JUnit 5/Mockito, WeChat Mini Program JavaScript/WXML/WXSS, Vue 3/Vite, Node test runner.

---

### Task 1: Backend Snapshot Generation Contract

**Files:**
- Create: `backend/src/main/java/com/familykitchen/dish/model/dto/ImportedDishTemplateSyncRequest.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/DishTemplateChangeRequestService.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateChangeRequestServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/mapper/DishMapper.java`
- Modify: `backend/src/main/resources/mapper/dish/DishMapper.xml`
- Test: `backend/src/test/java/com/familykitchen/dish/DishTemplateChangeRequestServiceTest.java`

- [ ] **Step 1: Write the failing happy-path service test**

Create an imported `DishEntity` with `sourceTemplateId`, current merchant fields, current ingredients, merchant dictionary categories, and a source template. Call the wished-for `submitFromImportedDish(user, dishId, request)` and capture the inserted request JSON. Assert merchant fields replace `name`, `description`, `imageUrl`, `referencePrice`, and ingredients while template category, image attribution, tags, meals, sort and enabled remain unchanged.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `mvn -f backend/pom.xml -Dtest=DishTemplateChangeRequestServiceTest test`

Expected: compilation failure because `submitFromImportedDish` and the request DTO do not exist.

- [ ] **Step 3: Write failing validation and fallback tests**

Cover a manually-created dish, an unowned/missing dish, a missing source template, an empty ingredient list, and ingredient category resolution in this order: merchant dictionary category, source template same-name category, then `其他`.

- [ ] **Step 4: Implement the minimal service method**

Add:

```java
DishTemplateChangeSubmitView submitFromImportedDish(
    CurrentUserContext user, Long dishId, ImportedDishTemplateSyncRequest request);
```

The implementation must require merchant backend access, query `DishMapper.selectDish(user.merchantId(), dishId)`, reject a null `sourceTemplateId`, read current dish ingredients and dictionary entries, build a strict `DishTemplateSnapshotRequest`, and route creation through one shared transactional submission helper so duplicate checks, locking, base snapshot capture and persistence stay identical to manual template edits.

- [ ] **Step 5: Run the focused service test and verify GREEN**

Run: `mvn -f backend/pom.xml -Dtest=DishTemplateChangeRequestServiceTest test`

Expected: all service tests pass.

### Task 2: Backend HTTP Contract and Dish Source Metadata

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/dish/controller/DishTemplateChangeRequestController.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/vo/DishView.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/model/vo/DishDetailView.java`
- Modify: `backend/src/main/java/com/familykitchen/dish/service/impl/DishApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/family/service/impl/FamilyApplicationServiceImpl.java`
- Test: `backend/src/test/java/com/familykitchen/dish/DishTemplateChangeControllerRouteTest.java`
- Test: existing dish service tests that construct `DishView` or `DishDetailView`

- [ ] **Step 1: Write the failing controller/API contract test**

Assert `POST /merchant/dishes/{dishId}/template-change-requests` accepts `{ "submitNote": "..." }`, delegates using the authenticated user, and returns `DishTemplateChangeSubmitView` through `ApiResponse`.

- [ ] **Step 2: Run the focused test and verify RED**

Expected: endpoint or constructor contract missing.

- [ ] **Step 3: Implement springdoc-compliant endpoint and response metadata**

Add `sourceTemplateId` and `templateImported` to merchant dish list/detail views. Preserve family-facing construction with null/false source metadata. Do not add source linkage to `DishRequest`.

- [ ] **Step 4: Run dish controller and service tests**

Run: `mvn -f backend/pom.xml -Dtest='Dish*Test' test`

Expected: pass.

### Task 3: Mini-Program API and Interaction

**Files:**
- Modify: `frontend/services/merchant.js`
- Modify: `frontend/utils/dish-list.js`
- Modify: `frontend/pages/merchant/merchant-dishes/index.js`
- Modify: `frontend/pages/merchant/merchant-dishes/index.wxml`
- Modify: `frontend/pages/merchant/merchant-dishes/index.wxss`
- Test: `frontend/tests/dish-template-change-requests.test.js`
- Test: `frontend/tests/dish-list.test.js`

- [ ] **Step 1: Write failing API and visibility tests**

Assert `submitImportedDishTemplateChange(dishId, body)` posts to `/api/merchant/dishes/{dishId}/template-change-requests`; assert list adaptation exposes an imported marker only when the real API returns `sourceTemplateId` or `templateImported`.

- [ ] **Step 2: Run focused Node tests and verify RED**

Run: `node --test frontend/tests/dish-template-change-requests.test.js frontend/tests/dish-list.test.js`

- [ ] **Step 3: Implement API and page action**

Show `同步模板` only for imported dishes. Confirm that submission enters platform review, collect an optional note using the existing dialog pattern available to the mini-program, guard duplicate taps, call the real endpoint, and navigate to `/pages/merchant/dish-template-change-detail/index?id={requestId}` on success.

- [ ] **Step 4: Run focused and full mini-program tests**

Run: `npm --prefix frontend test`

Expected: all tests pass with no mock production data added.

### Task 4: PC Merchant API and Interaction

**Files:**
- Modify: `admin-web/src/api/dishes.js`
- Modify: `admin-web/src/views/merchant/DishesView.vue`
- Test: `admin-web/tests/dish-template-change-requests.test.js`

- [ ] **Step 1: Write failing API contract and source-marker tests**

Assert the API module posts the optional note to the dedicated dish endpoint and no template snapshot is passed from the UI.

- [ ] **Step 2: Run focused PC tests and verify RED**

Run: `node --test admin-web/tests/dish-template-change-requests.test.js`

- [ ] **Step 3: Implement table/editor actions**

Add `同步模板` beside imported rows and in the editor action area when the selected detail is imported. Use the existing prompt/confirm utility for the optional note, disable while submitting, then route to the existing merchant change request detail/list using the returned request ID.

- [ ] **Step 4: Run PC tests and production build**

Run: `npm --prefix admin-web test`

Run: `npm --prefix admin-web run build`

Expected: all tests and Vite build pass.

### Task 5: Documentation and End-to-End Verification

**Files:**
- Modify: `docs/api-spec.md`
- Modify: `docs/frontend-api-guide.md`
- Modify: `docs/superpowers/plans/2026-08-15-imported-dish-sync-template.md`

- [ ] **Step 1: Document the endpoint and response metadata**

Add request/response examples, field preservation rules, permissions, exact errors, and mini/PC integration guidance. Explicitly state that cooking steps are excluded and existing merchant copies are not updated after approval.

- [ ] **Step 2: Run backend regression**

Run: `mvn -f backend/pom.xml test`

- [ ] **Step 3: Run both frontend regressions and PC build**

Run: `npm --prefix frontend test`

Run: `npm --prefix admin-web test`

Run: `npm --prefix admin-web run build`

- [ ] **Step 4: Verify UTF-8 and diff hygiene**

Run strict UTF-8 decoding over changed text files and `git diff --check`. Confirm no mock data, unrelated cleanup, database migration, or source-template edit field was introduced.

- [ ] **Step 5: Record verification evidence**

Update this plan's checkboxes and report exact test/build results, including any environment-limited checks.
