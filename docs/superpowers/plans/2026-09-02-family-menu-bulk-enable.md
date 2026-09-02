# Family Menu Bulk Enable Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add select-all, selected-item bulk enable, and one-click enable-all controls to the Mini Program merchant family-menu page without allowing concurrent full-draft saves.

**Architecture:** Keep the existing full-draft `saveFamilyMenu` API and make the page's `menuDraft` the only mutation source. Add selection as UI-only state and replace row/global split busy handling with one page-wide mutation guard so every save, copy, price change, and status change is serialized.

**Tech Stack:** WeChat Mini Program WXML/WXSS/CommonJS, Node `node:test` contract tests.

---

### Task 1: Specify selection and payload behavior with failing tests

**Files:**
- Create: `frontend/tests/family-menu-bulk-enable.test.js`
- Modify: `frontend/tests/custom-button-disabled-behavior.test.js`
- Modify: `frontend/tests/merchant-state-coverage.test.js`
- Modify: `frontend/tests/merchant-page-family-contract.test.js`
- Reference: `frontend/tests/merchant-page-family-contract.test.js`
- Reference: `frontend/pages/merchant/family-menu/index.js`

- [ ] **Step 1: Add a page test harness**

Capture the object passed to `Page`, stub `createApiRuntime`, `requireSession`, `wx.showToast`, and implement synchronous `setData`. Load two menu rows, including one row whose `familyFinalPrice` is numeric zero.

- [ ] **Step 2: Add named failing selection tests**

Add `empty menu never reports all selected`, `numeric and string dish ids share one selection key`, `select all toggles only current draft`, and `accepted reload clears selection`. Assert `deriveSelection([], {})` returns `{ selectedCount: 0, allSelected: false }` and both `4`/`"4"` address key `"4"`.

- [ ] **Step 3: Add an exact failing payload snapshot**

Invoke `enableSelected` with source rows `[{dishId:4,enabled:false,sortOrder:9,familyFinalPrice:0},{dishId:7,enabled:false,sortOrder:2,familyFinalPrice:18}]`. Assert the sole save snapshot is identical except dish 4 has `enabled:true`; assert the source deep snapshot, `page.menuDraft`, and visible `menuRows` are unchanged before refresh.

- [ ] **Step 4: Add named success, no-op, and rollback tests**

Add `empty selection sends no request`, `enable all no-ops when all enabled`, `successful bulk save performs one refresh and then clears selection`, and `rejected bulk save performs no refresh and preserves deep snapshots`. During the deferred successful refresh, assert `mutationBusy === true` and selection still exists; after the accepted refresh assert it is false/empty.

Add `enable all submits one exact full draft`: with mixed enabled rows and a zero family price, assert one save uses the resolved family ID, enables every row, preserves length/order/IDs/prices/sort, and performs exactly one refresh.

Add separate lifecycle tests `retryLoad clears selection after accepted response` and `non-busy bindFamily clears selection and loads selected family`; keep the busy-switch drop assertion in the concurrency test.

- [ ] **Step 5: Add a failing drop-while-busy concurrency test**

Hold the first save on a deferred Promise, then call a row toggle, price change, copy, family switch, and second bulk action. Assert each call is dropped immediately and no second request/navigation occurs. Resolve save and refresh, then invoke a fresh mutation and assert it succeeds, proving lock release.

- [ ] **Step 6: Replace stale busy-state contracts before production code**

Change JavaScript-only family-menu assertions in `merchant-state-coverage.test.js` and `merchant-page-family-contract.test.js` from `saving/rowBusyMap/rowBusyId` to one `mutationBusy` handler guard. Defer WXML/ARIA assertions, including `custom-button-disabled-behavior.test.js`, to Task 3 so Task 2 can reach GREEN without premature markup expectations.

- [ ] **Step 7: Run the focused tests and verify RED**

Run:

`cd frontend; node --test tests/family-menu-bulk-enable.test.js tests/merchant-state-coverage.test.js tests/merchant-page-family-contract.test.js`

Expected: FAIL because selection and bulk handlers do not exist and the current row/global busy model allows overlapping writes.

- [ ] **Step 8: Commit tests**

`git add frontend/tests/family-menu-bulk-enable.test.js frontend/tests/merchant-state-coverage.test.js frontend/tests/merchant-page-family-contract.test.js && git commit -m "test: cover family menu bulk enable"`

### Task 2: Implement page state and serialized mutations

**Files:**
- Modify: `frontend/pages/merchant/family-menu/index.js`
- Test: `frontend/tests/family-menu-bulk-enable.test.js`

- [ ] **Step 1: Add concrete UI-only selection helpers**

Add `selectedDishMap: {}`, `selectedCount: 0`, `allSelected: false`, and `mutationBusy: false` to `data`. Implement `dishKey(id) { return String(id); }` and `deriveSelection(draft,map)`; `allSelected` is `draft.length > 0 && selectedCount === draft.length`.

- [ ] **Step 2: Clear selection at accepted lifecycle boundaries**

Reset selection only when an accepted load result is applied and immediately before an accepted family switch. `bindFamily` must return unchanged while `mutationBusy`; after release it clears selection, updates family ID, and loads. Do not clear selection on mutation failure.

