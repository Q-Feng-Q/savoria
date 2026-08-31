# Mini-Program Delivery Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a repeatable delivery audit for all 37 mini-program pages and their backend contracts, fix every reproducible in-scope defect, and produce evidence that the release is stable without touching the current business database.

**Architecture:** Keep production boundaries unchanged. Add a shared test manifest that maps pages, service operations, HTTP requests, backend routes, state requirements, and authorization scopes; validate it from Node tests and Spring tests, then drive minimal TDD fixes from audit failures. Static WXML/WXSS and responsive checks complement a final WeChat Developer Tools compile; database tests are restricted to H2 or disposable Testcontainers databases.

**Tech Stack:** WeChat Mini Program WXML/WXSS/JavaScript, Node.js `node:test`, Spring Boot 3, JUnit 5, MockMvc/`RequestMappingHandlerMapping`, Maven, H2, Testcontainers/MySQL, Git.

---

## Execution Rules

- Work only in `D:\develop\Project\Kitchen\.worktrees\codex\mini-program-delivery-audit` until final integration.
- Use @superpowers:test-driven-development for every defect and @superpowers:systematic-debugging when a test or runtime path fails unexpectedly.
- Never connect tests to the business database. A resolved JDBC URL that is neither H2 memory nor a disposable Testcontainers URL is a hard failure.
- Preserve the baseline: frontend 348 tests; backend 285 tests with 12 environment skips. Counts may increase, never silently decrease.
- Audit findings are inherently discovered at runtime. Before changing a production file not named below, append its exact path, failing test, and root cause to `docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md`; then follow red → green → refactor for that file.
- All manifest-driven suites are parameterized with names containing the exact page/operation/identity/resource tuple. Treat each failing tuple as a separate finding: preserve one focused red case, make the minimal fix, run that case and its adjacent domain suite green, then commit before moving to a different root cause. Do not repair a large anonymous batch of failures.
- Do not redesign pages, add unrelated features, use native WeChat `button`, or alter the family-wallet/shared-cart/current-day expected-time architecture.

## File Structure

**Create**

- `frontend/tests/fixtures/delivery-audit-manifest.js` — single source of truth for the 37 pages, 11 service modules, service call fixtures, state requirements, and tenant scope.
- `frontend/tests/fixtures/delivery-baseline.json` — immutable baseline test-case identities and skip states, not just aggregate counts.
- `frontend/tests/delivery-audit-inventory.test.js` — proves manifest, `app.json`, page directories, page files, and service exports remain synchronized.
- `frontend/tests/delivery-api-contract.test.js` — invokes every manifest service operation and verifies final HTTP method, path, query encoding, headers/body shape, and response unwrapping.
- `frontend/tests/delivery-page-behavior.test.js` — verifies lifecycle refresh, identity isolation, loading/empty/error states, guarded mutations, and null-safe rendering contracts.
- `frontend/tests/delivery-responsive-compile.test.js` — verifies WXML/WXSS coverage, prohibited syntax/native buttons, touch targets, responsive/safe-area contracts, and stress-content rules.
- `backend/src/test/resources/contracts/mini-program-api-contract.json` — normalized frontend HTTP contract consumed by backend route tests.
- `backend/src/test/java/com/familykitchen/contract/MiniProgramEndpointContractTest.java` — validates every frontend method/path against the live Spring MVC route registry and representative request binding.
- `backend/src/test/java/com/familykitchen/contract/MiniProgramResourceAuthorizationTest.java` — verifies own/cross-family and own/cross-merchant access rules and stable non-leaking failures.
- `backend/src/test/java/com/familykitchen/database/TestDatabaseIsolationContractTest.java` — prevents tests from starting against a non-test JDBC target.
- `backend/src/test/java/com/familykitchen/testsupport/TestDatabaseUrlGuard.java` — pure pre-connect allowlist for H2 memory and the current disposable container.
- `backend/src/test/java/com/familykitchen/testsupport/TestDatabaseEnvironmentPostProcessor.java` — invokes the guard before datasource/Flyway beans initialize.
- `backend/src/test/java/com/familykitchen/testsupport/SafeTestFlyway.java` — guarded factory for tests that configure Flyway directly.
- `backend/src/test/resources/META-INF/spring.factories` — registers the test-only environment post-processor.
- `frontend/scripts/summarize-delivery-audit.js` — derives verification totals and open gates from machine-readable reports.
- `docs/superpowers/audits/2026-08-31-responsive-matrix.json` — 37 pages × 4 viewports × 2 safe-area values with stress-data evidence.
- `docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md` — checked audit record with finding, failing test, root cause, fix, and verification evidence.
- `docs/superpowers/audits/2026-08-31-mini-program-delivery-verification.md` — final counts, skipped tests, compiler/device matrix, and remaining environmental gates.

