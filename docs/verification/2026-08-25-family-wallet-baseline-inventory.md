# Family wallet predecessor baseline inventory

Date: 2026-08-25

## Baseline identity

- Branch: `dev`
- Pre-checkpoint HEAD: `1b950c34d66f16b1f7dca0fd0b6fac5a7e682672`
- Initial dirty state: 26 tracked modifications and 75 untracked files (101 paths total).
- Initial tracked diff: 1,182 insertions and 182 deletions across 26 files.
- Disposition: all 101 paths belong to previously requested family management, merchant profile, featured dish, dish-template review, documentation, or associated test work and are included in the checkpoint.
- Secret/temp/generated scan: no `.env`, key/certificate, log, temp, dependency, coverage, build, `target`, or `dist` path was present in the untracked inventory.
- Checkpoint SHA: reported in the Task 0 handoff because a Git commit cannot contain its own SHA.

## Tracked modifications captured before staging

### Backend family management

- `backend/src/main/java/com/familykitchen/family/controller/FamilyController.java`
- `backend/src/main/java/com/familykitchen/family/mapper/FamilyMapper.java`
- `backend/src/main/java/com/familykitchen/family/mapper/FamilyWorkflowMapper.java`
- `backend/src/main/java/com/familykitchen/family/model/dto/OwnerTransferRequest.java`
- `backend/src/main/java/com/familykitchen/family/model/dto/UpdateFamilyInfoRequest.java`
- `backend/src/main/java/com/familykitchen/family/service/FamilyApplicationService.java`
- `backend/src/main/java/com/familykitchen/family/service/FamilyMemberApplicationService.java`
- `backend/src/main/java/com/familykitchen/family/service/impl/FamilyApplicationServiceImpl.java`
- `backend/src/main/java/com/familykitchen/family/service/impl/FamilyMemberApplicationServiceImpl.java`
- `backend/src/main/resources/mapper/family/FamilyMapper.xml`

### Mini-program family and merchant UI/services

- `frontend/app.json`
- `frontend/components/quantity-stepper/index.wxss`
- `frontend/pages/family/family-management/index.js`
- `frontend/pages/family/family-management/index.wxml`
- `frontend/pages/family/family-management/index.wxss`
- `frontend/pages/merchant/index.js`
- `frontend/pages/merchant/index.wxml`
- `frontend/pages/merchant/index.wxss`
- `frontend/pages/merchant/merchant-family-detail/index.js`
- `frontend/pages/merchant/merchant-family-detail/index.wxml`
- `frontend/services/family.js`
- `frontend/services/merchant.js`
- `frontend/tests/merchant-page-family-contract.test.js`
- `frontend/tests/services.test.js`
- `frontend/tests/warm-animal-full-rebuild-contract.test.js`
- `frontend/utils/merchant-scenes.js`

## Untracked predecessor files captured before staging

### Admin web dish-template review

- `admin-web/src/api/dish-template-changes.js`
- `admin-web/src/components/SnapshotComparison.vue`
- `admin-web/src/utils/dish-template-changes.js`
- `admin-web/src/views/merchant/DishTemplateChangesView.vue`
- `admin-web/src/views/platform/DishTemplateChangeReviewsView.vue`
- `admin-web/tests/dish-template-change-requests.test.js`

### Backend dish-template and featured-dish implementation

