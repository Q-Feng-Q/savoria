# Personal Notebook Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a private, account-owned notebook in the WeChat mini program with versioned event templates, records, time-scoped collaboration, JSON export, a fifth native tab, and a platform-wide 1–36 month query limit.

**Architecture:** A `com.familykitchen.notebook` backend domain owns all notebook tables and authorization. It receives only `userId` from the existing session, exposes `/notebook/**` routes under the existing `/api` prefix, and does not use family or merchant ownership. A dedicated mini-program service and `pages/notebook/` surface consume those routes. The existing platform settings record stores the global span limit.

**Tech Stack:** Java 17 Spring Boot 3, MyBatis, Flyway/MySQL, JUnit 5; WeChat Mini Program CommonJS/WXML/WXSS, Node contract tests; Vue 3/Vite admin web.

**Spec:** `docs/superpowers/specs/2026-10-03-personal-notebook-design.md`

**Workspace:** `D:/develop/Project/Kitchen/.worktrees/codex/personal-notebook` on branch `codex/personal-notebook`. Do not edit the dirty root checkout. Existing baseline: admin-web 92/92 pass and build passes; frontend 623/625 pass, with two pre-existing brand-copy binding failures in `pages/auth/entry/index.wxml`; full backend suite has pre-existing Docker/test-temp failures. Use focused tests plus final full runs and report residual baseline failures honestly. On this Windows host, invoke Maven as `& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' ...` in PowerShell rather than relying on `mvn` in PATH.

---

## File map and contracts

### Backend

- `backend/src/main/resources/db/migration/V5__personal_notebook.sql`: forward-only schema and `system_settings.notebook_max_query_months DEFAULT 36`; no edits to V1–V4.
- `backend/src/main/java/com/familykitchen/notebook/model/*`: focused request/response/row types; avoid one giant mutable DTO.
- `backend/src/main/java/com/familykitchen/notebook/mapper/*`: event/template/record, contact/grant, audit/image persistence. Keep every query scoped by owner or an explicitly checked grant.
- `backend/src/main/java/com/familykitchen/notebook/service/*`: template validation, span policy, access policy, event/record application service, sharing, export, private image access.
- `backend/src/main/java/com/familykitchen/notebook/controller/*`: account-authenticated endpoints; controllers use `CurrentUserProvider.require(request).userId()` and do not trust owner IDs from request bodies.
- `backend/src/main/java/com/familykitchen/system/{model,service,controller,mapper}/*` and `backend/src/main/resources/mapper/system/SystemSettingMapper.xml`: admin global limit, exposed to notebook policy through one read-only service method.
- `backend/src/test/java/com/familykitchen/notebook/*Test.java`: direct domain/service tests plus controller and persistence contracts.

### Mini program

- `frontend/services/notebook.js`: all notebook HTTP calls; no page issues raw URLs.
- `frontend/utils/notebook-{calendar,template,permissions,export}.js`: pure view mapping and client validation, where needed.
- `frontend/pages/notebook/home/`, `events/`, `event-edit/`, `event-detail/`, `record-edit/`, `record-detail/`, `contacts/`, `sharing/`, `shared/`, `export/`: each page owns only its screen state; shared operations live in service/utils.
- `frontend/app.json`, `frontend/pages/ordering/menu/index.{js,wxml,wxss}`, and relevant route callers/tests: replace cart tab with notebook tab; keep cart as an ordinary page reachable from ordering.
- `frontend/tests/notebook-*.test.js`: service, view-state, navigation, template, sharing, export tests using the established Node harness.

### Admin and docs

- `admin-web/src/views/platform/SystemSettingsView.vue` and `admin-web/tests/notebook-settings.test.js`: validate and save the 1–36 month setting.
- `docs/api-spec.md`: endpoints, data shapes, auth, errors, pagination and export version.

All new public Java types and methods need Javadoc to satisfy `DocumentationCoverageTest`. All behavioral implementation uses @superpowers:test-driven-development: write a failing focused test, run it, implement the smallest code, rerun it, then run the nearby suite. Do not commit failed tests as a finished task.

## Task 1: Global month limit and schema foundation

**Files:** Create `V5__personal_notebook.sql`; modify system setting DTO/DO/view/service/mapper files listed above; test `backend/src/test/java/com/familykitchen/notebook/NotebookSchemaTest.java`, `backend/src/test/java/com/familykitchen/system/NotebookLimitSettingTest.java`.