**Modify when the new tests prove a defect**

- `frontend/services/*.js` and `frontend/utils/api.js` — request contract defects only.
- `frontend/pages/**/index.js`, `index.wxml`, `index.wxss`, `index.json` — page lifecycle/state/template/layout defects only.
- `frontend/components/**` and `frontend/styles/**` — shared UI defects only when at least two pages share the same root cause.
- Corresponding `backend/src/main/java/com/familykitchen/**` controller/application-service/mapper files — missing route, request binding, authorization, pagination, or freshness defects only.
- Corresponding `backend/src/test/java/com/familykitchen/**` domain tests — regression coverage adjacent to each backend fix.

### Task 1: Lock the Delivery Inventory and Baseline

**Files:**
- Create: `frontend/tests/fixtures/delivery-audit-manifest.js`
- Create: `frontend/tests/fixtures/delivery-baseline.json`
- Create: `frontend/tests/delivery-audit-inventory.test.js`
- Create: `docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md`
- Test: `frontend/tests/delivery-audit-inventory.test.js`

- [ ] **Step 1: Write the failing inventory test**

Create a test that derives registered pages from `frontend/app.json`, implemented page directories from `pages/**/index.js`, and service modules from `services/*.js`. Assert exact equality with the spec appendix, assert all page quartets (`.js`, `.json`, `.wxml`, `.wxss`) exist, and assert every manifest page declares its business domain plus applicable `async`, `list`, `form`, `refresh`, `tenantScope`, and explicit `operations` keys. Each operation reference must resolve to one service contract containing `factory`, `function`, `method`, `route`, `controllerClass`, `controllerMethod`, and `scope`.

