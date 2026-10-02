# Mini Program Brand Copy Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow platform administrators to configure eight mini-program brand/atmosphere messages while preserving the current copy as a safe default whenever configuration is absent or unavailable.

**Architecture:** Extend the singleton `system_settings` record with explicit nullable columns and expose normalized effective values through the existing admin and public settings endpoints. Extend the admin settings form to edit raw optional values and the mini-program branding store to normalize/copy/cache effective values once for all pages. Page templates consume a shared `brandCopy` object; functional labels and state/error text remain code-owned.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis XML, Flyway, JUnit 5/Mockito/H2, Vue 3/Vite, Node test runner, WeChat Mini Program WXML/WXSS/CommonJS.

---

## File Structure

### Backend

- Create `backend/src/main/resources/db/migration/V4__add_brand_copy_settings.sql`: forward-only nullable copy columns.
- Modify `backend/src/main/java/com/familykitchen/system/model/entity/SystemSettingDO.java`: persistence properties.
- Modify `backend/src/main/java/com/familykitchen/system/model/dto/SystemSettingRequest.java`: optional validated admin input.
- Modify `backend/src/main/java/com/familykitchen/system/model/vo/SystemSettingView.java`: admin effective values.
- Modify `backend/src/main/java/com/familykitchen/system/model/vo/PublicSystemSettingView.java`: public effective values.
- Modify `backend/src/main/java/com/familykitchen/system/service/impl/SystemSettingServiceImpl.java`: defaults, normalization, mapping and persistence.
- Modify `backend/src/main/resources/mapper/system/SystemSettingMapper.xml`: select/update columns.
- Create `backend/src/test/java/com/familykitchen/system/SystemBrandCopySettingTest.java`: defaults, custom values and reset behavior.
- Modify `backend/src/test/java/com/familykitchen/system/BrandSettingMapperTest.java`: actual MyBatis persistence coverage.

### Admin web

- Create `admin-web/src/components/BrandCopySettings.vue`: focused eight-field editor.
- Modify `admin-web/src/utils/branding.js`: copy defaults, normalization and payload helpers.
- Modify `admin-web/src/views/platform/SystemSettingsView.vue`: compose editor into the existing form.
- Create `admin-web/tests/brand-copy-settings.test.js`: helper and view wiring contracts.

### Mini program

- Modify `frontend/utils/branding.js`: shared defaults, normalization, caching and `brandCopy` page injection.
- Modify `frontend/pages/auth/entry/index.js` and `frontend/pages/auth/register/index.js`: subscribe auth pages to branding without overwriting their existing local edits.
- Modify the WXML files for auth entry/register, family home, ordering cart/orders and account profile: replace only the approved atmosphere literals.
- Modify `frontend/tests/branding-runtime.test.js`: fallback/cache/injection behavior.
- Create `frontend/tests/brand-copy-page-bindings.test.js`: target-page bindings and hard-coded copy removal.

## Task 1: Persist Brand Copy Fields

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__add_brand_copy_settings.sql`
- Modify: `backend/src/main/java/com/familykitchen/system/model/entity/SystemSettingDO.java`
- Modify: `backend/src/main/resources/mapper/system/SystemSettingMapper.xml`
- Modify: `backend/src/test/java/com/familykitchen/system/BrandSettingMapperTest.java`

- [ ] **Step 1: Write the failing mapper persistence test**

Extend the isolated H2 table with the eight nullable columns, set custom values on `SystemSettingDO`, call `mapper.update`, then assert `mapper.selectCurrent()` returns all eight values. Also update one field to `null` and assert it is stored as `null` so “restore default” is representable.

- [ ] **Step 2: Run the mapper test and verify failure**

Run:

```powershell
cd backend
mvn -q -Dtest=BrandSettingMapperTest test
```

Expected: FAIL because entity accessors and mapper columns do not exist.

- [ ] **Step 3: Add the forward-only schema migration**

Create `V4__add_brand_copy_settings.sql` with:

```sql
ALTER TABLE system_settings
  ADD COLUMN brand_tagline varchar(500) DEFAULT NULL COMMENT '通用品牌标语',
  ADD COLUMN home_hero_tagline varchar(500) DEFAULT NULL COMMENT '家庭首页顶部标语',
  ADD COLUMN home_footer_message varchar(500) DEFAULT NULL COMMENT '家庭首页与订单页底部寄语',
  ADD COLUMN cart_hero_tagline varchar(500) DEFAULT NULL COMMENT '餐篮顶部标语',
  ADD COLUMN delivery_message varchar(500) DEFAULT NULL COMMENT '配送氛围提示',
  ADD COLUMN pickup_message varchar(500) DEFAULT NULL COMMENT '自取氛围提示',
  ADD COLUMN cart_footer_message varchar(500) DEFAULT NULL COMMENT '餐篮底部寄语',
  ADD COLUMN profile_welcome_message varchar(500) DEFAULT NULL COMMENT '个人中心欢迎语';