- [ ] Add failing tests: a query spanning 36 touched calendar months succeeds, 37 fails; null/missing value or a thrown settings read falls back to 36 and logs the read failure; admin writes 1 and 36, rejects 0 and 37; migration declares account-owned notebook tables and does not modify existing migrations.
- [ ] Run `& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' '-Dtest=NotebookSchemaTest,NotebookLimitSettingTest' test` and confirm expected feature-missing failures.
- [ ] Add `notebook_max_query_months INT NOT NULL DEFAULT 36` to `system_settings`; add `notebook_events`, `notebook_template_versions`, `notebook_records`, `notebook_record_revisions`, `notebook_contacts`, `notebook_contact_invites`, `notebook_grants`, `notebook_audit`, `notebook_images`. Use foreign keys to `users` and notebook-owned entities for live rows, unique event/version and ordered indexes; no `family_id`. Audit rows store scalar actor/event/record ID snapshots and have no cascading foreign key to deletable events or records, so metadata survives deletion. For dates use MySQL-compatible `DATE`/`DATETIME` and store JSON as `TEXT` where H2 compatibility requires it.
- [ ] Implement `NotebookRangePolicy` with `months = (to.year-from.year)*12 + (to.month-from.month) + 1`; require `from <= to` and `months <= effectiveLimit`. Expose `notebookMaxQueryMonths()` on `SystemSettingService` and include the admin setting in read/update DTOs, mapper and SQL. Preserve current value for omitted requests where existing tests construct old DTOs. If the setting cannot be read, log the exception through the application logger and use 36 months for that request.
- [ ] Run focused tests, `& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' '-Dtest=ApplicationContextTest' test`, and `git diff --check`; commit `feat: add notebook schema and range setting`.

## Task 2: Events and immutable template versions

**Files:** Create `notebook/model/{Event*,Template*,Field*}`, `notebook/mapper/NotebookEventMapper.java`, `notebook/service/{NotebookEventService,NotebookTemplateValidator}.java`, `notebook/controller/NotebookEventController.java`; tests `NotebookEventServiceTest.java`, `NotebookTemplateValidatorTest.java`, `NotebookEventControllerTest.java`.

- [ ] Write failing tests for account-owned CRUD, archive/unarchive, sort order, focus flag, duplicate access denial, version increment, stable field keys, type-change new key, invalid field options/required settings, and old version immutability.
- [ ] Run the three focused tests and confirm expected failures.
- [ ] Implement `GET/POST /notebook/events`, `GET/PATCH/DELETE /notebook/events/{id}`, owner-only `GET /notebook/events/{id}/delete-impact` returning `{recordCount, templateVersionCount, grantCount}`, `PUT /notebook/events/order`, and `GET/POST /notebook/events/{id}/templates`. Create event and template v1 transactionally; publish each later template edit as a new immutable row. Server assigns field keys and validates the allowed eleven types, labels, choices, required flag and order. Owner can archive, focus and sort; only owner can change templates.
- [ ] Implement owner-confirmed deletion transactionally: revoke grants and delete notebook rows, private attachment references/files via a bounded cleanup operation, while retaining only metadata audit entries. Explicitly deny other accounts and platform/family/merchant roles without ownership.
- [ ] Run focused tests and `mvn -Dtest=ApplicationContextTest test`; commit `feat: add notebook events and versioned templates`.

## Task 3: Records, template snapshots and bounded queries

**Files:** Create `notebook/model/Record*`, `notebook/mapper/NotebookRecordMapper.java`, `notebook/service/{NotebookRecordService,NotebookRecordValidator}.java`, `notebook/controller/NotebookRecordController.java`; tests `NotebookRecordServiceTest.java`, `NotebookRecordQueryTest.java`, `NotebookRecordControllerTest.java`.

- [ ] Write failing tests for every field type and required value, single/cross-day occurrence, `occurredFrom <= occurredTo`, old template rendering/edit, explicit upgrade with prior revision preservation, optimistic edit conflict, owner deletion, interval-overlap owner query, pagination and 36/37-month boundary.
- [ ] Run focused tests and confirm expected failures.
- [ ] Implement `GET/POST /notebook/events/{id}/records`, `GET/PATCH/DELETE /notebook/records/{id}`, and an explicit `POST /notebook/records/{id}/upgrade-template`. Record creation binds current template version; ordinary edits validate against stored version; upgrade writes an immutable prior revision then current-version values. Every write stores author and last editor. Use version in `UPDATE ... WHERE version = ?` and return a conflict on zero updated rows.
- [ ] All list queries require bounded `from`/`to`, filter owner records by interval overlap, and page with deterministic `(occurred_from,id)` ordering. No unbounded body list. Add calendar summary as a bounded projection rather than loading every field value for a full month.
- [ ] Run focused tests and nearby backend tests; commit `feat: add notebook records and bounded queries`.