```js
test('delivery manifest covers every registered and implemented page', () => {
  assert.deepEqual([...registeredPages].sort(), [...manifest.pages.keys()].sort())
  assert.deepEqual([...implementedPages].sort(), [...registeredPages].sort())
})

test('delivery manifest covers every service module', () => {
  assert.deepEqual(serviceFiles, manifest.serviceModules)
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `D:\develop\WebDev\node\node.exe --test tests/delivery-audit-inventory.test.js`

Working directory: `frontend`

Expected: FAIL because `delivery-audit-manifest.js` is missing or incomplete.

- [ ] **Step 3: Add the complete manifest**

Populate all 37 paths and 11 modules exactly as listed in the approved spec. Add page metadata without changing production code. Include a stable manifest version and baseline counts:

```js
module.exports = Object.freeze({
  version: 1,
  baseline: { frontendTests: 348, backendTests: 285, backendSkipped: 12 },
  serviceModules: ['_shared.js', 'auth.js', 'cart.js', 'family.js', 'files.js',
    'merchant.js', 'notifications.js', 'orders.js', 'purchase.js', 'system.js', 'user.js'],
  pages: new Map([
    ['pages/auth/entry/index', { domain: 'auth', async: true, form: true, refresh: false,
      tenantScope: 'public', operations: ['auth.portalLogin', 'auth.wechatLogin'] }],
    // Continue with every page in Appendix A; no wildcard or count-only entries.
  ])
})
```

Define the backend route boundary in the same manifest: all non-admin handlers under `/api/auth/**`, `/api/users/me/**`, `/api/family/**`, `/api/merchant/**`, `/api/notifications/**`, `/api/files/**`, and `/api/public/system-settings`; explicitly exclude `/api/admin/**` and list any other exclusion with a reason. This boundary is used for reverse comparison so an unregistered mini-program backend handler fails the audit.

Capture baseline test identities, not only totals: Node TAP full test names plus status, and Maven Surefire `testsuite/testcase` class/name plus skipped status. Store the sorted sets in `delivery-baseline.json`. The final audit compares set differences; a removed, renamed, skipped, or disabled baseline case requires an explicit finding and user-visible explanation.

Initialize the findings document with columns: ID, severity, domain, reproduction, failing test, root cause, changed files, verification, status.

- [ ] **Step 4: Run the inventory and full frontend tests**

Run: `D:\develop\WebDev\node\node.exe --test tests/delivery-audit-inventory.test.js`

Expected: PASS.

Run: `D:\develop\WebDev\node\npm.cmd test`

Expected: at least 348 tests, 0 failures, no unexplained skip/disabled reduction.

- [ ] **Step 5: Commit**

```powershell
git add frontend/tests/fixtures/delivery-audit-manifest.js frontend/tests/fixtures/delivery-baseline.json frontend/tests/delivery-audit-inventory.test.js
git add -f docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md
git commit -m "test: lock mini program delivery inventory"
```

### Task 2: Enforce Frontend-to-Backend HTTP Contracts

**Files:**
- Modify: `frontend/tests/fixtures/delivery-audit-manifest.js`
- Create: `frontend/tests/delivery-api-contract.test.js`
- Create: `backend/src/test/resources/contracts/mini-program-api-contract.json`
- Create: `backend/src/test/java/com/familykitchen/contract/MiniProgramEndpointContractTest.java`
- Test: `frontend/tests/delivery-api-contract.test.js`
- Test: `backend/src/test/java/com/familykitchen/contract/MiniProgramEndpointContractTest.java`

- [ ] **Step 1: Inventory every service operation**

For each factory export in the 11 service modules, add an invocation fixture and expected normalized request. Dynamic identifiers use `{id}` in the backend JSON and concrete sentinel values in Node tests. Every page's `operations` array must resolve to these contracts, and every contract records the exact Spring controller class/method.

```js
{
  key: 'merchant.advanceOrderStatus',
  create: 'createMerchantService',
  invoke: (service) => service.advanceOrderStatus(91, { status: 'PREPARING' }),
  expected: {
    method: 'POST',
    path: '/api/merchant/orders/91/status',
    route: '/api/merchant/orders/{id}/status',
    controllerClass: 'MerchantOrderController',
    controllerMethod: 'advanceStatus',
    data: { status: 'PREPARING' },
    scope: 'merchant'
  }
}
```

Assert that every public function returned by each service factory has exactly one fixture. Upload functions use a dedicated upload fixture and are not silently omitted.

- [ ] **Step 2: Write and run the failing frontend contract test**

Capture calls through injected `request`/`upload`, then assert method, path, encoded query, omitted `undefined`, JSON body, and response unwrapping. Add negative fixtures for missing IDs, invalid dates/enums, and `null` query values. Parameterized test names must include the operation key so each failure is independently actionable.

Run: `D:\develop\WebDev\node\node.exe --test tests/delivery-api-contract.test.js`

Expected: FAIL on the first unlisted operation or request mismatch.

- [ ] **Step 3: Normalize the backend contract fixture**

Create JSON entries with `operation`, `pages`, `method`, `route`, `controllerClass`, `controllerMethod`, `scope`, `validRequest`, `invalidRequests`, and `successStatus`. Every invalid request contains its own `expectedStatus`, `expectedErrorCode`, `expectedEnvelope`, and `mustNotContain` values. Generate the values from the reviewed manifest, but check in deterministic JSON so Java tests do not execute frontend code.

- [ ] **Step 4: Write and run the failing Spring route test**

Use an MVC slice (`@WebMvcTest` plus mocked application services) so this test cannot create a datasource. Normalize `RequestMappingHandlerMapping` routes and assert every JSON method/route pair exists exactly once and maps to the specified controller class/method. Parameterized MockMvc executes `validRequest` for every operation, not a representative subset. It also executes every operation's invalid requests and asserts their exact status, application error code/envelope, and forbidden internal text. Dynamic IDs cover a legal value, malformed value, and missing path shape where applicable; authorization failure and parameter-binding failure are separate cases.

Reverse-compare all Spring handlers inside the manifest's mini-program route boundary against the JSON contract. Unregistered handlers fail unless they appear in the explicit exclusion list with a reason; `/api/admin/**` remains outside scope.

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -Dtest=MiniProgramEndpointContractTest test`

Working directory: `backend`

Expected: FAIL on missing/mismatched routes or binding.

- [ ] **Step 5: Fix each proven contract defect minimally**

For each failure, first append a finding. Modify only the implicated `frontend/services/<domain>.js`, request DTO/controller, or request adapter. Do not add compatibility aliases unless an existing released caller needs one. Re-run the exact failing case after each edit.

- [ ] **Step 6: Run both contract suites and commit**

Expected: both targeted suites PASS; no unlisted service operation.

```powershell
git add frontend/tests/fixtures/delivery-audit-manifest.js frontend/tests/delivery-api-contract.test.js frontend/services frontend/utils/api.js backend/src/test/resources/contracts backend/src/test/java/com/familykitchen/contract backend/src/main/java docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md
git commit -m "test: enforce mini program api parity"
```

### Task 3: Verify Pagination, Refresh, and Identity Isolation

**Files:**
- Create: `frontend/tests/delivery-page-behavior.test.js`
- Modify: `frontend/tests/fixtures/delivery-audit-manifest.js`
- Modify as proven: `frontend/pages/**/index.js`
- Modify as proven: `frontend/services/*.js`
- Test: `frontend/tests/delivery-page-behavior.test.js`
- Test: existing domain tests under `frontend/tests/`

- [ ] **Step 1: Add failing pagination scenarios**

Model at least 2.5 pages of results for system dish import, family-menu candidates, merchant dishes/families/orders, family orders, notifications, and any member selector. Assert the loader requests subsequent pages until a short/empty page, preserves stable order, deduplicates by ID, and exposes all records to selection.

```js
assert.deepEqual(result.map(({ id }) => id), expectedIds)
assert.deepEqual(requestedPages, [1, 2, 3])
```

Run the targeted test and expect failure for any first-page-only implementation.

- [ ] **Step 2: Add failing lifecycle refresh scenarios**

For each manifest page marked `refresh`, load its module through the existing `Page` harness and assert `onShow` triggers a fresh request after returning from add/edit/detail. Cover create dish → merchant dish list/menu candidates, edit profile → profile/workbench, menu save → ordering menu, recommendation/status change → home/menu/list, and order status mutation → both order list/detail.

- [ ] **Step 3: Add failing identity-switch scenarios**

Seed family/merchant page state for account A, switch to account B using the existing session harness, and assert identity-scoped cached data is cleared before B's request resolves. Verify unavailable merchant mode disappears immediately and stale family/cart/order data is not rendered.

- [ ] **Step 4: Implement minimal fixes**

Prefer existing page `load()` methods from `onShow`, request-scoped tokens, and shared pagination helpers already present in the relevant service. Do not add a global state library. Record every exact production path before editing.

- [ ] **Step 5: Run targeted and full frontend tests**

Run: `D:\develop\WebDev\node\node.exe --test tests/delivery-page-behavior.test.js`

Expected: PASS.

Run: `D:\develop\WebDev\node\npm.cmd test`

Expected: baseline 348 plus new tests, 0 failures.

- [ ] **Step 6: Commit**

```powershell
git add frontend/tests/delivery-page-behavior.test.js frontend/tests/fixtures/delivery-audit-manifest.js frontend/pages frontend/services
git add -f docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md
git commit -m "fix: keep mini program data complete and fresh"
```

### Task 4: Enforce Tenant Authorization and Test Database Isolation

**Files:**
- Create: `backend/src/test/java/com/familykitchen/contract/MiniProgramResourceAuthorizationTest.java`
- Create: `backend/src/test/java/com/familykitchen/database/TestDatabaseIsolationContractTest.java`
- Create: `backend/src/test/java/com/familykitchen/testsupport/TestDatabaseUrlGuard.java`
- Create: `backend/src/test/java/com/familykitchen/testsupport/TestDatabaseEnvironmentPostProcessor.java`
- Create: `backend/src/test/java/com/familykitchen/testsupport/SafeTestFlyway.java`
- Create: `backend/src/test/resources/META-INF/spring.factories`
- Modify: `backend/src/test/java/com/familykitchen/database/DishTemplateChangeMigrationMySqlTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationMySqlTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/FamilyCartWalletMigrationRecoveryMySqlTest.java`
- Modify: `backend/src/test/java/com/familykitchen/database/MerchantFeaturedDishMigrationMySqlTest.java`
- Modify as proven: relevant `backend/src/main/java/com/familykitchen/**/service/impl/*.java`
- Modify as proven: relevant `backend/src/main/resources/mapper/**/*.xml`
- Test: existing family, merchant, cart, wallet, and order tests.

- [ ] **Step 1: Write the failing authorization matrix**

Cover family member/admin/merchant admin against own family, another family under the same merchant, and another merchant. Test read and mutation endpoints for family detail/menu/cart/wallet/order, merchant family/menu/wallet/order/dish resources, forged IDs, and expired/switched identity context. Assert no cross-tenant data or existence signal leaks. Use parameterized names containing identity, resource type, ownership, and operation; create one finding and one minimal red/green loop per failing tuple rather than repairing the whole matrix at once.

```java
assertDenied(() -> service.getFamily(otherMerchantFamilyId), ErrorCode.RESOURCE_NOT_FOUND);
verify(mapper, never()).update(any());
```

- [ ] **Step 2: Run the matrix and confirm failures are real**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd -Dtest=MiniProgramResourceAuthorizationTest test`

