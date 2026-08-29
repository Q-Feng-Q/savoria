# Mini Program Role And Dish Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Make real crew names consistent across family screens and improve ordering/detail readability.

**Architecture:** Centralize role-label formatting in frontend/utils/api-scenes.js, expose the formatted crew to every relevant scene, and keep WXML presentation declarative. Extend existing contract tests before changing production files.

**Tech Stack:** WeChat Mini Program WXML/WXSS/CommonJS, Node.js built-in test runner.

---

### Task 1: Lock the desired UI contracts

**Files:**
- Modify: frontend/tests/api-scenes.test.js
- Modify: frontend/tests/shared-cart-pages.test.js
- Modify: frontend/tests/warm-animal-visual-contract.test.js

- [ ] Add assertions for 老祁主厨, 阿禾帮厨, 无试吃员, readable category fallback, exactly one price, section heading/body containers, and no visible 小熊 copy in production WXML.
- [ ] Add a structural style contract requiring the stepper action row to stay in normal flow below copy (no absolute overlay), retain 38rpx glyphs, and wrap/shrink safely in the max-width 360px media rule.
- [ ] Run the three test files and confirm failures are caused by the old UI.

### Task 2: Centralize role labels and readable dish metadata

**Files:**
- Modify: frontend/utils/api-scenes.js

- [ ] Add one buildCrewRoleLabels(crew) API mapping chefName/helperName/tasterName; missing or blank values return 无主厨/无帮厨/无试吃员.
- [ ] Call that same API with homeData.crew from home, menu, and profile scene builders.
- [ ] Prefer categoryName; otherwise use 今日菜单.
- [ ] Keep only the customer-facing final price in the detail scene.
- [ ] Run api-scenes.test.js and confirm scene tests pass; markup/style contract tests intentionally remain red until Task 3.

### Task 3: Update family-facing WXML and layout

**Files:**
- Modify: frontend/pages/family/home/index.wxml
- Modify: frontend/pages/ordering/menu/index.wxml
- Modify: frontend/pages/account/profile/index.wxml
- Modify: frontend/pages/ordering/dish-detail/index.wxml
- Modify: frontend/pages/ordering/dish-detail/index.wxss
- Modify: frontend/components/dish-row/index.wxml
- Modify: frontend/components/dish-row/index.wxss

- [ ] Replace fixed animal role copy with shared formatted labels.
- [ ] Render exactly one family price and add separate 点餐信息, 菜品说明, and 主要食材 heading/body sections.
- [ ] Move the menu stepper into a normal-flow bottom action row after the dish copy, without changing its 38rpx glyph size; add compact-screen wrapping/shrinking rules.
- [ ] Run api-scenes.test.js, shared-cart-pages.test.js, and warm-animal-visual-contract.test.js together and confirm all pass.

### Task 4: Full verification

**Files:**
- Verify all modified frontend files.

- [ ] Run npm test from frontend.
- [ ] Scan production WXML for the exact forbidden visible text patterns 小熊, 兔子帮厨, and 橘猫试吃员; confirm no matches.
- [ ] Inspect git diff and preserve unrelated backend changes.