## Task 4: Contacts, invitations and sharing policy

**Files:** Create `notebook/model/{Contact*,Invite*,Grant*}`, `notebook/mapper/{NotebookContactMapper,NotebookGrantMapper}.java`, `notebook/service/{NotebookContactService,NotebookGrantService,NotebookAccessPolicy}.java`, `notebook/controller/{NotebookContactController,NotebookShareController}.java`; tests `NotebookContactServiceTest.java`, `NotebookGrantPolicyTest.java`, `NotebookShareControllerTest.java`.

- [ ] Write failing tests for self-invite/replay/expired invite rejection, target confirmation, external-account contact, no family dependency, default-private response, independent create/edit/export toggles, grant start/end validation, grant revocation/expiry, full containment of a record in granted data interval, no re-sharing and non-disclosing forbidden responses.
- [ ] Run focused tests and confirm expected failures.
- [ ] Implement contact lookup by login identifier through a narrow identity port; optional family member candidates are lookup-only and never stored as owner links. Invite tokens are random, hashed and expire; invitee must accept. Contact removal revokes grants. API: `/notebook/contacts`, `/notebook/contacts/invites`, `/notebook/contacts/invites/{id}/accept|reject`; `/notebook/events/{id}/grants`, `/notebook/grants/{id}`; `/notebook/shared`.
- [ ] On grant create/update, apply `NotebookRangePolicy` to its inclusive data dates using the current effective platform setting (1–36 months); test both valid 36-month and invalid 37-month grants. Grant validity dates are independent of this data-range bound.
- [ ] `NotebookAccessPolicy` checks current account, contact state, grant validity at request time, requested query interval and complete record containment. `canCreate`, `canEdit`, `canExport` are independent booleans. A shared editor cannot delete, change template/event or grant access. Enforce in services, not only controllers.
- [ ] Run focused tests and nearby backend tests; commit `feat: add notebook contacts and scoped sharing`.

## Task 5: Export, private images and audit

**Files:** Create `notebook/service/{NotebookExportService,NotebookImageService,NotebookAuditService}.java`, `notebook/mapper/{NotebookImageMapper,NotebookAuditMapper}.java`, `notebook/controller/{NotebookExportController,NotebookImageController,NotebookAuditController}.java`; tests `NotebookExportServiceTest.java`, `NotebookImageAccessTest.java`, `NotebookAuditTest.java`.

- [ ] Write failing tests for schema v1 export with all referenced template versions, old field values, stable ordering, authorized/shared export and denied copy, expiry/revocation between preview and export, record interval containment, image metadata without public URL/binary, private image owner/grantee read, and audit entries without text/image contents.
- [ ] Run focused tests and confirm expected failures.
- [ ] Implement `POST /notebook/events/{id}/export` returning one JSON payload for both clipboard and file flows; client passes bounded range, server checks rights immediately before serialization and returns no partial output on failure. Response includes `schemaVersion`, `exportedAt`, `range`, `event`, `templateVersions`, `records`. For shared users, apply full-containment grant filter. Implement private image upload/read/delete under `/notebook/images/**` with account authorization; adapt the existing image validation/storage mechanics without reusing public URLs.
- [ ] Add metadata-only audit for view, create, edit, delete, grant changes, copy/export and image access. Readable owner-only `/notebook/audit` is bounded and paged. Never log form field values, full JSON or access tokens.
- [ ] Run focused tests, `mvn -Dtest=ApplicationContextTest test`, and `git diff --check`; commit `feat: add notebook export and private images`.

## Task 6: Mini-program service, native tab and cart entry

**Files:** Create `frontend/services/notebook.js`, `frontend/utils/notebook-calendar.js`; modify `frontend/utils/api-runtime.js`, `frontend/app.json`, `frontend/pages/ordering/menu/index.{js,wxml,wxss}` and callers that assume cart is a tab; tests `frontend/tests/notebook-api.test.js`, `frontend/tests/notebook-navigation.test.js`, update `frontend/tests/app-config.test.js` and relevant route contracts.

- [ ] Write failing Node tests for account-token notebook requests without family ownership, `GET /notebook/config` returning the server's effective maximum month count, calendar month/date mapping, five-tab order, notebook page registration, cart accessible from menu with count, and no remaining `wx.switchTab` call targeting cart.
- [ ] Run `node --test tests/notebook-api.test.js tests/notebook-navigation.test.js tests/app-config.test.js` and confirm expected failures.
- [ ] Add one runtime notebook service and a logged-in `GET /notebook/config` backend endpoint that returns the server's effective month limit without exposing user data. Replace cart tab with notebook tab and matching local normal/selected icons. Keep the cart page registered outside the tab bar; point all cart navigation to `wx.navigateTo`, while preserving `wx.switchTab` only for true tabs. Add an obvious cart control and count in menu header with safe-area behavior. Do not alter unrelated auth WXML baseline differences.
- [ ] Run focused tests plus frontend `npm test`, recording the two known brand-copy failures separately; commit `feat: add notebook tab and ordering cart entry`.

