# Menu Price Stepper Row Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Put the dish price and quantity stepper on one aligned bottom row with restrained spacing and compact-phone safety.

**Architecture:** Keep the existing `dish-row` component boundary and move only its price node into the existing action row. Add a non-shrinking wrapper around the stepper while making the price the only shrinkable, ellipsized flex item; preserve the quantity-stepper component's existing 38rpx visual circles and 88rpx touch targets.

**Tech Stack:** WeChat Mini Program WXML/WXSS, Node.js built-in test runner.

---

### Task 1: Align price and quantity controls

**Files:**
- Modify: `frontend/tests/shared-cart-pages.test.js`
- Modify: `frontend/components/dish-row/index.wxml`
- Modify: `frontend/components/dish-row/index.wxss`

- [ ] **Step 1: Write the failing layout contract test**

Extend `dish row keeps the 38rpx stepper in a non-overlapping bottom action row` to require:

```js
assert.match(markup, /dish-row__action-row[\s\S]*dish-row__price[\s\S]*dish-row__stepper[\s\S]*quantity-stepper/);
assert.doesNotMatch(markup, /dish-row__body[\s\S]*dish-row__price[\s\S]*<\/view>\s*<view class="dish-row__action-row"/);
assert.match(styles, /\.dish-row__action-row\s*\{[^}]*justify-content:\s*space-between[^}]*align-items:\s*center[^}]*margin-top:\s*14rpx/s);
assert.match(styles, /\.dish-row__price\s*\{[^}]*min-width:\s*0[^}]*overflow:\s*hidden[^}]*text-overflow:\s*ellipsis[^}]*white-space:\s*nowrap/s);
assert.match(styles, /\.dish-row__stepper\s*\{[^}]*flex:\s*0\s+0\s+auto/s);
```

Keep the existing assertions that prohibit absolute positioning and verify compact-phone rules.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
& 'D:\develop\WebDev\node\node.exe' --test tests/shared-cart-pages.test.js
```

Expected: FAIL because the price is still inside `dish-row__body`, the action row is right-aligned, and no non-shrinking stepper wrapper exists.

- [ ] **Step 3: Implement the minimal WXML structure**

Change `frontend/components/dish-row/index.wxml` so the body ends after the description and the action row owns both items:

```xml
<view class="dish-row__action-row" catchtap="noop">
  <text class="dish-row__price">{{dish.priceText || ('¥' + dish.price)}}</text>
  <view class="dish-row__stepper">
    <quantity-stepper value="{{quantity}}" bind:change="quantityChange"/>
  </view>
</view>
```

- [ ] **Step 4: Implement the minimal responsive WXSS**

Update `frontend/components/dish-row/index.wxss`:

```css
.dish-row__price {
  min-width: 0;
  overflow: hidden;
  color: #ed7657;
  font-size: 28rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-row__action-row {
  grid-column: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  margin-top: 14rpx;
}

.dish-row__stepper {
  flex: 0 0 auto;
}
```

Preserve the compact `max-width: 360px` rule and the existing quantity-stepper 38rpx glyph / 88rpx button rules.

- [ ] **Step 5: Run the focused test and verify GREEN**

Run the command from Step 2.

Expected: all tests in `shared-cart-pages.test.js` PASS.

- [ ] **Step 6: Run responsive source and rendered checks**

Run the responsive/visual contract tests:

```powershell
& 'D:\develop\WebDev\node\node.exe' --test tests/shared-cart-pages.test.js tests/responsive-contract.test.js tests/warm-animal-visual-contract.test.js
```

Inspect the dish row at `320px` and `375px` using representative price `¥999999.99` and quantity `999`; confirm the price ellipsizes before the stepper and the row has no overlap or horizontal overflow.

- [ ] **Step 7: Run the full frontend suite**

Run:

```powershell
& 'D:\develop\WebDev\node\npm.cmd' test
```

Expected: all frontend tests PASS with zero failures.

- [ ] **Step 8: Commit**

```powershell
git add frontend/tests/shared-cart-pages.test.js frontend/components/dish-row/index.wxml frontend/components/dish-row/index.wxss
git commit -m "fix: align menu price and quantity controls"
```
