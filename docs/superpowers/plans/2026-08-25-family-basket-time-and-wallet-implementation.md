# Family Shared Basket, Expected Meal Time, and Wallet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace meal-slot/member-wallet ordering with a versioned family-shared basket, today-only expected meal time, aggregate dish rows with detail-only member attribution, and family-wallet settlement.

**Architecture:** Introduce explicit family wallet, order hold, selection, migration-batch, and idempotency persistence before redirecting any production flow. Cart writes use `cartId + cartVersion + requestId` and absolute member quantities; order submission snapshots aggregate items and member selections, then freezes one family wallet under a global lock order. The mini program removes meal-slot decisions, renders aggregate rows by default, and lazily expands member attribution in details.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis/MyBatis-Plus, Flyway, MySQL 8, JUnit 5, Mockito, Testcontainers, WeChat Mini Program WXML/WXSS/CommonJS, Node.js test runner.

---

## Working-tree guardrails

- The repository already contains unrelated modified and untracked work. Never use `git add .`, `git commit -a`, reset, checkout, or cleanup commands.
- Stage only the exact files listed in the active task. Before every commit run `git diff --cached --name-only` and verify no unrelated path is staged.
- Untracked migrations `V7__backfill_default_family_meal_slots.sql` through `V10__add_merchant_featured_dishes.sql` are required predecessors. They must be reviewed, checkpointed, and present in the isolated baseline before this feature starts at `V11`; never renumber, absorb, or overwrite them.
- Run all money tests against MySQL 8 where row locking, generated columns, checks, and decimal behavior matter.
- Money input with more than two decimal places is rejected; do not normalize it with `HALF_UP`. Dish/fee calculation has one documented rounding point before wallet authorization, and migration copies stored decimals exactly without rounding.

## File map

### New backend units

- `backend/src/main/resources/db/migration/V11__add_family_cart_time_and_wallet.sql`: additive compatibility schema, migration/cutover metadata tables, constraints, and nullable legacy columns only; it never moves money or consolidates carts.
- `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationRunner.java`: property-gated `OFF/PREFLIGHT/QUIESCE/ABORT_BEFORE_EXECUTE/EXECUTE/VERIFY/FINALIZE` maintenance command; every mutating phase binds one batch/drain epoch and never runs automatically.
- `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationService.java`: all-read preflight, per-family migration transactions, conservation verification, and guarded final schema creation.
- `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationMapper.java`: migration candidates, anomalies, source keys, and totals.
- `backend/src/main/resources/mapper/migration/FamilyCartWalletMigrationMapper.xml`: migration SQL and exact decimal aggregation.
- `backend/src/main/java/com/familykitchen/migration/ApplicationInstanceLeaseService.java`: compatibility-release instance heartbeat/version lease and quiesce drain verification.
- `backend/src/main/java/com/familykitchen/common/idempotency/CommandIdempotencyService.java`: durable request claim/replay/conflict boundary.
- `backend/src/main/java/com/familykitchen/common/idempotency/CommandIdempotencyMapper.java`: command result persistence.
- `backend/src/main/resources/mapper/common/CommandIdempotencyMapper.xml`: actor/family/operation/request-key lookup and unique insert.
- `backend/src/main/java/com/familykitchen/wallet/model/entity/FamilyWalletAccountDO.java`: family account persistence model.
- `backend/src/main/java/com/familykitchen/wallet/model/entity/FamilyWalletLedgerDO.java`: immutable family ledger model.
- `backend/src/main/java/com/familykitchen/wallet/model/entity/FamilyWalletOrderHoldDO.java`: order-level authorization/capture/release/refund model.
- `backend/src/main/java/com/familykitchen/wallet/model/bo/FamilyWalletAccount.java`: non-negative money state transitions.
- `backend/src/main/java/com/familykitchen/wallet/mapper/FamilyWalletMapper.java`: lock/read/update/ledger/hold SQL in the global lock order.
- `backend/src/main/resources/mapper/wallet/FamilyWalletMapper.xml`: family wallet, ledger, and hold statements.
- `backend/src/main/java/com/familykitchen/wallet/service/FamilyWalletService.java` and `impl/FamilyWalletServiceImpl.java`: idempotent freeze, append-freeze, release, capture, refund, and merchant adjustment boundary.
- `backend/src/main/java/com/familykitchen/wallet/controller/FamilyWalletController.java`: current-family summary and paged ledgers.
- `backend/src/main/java/com/familykitchen/wallet/controller/MerchantFamilyWalletController.java`: merchant-owned family summary, ledgers, and adjustment.
- `backend/src/main/java/com/familykitchen/wallet/model/dto/AdjustFamilyBalanceRequest.java`, `FamilyWalletSummaryView.java`, `FamilyWalletLedgerView.java`: public family-wallet contract.
- `backend/src/main/java/com/familykitchen/cart/model/entity/CartItemSelectionEntity.java`: per-member quantity and remark.
- `backend/src/main/java/com/familykitchen/cart/model/dto/CartMutationRequest.java`, `ExpectedMealTimeRequest.java`: versioned/idempotent cart commands.
- `backend/src/main/java/com/familykitchen/cart/service/ExpectedMealTimePolicy.java`: Asia/Shanghai today/future/15-minute validation.
- `backend/src/main/java/com/familykitchen/order/model/entity/OrderItemSelectionEntity.java`: immutable member attribution snapshot.

### Existing backend units to modify

- Cart: `CartController.java`, `CartApplicationService.java`, `CartApplicationServiceImpl.java`, `CartMapper.java`, `CartEntity.java`, `CartItemEntity.java`, `CartItemRequest.java`, `CartRemarkRequest.java`, `CartView.java`.
- Order: `FamilyOrderController.java`, `MerchantOrderController.java`, `FamilyOrderApplicationService.java`, `FamilyOrderApplicationServiceImpl.java`, `MerchantOrderApplicationService.java`, `MerchantOrderApplicationServiceImpl.java`, `OrderSubmissionService.java`, `OrderSubmissionServiceImpl.java`, `OrderPersistenceMapper.java`, `SubmitOrderRequest.java`, `DeliveryFeeRequest.java`, `OrderCheckoutCommand.java`, `OrderCartSnapshot.java`, `CheckoutItem.java`, `OrderSubmissionResult.java`, `OrderRecordEntity.java`, `OrderItemEntity.java`, `OrderView.java`.
- Family/member lifecycle: `FamilyController.java`, `FamilyApplicationService.java`, `FamilyApplicationServiceImpl.java`, `FamilyMemberApplicationServiceImpl.java`, `FamilyMapper.java`, `FamilyWorkflowMapper.java`, `FamilyHomeResponse.java`.
- Purchase/notification: `PurchaseApplicationServiceImpl.java`, `PurchaseMapper.java`, `PurchaseItemSummary.java`, `NotificationApplicationServiceImpl.java`.
- Legacy wallet deactivation: `MerchantWalletController.java`, `MerchantWalletApplicationService.java`, `MerchantWalletApplicationServiceImpl.java`, `WalletPersistenceMapper.java`.
- Existing mapper XML to modify: `backend/src/main/resources/mapper/cart/CartMapper.xml`, `backend/src/main/resources/mapper/order/OrderPersistenceMapper.xml`, `backend/src/main/resources/mapper/wallet/WalletPersistenceMapper.xml`, `backend/src/main/resources/mapper/purchase/PurchaseMapper.xml`, `backend/src/main/resources/mapper/family/FamilyMapper.xml`.
- Docs: `docs/frontend-api-guide.md`.

