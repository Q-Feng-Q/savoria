const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const root = path.resolve(__dirname, '..');

function loadCartPage(wx, submitOrder) {
  let definition;
  const source = fs.readFileSync(path.join(root, 'pages/ordering/cart/index.js'), 'utf8');
  vm.runInNewContext(source, {
    Page(value) { definition = value; },
    setTimeout,
    wx,
    require(id) {
      if (id.endsWith('/utils/branding')) return { withBranding: (page) => page };
      if (id.endsWith('/utils/cart-submit-validation')) return require('../utils/cart-submit-validation');
      if (id.endsWith('/utils/action-request')) return { createRequestId: () => 'request-1' };
      if (id.endsWith('/utils/identity-load')) return { createIdentityLoadGuard: () => ({}) };
      if (id.endsWith('/utils/page-api')) return { requireSession: () => null, showApiError() {} };
      if (id.endsWith('/utils/api-runtime')) return { createApiRuntime: () => ({}) };
      if (id.endsWith('/utils/api-scenes')) return { buildApiCartScene: () => ({}) };
      if (id.endsWith('/utils/family-api')) return { loadFamilyBundle: async () => ({}) };
      throw new Error(`unexpected require: ${id}`);
    }
  });
  return {
    ...definition,
    data: {
      ...definition.data,
      canSubmit: false,
      bookingEnded: false,
      groupedItems: [],
      expectedMealTime: null,
      deliveryMode: 'PICKUP',
      currentAddress: null,
      mutationBusy: false
    },
    source: { cart: { cartId: 9, version: 2 }, runtime: { orders: { submitOrder } } },
    setData(patch) { Object.assign(this.data, patch); }
  };
}

test('cart submit validation reports all missing fields in display order', () => {
  const { validateCartSubmit, formatCartSubmitIssues } = require('../utils/cart-submit-validation');
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
  assert.equal(formatCartSubmitIssues(result.issues),
    '请先完善以下信息：\n1. 请先选择至少一道菜品\n2. 请选择今天的预计用餐时间\n3. 请选择配送地址');
});

test('cart submit validation covers ended booking and unavailable dishes', () => {
  const { validateCartSubmit } = require('../utils/cart-submit-validation');
  const result = validateCartSubmit({
    bookingEnded: true,
    groupedItems: [{ rows: [{ id: 1, available: false }] }],
    expectedMealTime: '2026-10-02 18:30',
    deliveryMode: 'PICKUP',
    currentAddress: null,
    canSubmit: false
  });

  assert.deepEqual(result.issues.map((item) => item.message), [
    '今天已停止预约，请明天再来',
    '餐篮中有不可用菜品，请减少到 0 后再提交'
  ]);
  assert.equal(result.target, '#cart-time-section');
});

test('cart submit validation has a safe unknown fallback and accepts a complete cart', () => {
  const { validateCartSubmit } = require('../utils/cart-submit-validation');
  const base = {
    bookingEnded: false,
    groupedItems: [{ rows: [{ id: 1, available: true }] }],
    expectedMealTime: '2026-10-02 18:30',
    deliveryMode: 'PICKUP',
    currentAddress: null
  };

  assert.deepEqual(validateCartSubmit({ ...base, canSubmit: false }).issues.map((item) => item.message),
    ['请先完善下单信息']);
  assert.deepEqual(validateCartSubmit({ ...base, canSubmit: true }), { issues: [], target: '' });
});

test('blocked cart submission shows a modal, scrolls to the first issue and skips the API', async () => {
  let modal;
  let scroll;
  let submissions = 0;
  const wx = {
    showToast() {},
    showModal(options) { modal = options; options.success({ confirm: true }); },
    pageScrollTo(options) { scroll = options; }
  };
  const page = loadCartPage(wx, async () => { submissions++; });

  await page.submitOrder();

  assert.equal(submissions, 0);
  assert.equal(modal.title, '还不能提交订单');
  assert.equal(modal.showCancel, false);
  assert.match(modal.content, /请先选择至少一道菜品/);
  assert.match(modal.content, /请选择今天的预计用餐时间/);
  assert.equal(scroll.selector, '#cart-items-section');
  assert.equal(scroll.duration, 300);
});

test('cart markup keeps the submit action clickable and exposes scroll anchors', () => {
  const markup = fs.readFileSync(path.join(root, 'pages/ordering/cart/index.wxml'), 'utf8');
  for (const id of ['cart-time-section', 'cart-fulfillment-section', 'cart-items-section']) {
    assert.match(markup, new RegExp(`id="${id}"`));
  }
  assert.match(markup, /loading="{{mutationBusy}}"/);
  assert.match(markup, /disabled="{{mutationBusy}}"/);
  assert.doesNotMatch(markup, /disabled="{{!canSubmit \|\| mutationBusy}}"/);
});