```

- [ ] **Step 4: Implement entity and mapper support**

Add eight `String` fields with getters/setters. Add all columns to `selectCurrent`; add direct assignments to the full `update` statement so `null` clears configuration. Do not add these fields to `updateBranding`, which must remain Logo/name-only.

- [ ] **Step 5: Run the mapper test and verify pass**

Run the command from Step 2. Expected: PASS.

- [ ] **Step 6: Commit the persistence slice**

```powershell
git add backend/src/main/resources/db/migration/V4__add_brand_copy_settings.sql backend/src/main/java/com/familykitchen/system/model/entity/SystemSettingDO.java backend/src/main/resources/mapper/system/SystemSettingMapper.xml backend/src/test/java/com/familykitchen/system/BrandSettingMapperTest.java
git commit -m "feat: persist configurable brand copy"
```

## Task 2: Expose Validated Effective Copy Values

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/system/model/dto/SystemSettingRequest.java`
- Modify: `backend/src/main/java/com/familykitchen/system/model/vo/SystemSettingView.java`
- Modify: `backend/src/main/java/com/familykitchen/system/model/vo/PublicSystemSettingView.java`
- Modify: `backend/src/main/java/com/familykitchen/system/service/impl/SystemSettingServiceImpl.java`
- Create: `backend/src/test/java/com/familykitchen/system/SystemBrandCopySettingTest.java`

- [ ] **Step 1: Write failing service tests**

Cover these cases with mocked mapper/audit/cipher:

```java
assertThat(service.publicCurrent().pickupMessage())
    .isEqualTo("先在一起，好好吃饭，期待您的到来");
assertThat(service.current().deliveryMessage())
    .isEqualTo("美味正在路上，用食物，把温暖送到家");
```

Then load a row with custom values and assert they are trimmed but internal newlines remain. Submit blank strings through `update`, capture the entity, and assert fields are `null` before persistence.

- [ ] **Step 2: Run the service test and verify failure**

```powershell
cd backend
mvn -q -Dtest=SystemBrandCopySettingTest test
```

Expected: FAIL because DTO/VO fields and default resolution do not exist.

- [ ] **Step 3: Extend DTOs and views**

Append optional fields to `SystemSettingRequest` using `@Size(max=120)` except `deliveryMessage` and `pickupMessage`, which use `@Size(max=80)`. Preserve the existing compatibility constructor by passing eight trailing nulls. Append the same eight effective strings to admin and public view records.

- [ ] **Step 4: Implement normalization and view mapping**

In `SystemSettingServiceImpl`, define named constants for the eight defaults and helpers:

```java
private static String effective(String value, String fallback) {
  return value == null || value.isBlank() ? fallback : value.trim();
}
private static String optional(String value) {
  return value == null || value.isBlank() ? null : value.trim();
}
```

Use `optional` when mapping the update request into the entity. Use `effective` in both admin and public view construction. Keep SMTP and Logo behavior unchanged.

- [ ] **Step 5: Run focused backend tests**