Expected: FAIL only where a service trusts an unvalidated resource ID or stale context.

- [ ] **Step 3: Add a pre-connect database isolation guard**

Implement `TestDatabaseUrlGuard.requireSafe(url, profile, containerUrl, username)` as a pure function. Permit only `jdbc:h2:mem:` or the exact JDBC URL exposed by the currently owned Testcontainers instance, with a randomized database name prefixed `family_kitchen_test_`, the dedicated `test-container` profile, and container-provided credentials. Reject blank/unknown profiles, fixed MySQL hosts, the application database name, and any URL merely containing the word `test`.

Register `TestDatabaseEnvironmentPostProcessor` in test resources so it validates `spring.datasource.url` before datasource or Flyway beans initialize. Add a negative unit test that supplies the business JDBC URL and proves rejection occurs without constructing a DataSource. MVC contract tests remain `@WebMvcTest` and therefore never create a datasource.

Replace direct `Flyway.configure()` calls in the four named migration tests with `SafeTestFlyway.configure(container)`, which invokes the same guard before returning a Flyway fluent configuration. Other Testcontainers Spring tests register only the exact live container URL through `@DynamicPropertySource`; the isolation contract source-scans those registrations and rejects fixed external URLs.

- [ ] **Step 4: Apply minimal authorization fixes**