### Mini-program units to modify

- Services/runtime: `frontend/services/cart.js`, `frontend/services/orders.js`, `frontend/services/family.js`, `frontend/services/merchant.js`, `frontend/utils/family-api.js`, `frontend/utils/api-runtime.js`, `frontend/utils/api-scenes.js`.
- Family ordering: `frontend/pages/ordering/menu/index.js|wxml|wxss`, `dish-detail/index.js|wxml|wxss`, `cart/index.js|wxml|wxss`, `orders/index.js|wxml`, `order-detail/index.js|wxml|wxss`.
- Merchant/order/wallet: `frontend/pages/merchant/merchant-orders/index.js|wxml`, `merchant-order-detail/index.js|wxml|wxss`, `merchant-family-detail/index.js|wxml|wxss`, `frontend/pages/family/wallet/index.js|wxml`, `wallet-ledger/index.js|wxml`.
- Shared rendering: `frontend/components/order-row/index.js|wxml`, `frontend/utils/purchase.js`, `frontend/utils/purchase-state.js`.

## Task 0: Reconcile overlapping work and create an isolated baseline

**Files:**
- Inspect only: every path from `git status --short`, especially the overlapping backend family files, frontend family/merchant services and pages, `docs/frontend-api-guide.md`, and migrations `V7`–`V10`.
- Create: `docs/verification/2026-08-25-family-wallet-baseline-inventory.md`

- [ ] **Step 1: Capture the exact dirty baseline**

Run `git status --short`, `git diff --binary`, and enumerate untracked files. Record which change belongs to already-approved prior features and which, if any, is unknown. Do not stage anything yet.

- [ ] **Step 2: Resolve every overlapping path before implementation**

For known prior-feature changes, run their focused tests and create an explicit checkpoint commit using exact file paths. For unknown/user-owned changes, stop and ask the user whether to checkpoint them, leave them outside the feature, or supply a clean base. Never combine unknown code with this feature.

- [ ] **Step 3: Commit the approved baseline and inventory**

Commit the reconciled prior-feature files and `docs/verification/2026-08-25-family-wallet-baseline-inventory.md` with exact paths. Record the resulting commit SHA. Do not proceed while an overlapping path is still dirty or its ownership is unknown.

- [ ] **Step 4: Create the feature worktree from that exact commit**

Use `superpowers:using-git-worktrees`; create a `codex/family-shared-basket-wallet` branch/worktree from the recorded SHA. Verify HEAD equals that SHA, `git status --short` is empty, and the inventory plus `V7`–`V10` all exist. All following tasks run only there.

## Task 1: Add compatibility schema only (no money movement)

**Files:**
- Create: `backend/src/main/resources/db/migration/V11__add_family_cart_time_and_wallet.sql`
- Create: `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationContractTest.java`
- Create: `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationMySqlTest.java`

- [ ] **Step 1: Write the failing migration contract test**

Assert V11 contains compatibility tables/columns and no `UPDATE member_wallets`, no wallet clearing, no legacy cart merge, and no final `uk_carts_active_family`. Assert it does not drop `meal_slots`, `member_wallets`, `wallet_ledgers`, or `order_member_charges`.

- [ ] **Step 2: Run the contract test and verify failure**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -Dtest=FamilyCartWalletMigrationContractTest test`
Expected: FAIL because `V11__add_family_cart_time_and_wallet.sql` is absent.

- [ ] **Step 3: Write failing MySQL compatibility tests before SQL implementation**

Start at migration target V10, apply V11, then insert a new cart/order with all retired fields null. Assert old rows still read, new tables/checks exist, and V11 never changes legacy balances or carts.

- [ ] **Step 4: Add additive schema only**

Create tables and columns with these critical constraints:

```sql
ALTER TABLE carts
  MODIFY user_id bigint NULL,
  MODIFY meal_slot_id bigint NULL,
  MODIFY service_date date NULL,
  ADD COLUMN expected_meal_time datetime NULL,
  ADD COLUMN version bigint NOT NULL DEFAULT 0;

ALTER TABLE orders
  MODIFY meal_slot_id bigint NULL,
  MODIFY service_date date NULL,
  MODIFY delivery_fee_payer_user_id bigint NULL,
  ADD COLUMN source_cart_id bigint NULL,
  ADD COLUMN expected_meal_time datetime NULL,
  ADD UNIQUE KEY uk_orders_source_cart(source_cart_id);

ALTER TABLE order_items MODIFY owner_user_id bigint NULL;
CREATE UNIQUE INDEX uk_cart_item_selection ON cart_item_selections(cart_item_id,user_id);
CREATE UNIQUE INDEX uk_order_item_selection ON order_item_selections(order_item_id,user_id);
CREATE UNIQUE INDEX uk_family_wallet_business ON family_wallet_ledgers(business_type,business_key);
```

Add `command_idempotency` with unique `(actor_user_id,family_id,operation,request_id)`, payload hash, state, result resource/type/body and timestamps. Add migration batch/source/anomaly tables and family wallet/hold tables with database non-negative and hold-equation `CHECK` constraints. Preserve the old active-cart key until the migration runner finalizes; document its later removal.

- [ ] **Step 5: Run compatibility tests**

Run both Task 1 tests. Expected: PASS and legacy data unchanged.

- [ ] **Step 6: Commit exact files**

Commit message: `feat: add family cart and wallet compatibility schema`.

## Task 1B: Build the resumable migration runner and guarded finalization

**Files:**
- Create: `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationRunner.java`
- Create: `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationService.java`
- Create: `backend/src/main/java/com/familykitchen/migration/FamilyCartWalletMigrationMapper.java`
- Create: `backend/src/main/resources/mapper/migration/FamilyCartWalletMigrationMapper.xml`
- Create: `backend/src/main/java/com/familykitchen/migration/ApplicationInstanceLeaseService.java`
- Create: `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationRunnerTest.java`
- Create: `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationRecoveryMySqlTest.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Write all failing preflight, recovery, and fault-injection tests**