- `backend/src/main/java/com/familykitchen/dish/controller/AdminDishTemplateChangeRequestController.java`
- `backend/src/main/java/com/familykitchen/dish/controller/DishTemplateChangeRequestController.java`
- `backend/src/main/java/com/familykitchen/dish/mapper/DishTemplateChangeRequestMapper.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/AdminDishTemplateChangeQuery.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishFeaturedRequest.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateApproveRequest.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateChangeSubmitRequest.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateIngredientSnapshotRequest.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateRejectRequest.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/DishTemplateSnapshotRequest.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/ImportedDishTemplateSyncRequest.java`
- `backend/src/main/java/com/familykitchen/dish/model/dto/MerchantDishTemplateChangeQuery.java`
- `backend/src/main/java/com/familykitchen/dish/model/entity/DishTemplateChangeRequestDO.java`
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateChangeDetailView.java`
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateChangeItemView.java`
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateChangePageView.java`
- `backend/src/main/java/com/familykitchen/dish/model/vo/DishTemplateChangeSubmitView.java`
- `backend/src/main/java/com/familykitchen/dish/service/DishTemplateChangeRequestService.java`
- `backend/src/main/java/com/familykitchen/dish/service/DishTemplateSnapshotValidator.java`
- `backend/src/main/java/com/familykitchen/dish/service/MerchantDishMutationLock.java`
- `backend/src/main/java/com/familykitchen/dish/service/impl/DishTemplateChangeRequestServiceImpl.java`
- `backend/src/main/resources/mapper/dish/DishTemplateChangeRequestMapper.xml`

### Backend family and merchant profile implementation/tests

- `backend/src/main/java/com/familykitchen/family/model/vo/FamilyInfoView.java`
- `backend/src/main/java/com/familykitchen/family/model/vo/OwnerCandidateView.java`
- `backend/src/main/java/com/familykitchen/merchant/controller/MerchantProfileController.java`
- `backend/src/main/java/com/familykitchen/merchant/mapper/MerchantProfileMapper.java`
- `backend/src/main/java/com/familykitchen/merchant/model/dto/UpdateMerchantProfileRequest.java`
- `backend/src/main/java/com/familykitchen/merchant/model/vo/MerchantProfileView.java`
- `backend/src/main/java/com/familykitchen/merchant/service/MerchantProfileService.java`
- `backend/src/main/java/com/familykitchen/merchant/service/impl/MerchantProfileServiceImpl.java`
- `backend/src/test/java/com/familykitchen/family/FamilyFamilyControllerContractTest.java`
- `backend/src/test/java/com/familykitchen/family/FamilyInfoApplicationServiceTest.java`
- `backend/src/test/java/com/familykitchen/family/FamilyOwnerTransferConcurrencyMySqlTest.java`
- `backend/src/test/java/com/familykitchen/family/FamilyOwnerTransferServiceTest.java`
- `backend/src/test/java/com/familykitchen/merchant/MerchantProfileControllerContractTest.java`
- `backend/src/test/java/com/familykitchen/merchant/MerchantProfileMapperContractTest.java`
- `backend/src/test/java/com/familykitchen/merchant/MerchantProfileMySqlTest.java`
- `backend/src/test/java/com/familykitchen/merchant/MerchantProfileServiceTest.java`

### Ordered predecessor migrations

- `backend/src/main/resources/db/migration/V7__backfill_default_family_meal_slots.sql`
- `backend/src/main/resources/db/migration/V8__add_dish_template_change_review.sql`
- `backend/src/main/resources/db/migration/V9__add_family_featured_dish.sql`
- `backend/src/main/resources/db/migration/V10__add_merchant_featured_dishes.sql`

Review notes: V7 idempotently backfills three active-family meal slots; V8 adds template versioning and the review request table; V9 adds the legacy family featured-dish reference; V10 adds merchant-level featured timestamps and migrates eligible family references/menu items. The version order is contiguous and these files are required predecessors for later family-wallet work.

### Documentation

- `docs/frontend-api-guide.md`
- `docs/verification/2026-08-10-merchant-workspace-verification.md`

### Mini-program template-review pages

- `frontend/pages/merchant/dish-template-change-detail/index.js`
- `frontend/pages/merchant/dish-template-change-detail/index.json`
- `frontend/pages/merchant/dish-template-change-detail/index.wxml`
- `frontend/pages/merchant/dish-template-change-detail/index.wxss`
- `frontend/pages/merchant/dish-template-change-edit/index.js`
- `frontend/pages/merchant/dish-template-change-edit/index.json`
- `frontend/pages/merchant/dish-template-change-edit/index.wxml`
- `frontend/pages/merchant/dish-template-change-edit/index.wxss`
- `frontend/pages/merchant/dish-template-changes/index.js`
- `frontend/pages/merchant/dish-template-changes/index.json`
- `frontend/pages/merchant/dish-template-changes/index.wxml`
- `frontend/pages/merchant/dish-template-changes/index.wxss`

### Mini-program merchant profile and feature tests/utilities

- `frontend/pages/merchant/merchant-profile-edit/index.js`
- `frontend/pages/merchant/merchant-profile-edit/index.json`
- `frontend/pages/merchant/merchant-profile-edit/index.wxml`
- `frontend/pages/merchant/merchant-profile-edit/index.wxss`
- `frontend/tests/dish-template-change-requests.test.js`
- `frontend/tests/family-management-page-contract.test.js`
- `frontend/tests/family-management.test.js`
- `frontend/tests/merchant-profile-page-behavior.test.js`
- `frontend/tests/merchant-profile-page-contract.test.js`
- `frontend/tests/merchant-profile.test.js`
- `frontend/utils/dish-template-change.js`
- `frontend/utils/family-management.js`
- `frontend/utils/merchant-profile.js`

## Task 0 reconciliation changes

- `.gitignore`: explicitly permits this inventory and ignores project-local `/.worktrees/` before worktree creation.
- `frontend/components/quantity-stepper/index.wxss`: reconciled an overlapping UI regression by restoring the contractually required 88rpx minimum touch target. The existing focused contract failed at 38rpx before the fix and passed 3/3 afterward.
- `docs/verification/2026-08-25-family-wallet-baseline-inventory.md`: this inventory.

## Verification before checkpoint

- Backend: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd test` — 182 tests, 177 passed, 0 failures, 0 errors, 5 skipped because Docker/Testcontainers was unavailable. The skipped suites were the five MySQL integration suites; unit, contract, H2, migration-contract, and application-context tests passed.
- Mini-program initial full run: `npm test` — 326 tests, 325 passed, 1 failed (`custom-button-components.test.js`, 88rpx minimum target).
- Mini-program focused regression after reconciliation: `node --test tests/custom-button-components.test.js` — 3 passed, 0 failed.
- Admin web: `npm test` — 46 passed, 0 failed.
- Admin web: `npm run build` — passed; generated `admin-web/dist` remained ignored and excluded.
- Final mini-program full rerun: `npm test` — 326 passed, 0 failed. Clean-worktree verification is recorded in the Task 0 handoff after execution.

## Clean-worktree command notes

- Run mini-program tests from `frontend`: `npm test`; focused account-management contract: `node --test tests/account-management-page-contract.test.js`.
- Run admin tests and build from `admin-web`: `npm test` and `npm run build`; focused Vite contract: `node --test tests/vite-config.test.js`.
- The tracked mini-program source uses CRLF line endings, so source-contract method-boundary expressions accept both `\n` and `\r\n`.
- `admin-web/public/backend.config.json` is intentionally ignored and may be absent in a clean clone. The Vite proxy contract therefore verifies the resolved environment/external target and the tracked localhost fallback rather than assuming that runtime-only file exists.

## Exclusions

- Ignored IDE/runtime directories (`.idea`, `.run`, `.agents`, `.superpowers`) were not candidates and are excluded.
- Backend `target`, admin `dist`, dependency directories, logs, temporary files, credentials, and secret material are excluded.