## Task 7: Mini-program event and record workflows

**Files:** Create `frontend/pages/notebook/{home,events,event-edit,event-detail,record-edit,record-detail}/index.{js,json,wxml,wxss}`, `frontend/utils/notebook-template.js`; tests `frontend/tests/notebook-views.test.js`, `frontend/tests/notebook-template.test.js`.

- [ ] Write failing tests for default calendar, view switch preserving filters, date selection, monthly bounded request, event sorting/focus/archive/CRUD controls, event deletion showing affected record/template/grant counts followed by a second confirmation, eleven template field editors, immutable historical template display, record save/edit/conflict and unsaved-leave confirmation.
- [ ] Run focused Node tests and confirm expected failures.
- [ ] Build the approved warm-paper layout from the visual mockup, with private indicator, orange primary action, sage secondary accent, clear loading/empty/error/retry states, and narrow-screen/safe-area support. Page JS orchestrates `runtime.notebook`, while reusable field validation and view mapping stay in utils. Use `wx.navigateTo` for notebook subpages. Event deletion first loads and displays affected record/template/grant counts, then asks a second irreversible-action confirmation before calling DELETE. Event editor creates v1 fields; later edits publish a version. Record edit uses its stored version unless user explicitly upgrades.
- [ ] Run focused tests and WeChat markup/style contract tests (`npm test`); commit `feat: add notebook calendar and record pages`.

## Task 8: Mini-program contacts, sharing, export and images

**Files:** Create `frontend/pages/notebook/{contacts,sharing,shared,export}/index.{js,json,wxml,wxss}`, `frontend/utils/{notebook-permissions,notebook-export}.js`; tests `frontend/tests/notebook-sharing.test.js`, `frontend/tests/notebook-export.test.js`.

- [ ] Write failing tests for invite acceptance, external contact selection, date-range/expiry controls, default read-only toggles, revoked share state, read-only versus create versus edit versus export UI, copy JSON, large-clipboard fallback, JSON file creation/share and upload/preview permission failures.
- [ ] Run focused tests and confirm expected failures.
- [ ] Add contact invitation and shared-with-me pages. Sharing form treats data range and authorization validity as distinct dates; create/edit/export switches begin off. Server still enforces everything. Export page previews count, confirms external-data risk, calls the single export endpoint, copies with `wx.setClipboardData`, or writes a `.json` file under `wx.env.USER_DATA_PATH` using `wx.getFileSystemManager().writeFile`, then offers `wx.shareFileMessage` when available. Never cache exported JSON in global state or storage. Clear in-page payload after completion/identity change.
- [ ] Run focused tests and full frontend suite, recording baseline deviations; commit `feat: add notebook sharing and json export pages`.

## Task 9: Admin control, API contract and final verification

**Files:** Modify `admin-web/src/views/platform/SystemSettingsView.vue`, `admin-web/tests/notebook-settings.test.js`, `docs/api-spec.md`; add any necessary end-to-end backend contract tests under `backend/src/test/java/com/familykitchen/notebook/`.

- [ ] Write failing admin test for the month input (default 36, integer 1–36, persisted through current save flow) and backend contract tests covering account isolation, authorization lifecycle, no body disclosure to platform admins, and export shape.
- [ ] Run focused tests and confirm expected failures; implement input, validation, API spec and any integration fixes.
- [ ] Run `npm test` and `npm run build` in `admin-web`; focused backend notebook tests and `ApplicationContextTest`; `npm test` in `frontend`; full backend test suite via the explicit Maven path if environment permits. Distinguish known baseline failures from new failures.
- [ ] Run `git diff --check`, inspect all changed paths and verify no secrets or unrelated files entered commits. Perform review against all seven acceptance criteria in the spec; commit `docs: document notebook api and verify integration` for final doc/test changes.

## Final review and handoff

- [ ] Run a spec-compliance review followed by a code-quality review for each task (subagent-driven workflow), then a final whole-feature review.
- [ ] Open the resulting mini-program files or review panel for the user. Summarize implemented behavior, exact test results and environment limitations. Use @superpowers:finishing-a-development-branch to decide how to integrate branch; do not merge into the dirty root checkout without the user's direction.