- [ ] **Step 3: Implement selection handlers**

`toggleSelection(event)` toggles one existing dish unless busy. `toggleSelectAll()` selects every current draft item when not all selected and clears all when already selected.

- [ ] **Step 4: Replace split write guards and define release**

Make `runSaving` return immediately when `mutationBusy` is true, otherwise set it before invoking the task and clear it in `finally` after save plus refresh completes. Use this guard for copy, row toggle, price change, selected bulk enable, enable-all, and family switching. Remove `saving`, `rowBusyMap`, and `rowBusyId` rather than keeping two concurrency models.

- [ ] **Step 5: Implement non-optimistic bulk actions**

`enableSelected()` and `enableAll()` clone the full draft and call `saveMenuDraft(nextDraft)` directly without assigning `this.menuDraft = nextDraft`. The lock remains held through exactly one `load({silent:true})`; that accepted load becomes the only place that updates rows/draft and clears selection. A rejected save performs no load. No-op paths show a concise toast and do not call the API.

- [ ] **Step 6: Run focused tests and verify GREEN**

`cd frontend; node --test tests/family-menu-bulk-enable.test.js`

Expected: PASS.

- [ ] **Step 7: Run existing family-menu tests**

`cd frontend; node --test tests/family-menu-bulk-enable.test.js tests/merchant-page-family-contract.test.js tests/merchant-state-coverage.test.js`

Expected: PASS.

- [ ] **Step 8: Commit implementation**

`git add frontend/pages/merchant/family-menu/index.js frontend/tests/family-menu-bulk-enable.test.js && git commit -m "feat: bulk enable family menu dishes"`

### Task 3: Add accessible responsive controls

**Files:**
- Modify: `frontend/pages/merchant/family-menu/index.wxml`
- Modify: `frontend/pages/merchant/family-menu/index.wxss`
- Modify: `frontend/tests/merchant-responsive-contract.test.js`
- Modify: `frontend/tests/native-button-free-contract.test.js`
- Modify: `frontend/tests/custom-button-disabled-behavior.test.js`
- Modify: `frontend/tests/merchant-state-coverage.test.js`
- Modify: `frontend/tests/merchant-page-family-contract.test.js`
- Test: `frontend/tests/family-menu-bulk-enable.test.js`

- [ ] **Step 1: Add failing markup contracts**

Update `custom-button-disabled-behavior.test.js`, the family-menu WXML assertion in `merchant-state-coverage.test.js`, and markup assertions in `merchant-page-family-contract.test.js` from obsolete row-local busy state to `mutationBusy`. Assert the page has custom `view` controls for select-all, per-row selection, “批量启用（N）”, and “一键启用全部”. Selection controls use `aria-role="checkbox"`/`aria-checked` and are disabled/busy under `mutationBusy`; bulk enable is disabled when `mutationBusy || selectedCount === 0`. Every handler also guards because ARIA/classes do not suppress `bindtap`.

- [ ] **Step 2: Add failing responsive contracts**

Assert `.bulk-toolbar { display:flex; flex-wrap:wrap; min-width:0 }`, child actions use `min-width:0` and may grow/shrink, checkbox touch targets are at least `88rpx`, and a new `@media (max-width: 320px)` stacks the bulk action group without overflow.

- [ ] **Step 3: Run contracts and verify RED**

`cd frontend; node --test tests/family-menu-bulk-enable.test.js tests/merchant-responsive-contract.test.js tests/native-button-free-contract.test.js tests/custom-button-disabled-behavior.test.js tests/merchant-state-coverage.test.js tests/merchant-page-family-contract.test.js`

Expected: FAIL before markup/style changes.

- [ ] **Step 4: Implement WXML controls**

Insert the bulk toolbar above `menu-list`, add row checkboxes, and bind every write control's class, `aria-disabled`, and `aria-busy` to `mutationBusy`. Do not add native `button` elements.

- [ ] **Step 5: Implement responsive WXSS**

Add warm-theme bulk toolbar, selected checkbox, disabled/busy, wrapping, and narrow-screen rules. Preserve existing row hierarchy and price controls.

- [ ] **Step 6: Run focused contracts and verify GREEN**

`cd frontend; node --test tests/family-menu-bulk-enable.test.js tests/merchant-responsive-contract.test.js tests/native-button-free-contract.test.js tests/custom-button-disabled-behavior.test.js tests/merchant-state-coverage.test.js tests/merchant-page-family-contract.test.js`

Expected: PASS.

- [ ] **Step 7: Run complete frontend suite**

`cd frontend; npm test`

Expected: PASS with zero failures.

- [ ] **Step 8: Commit UI**

`git add frontend/pages/merchant/family-menu/index.wxml frontend/pages/merchant/family-menu/index.wxss frontend/tests/merchant-responsive-contract.test.js frontend/tests/native-button-free-contract.test.js frontend/tests/custom-button-disabled-behavior.test.js frontend/tests/merchant-state-coverage.test.js frontend/tests/merchant-page-family-contract.test.js frontend/tests/family-menu-bulk-enable.test.js && git commit -m "feat: add family menu bulk controls"`