Before implementation, test unmapped/multi-family non-zero wallets, negative/overflow money, unmatched order frozen money, failure after family N, restart/continue, repeated source key, and exact available/frozen conservation. Test normal duplicate active carts as merge input, not an automatic anomaly. Also test preflight followed by wallet/order/member/cart mutation, an old instance that ignores maintenance mode, accidental family-mode traffic before finalization, default mode `OFF`, wrong/expired drain epoch, wrong batch ID, concurrent runners, `ABORT_BEFORE_EXECUTE` before any source migration, and rejection of abort after the first source migration.

- [ ] **Step 2: Run and record expected failures**

Run the two new classes; expected FAIL because the runner is absent.

- [ ] **Step 3: Implement explicit maintenance phases**

`PREFLIGHT` is all-read, creates one batch draft, and prints its required non-null `batchId`. The compatibility release registers every process in `application_instance_leases` with build version and expiring heartbeat and already respects the persisted barrier. The only supported deployment order is: replace all older builds with this compatibility release and verify only the expected build lease remains → first `PREFLIGHT` → `QUIESCE --batchId=...` atomically sets the global barrier, creates/prints a monotonically increasing `drainEpoch`, blocks personal-wallet writes, old order freeze/release/settle, member-relation mutations and old cart writes, then waits for all web-instance leases to drain → stop those instances → rerun `PREFLIGHT --batchId --drainEpoch` under the barrier → `EXECUTE --batchId --drainEpoch` → `VERIFY --batchId --drainEpoch` → `FINALIZE --batchId --drainEpoch` → start only family-mode instances. Every legacy/new service checks cutover state; no phase before `FAMILY_READY` can create family-mode carts/orders.

`ABORT_BEFORE_EXECUTE --batchId --drainEpoch` may clear the barrier only when the batch has zero migrated source accounts, zero migrated carts, and no finalized DDL; otherwise it returns a hard error and the operator must repair/resume. Every independent runner JVM acquires a database advisory/row lease and verifies the same batch, current drain epoch, unexpired drain proof and safety target token. Wrong batch, stale epoch or concurrent runner fails before writes.

`EXECUTE` migrates one family per `REQUIRES_NEW` transaction using unique source keys. It creates per-order holds and consolidates old active carts as follows: use the current valid family dish price; unmappable, inactive, or off-shelf dishes are reported and block that family without data loss; source cart user becomes selection owner; same-member/same-dish remarks keep the newest non-empty value and report discarded conflicts; basket remark keeps the newest non-empty value; invalid/non-today expected time becomes null. Successfully absorbed old carts become lowercase `migrated`; it does not create the final index. `VERIFY` checks global/per-family available and frozen conservation, every hold equation, aggregate-selection sums, and zero unresolved migration anomalies.

- [ ] **Step 4: Implement guarded FINALIZE**

Only a verified batch under the matching `drainEpoch` may remove `uk_carts_active_cart`, add generated `active_family_id`, create `uk_carts_active_family`, and atomically advance cutover to `FAMILY_READY` after DDL confirmation. DDL finalization is recorded before/after and safely detectable after interruption; never run from normal startup. New databases may finalize after an empty verified batch. Add tests proving new endpoints return maintenance/not-ready errors before `FAMILY_READY`, legacy writes remain disabled after it, and an old/missing lease proof cannot finalize.

- [ ] **Step 5: Run recovery tests and a real MySQL dry run**

Expected: every injected interruption resumes without duplicate credits; a failed verification leaves family mode disabled, processed families auditable, unprocessed personal wallets unchanged, and the global write barrier in place until an operator repairs and resumes or performs a documented rollback before any clearing.

- [ ] **Step 6: Commit exact runner/config/test files**

Commit message: `feat: add resumable family wallet migration runner`.

## Task 2: Implement family-wallet state transitions and holds

**Files:**
- Create: `backend/src/main/java/com/familykitchen/wallet/model/entity/FamilyWalletAccountDO.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/model/entity/FamilyWalletLedgerDO.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/model/entity/FamilyWalletOrderHoldDO.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/model/bo/FamilyWalletAccount.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/mapper/FamilyWalletMapper.java`
- Create: `backend/src/main/resources/mapper/wallet/FamilyWalletMapper.xml`
- Create: `backend/src/main/java/com/familykitchen/wallet/service/FamilyWalletService.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/service/impl/FamilyWalletServiceImpl.java`
- Create: `backend/src/test/java/com/familykitchen/wallet/FamilyWalletAccountTest.java`
- Create: `backend/src/test/java/com/familykitchen/wallet/FamilyWalletServiceTest.java`
- Create: `backend/src/test/java/com/familykitchen/wallet/FamilyWalletConcurrencyMySqlTest.java`
- Create: `backend/src/test/java/com/familykitchen/common/idempotency/CommandIdempotencyServiceTest.java`
- Create: `backend/src/main/java/com/familykitchen/common/idempotency/CommandIdempotencyService.java`
- Create: `backend/src/main/java/com/familykitchen/common/idempotency/CommandIdempotencyMapper.java`
- Create: `backend/src/main/resources/mapper/common/CommandIdempotencyMapper.xml`

- [ ] **Step 1: Write failing domain tests**

Before implementation, test freeze, append freeze, release, capture, refund, manual credit/debit, insufficient available balance, over-release, over-refund, amount precision/overflow rejection, database `CHECK` constraints, and invariant preservation. Also write the real MySQL races now: two freezes against one balance, request replay after service restart, same key/different payload conflict, cancel versus capture, and merchant debit versus submit.

```java
assertEquals(
    hold.initialAmount().add(hold.additionalFrozenAmount()),
    hold.remainingFrozenAmount().add(hold.capturedAmount()).add(hold.releasedAmount())
);
assertTrue(hold.refundedAmount().compareTo(hold.capturedAmount()) <= 0);
```