Resolve resources through merchant/family-scoped mapper methods before reads or writes. Avoid post-query filtering and avoid returning different errors for “exists but forbidden” versus “not found” where that leaks tenant existence.

- [ ] **Step 5: Run domain and full backend tests**

Run the two new tests, then:

`D:\develop\apache-maven-3.9.9\bin\mvn.cmd test`

Expected: at least 285 tests plus new tests, 0 failures; skips explicitly listed. If critical cart/wallet/order MySQL tests remain skipped, mark the environment gate open in the verification document.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/test/java/com/familykitchen/contract backend/src/test/java/com/familykitchen/database backend/src/test/java/com/familykitchen/testsupport backend/src/test/resources/META-INF backend/src/main/java backend/src/main/resources/mapper
git add -f docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md
git commit -m "fix: enforce mini program resource boundaries"
```

### Task 5: Audit All Page States and Null-Safe Presentation

**Files:**
- Modify: `frontend/tests/delivery-page-behavior.test.js`
- Modify: `frontend/tests/fixtures/delivery-audit-manifest.js`
- Modify as proven: `frontend/pages/**/index.js`
- Modify as proven: `frontend/pages/**/index.wxml`
- Modify as proven: `frontend/components/page-state/**`

- [ ] **Step 1: Add manifest-driven failing state tests**

For every `async` page, require a loading state and a retryable error state. For every `list` page, require a distinct empty state. For every `form` or mutation page, require pending/disabled protection and failure recovery. Split null safety into executable layers: inject representative null payloads into each Page/view-model harness and assert normalized `setData` display fields never contain literal `null`, `undefined`, `NaN`, raw enum codes, or unlabeled internal IDs; separately assert WXML binds only normalized display fields or provides an explicit fallback. Retired personal-wallet/meal-slot copy is prohibited by static contract. Final rendered appearance belongs to the executed responsive matrix and remains an open release gate until performed.

- [ ] **Step 2: Verify the audit fails on concrete omissions**

Run: `D:\develop\WebDev\node\node.exe --test tests/delivery-page-behavior.test.js`

Expected: FAIL with page path and missing state/unsafe value, not a generic count error.

- [ ] **Step 3: Fix page state defects**

Use `page-state` for asynchronous loading/error/empty branches and existing formatting utilities for nullable values/enums. Do not convert request errors into empty lists. Keep user-visible messages actionable and domain-specific.

- [ ] **Step 4: Run affected domain tests and full frontend tests**

Expected: all pass with no reduction from baseline.

- [ ] **Step 5: Commit**

```powershell
git add frontend/tests/delivery-page-behavior.test.js frontend/tests/fixtures/delivery-audit-manifest.js frontend/pages frontend/components/page-state
git add -f docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md
git commit -m "fix: complete mini program page states"
```

### Task 6: Audit WXML/WXSS, Custom Buttons, and Responsive Safety

**Files:**
- Create: `frontend/tests/delivery-responsive-compile.test.js`
- Create: `docs/superpowers/audits/2026-08-31-responsive-matrix.json`
- Modify as proven: `frontend/pages/**/*.wxml`
- Modify as proven: `frontend/pages/**/*.wxss`
- Modify as proven: `frontend/components/action-button/**`
- Modify as proven: `frontend/components/bottom-action-bar/**`
- Modify as proven: `frontend/styles/warm-kitchen-*.wxss`
- Test: existing `wxss-compiler-compat.test.js`, `wxml-expression-contract.test.js`, `native-button-free-contract.test.js`, responsive tests.

- [ ] **Step 1: Write the all-files compile-safety audit**

Derive every WXML/WXSS path from the 37-page manifest. Assert no orphan/missing template, invalid `wx:else` adjacency, unsupported selector/token patterns, unbalanced blocks, raw business `<button>`, or inline expression patterns rejected by current tests. The failure must name the exact file and rule.

- [ ] **Step 2: Add custom-button and stress-layout contracts**

Assert action controls use the custom component, have an accessible label, disabled/pending behavior, and an 88rpx/44px-equivalent tap target. Static CSS checks cover shrink-safe grids, wrapping, scroll clearance, and overflow prevention but are not accepted as rendered-layout proof. Generate a 296-row matrix (37 pages × 4 viewports × 2 safe-area values) with stress cases for doubled text, maximum phone/amount, missing image, and failed image fallback. Every row records status, date, tool/device version, evidence reference, horizontal overflow, clipping, primary-action visibility/clickability, and safe-area clearance.

- [ ] **Step 3: Run the focused tests and observe failures**

Run:

`D:\develop\WebDev\node\node.exe --test tests/delivery-responsive-compile.test.js tests/wxss-compiler-compat.test.js tests/wxml-expression-contract.test.js tests/native-button-free-contract.test.js tests/warm-animal-responsive-matrix.test.js`

Expected: FAIL only on concrete file/rule violations.

- [ ] **Step 4: Fix concrete template/style defects minimally**

Prefer page-local fixes; move a rule to shared components/styles only when two or more pages need the same correction. Preserve the approved warm visual language and do not add large new illustrations.

- [ ] **Step 5: Compile with WeChat Developer Tools**

Use the installed current Developer Tools project at `frontend/project.config.json`. Trigger a full compile (CLI if configured, otherwise GUI), record tool/library versions, and require zero WXML/WXSS errors across 37 pages. Then execute all 296 responsive-matrix rows in Developer Tools or real devices and attach the recorded result/evidence. An unexecuted row is `OPEN`, never `PASS`; unavailable tools leave compiler and layout release gates open rather than allowing a delivery claim.

- [ ] **Step 6: Run full frontend tests and commit**

Expected: baseline plus new tests, 0 failures.

```powershell
git add frontend/tests/delivery-responsive-compile.test.js frontend/pages frontend/components frontend/styles
git add -f docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md docs/superpowers/audits/2026-08-31-responsive-matrix.json
git commit -m "fix: harden mini program presentation"
```

### Task 7: Validate Core Journeys and Close All Findings

**Files:**
- Modify: `docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md`
- Create: `docs/superpowers/audits/2026-08-31-mini-program-delivery-verification.md`
- Create: `frontend/scripts/summarize-delivery-audit.js`
- Modify as proven: only exact production/test files linked from an open finding.

- [ ] **Step 1: Run the full frontend suite**

Run: `D:\develop\WebDev\node\npm.cmd test`

Expected: at least 348 baseline tests plus all new tests; 0 failures. Parse the report and compare exact test-name/status sets with `delivery-baseline.json`; every removed, renamed, newly skipped, or disabled baseline test is a blocking unexplained delta until recorded and approved.

- [ ] **Step 2: Run the full backend suite**

Run: `D:\develop\apache-maven-3.9.9\bin\mvn.cmd test`

Expected: at least 285 baseline tests plus all new tests; 0 failures. Parse Surefire XML and compare exact class/name/status sets with `delivery-baseline.json`; record each skip-set change and whether critical MySQL gates executed.

- [ ] **Step 3: Exercise the core journey matrix**

Verify with test harnesses and available local runtime: login/register/recovery; account/role switch; family create/join/edit/member transfer/address; merchant profile/family/menu/dish/import/recommendation; ordering category/full list/detail/shared cart/expected time; family wallet/order submission; merchant order transition/purchase. Every journey must link to a passing automated test or an already executed manual record containing date, environment/tool version, exact steps, result, and evidence reference. A named but unexecuted manual check remains an open release gate and cannot close a journey.

- [ ] **Step 4: Resolve or explicitly gate every finding**

No P0/P1/P2 finding may remain open. P3 can remain only with user-visible justification and must not contradict the approved visual system. Critical Docker/MySQL, WeChat compiler, journey, or responsive-matrix checks that cannot run are release gates, not “passed” items. Run `frontend/scripts/summarize-delivery-audit.js` to derive totals and open gates from TAP/Surefire/matrix data; the verification Markdown must embed this generated summary rather than rely on free-text assertions.

- [ ] **Step 5: Request independent code review**

Use @superpowers:requesting-code-review with the approved spec, this plan, commit range `38bf3d2..HEAD`, test evidence, and findings log. Fix accepted review issues using TDD, then rerun affected and full suites.

- [ ] **Step 6: Commit verification artifacts**

```powershell
git add frontend/scripts/summarize-delivery-audit.js
git add -f docs/superpowers/audits/2026-08-31-mini-program-delivery-findings.md docs/superpowers/audits/2026-08-31-mini-program-delivery-verification.md docs/superpowers/audits/2026-08-31-responsive-matrix.json
git commit -m "docs: record mini program delivery verification"
```

### Task 8: Integrate Without Overwriting User Work

**Files:**
- Verify only: main worktree user changes in cart mapper/service/XML/test.
- Integrate: commits from `codex/mini-program-delivery-audit`.

- [ ] **Step 1: Verify the audit worktree is clean**

Run: `git status --short`

Expected: no uncommitted audit changes.

- [ ] **Step 2: Fingerprint main worktree user changes**

In `D:\develop\Project\Kitchen`, record `git status --short`, save `git diff --binary --output=<temporary-absolute-path>` for the existing cart files, and record SHA-256 hashes of both the patch and each modified working-tree file. Keep the backup outside the repository in a dedicated temporary directory. Compare the audit commit path list against this user-modified path list before integration.

- [ ] **Step 3: Integrate commits safely**

Use a non-destructive merge or ordered cherry-picks only when the path sets do not overlap. If any audit commit overlaps a user-modified cart path, stop integration and request the user's decision; do not automatically resolve it. Never reset, checkout, stash, or discard the user's version.

- [ ] **Step 4: Re-run full verification in the integrated main worktree**

Recompute the user patch and working-file hashes first and compare them with the pre-integration fingerprint; record the result in the verification summary. Then run frontend and backend full suites again. Expected: 0 failures, exact baseline identities retained, and counts no lower than the audit worktree. Report any environment-gated checks separately.

- [ ] **Step 5: Finish branch workflow**

Use @superpowers:finishing-a-development-branch only after all available verification is green and integration safety is confirmed.
