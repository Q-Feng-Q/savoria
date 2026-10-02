# Cart Submit Required Reminder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every blocked order submission produce a visible modal that lists missing or invalid information and scrolls to the first affected cart section.

**Architecture:** Add a pure cart-submit validation utility that converts page state into ordered issues and a target selector. Keep the cart page responsible only for rendering the modal, scrolling after confirmation, and calling the existing submit API after validation passes. Keep inline warning text as secondary feedback and allow the action bar to receive taps whenever no mutation is running.

**Tech Stack:** WeChat Mini Program CommonJS/WXML/WXSS, Node built-in test runner, VM-based page contract tests.

---

## File Structure

- Create `frontend/utils/cart-submit-validation.js`: pure ordered validation and modal text formatting.
- Create `frontend/tests/cart-submit-validation.test.js`: validation combinations and cart page interaction contract.
- Modify `frontend/pages/ordering/cart/index.js`: call validation before the API, show modal and scroll.
- Modify `frontend/pages/ordering/cart/index.wxml`: stable section anchors and clickable action-bar behavior.

## Task 1: Build Ordered Cart Submission Validation

**Files:**
- Create: `frontend/utils/cart-submit-validation.js`
- Create: `frontend/tests/cart-submit-validation.test.js`

- [ ] **Step 1: Write failing pure-function tests**

Cover empty cart, invalid expected time, unavailable rows, delivery without an address, booking ended, multiple issue ordering, unknown `canSubmit=false`, and a valid cart. Assert each issue has a readable message and selector.

```js
const result = validateCartSubmit({
  bookingEnded: false,
  groupedItems: [],
  expectedMealTime: null,
  deliveryMode: 'DELIVERY',
  currentAddress: null,
  canSubmit: false
});
assert.deepEqual(result.issues.map((item) => item.message), [
  '请先选择至少一道菜品',
  '请选择今天的预计用餐时间',
  '请选择配送地址'
]);
assert.equal(result.target, '#cart-items-section');
```

- [ ] **Step 2: Run the test and verify failure**

Run:

```powershell
cd frontend
node --test tests/cart-submit-validation.test.js
```

Expected: FAIL because `utils/cart-submit-validation.js` does not exist.

- [ ] **Step 3: Implement the minimal pure utility**

Export:

```js
function validateCartSubmit(state = {}) { /* ordered issue collection */ }
function formatCartSubmitIssues(issues) {
  return `请先完善以下信息：\n${issues.map((item, index) => `${index + 1}. ${item.message}`).join('\n')}`;
}
```

Validation order and targets:

1. booking ended → `#cart-time-section`
2. no rows → `#cart-items-section`
3. invalid/missing `expectedMealTime` → `#cart-time-section`
4. any unavailable row → `#cart-items-section`
5. delivery without `currentAddress` → `#cart-fulfillment-section`
6. otherwise `canSubmit === false` → generic issue targeting `#cart-time-section`

- [ ] **Step 4: Run the pure tests and verify pass**

Run the command from Step 2. Expected: PASS.

## Task 2: Show Modal, Scroll and Preserve Submission

**Files:**
- Modify: `frontend/pages/ordering/cart/index.js`
- Modify: `frontend/pages/ordering/cart/index.wxml`
- Modify: `frontend/tests/cart-submit-validation.test.js`

- [ ] **Step 1: Write failing page interaction and markup tests**

Load `pages/ordering/cart/index.js` in a VM with `withBranding` as a pass-through. For invalid state, assert:

- `wx.showModal` receives `title: '还不能提交订单'` and `showCancel: false`;
- the order API is not called;
- confirming the modal calls `wx.pageScrollTo` with the first issue selector.

Statically assert the WXML has `cart-time-section`, `cart-fulfillment-section`, and `cart-items-section`, and the bottom action bar uses `disabled="{{mutationBusy}}"` rather than `!canSubmit`.

- [ ] **Step 2: Run the test and verify failure**

Run:

```powershell
cd frontend
node --test tests/cart-submit-validation.test.js
```

Expected: FAIL because the page still uses a toast and the action bar suppresses taps.

- [ ] **Step 3: Implement modal and scroll behavior**

Import the utility. Add a page method that calls:

```js
wx.showModal({
  title: '还不能提交订单',
  content: formatCartSubmitIssues(validation.issues),
  showCancel: false,
  confirmText: '去完善',
  success: ({ confirm }) => {
    if (confirm && validation.target && wx.pageScrollTo) {
      wx.pageScrollTo({ selector: validation.target, duration: 300 });
    }
  }
});
```

At the start of `submitOrder`, return only when `mutationBusy` is true. Validate page data before reading the cart or invoking the API; show the modal and return when issues exist. Preserve the existing success, conflict refresh, error, and finally behavior.

- [ ] **Step 4: Add anchors and change action-bar disabled state**

Add IDs to the three existing sections and render:

```xml
<bottom-action-bar
  ...
  loading="{{mutationBusy}}"
  disabled="{{mutationBusy}}"
  bind:primary="submitOrder"
>
```

Do not remove `warningText` or its red inline rendering.

- [ ] **Step 5: Run focused tests**

```powershell
cd frontend
node --test tests/cart-submit-validation.test.js tests/api-scenes.test.js tests/shared-cart-pages.test.js
```

Expected: PASS.

- [ ] **Step 6: Run the full mini-program suite**

```powershell
cd frontend
npm test
```

Expected: all tests PASS.

- [ ] **Step 7: Commit**

```powershell
git add frontend/utils/cart-submit-validation.js frontend/pages/ordering/cart/index.js frontend/pages/ordering/cart/index.wxml frontend/tests/cart-submit-validation.test.js
git commit -m "fix: surface missing cart submission details"
```

## Task 3: Final Verification

- [ ] **Step 1: Run formatting and ownership checks**

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; unrelated existing workspace changes remain untouched.

- [ ] **Step 2: Record evidence**

Report focused/full test results, the new modal behavior, selector targets, commit hash, and that no backend or database changes were required.