- [ ] **Step 2: Run tests and verify failure**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -Dtest=FamilyWalletAccountTest,FamilyWalletServiceTest,FamilyWalletConcurrencyMySqlTest,CommandIdempotencyServiceTest test`
Expected: FAIL because family-wallet types do not exist.

- [ ] **Step 3: Implement account, hold, mapper, and service**

Reject values whose normalized decimal scale exceeds two; do not silently round wallet commands. Keep calculation rounding in the order-pricing boundary only. Implement three non-overlapping lock entry points: new submit receives an already locked cart and locks wallet before inserting new invisible order/hold; existing-order operations lock order → hold → wallet → ledger; merchant adjustment locks wallet → ledger and cannot call back into order lookup. `CommandIdempotencyService` uses non-null normalized scope values and performs claim insert, payload-hash validation, business mutations, ledger insert, and result persistence in the same database transaction. A crash before commit rolls back both claim and business data; a lost response after commit replays the stored result. Concurrent contenders block on the unique key, then replay the committed result or receive `STATE_CONFLICT`. Inject failures immediately after claim, before business commit, and after commit/before response.

- [ ] **Step 4: Make the already-written MySQL concurrency tests pass**

Assert no negative balance, no duplicate ledger, no deadlock, and exactly one valid terminal hold transition. Add SQL lock-order contract assertions for each of the three entry points.

- [ ] **Step 5: Run wallet tests**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -Dtest=FamilyWalletAccountTest,FamilyWalletServiceTest,FamilyWalletConcurrencyMySqlTest,CommandIdempotencyServiceTest test`
Expected: PASS.

- [ ] **Step 6: Commit exact family-wallet files and tests**

Commit message: `feat: add idempotent family wallet domain`.

## Task 3: Replace member-specific carts with a versioned shared family cart

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/cart/controller/CartController.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/service/CartApplicationService.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/service/impl/CartApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/mapper/CartMapper.java`
- Modify: `backend/src/main/resources/mapper/cart/CartMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/cart/model/entity/CartEntity.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/model/entity/CartItemEntity.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/model/dto/CartItemRequest.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/model/dto/CartRemarkRequest.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/model/vo/CartView.java`
- Create: `backend/src/main/java/com/familykitchen/cart/model/entity/CartItemSelectionEntity.java`
- Create: `backend/src/main/java/com/familykitchen/cart/model/dto/CartMutationRequest.java`
- Create: `backend/src/main/java/com/familykitchen/cart/model/dto/ExpectedMealTimeRequest.java`
- Create: `backend/src/main/java/com/familykitchen/cart/service/ExpectedMealTimePolicy.java`
- Create `backend/src/test/java/com/familykitchen/cart/ExpectedMealTimePolicyTest.java`
- Create `backend/src/test/java/com/familykitchen/cart/CartApplicationServiceTest.java`
- Create `backend/src/test/java/com/familykitchen/cart/SharedCartConcurrencyMySqlTest.java`

- [ ] **Step 1: Write failing time and cart tests**

Use an injected `Clock` fixed to Asia/Shanghai. Assert 14:08 produces 14:30, 14:15 produces 14:30, yesterday/tomorrow/past/non-quarter-hour fail, and end-of-day reports no valid time. Assert two members see one cart, aggregate quantity equals selection sum, each member can change only their absolute quantity, stale version fails, and a replayed `requestId` survives service restart. Before implementation also write concurrent-create and submit-versus-write MySQL tests.

- [ ] **Step 2: Run focused tests and verify failure**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -Dtest=ExpectedMealTimePolicyTest,CartApplicationServiceTest,SharedCartConcurrencyMySqlTest test`
Expected: FAIL against meal-slot/member cart behavior.

- [ ] **Step 3: Implement the new cart contract**

`GET /family/cart` has no query parameters and returns authoritative `serverNow`, `serverDate`, `minimumExpectedMealTime`, `timeStepMinutes=15`, and `bookingEnded`, plus cart/version. Mutation DTOs contain `cartId`, `cartVersion`, `requestId`, dish/absolute current-member quantity, and optional current-member remark. Update with SQL equivalent to `UPDATE carts SET version=version+1 WHERE id=? AND status='active' AND version=?`; zero removes only the caller selection and deletes aggregate row only when the sum reaches zero. All mutations use the durable command idempotency table, not process memory.

- [ ] **Step 4: Add concurrent-create/submit-write tests**

Assert the generated unique key allows one active cart. If submit wins, a stale add/update returns `CART_SUBMITTED` and never lands in the next cart. If a cart mutation wins first, submission sees the new version and requires refresh.

- [ ] **Step 5: Run cart tests**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -Dtest=ExpectedMealTimePolicyTest,CartApplicationServiceTest,SharedCartConcurrencyMySqlTest test`
Expected: PASS.

- [ ] **Step 6: Commit exact cart files**

Commit message: `feat: add versioned shared family cart`.

## Task 4: Remove exiting members from the active cart safely

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/family/service/impl/FamilyMemberApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/family/mapper/FamilyWorkflowMapper.java`
- Modify: `backend/src/main/java/com/familykitchen/family/mapper/FamilyRelationMapper.java`
- Modify: `backend/src/main/resources/mapper/family/FamilyRelationMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/admin/service/AdminFamilyService.java`
- Modify: `backend/src/main/java/com/familykitchen/admin/service/impl/AdminFamilyServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/admin/mapper/AdminFamilyMapper.java`
- Modify: `backend/src/main/java/com/familykitchen/admin/service/AdminUserService.java`
- Modify: `backend/src/main/java/com/familykitchen/admin/mapper/AdminUserMapper.java`
- Modify: `backend/src/main/java/com/familykitchen/cart/mapper/CartMapper.java`
- Modify: `backend/src/main/resources/mapper/cart/CartMapper.xml`
- Create: `backend/src/test/java/com/familykitchen/family/FamilyMemberCartCleanupTest.java`
- Create: `backend/src/test/java/com/familykitchen/family/FamilyMemberCartCleanupConcurrencyMySqlTest.java`

- [ ] **Step 1: Write failing exit cleanup tests**

Before implementation, assert voluntary exit, admin removal, platform user removal, and family dissolution remove affected active selections, recompute totals, delete zero aggregate rows, increment cart version, and preserve submitted order snapshots. Write the exit/removal/dissolution-versus-submit MySQL races now.