```powershell
cd backend
mvn -q -Dtest=SystemBrandCopySettingTest,BrandSettingMapperTest,BrandSettingTest,BrandSettingControllerTest,SystemBindingSettingServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit the backend API slice**

```powershell
git add backend/src/main/java/com/familykitchen/system backend/src/test/java/com/familykitchen/system
git commit -m "feat: expose effective brand copy settings"
```

## Task 3: Add the Platform Brand Copy Editor

**Files:**
- Create: `admin-web/src/components/BrandCopySettings.vue`
- Modify: `admin-web/src/utils/branding.js`
- Modify: `admin-web/src/views/platform/SystemSettingsView.vue`
- Create: `admin-web/tests/brand-copy-settings.test.js`

- [ ] **Step 1: Write failing helper and wiring tests**

Assert that `normalizeBrandCopy({ pickupMessage: ' 自定义 ' })` returns the trimmed custom value and defaults all other fields. Assert `brandCopyPayload` converts blank values to `null`, rejects overlong values, and emits exactly eight fields. Assert the settings view renders `BrandCopySettings` and merges `update:model-value` into the existing form.

- [ ] **Step 2: Run the admin test and verify failure**

```powershell
cd admin-web
npm test -- brand-copy-settings.test.js
```

Expected: FAIL because the helpers/component do not exist.

- [ ] **Step 3: Implement copy helpers**

Export `BRAND_COPY_DEFAULTS`, `BRAND_COPY_LIMITS`, `normalizeBrandCopy`, and `brandCopyPayload` from `src/utils/branding.js`. Keep `brandPayload` Logo/name-only. `brandCopyPayload` returns `null` for blank values so clearing a field restores the server default.

- [ ] **Step 4: Build the focused editor component**

Use a `SectionCard` titled “品牌文案”. Render eight labeled textareas with purpose, default hint, `maxlength`, and live character count. Emit a complete copy patch on input. Do not save independently; the parent’s existing “保存全部配置” button owns persistence and error handling.

- [ ] **Step 5: Wire the component into system settings**

Initialize the reactive form with copy defaults, normalize server values on load, and include `brandCopyPayload(form)` in `updateSystemSettings`. Keep failed submissions from mutating the local draft.

- [ ] **Step 6: Run admin tests and build**

```powershell
cd admin-web
npm test -- brand-copy-settings.test.js branding.test.js account-binding-settings-contract.test.js
npm run build
```

Expected: tests PASS and Vite build succeeds.

- [ ] **Step 7: Commit the admin slice**

```powershell
git add admin-web/src/components/BrandCopySettings.vue admin-web/src/utils/branding.js admin-web/src/views/platform/SystemSettingsView.vue admin-web/tests/brand-copy-settings.test.js
git commit -m "feat: edit mini program brand copy"
```

## Task 4: Extend the Mini Program Branding Runtime

**Files:**
- Modify: `frontend/utils/branding.js`
- Modify: `frontend/tests/branding-runtime.test.js`

- [ ] **Step 1: Write failing runtime tests**

Test all eight defaults, custom trimming, blank/non-string fallback, 80/120-character bounding, storage round-trip, and subscriber updates. Verify `withBranding` initializes and updates:

```js
assert.deepEqual(instance.data.brandCopy, normalizeBranding().brandCopy);
```

- [ ] **Step 2: Run the runtime test and verify failure**

```powershell
cd frontend
npm test -- branding-runtime.test.js
```

Expected: FAIL because `brandCopy` does not exist.

- [ ] **Step 3: Implement normalized copy state**

Add `BRAND_COPY_DEFAULTS` and per-field limits. `normalizeBranding` returns a nested immutable-by-copy `brandCopy` object while retaining the existing flat Logo fields. Reject invalid values by replacing them with defaults, not by returning blanks.

Change `get`, `notify`, and page injection to clone the nested object so pages/listeners cannot mutate store state. `withBranding` must set both `brandName` and `brandCopy` during initial data creation and subscription updates.

- [ ] **Step 4: Run the runtime tests and verify pass**

Run the command from Step 2. Expected: PASS.

- [ ] **Step 5: Commit the runtime slice**

```powershell
git add frontend/utils/branding.js frontend/tests/branding-runtime.test.js
git commit -m "feat: cache configurable mini program copy"
```

## Task 5: Bind Approved Pages to Shared Copy

**Files:**
- Modify: `frontend/pages/auth/entry/index.js`
- Modify: `frontend/pages/auth/entry/index.wxml`
- Modify: `frontend/pages/auth/register/index.js`
- Modify: `frontend/pages/auth/register/index.wxml`
- Modify: `frontend/pages/family/home/index.wxml`
- Modify: `frontend/pages/ordering/cart/index.wxml`
- Modify: `frontend/pages/ordering/orders/index.wxml`
- Modify: `frontend/pages/account/profile/index.wxml`
- Create: `frontend/tests/brand-copy-page-bindings.test.js`

- [ ] **Step 1: Preserve and inspect existing local auth-page edits**

Run:

```powershell
git diff -- frontend/pages/auth/entry/index.wxml frontend/pages/auth/register/index.wxml
```

Treat these as user-owned edits. Change only the relevant text binding and wrapper initialization; do not reformat or replace surrounding markup.

- [ ] **Step 2: Write the failing page-binding contract test**

Assert target WXML uses these bindings:

```text
brandCopy.brandTagline
brandCopy.homeHeroTagline
brandCopy.homeFooterMessage
brandCopy.cartHeroTagline
brandCopy.deliveryMessage
brandCopy.pickupMessage
brandCopy.cartFooterMessage
brandCopy.profileWelcomeMessage
```

Assert the approved atmosphere literals no longer remain in target WXML, except the fixed unbound-state message “从这里，认识新家”. Assert auth entry/register JS use `withBranding`.

- [ ] **Step 3: Run the binding test and verify failure**

```powershell
cd frontend
npm test -- brand-copy-page-bindings.test.js
```

Expected: FAIL on hard-coded WXML copy and unwrapped auth pages.

- [ ] **Step 4: Replace literals with bindings**

Use the exact shared fields. Preserve conditional behavior:

```xml
{{deliveryMode === 'DELIVERY' ? brandCopy.deliveryMessage : brandCopy.pickupMessage}}
```

Keep the personal center unbound message code-owned:

```xml
{{isUnbound ? '从这里，认识新家' : brandCopy.profileWelcomeMessage}}
```

Keep `brandName` separate from `brandCopy.cartFooterMessage` so a site-name change remains automatic.

- [ ] **Step 5: Wrap auth pages with `withBranding`**

Import `withBranding` and wrap the existing page object without altering authentication behavior or lifecycle semantics.

- [ ] **Step 6: Run mini-program focused and full tests**

```powershell
cd frontend
npm test -- branding-runtime.test.js brand-copy-page-bindings.test.js warm-animal-full-rebuild-contract.test.js
npm test
```

Expected: all tests PASS.

- [ ] **Step 7: Commit the page slice**

```powershell
git add frontend/pages/auth/entry frontend/pages/auth/register frontend/pages/family/home/index.wxml frontend/pages/ordering/cart/index.wxml frontend/pages/ordering/orders/index.wxml frontend/pages/account/profile/index.wxml frontend/tests/brand-copy-page-bindings.test.js
git commit -m "feat: render configurable mini program copy"
```

## Task 6: Cross-Module Verification

**Files:**
- Modify only if a failing contract reveals an omission.

- [ ] **Step 1: Run the backend suite**

```powershell
cd backend
mvn -q test
```

Expected: exit code 0. Expected test-generated warning/error logs may appear, but Surefire reports no failures.

- [ ] **Step 2: Run the admin suite and production build**

```powershell
cd admin-web
npm test
npm run build
```

Expected: tests and build PASS.

- [ ] **Step 3: Run the mini-program suite**

```powershell
cd frontend
npm test
```

Expected: PASS.

- [ ] **Step 4: Review the final diff and workspace ownership**

```powershell
git diff --check
git status --short
```

Confirm only planned files are committed. Preserve unrelated existing changes in `admin-web/vite.config.mjs`, `docker/single-image/entrypoint.sh`, auth-page user edits, `AGENTS.md`, `output/`, and `uploads/` unless a target file was deliberately merged.

- [ ] **Step 5: Record final verification evidence**

Summarize test commands, pass counts/exit codes, commits, migration name, and any deployment action still required. Do not claim cloud deployment until the GitHub push and WeChat Cloud Hosting build both complete.