- [ ] **Step 2: Run and verify failure**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -Dtest=FamilyMemberCartCleanupTest,FamilyMemberCartCleanupConcurrencyMySqlTest test`
Expected: FAIL because removal currently leaves cart rows.

- [ ] **Step 3: Implement cleanup in the membership transaction**

Lock active cart → cart items/selections → every participating active `family_user_relations` row with `FOR UPDATE` → remove/exit/dissolve relation. Submission uses the identical prefix and locks/verifies all selection-owner relations before snapshotting. Centralize this cleanup so voluntary exit, admin removal, platform deletion, and family dissolution cannot bypass it. Account cancellation already blocks while active membership exists and receives a regression test.

- [ ] **Step 4: Test exit racing submission**

Assert either the selection is included while membership was valid and the order commits first, or exit commits first and the selection is absent; never create an order attributed to an already-invalid member.

- [ ] **Step 5: Run tests and commit**

Commit message: `feat: clean shared cart when member exits`.

## Task 5: Submit immutable aggregate orders and freeze the family wallet

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/order/controller/FamilyOrderController.java`
- Modify: `backend/src/main/java/com/familykitchen/order/service/FamilyOrderApplicationService.java`
- Modify: `backend/src/main/java/com/familykitchen/order/service/OrderSubmissionService.java`
- Modify: `backend/src/main/java/com/familykitchen/order/service/impl/FamilyOrderApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/order/service/impl/OrderSubmissionServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/order/mapper/OrderPersistenceMapper.java`
- Modify: `backend/src/main/resources/mapper/order/OrderPersistenceMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/order/model/dto/SubmitOrderRequest.java`
- Modify: `backend/src/main/java/com/familykitchen/order/model/bo/OrderCheckoutCommand.java`
- Modify: `backend/src/main/java/com/familykitchen/order/model/bo/OrderCartSnapshot.java`
- Modify: `backend/src/main/java/com/familykitchen/order/model/bo/CheckoutItem.java`
- Modify: `backend/src/main/java/com/familykitchen/order/model/bo/OrderSubmissionResult.java`
- Modify: `backend/src/main/java/com/familykitchen/order/model/entity/OrderRecordEntity.java`
- Modify: `backend/src/main/java/com/familykitchen/order/model/entity/OrderItemEntity.java`
- Modify: `backend/src/main/java/com/familykitchen/order/model/vo/OrderView.java`
- Create: `backend/src/main/java/com/familykitchen/order/model/entity/OrderItemSelectionEntity.java`
- Modify: `backend/src/test/java/com/familykitchen/order/OrderSubmissionServiceTest.java`
- Create: `backend/src/test/java/com/familykitchen/order/FamilyOrderSubmissionMySqlTest.java`
- Create: `backend/src/test/java/com/familykitchen/order/FamilyOrderControllerContractTest.java`
- Create: `backend/src/main/java/com/familykitchen/order/service/LegacyOrderPayloadGuard.java`

- [ ] **Step 1: Rewrite tests for the new submission contract**

Request fields are `cartId`, `cartVersion`, `requestId`, delivery mode/address/remark. Assert no active use of `mealSlotId`, `serviceDate`, payer member, or order-update endpoint. Add old-client payload tests that send each retired write field and expect stable `CLIENT_UPGRADE_REQUIRED`, never silent Jackson ignore. Validate expected time again at submit. Assert one aggregate order item per dish and `(order_item_id,user_id)` unique positive selections whose sum equals item quantity. Before implementation, make `FamilyOrderSubmissionMySqlTest` cover two different request IDs racing on the same cart, same request replay after restart, same key/different payload, and `source_cart_id → order_id` uniqueness.

- [ ] **Step 2: Run and verify failure**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -Dtest=OrderSubmissionServiceTest,FamilyOrderControllerContractTest,FamilyOrderSubmissionMySqlTest test`
Expected: FAIL against existing member-wallet/meal-slot checkout.

- [ ] **Step 3: Implement immutable submit**

`LegacyOrderPayloadGuard` inspects the raw submission object (or explicitly bound deprecated fields) and rejects `mealSlotId`, `serviceDate`, `payerMemberId`, and `deliveryFeePayerUserId` before service execution. Then lock cart → items/selections → every selection owner's active membership relation → family wallet. Reprice active family dishes, reject stale/invalid member selections, snapshot member names/remarks, freeze total amount, insert order with unique `source_cart_id`, hold and ledger, store the durable idempotency result, then set cart `submitted`. Do not auto-create the next cart inside this transaction; the next GET creates it safely.

- [ ] **Step 4: Remove post-submit family update**

Delete `PUT /family/orders/{orderId}` from controller/service/frontend. Cancellation remains state-gated and idempotent. Historical order mapper reads `expectedMealTime` first and falls back to service date/meal slot only when null.

- [ ] **Step 5: Run unit and MySQL tests**

Cover duplicate submit, stale cart, just-expired time, insufficient family balance, price change, invalid address, aggregate/member mismatch, and submit followed by immediate new cart.

- [ ] **Step 6: Commit exact order files**

Commit message: `feat: submit shared cart with family wallet`.

## Task 6: Redirect order lifecycle and delivery-fee changes to order holds

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/order/service/impl/MerchantOrderApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/order/service/impl/FamilyOrderApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/order/mapper/OrderPersistenceMapper.java`
- Modify: `backend/src/main/resources/mapper/order/OrderPersistenceMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/order/model/dto/DeliveryFeeRequest.java`
- Modify: `backend/src/main/java/com/familykitchen/order/model/bo/OrderSubmissionResult.java`
- Modify: `backend/src/test/java/com/familykitchen/order/OrderStateMachineTest.java`
- Modify: `backend/src/test/java/com/familykitchen/wallet/WalletOrderLockingContractTest.java`
- Create: `backend/src/test/java/com/familykitchen/order/FamilyWalletOrderLifecycleTest.java`
- Create: `backend/src/test/java/com/familykitchen/order/FamilyWalletOrderLifecycleMySqlTest.java`

- [ ] **Step 1: Write failing lifecycle tests**

Before implementation, assert reject/cancel releases remaining frozen money, complete captures it, fee increase appends freeze, fee decrease releases, insufficient increase rolls back, settled refund credits available and increments `refunded_amount`, and duplicate commands are idempotent. Write all MySQL deadlock races and SQL lock-order contract tests before changing the services.

- [ ] **Step 2: Run and verify failure**

Expected failure: services still load member wallets and member charge maps.

- [ ] **Step 3: Replace member-wallet loops with one hold transition**

All existing-order money paths lock `orders → family_wallet_order_holds → family_wallets → ledger`. Remove new writes to `order_member_charges`; retain read-only legacy mapping for historical display/audit.

- [ ] **Step 4: Run deadlock and invariant tests**

Race cancel/complete, fee adjust/cancel, refund/merchant debit. Assert the hold equations and account conservation after every committed result.

- [ ] **Step 5: Commit**

Commit message: `feat: settle orders through family wallet holds`.

## Task 7: Expose family-wallet APIs and retire personal-wallet mutations

**Files:**
- Create: `backend/src/main/java/com/familykitchen/wallet/controller/FamilyWalletController.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/controller/MerchantFamilyWalletController.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/model/dto/AdjustFamilyBalanceRequest.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/model/vo/FamilyWalletSummaryView.java`
- Create: `backend/src/main/java/com/familykitchen/wallet/model/vo/FamilyWalletLedgerView.java`
- Modify: `backend/src/main/java/com/familykitchen/family/controller/FamilyController.java`
- Modify: `backend/src/main/java/com/familykitchen/family/service/FamilyApplicationService.java`
- Modify: `backend/src/main/java/com/familykitchen/family/service/impl/FamilyApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/family/model/vo/FamilyHomeResponse.java`
- Modify: `backend/src/main/resources/mapper/family/FamilyMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/wallet/controller/MerchantWalletController.java`
- Modify: `backend/src/main/java/com/familykitchen/wallet/service/MerchantWalletApplicationService.java`
- Modify: `backend/src/main/java/com/familykitchen/wallet/service/impl/MerchantWalletApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/wallet/mapper/WalletPersistenceMapper.java`
- Modify: `backend/src/main/resources/mapper/wallet/WalletPersistenceMapper.xml`
- Create: `backend/src/test/java/com/familykitchen/wallet/FamilyWalletControllerContractTest.java`
- Create: `backend/src/test/java/com/familykitchen/wallet/MerchantFamilyWalletServiceTest.java`
- Create: `backend/src/test/java/com/familykitchen/wallet/PersonalWalletCutoverGuardTest.java`

- [ ] **Step 1: Write failing controller and authorization tests**

Contract:

```text
GET  /family/wallet
GET  /family/wallet/ledgers?page=1&pageSize=20
GET  /merchant/families/{familyId}/wallet
GET  /merchant/families/{familyId}/wallet/ledgers?page=1&pageSize=20
POST /merchant/families/{familyId}/wallet/adjust
```

The adjustment body contains `requestId`, type, amount, remark; never accepts a body `familyId`. Test wrong merchant, inactive family, negative/zero amount, insufficient debit, and idempotency conflict.

- [ ] **Step 2: Run tests and verify failure**

Expected: endpoints absent and current family ledger reads `user.memberId()`.

- [ ] **Step 3: Implement summary/ledger/adjustment APIs**

Home response includes family wallet summary, not member wallet balance. Add a runtime cutover guard queried by submit and health/diagnostic checks: when family mode is active, any non-zero personal wallet produces a structured alert and blocks new order submission until reconciled. Disable every personal-wallet write path, including old internal services, with an explicit upgrade error; account creation may keep a zero legacy row only for schema compatibility.

- [ ] **Step 4: Run focused wallet/family tests and commit**

Commit message: `feat: expose family wallet APIs`.

## Task 8: Remove meal slots from home, purchase, and notifications

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/family/controller/FamilyController.java`
- Modify: `backend/src/main/java/com/familykitchen/family/service/FamilyApplicationService.java`
- Modify: `backend/src/main/java/com/familykitchen/family/service/impl/FamilyApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/family/mapper/FamilyMapper.java`
- Modify: `backend/src/main/resources/mapper/family/FamilyMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/family/mapper/FamilyWorkflowMapper.java`
- Modify: `backend/src/main/java/com/familykitchen/family/model/vo/FamilyHomeResponse.java`
- Modify: `backend/src/main/java/com/familykitchen/purchase/service/impl/PurchaseApplicationServiceImpl.java`
- Modify: `backend/src/main/java/com/familykitchen/purchase/mapper/PurchaseMapper.java`
- Modify: `backend/src/main/resources/mapper/purchase/PurchaseMapper.xml`
- Modify: `backend/src/main/java/com/familykitchen/purchase/model/vo/PurchaseItemSummary.java`
- Modify: `backend/src/main/java/com/familykitchen/notification/service/impl/NotificationApplicationServiceImpl.java`
- Modify: `backend/src/test/java/com/familykitchen/purchase/PurchaseApplicationServiceTest.java`
- Modify: `backend/src/test/java/com/familykitchen/notification/FamilyNotificationEventContractTest.java`
- Modify: `backend/src/test/java/com/familykitchen/family/FamilyFeaturedDishResponseTest.java`
- Create: `backend/src/test/java/com/familykitchen/family/FamilyHomeExpectedTimeContractTest.java`

- [ ] **Step 1: Write failing compatibility tests**

New home and purchase output must not require meal-slot configuration. New order messages say `预计 18:30 用餐`; historical orders with null expected time still render original date/slot. Procurement groups/sorts by expected datetime rather than slot.

- [ ] **Step 2: Run and verify failure**

Expected: home calls `mealSlots(user)` and mini bundle throws when none exist.

- [ ] **Step 3: Stop new-flow meal-slot reads and default creation**

Remove `/family/meal-slots` from the new client contract and stop `FamilyWorkflowMapper.insertDefaultMealSlots` in new family onboarding. Keep old tables and history mapper fallback. Do not delete historical rows.

- [ ] **Step 4: Update purchase/notification mapping and tests**

Ensure all new order-derived purchase demands and notifications carry expected datetime. Run focused tests, then commit with `feat: replace meal slots with expected meal time`.

## Task 9: Update mini-program service contracts and scene adapters

**Files:**
- Modify: `frontend/services/cart.js`
- Modify: `frontend/services/orders.js`
- Modify: `frontend/services/family.js`
- Modify: `frontend/services/merchant.js`
- Modify: `frontend/utils/family-api.js`
- Modify: `frontend/utils/api-runtime.js`
- Modify: `frontend/utils/api-scenes.js`
- Modify: `frontend/tests/services.test.js`
- Create: `frontend/tests/family-shared-cart.test.js`
- Create: `frontend/tests/family-wallet-scenes.test.js`
- Create: `frontend/tests/expected-meal-time.test.js`

- [ ] **Step 1: Write failing Node contract tests**

Assert `getCart()` sends no meal-slot/date query, consumes mandatory authoritative `serverNow/serverDate/minimumExpectedMealTime/timeStepMinutes/bookingEnded`, mutation bodies include cart/version/request ID and absolute quantity, submit has no meal/payer fields, wallet calls use family routes, and `loadFamilyBundle` does not fetch meal slots or throw when none exist.

- [ ] **Step 2: Run and verify failure**

Run: `D:\develop\WebDev\node\npm.cmd --prefix frontend test`
Expected: new tests FAIL.

- [ ] **Step 3: Implement service and pure-view adapters**

Add a UUID/request-ID helper with stable ID per user action. Build 15-minute choices only from mandatory server-provided availability fields; local clock never expands the allowed range and server errors remain authoritative. Scene rows expose `totalQuantity`, `myQuantity`, collapsed `selections`, `hasSelectionDetails`, and `expectedMealTimeText`.

- [ ] **Step 4: Run Node tests and commit**

Commit message: `feat: update mini program ordering contracts`.

## Task 10: Rebuild family menu, dish detail, and cart interactions

**Files:**
- Modify: `frontend/pages/ordering/menu/index.js`
- Modify: `frontend/pages/ordering/menu/index.wxml`
- Modify: `frontend/pages/ordering/menu/index.wxss`
- Modify: `frontend/pages/ordering/dish-detail/index.js`
- Modify: `frontend/pages/ordering/dish-detail/index.wxml`
- Modify: `frontend/pages/ordering/dish-detail/index.wxss`
- Modify: `frontend/pages/ordering/cart/index.js`
- Modify: `frontend/pages/ordering/cart/index.wxml`
- Modify: `frontend/pages/ordering/cart/index.wxss`
- Create: `frontend/tests/shared-cart-pages.test.js`
- Create: `frontend/tests/ordering-responsive-contract.test.js`

- [ ] **Step 1: Write failing page/markup tests**

Assert no breakfast/lunch/dinner controls or native `<button>`, a today-only expected-time picker exists, aggregate dish rows show only total quantity, member attribution is absent until detail expansion, minus affects only `myQuantity`, stale cart errors trigger refresh/confirmation, and submission success reloads a new cart.

- [ ] **Step 2: Run and verify failure**

Run the two new Node test files directly; expected FAIL.

- [ ] **Step 3: Implement menu and dish-detail quantity behavior**

Use absolute target quantities and disable repeat taps while a versioned request is pending. On `CART_CHANGED` refresh and explain; on `CART_SUBMITTED` refresh into the new empty cart without replaying the old action.

- [ ] **Step 4: Implement cart time picker and detail-only attribution**

Main row: dish, total `× N`, total amount, current-member stepper. Expanded child box: read-only member name/quantity and per-member remark. Expected time remains editable until submit and has an explicit ended state.

- [ ] **Step 5: Verify responsive/custom-button rules**

Check static contracts first; then use the WeChat developer tool or an equivalent real renderer to capture menu/cart/detail at 320px, 360px, a standard device, 200% font, landscape, and a bottom-safe-area device. Record screenshots and computed evidence for no horizontal overflow, clipping, overlap, or inaccessible controls. Use wrapping/min-width/box-sizing, 88rpx targets, `role="button"`, `aria-label`, press/disabled states.

- [ ] **Step 6: Run frontend tests and commit**

Commit message: `feat: rebuild mini program shared cart flow`.

## Task 11: Update order, merchant, and wallet screens

**Files:**
- Modify: `frontend/pages/ordering/orders/index.js`
- Modify: `frontend/pages/ordering/orders/index.wxml`
- Modify: `frontend/pages/ordering/order-detail/index.js`
- Modify: `frontend/pages/ordering/order-detail/index.wxml`
- Modify: `frontend/pages/ordering/order-detail/index.wxss`
- Modify: `frontend/pages/merchant/merchant-orders/index.js`
- Modify: `frontend/pages/merchant/merchant-orders/index.wxml`
- Modify: `frontend/pages/merchant/merchant-order-detail/index.js`
- Modify: `frontend/pages/merchant/merchant-order-detail/index.wxml`
- Modify: `frontend/pages/merchant/merchant-order-detail/index.wxss`
- Modify: `frontend/pages/merchant/merchant-family-detail/index.js`
- Modify: `frontend/pages/merchant/merchant-family-detail/index.wxml`
- Modify: `frontend/pages/merchant/merchant-family-detail/index.wxss`
- Modify: `frontend/pages/family/wallet/index.js`
- Modify: `frontend/pages/family/wallet/index.wxml`
- Modify: `frontend/pages/family/wallet-ledger/index.js`
- Modify: `frontend/pages/family/wallet-ledger/index.wxml`
- Modify: `frontend/components/order-row/index.js`
- Modify: `frontend/components/order-row/index.wxml`
- Modify: `frontend/utils/purchase.js`
- Modify: `frontend/utils/purchase-state.js`
- Modify: `backend/src/main/java/com/familykitchen/order/controller/FamilyOrderController.java`
- Modify: `backend/src/main/java/com/familykitchen/order/controller/MerchantOrderController.java`
- Modify: `backend/src/main/java/com/familykitchen/order/mapper/OrderPersistenceMapper.java`
- Modify: `backend/src/main/resources/mapper/order/OrderPersistenceMapper.xml`
- Create: `frontend/tests/family-wallet-pages.test.js`
- Create: `frontend/tests/order-member-selection-details.test.js`
- Create: `backend/src/test/java/com/familykitchen/order/OrderListDetailProjectionContractTest.java`

- [ ] **Step 1: Write failing rendering tests**

Before implementation, assert backend family/merchant list queries and payloads do not select or return `selections`; family/merchant detail payloads do return immutable selection snapshots. Then assert order cards show expected time and aggregate dishes only, detail pages reveal member selection boxes only after explicit expansion, wallet pages say “家庭钱包”, and merchant family detail adjusts family balance with no member recharge control.

- [ ] **Step 2: Run and verify failure**

Expected: existing pages show meal labels/member-wallet routes.

- [ ] **Step 3: Implement order/detail fallback rendering**

Use `expectedMealTime` for new orders. Only when null, display `serviceDate + legacyMealSlotName`. Do not infer a fake meal slot. Preserve order member nickname snapshots.

- [ ] **Step 4: Implement family-wallet pages and merchant adjustment**

Use custom view buttons, request IDs, busy locks, error-preserved forms, pagination, and merchant-family authorization errors. Remove actor/memberId wallet-ledger branching.

- [ ] **Step 5: Run frontend suite and commit**

Commit message: `feat: show family wallet and aggregate order details`.

## Task 12: Documentation, full regression, and live interface verification

**Files:**
- Modify: `docs/frontend-api-guide.md`
- Create: `docs/verification/2026-08-25-family-basket-wallet-verification.md`
- Modify explicitly identified legacy fixtures only after recording their exact paths and proving they encode the retired meal-slot/member-wallet contract; do not bulk-update unrelated snapshots.

- [ ] **Step 1: Update API documentation**

Document cart/version/request IDs, expected time boundaries, aggregate selection response, immutable submitted orders, family-wallet endpoints, idempotency conflict behavior, legacy history fallback, and deprecated personal-wallet/meal-slot routes.

- [ ] **Step 2: Run backend focused suites**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -Dtest=FamilyCartWalletMigrationContractTest,FamilyCartWalletMigrationMySqlTest,FamilyCartWalletMigrationRunnerTest,FamilyCartWalletMigrationRecoveryMySqlTest,FamilyWalletAccountTest,FamilyWalletServiceTest,FamilyWalletConcurrencyMySqlTest,CommandIdempotencyServiceTest,ExpectedMealTimePolicyTest,CartApplicationServiceTest,SharedCartConcurrencyMySqlTest,FamilyMemberCartCleanupTest,FamilyMemberCartCleanupConcurrencyMySqlTest,FamilyOrderControllerContractTest,FamilyOrderSubmissionMySqlTest,OrderSubmissionServiceTest,FamilyWalletOrderLifecycleTest,FamilyWalletOrderLifecycleMySqlTest,FamilyWalletControllerContractTest,MerchantFamilyWalletServiceTest,PersonalWalletCutoverGuardTest,FamilyHomeExpectedTimeContractTest,OrderListDetailProjectionContractTest,PurchaseApplicationServiceTest,FamilyNotificationEventContractTest test`
Expected: PASS with MySQL concurrency/migration tests executed.

- [ ] **Step 3: Run full backend and frontend suites**

```powershell
D:\develop\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml test
D:\develop\WebDev\node\npm.cmd --prefix frontend test
```

Expected: both exit 0. Run `git diff --check`, then execute this reproducible JavaScript syntax check:

```powershell
$changedJs = git diff --name-only --diff-filter=ACMR HEAD -- 'frontend/*.js' 'frontend/**/*.js'
foreach ($file in $changedJs) {
  & 'C:\Users\Q\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --check $file
  if ($LASTEXITCODE -ne 0) { throw "node --check failed: $file" }
}
```

- [ ] **Step 4: Run migration against a disposable clone of the configured MySQL data**

Never test destructive clearing on the only local database. Back up or clone it first, use a database name ending `_family_wallet_disposable`, and build the JAR explicitly:

```powershell
$mvn = 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd'
& $mvn -f backend\pom.xml clean package
if ($LASTEXITCODE -ne 0) { throw 'backend package failed' }

$migrationJar = 'backend\target\family-kitchen-backend-0.1.0-SNAPSHOT.jar'
$cloneUrl = 'jdbc:mysql://127.0.0.1:3306/kitchen_family_wallet_disposable?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$cloneUser = 'kitchen_migration'
$clonePassword = '<read-from-approved-secret-source>'
$safetyToken = '<one-time-token-issued-for-kitchen_family_wallet_disposable>'
$dbArgs = @(
  "--spring.datasource.url=$cloneUrl",
  "--spring.datasource.username=$cloneUser",
  "--spring.datasource.password=$clonePassword",
  '--app.family-wallet-migration.expected-database=kitchen_family_wallet_disposable',
  "--app.family-wallet-migration.safety-token=$safetyToken"
)

& java -jar $migrationJar --spring.main.web-application-type=none @dbArgs --app.family-wallet-migration.mode=PREFLIGHT
if ($LASTEXITCODE -ne 0) { throw 'PREFLIGHT failed' }
$batchId = '<MIGRATION_BATCH_ID printed by PREFLIGHT>'
& java -jar $migrationJar --spring.main.web-application-type=none @dbArgs --app.family-wallet-migration.mode=QUIESCE "--app.family-wallet-migration.batch-id=$batchId"
if ($LASTEXITCODE -ne 0) { throw 'QUIESCE failed' }
$drainEpoch = '<DRAIN_EPOCH printed by QUIESCE after all compatibility instance leases expire>'
& java -jar $migrationJar --spring.main.web-application-type=none @dbArgs --app.family-wallet-migration.mode=PREFLIGHT "--app.family-wallet-migration.batch-id=$batchId" "--app.family-wallet-migration.drain-epoch=$drainEpoch"
if ($LASTEXITCODE -ne 0) { throw 'barrier PREFLIGHT failed' }
& java -jar $migrationJar --spring.main.web-application-type=none @dbArgs --app.family-wallet-migration.mode=EXECUTE "--app.family-wallet-migration.batch-id=$batchId" "--app.family-wallet-migration.drain-epoch=$drainEpoch"
if ($LASTEXITCODE -ne 0) { throw 'EXECUTE failed' }
& java -jar $migrationJar --spring.main.web-application-type=none @dbArgs --app.family-wallet-migration.mode=VERIFY "--app.family-wallet-migration.batch-id=$batchId" "--app.family-wallet-migration.drain-epoch=$drainEpoch"
if ($LASTEXITCODE -ne 0) { throw 'VERIFY failed' }
& java -jar $migrationJar --spring.main.web-application-type=none @dbArgs --app.family-wallet-migration.mode=FINALIZE "--app.family-wallet-migration.batch-id=$batchId" "--app.family-wallet-migration.drain-epoch=$drainEpoch"
if ($LASTEXITCODE -ne 0) { throw 'FINALIZE failed' }
```

Before the first command, print and manually confirm the parsed target host/database; the runner independently rejects a default database, a database without the disposable suffix, or an invalid one-time safety token. Secrets are not written to the verification document. The runner refuses `EXECUTE` unless the write barrier and old-instance drain proof are current. If aborting before any migration source is processed, run `ABORT_BEFORE_EXECUTE` with the same batch/epoch; after any clearing, abort is forbidden. After injected failure, repair and rerun `EXECUTE` with the same batch/epoch, then `VERIFY`; never rerun Flyway as a substitute. Record each phase, exit code, batch/cutover state, redacted target, pre/post member/family available and frozen sums, zero personal balances, holds per unfinished order, cart aggregate/selection sums, and final `uk_carts_active_family`. Any failed conservation check blocks `FINALIZE` and deployment.

- [ ] **Step 5: Restart the backend from the newly built JAR and run authorized smoke tests**

Verify APIs and real mini-program pages: get shared cart; authoritative time availability; set valid/invalid time; two accounts add the same dish; main row hides attribution and detail expansion shows it; submit using family balance; immediate second cart; cancel/release; merchant fee change; family wallet ledger; merchant family wallet adjustment; historical order fallback. Repeat the Task 10 viewport/font/orientation matrix and attach screenshots. Do not mutate production-like data without a disposable family/account.

- [ ] **Step 6: Record evidence and commit docs/tests**

Write exact commands, exit codes, migration conservation totals, endpoint statuses, and any intentionally unrun environment-dependent checks in the verification document. Stage only the docs and fixture/test files owned by this task. Commit message: `test: verify family basket and wallet flow`.

## Final acceptance checklist

- [ ] New code never creates a meal-slot cart/order or charges/freeze a member wallet.
- [ ] Every family has at most one active cart at database level.
- [ ] Every aggregate cart/order quantity equals the sum of positive member selections.
- [ ] Main lists hide member attribution; explicit detail expansion shows it.
- [ ] Expected time is today-only, future, 15-minute aligned, and revalidated on submit.
- [ ] A submitted cart cannot receive a late write; the next cart accepts only a newly confirmed action.
- [ ] Family wallet available/frozen balances never go negative, holds satisfy their invariants, and all money operations are idempotent.
- [ ] Personal wallet available/frozen amounts are zero after guarded migration and all sums are conserved.
- [ ] Historical orders still render their old service date/meal slot without affecting new flows.
- [ ] Backend, frontend, MySQL migration, concurrency, responsive, and authorized interface checks pass with recorded evidence.
- [ ] Testcontainers logs prove MySQL tests executed rather than being skipped; Docker unavailability is recorded as a blocker, not a pass.
