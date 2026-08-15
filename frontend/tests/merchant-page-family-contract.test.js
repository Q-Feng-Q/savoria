const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const deferred = () => {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
};

async function withMerchantPage(runtimeFactory, resolveApiErrorMessage, run) {
  const pagePath = path.join(root, 'pages', 'merchant', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const previousPage = global.Page;
  const previousWx = global.wx;
  const previousRuntime = require.cache[runtimePath];
  const previousPageApi = require.cache[pageApiPath];
  let definition;

  require.cache[runtimePath] = { exports: { createApiRuntime: runtimeFactory } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    resolveApiErrorMessage,
    showApiError: () => {}
  } };
  global.Page = (value) => { definition = value; };
  global.wx = { navigateTo() {}, redirectTo() {}, reLaunch() {}, showToast() {} };

  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); }
    });
    await run(page, definition);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath];
    if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath];
    global.Page = previousPage;
    global.wx = previousWx;
  }
}

test('merchant overview exposes the approved restrained workbench structure', () => {
  const source = read('pages/merchant/index.wxml');

  assert.match(source, /<merchant-workbench-nav\s+active="overview"\s*\/>/);
  assert.match(source, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  assert.match(source, /今日待处理/);
  assert.match(source, /\{\{todayCommand\.activeOrderCount\}\}/);
  assert.match(source, /服务家庭/);
  assert.match(source, /今日采购/);
  assert.match(source, /切换账号\/身份/);
  assert.match(source, /regionStates\.menu\.phase === 'error'/);
  assert.match(source, /\{\{regionStates\.menu\.message\}\}/);
  assert.match(source, /bindtap="retryMenu"[^>]+aria-role="button"[^>]+aria-label="重试菜单概况"/);
  assert.match(source, /regionStates\.families\.phase === 'error'/);
  assert.match(source, /\{\{regionStates\.families\.message\}\}/);
  assert.match(source, /bindtap="retryFamilies"[^>]+aria-role="button"[^>]+aria-label="重试家庭概况"/);
  assert.match(source, /regionStates\.families\.phase === 'error'[^>]*>[\s\S]*?bindtap="retryFamilies"/);

  const quickActions = source.match(/class="[^"]*merchant-quick-action[^"]*"/g) || [];
  assert.equal(quickActions.length, 3);
  assert.equal((source.match(/<action-button[^>]+bind:action=/g) || []).length, 3);
  for (const label of ['订单与备餐', '今日采购', '通知与提醒']) assert.match(source, new RegExp(label));

  for (const asset of ['merchant-cooking.webp', 'merchant-orders.webp', 'merchant-purchase.webp']) {
    assert.match(source, new RegExp(asset));
  }
  assert.doesNotMatch(source, /merchant-scene-pieces|<picker\b|quick-grid|wx:for="\{\{families\}\}"|wx:for="\{\{activeOrders\}\}"|wx:for="\{\{selectedFamilyMenu\}\}"/);
  assert.doesNotMatch(source, /<\/?button\b/);
});

test('merchant overview registers reusable state and action components', () => {
  const config = JSON.parse(read('pages/merchant/index.json'));
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');
  assert.equal(config.usingComponents['action-button'], '/components/action-button/index');
});

test('merchant overview source isolates region failures with allSettled and retries', () => {
  const source = read('pages/merchant/index.js');
  assert.match(source, /Promise\.allSettled/);
  assert.match(source, /regionStates/);
  assert.match(source, /orders:\s*\{\s*phase:/s);
  assert.match(source, /purchase:\s*\{\s*phase:/s);
  assert.match(source, /menu:\s*\{\s*phase:/s);
  assert.match(source, /families:\s*\{\s*phase:/s);
  assert.match(source, /retryOrders\s*\(/);
  assert.match(source, /retryPurchase\s*\(/);
  assert.match(source, /retryMenu\s*\(/);
  assert.match(source, /retryFamilies\s*\(/);
  assert.match(source, /resolveApiErrorMessage/);
});

test('purchase rejection keeps merchant overview ready with family and order summaries', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const previousPage = global.Page;
  const previousWx = global.wx;
  const previousRuntime = require.cache[runtimePath];
  const previousPageApi = require.cache[pageApiPath];
  let definition;

  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({
    merchant: {
      getFamilies: async () => [{ familyId: 7, familyName: '春日家庭', merchantName: '暖炉小馆' }],
      getOrders: async () => [{ orderId: 9, familyId: 7, status: 'PENDING', items: [] }],
      getFamilyMenu: async () => []
    },
    purchase: { getSummary: async () => { throw new Error('purchase offline'); } }
  }) } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    resolveApiErrorMessage: (error, fallback) => (error && error.message) || fallback,
    showApiError: () => assert.fail('partial rejection must not become a total page error')
  } };
  global.Page = (value) => { definition = value; };
  global.wx = { navigateTo() {}, redirectTo() {} };

  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); }
    });
    await definition.load.call(page);

    assert.equal(page.data.phase, 'ready');
    assert.equal(page.data.context.merchant.name, '暖炉小馆');
    assert.equal(page.data.familyCount, 1);
    assert.equal(page.data.todayCommand.activeOrderCount, 1);
    assert.equal(page.data.regionStates.orders.phase, 'ready');
    assert.equal(page.data.regionStates.purchase.phase, 'error');
    assert.equal(page.data.todayPurchaseItemCount, 0);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath];
    if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath];
    global.Page = previousPage;
    global.wx = previousWx;
  }
});

test('all successfully empty overview regions show the page empty state', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const previousPage = global.Page;
  const previousWx = global.wx;
  const previousRuntime = require.cache[runtimePath];
  const previousPageApi = require.cache[pageApiPath];
  let definition;

  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({
    merchant: {
      getFamilies: async () => [],
      getOrders: async () => [],
      getFamilyMenu: async () => assert.fail('menu endpoint should not be called without a family')
    },
    purchase: { getSummary: async () => [] }
  }) } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    resolveApiErrorMessage: (error, fallback) => (error && error.message) || fallback,
    showApiError: () => assert.fail('successful empty responses are not errors')
  } };
  global.Page = (value) => { definition = value; };
  global.wx = { navigateTo() {}, redirectTo() {} };

  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); }
    });
    await definition.load.call(page);

    assert.equal(page.data.phase, 'empty');
    assert.equal(page.data.regionStates.orders.phase, 'ready');
    assert.equal(page.data.regionStates.purchase.phase, 'ready');
    assert.equal(page.data.regionStates.menu.phase, 'ready');
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath];
    if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath];
    global.Page = previousPage;
    global.wx = previousWx;
  }
});

test('family failure is explicit and keeps menu unavailable until family retry succeeds', async () => {
  let familyAttempts = 0;
  let menuCalls = 0;
  const resolvedErrors = [];
  const runtime = {
    merchant: {
      getFamilies: async () => {
        familyAttempts += 1;
        if (familyAttempts === 1) throw new Error('家庭服务暂不可用');
        return [{ familyId: 8, familyName: '新家庭', merchantName: '新商户' }];
      },
      getOrders: async () => [{ orderId: 2, familyId: 8, status: 'PENDING', items: [] }],
      getFamilyMenu: async () => { menuCalls += 1; return [{ dishId: 1, enabled: true }]; }
    },
    purchase: { getSummary: async () => [] }
  };

  await withMerchantPage(
    () => runtime,
    (error, fallback) => { resolvedErrors.push(error); return error.message || fallback; },
    async (page) => {
      await page.load();
      assert.equal(page.data.regionStates.families.phase, 'error');
      assert.equal(page.data.regionStates.families.message, '家庭服务暂不可用');
      assert.equal(page.data.regionStates.menu.phase, 'error');
      assert.equal(menuCalls, 0);

      await page.retryMenu();
      assert.equal(page.data.regionStates.menu.phase, 'error');
      assert.equal(page.data.regionStates.menu.message, '家庭概况不可用，菜单暂不可加载');
      assert.equal(menuCalls, 0);

      await page.retryFamilies();
      assert.equal(page.data.regionStates.families.phase, 'ready');
      assert.equal(page.data.regionStates.menu.phase, 'ready');
      assert.equal(page.data.familyCount, 1);
      assert.equal(menuCalls, 1);
      assert.equal(resolvedErrors.length, 1);
    }
  );
});

test('stale authentication failures do not resolve or redirect after a newer load wins', async () => {
  const oldFamilies = deferred();
  const resolvedCodes = [];
  const runtimes = [
    {
      merchant: { getFamilies: () => oldFamilies.promise, getOrders: async () => [], getFamilyMenu: async () => [] },
      purchase: { getSummary: async () => [] }
    },
    {
      merchant: {
        getFamilies: async () => [{ familyId: 2, familyName: '当前家庭', merchantName: '当前商户' }],
        getOrders: async () => [],
        getFamilyMenu: async () => []
      },
      purchase: { getSummary: async () => [] }
    }
  ];
  let runtimeIndex = 0;

  await withMerchantPage(
    () => runtimes[runtimeIndex++],
    (error, fallback) => { resolvedCodes.push(error && error.code); return error.message || fallback; },
    async (page) => {
      const oldLoad = page.load();
      await page.load();
      oldFamilies.reject(Object.assign(new Error('expired'), { code: 40101 }));
      await oldLoad;

      assert.equal(page.data.context.merchant.name, '当前商户');
      assert.deepEqual(resolvedCodes, []);
    }
  );
});

test('stale full loads and regional retries cannot overwrite a newer generation', async () => {
  const oldFamilies = deferred();
  const stalePurchase = deferred();
  let secondPurchaseCalls = 0;
  const runtimes = [
    {
      merchant: { getFamilies: () => oldFamilies.promise, getOrders: async () => [], getFamilyMenu: async () => [] },
      purchase: { getSummary: async () => [] }
    },
    {
      merchant: {
        getFamilies: async () => [{ familyId: 2, familyName: '新结果', merchantName: '新结果' }],
        getOrders: async () => [],
        getFamilyMenu: async () => []
      },
      purchase: { getSummary: async () => { secondPurchaseCalls += 1; return secondPurchaseCalls === 1 ? [] : stalePurchase.promise; } }
    },
    {
      merchant: {
        getFamilies: async () => [{ familyId: 3, familyName: '最新结果', merchantName: '最新结果' }],
        getOrders: async () => [],
        getFamilyMenu: async () => []
      },
      purchase: { getSummary: async () => [{ ingredientName: '新采购' }] }
    }
  ];
  let runtimeIndex = 0;

  await withMerchantPage(
    () => runtimes[runtimeIndex++],
    (error, fallback) => (error && error.message) || fallback,
    async (page) => {
      const oldLoad = page.load();
      await page.load();
      oldFamilies.resolve([{ familyId: 1, familyName: '旧结果', merchantName: '旧结果' }]);
      await oldLoad;
      assert.equal(page.data.context.merchant.name, '新结果');

      const oldRetry = page.retryPurchase();
      await page.load();
      stalePurchase.resolve([{ ingredientName: '旧采购1' }, { ingredientName: '旧采购2' }]);
      await oldRetry;
      assert.equal(page.data.context.merchant.name, '最新结果');
      assert.equal(page.data.todayPurchaseItemCount, 1);
    }
  );
});

test('partial and retry authentication failures use centralized resolution', async () => {
  const authError = Object.assign(new Error('expired'), { code: 40101 });
  const resolvedCodes = [];
  let purchaseAttempts = 0;
  const runtime = {
    merchant: {
      getFamilies: async () => { throw authError; },
      getOrders: async () => [{ orderId: 4, status: 'PENDING', items: [] }],
      getFamilyMenu: async () => []
    },
    purchase: {
      getSummary: async () => {
        purchaseAttempts += 1;
        if (purchaseAttempts > 1) throw authError;
        return [];
      }
    }
  };

  await withMerchantPage(
    () => runtime,
    (error, fallback) => { resolvedCodes.push(error && error.code); return error.message || fallback; },
    async (page) => {
      await page.load();
      assert.deepEqual(resolvedCodes, [40101]);
      await page.retryPurchase();
      assert.deepEqual(resolvedCodes, [40101, 40101]);
      assert.equal(page.data.regionStates.purchase.message, 'expired');
    }
  );
});

test('merchant overview uses the approved navigation modes and routes', () => {
  const source = read('pages/merchant/index.js');
  assert.match(source, /openMerchantOrders\s*\([^)]*\)\s*\{[\s\S]*?wx\.redirectTo\(\{\s*url:\s*['"]\/pages\/merchant\/merchant-orders\/index['"]/);
  assert.match(source, /openPurchase\s*\([^)]*\)\s*\{[\s\S]*?wx\.navigateTo\(\{\s*url:\s*`\/pages\/merchant\/purchase\/index\?date=/);
  assert.match(source, /openNotifications\s*\([^)]*\)\s*\{[\s\S]*?wx\.navigateTo\(\{\s*url:\s*['"]\/pages\/account\/notifications\/index\?actor=merchant['"]/);
  assert.match(source, /openAccountSwitcher\s*\([^)]*\)\s*\{[\s\S]*?wx\.navigateTo\(\{\s*url:\s*['"]\/pages\/account\/account-management\/index['"]/);
});

test('merchant orders is a root workbench with explicit backend filters, grouped rows and purchase entry', () => {
  const markup = read('pages/merchant/merchant-orders/index.wxml');
  const source = read('pages/merchant/merchant-orders/index.js');
  const config = JSON.parse(read('pages/merchant/merchant-orders/index.json'));

  assert.match(markup, /<merchant-workbench-nav\s+active="orders"\s*\/>/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  assert.match(markup, /wx:for="\{\{orderGroups\}\}"/);
  assert.match(markup, /item\.familyName/);
  assert.match(markup, /item\.statusLabel/);
  assert.match(markup, /item\.mealLabel/);
  assert.match(markup, /item\.itemCountText/);
  assert.match(markup, /item\.chargeSummaryText/);
  assert.match(markup, /bindtap="openPurchase"[^>]+aria-role="button"[^>]+aria-label="查看采购汇总"/);
  assert.doesNotMatch(markup, /<button\b/);
  assert.equal(config.usingComponents['merchant-workbench-nav'], '/components/merchant-workbench-nav/index');
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');

  assert.match(source, /MERCHANT_ORDER_FILTERS/);
  assert.match(source, /groupOrdersByServiceDate/);
  assert.match(source, /openPurchase\s*\([^)]*\)[\s\S]*?wx\.navigateTo/);
  assert.match(source, /busyOrderMap/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /resolveApiErrorMessage/);
  assert.match(source, /retryLoad\s*\(/);
});

test('merchant order detail is a secondary page with plain sections, state handling and guarded actions', () => {
  const markup = read('pages/merchant/merchant-order-detail/index.wxml');
  const source = read('pages/merchant/merchant-order-detail/index.js');
  const config = JSON.parse(read('pages/merchant/merchant-order-detail/index.json'));

  assert.doesNotMatch(markup, /merchant-workbench-nav/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  for (const heading of ['订单信息', '菜品明细', '费用与配送', '状态时间线']) assert.match(markup, new RegExp(heading));
  for (const action of ['saveFee', 'advanceOrder', 'rejectOrder', 'cancelOrder']) assert.match(markup, new RegExp(`bindtap="${action}"`));
  assert.match(markup, /safe-action-bar/);
  assert.doesNotMatch(markup, /<button\b/);
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');

  assert.match(source, /phase:\s*'loading'/);
  assert.match(source, /retryLoad\s*\(/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /mutationBusy/);
  assert.match(source, /resolveApiErrorMessage/);
  assert.match(source, /getOrderDetail\(this\.data\.id\)/);
});

test('purchase derives meal choices from the all-day response and renders truthful source quantities', () => {
  const markup = read('pages/merchant/purchase/index.wxml');
  const source = read('pages/merchant/purchase/index.js');
  const config = JSON.parse(read('pages/merchant/purchase/index.json'));

  assert.doesNotMatch(markup, /merchant-workbench-nav/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  assert.match(markup, /<page-state\s+wx:if="\{\{phase === 'loading' \|\| phase === 'error'\}\}"/);
  assert.match(markup, /\{\{item\.quantityText\}\}/);
  assert.match(markup, /\{\{sourceSummary\.text\}\}/);
  assert.match(markup, /bindtap="copyList"[^>]+aria-role="button"[^>]+aria-label="复制采购清单"/);
  assert.doesNotMatch(markup, /<button\b/);
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');

  assert.match(source, /buildPurchaseMealOptions/);
  assert.doesNotMatch(source, /mealSlotId:\s*(10|20|30)\b/);
  assert.doesNotMatch(source, /getSummary\(\{[^}]*mealSlotId/);
  assert.match(source, /phase:\s*'loading'/);
  assert.match(source, /retryLoad\s*\(/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /resolveApiErrorMessage/);
});

async function withPurchasePage(runtime, run) {
  const pagePath = path.join(root, 'pages', 'merchant', 'purchase', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const previousPage = global.Page;
  const previousWx = global.wx;
  const previousRuntime = require.cache[runtimePath];
  const previousPageApi = require.cache[pageApiPath];
  let definition;
  const toasts = [];
  const clipboard = [];

  require.cache[runtimePath] = { exports: { createApiRuntime: () => runtime } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    resolveApiErrorMessage: (error, fallback) => (error && error.message) || fallback
  } };
  global.Page = (value) => { definition = value; };
  global.wx = {
    showToast(value) { toasts.push(value); },
    setClipboardData(value) { clipboard.push(value); }
  };

  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); }
    });
    page.setData({ date: '2026-08-02' });
    await run(page, toasts, clipboard);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath];
    if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath];
    global.Page = previousPage;
    global.wx = previousWx;
  }
}

test('purchase retains real temporary checkout items and binds checked/delete mutations', async () => {
  const calls = [];
  const runtime = {
    merchant: { getFamilies: async () => [{ familyId: 2, familyName: '陈家' }] },
    purchase: {
      getSummary: async () => [],
      getTempItems: async () => [{ itemId: 71, mealSlotId: null, ingredientName: '餐巾纸', quantity: 2, unit: '包', remark: '补货', checked: false, temporary: true }],
      getByFamily: async () => [{ itemId: 71, ingredientName: '餐巾纸', quantity: 2, unit: '包', remark: '补货', checked: false, temporary: true }],
      toggleChecked: async (id, payload) => { calls.push(['toggle', id, payload]); },
      deleteTempItem: async (id) => { calls.push(['delete', id]); }
    }
  };

  await withPurchasePage(runtime, async (page) => {
    await page.load();
    assert.equal(page.data.phase, 'ready');
    assert.deepEqual(page.data.tempItems, [{ itemId: 71, mealSlotId: null, ingredientName: '餐巾纸', quantity: 2, unit: '包', remark: '补货', checked: false, temporary: true }]);
    await page.toggleTempItem({ currentTarget: { dataset: { id: 71, checked: false } } });
    await page.deleteTempItem({ currentTarget: { dataset: { id: 71 } } });
    assert.deepEqual(calls, [['toggle', 71, { checked: true }], ['delete', 71]]);
  });
});

test('purchase remains usable when an optional per-family request fails', async () => {
  const runtime = {
    merchant: { getFamilies: async () => [{ familyId: 2, familyName: '测试家庭' }] },
    purchase: {
      getSummary: async () => [{ ingredientName: '鸡蛋', quantity: 2, unit: '个', sourceStatus: 'CONFIRMED', sources: [] }],
      getTempItems: async () => [],
      getByFamily: async () => { throw new Error('family purchase unavailable'); }
    }
  };

  await withPurchasePage(runtime, async (page) => {
    await page.load();
    assert.equal(page.data.phase, 'ready');
    assert.equal(page.data.items.length, 1);
  });
});

test('purchase copy includes visible temporary items for all-day and selected-meal views', async () => {
  const runtime = {
    merchant: { getFamilies: async () => [] },
    purchase: {
      getSummary: async () => [{ ingredientName: '鸡蛋', quantity: 2, unit: '个', sourceStatus: 'CONFIRMED', sources: [{ mealSlotId: 42 }] }],
      getTempItems: async () => [
        { itemId: 1, mealSlotId: null, ingredientName: '餐巾纸', quantity: 2, unit: '包', temporary: true },
        { itemId: 2, mealSlotId: 42, ingredientName: '牛奶', quantity: 1, unit: '盒', temporary: true }
      ],
      getByFamily: async () => [],
      getCopyText: async () => '鸡蛋 2 个 / 已确认'
    }
  };

  await withPurchasePage(runtime, async (page, _toasts, clipboard) => {
    await page.load();
    await page.copyList();
    assert.match(clipboard.at(-1).data, /鸡蛋/);
    assert.match(clipboard.at(-1).data, /餐巾纸/);
    assert.match(clipboard.at(-1).data, /牛奶/);

    page.setData({ mealIndex: 1 });
    page.rebuildScene();
    await page.copyList();
    assert.match(clipboard.at(-1).data, /鸡蛋/);
    assert.match(clipboard.at(-1).data, /餐巾纸/);
    assert.match(clipboard.at(-1).data, /牛奶/);
  });
});

test('purchase validates and creates a temporary item with the exact payload and derived meal id', async () => {
  const payloads = [];
  const runtime = {
    merchant: { getFamilies: async () => [{ familyId: 2, familyName: '陈家' }] },
    purchase: {
      getSummary: async () => [{ ingredientName: '鸡蛋', quantity: 2, unit: '个', sourceStatus: 'CONFIRMED', sources: [{ familyId: 2, mealSlotId: 42 }] }],
      getTempItems: async () => [],
      getByFamily: async () => [],
      createTempItem: async (payload) => { payloads.push(payload); }
    }
  };

  await withPurchasePage(runtime, async (page, toasts) => {
    await page.load();
    page.setData({ mealIndex: 1, tempIngredientName: '', tempQuantity: '2', tempUnit: '包', tempRemark: '备用' });
    await page.createTempItem();
    assert.equal(payloads.length, 0);
    assert.equal(toasts.at(-1).title, '请填写食材、数量和单位');

    page.setData({ tempIngredientName: '餐巾纸' });
    await page.createTempItem();
    assert.deepEqual(payloads, [{
      date: '2026-08-02',
      mealSlotId: 42,
      ingredientName: '餐巾纸',
      quantity: 2,
      unit: '包',
      remark: '备用'
    }]);
  });
});

test('merchant fulfillment controls expose accessible 88rpx touch targets', () => {
  const ordersMarkup = read('pages/merchant/merchant-orders/index.wxml');
  const ordersStyles = read('pages/merchant/merchant-orders/index.wxss');
  const purchaseMarkup = read('pages/merchant/purchase/index.wxml');
  const purchaseStyles = read('pages/merchant/purchase/index.wxss');
  const detailStyles = read('pages/merchant/merchant-order-detail/index.wxss');

  assert.match(ordersMarkup, /class="filter-picker"[^>]+aria-role="button"[^>]+aria-label="筛选家庭"/);
  assert.match(purchaseMarkup, /class="control-value"[^>]+aria-role="button"[^>]+aria-label="选择采购日期"/);
  assert.match(purchaseMarkup, /class="control-value"[^>]+aria-role="button"[^>]+aria-label="选择采购餐次"/);
  assert.match(ordersStyles, /\.filter-picker\s*\{[^}]*min-height:\s*88rpx/s);
  assert.match(ordersStyles, /\.status-filter\s*\{[^}]*min-height:\s*88rpx/s);
  assert.match(purchaseStyles, /\.control-value[^\{]*\{[^}]*min-height:\s*88rpx/s);
  assert.match(detailStyles, /\.fee-input\s*\{[^}]*min-height:\s*88rpx/s);
  assert.match(ordersStyles, /\.order-row__action\s*\{[^}]*min-height:\s*88rpx/s);
  assert.match(detailStyles, /\.text-action\s*\{[^}]*min-height:\s*88rpx/s);
  assert.match(detailStyles, /\.primary-action[^\{]*\{[^}]*min-height:\s*88rpx/s);
  assert.match(detailStyles, /\.quiet-action[^\{]*\{[^}]*min-height:\s*88rpx/s);
});

test('merchant dishes is a searchable root workbench with guarded linear rows', () => {
  const markup = read('pages/merchant/merchant-dishes/index.wxml');
  const source = read('pages/merchant/merchant-dishes/index.js');
  const styles = read('pages/merchant/merchant-dishes/index.wxss');
  const config = JSON.parse(read('pages/merchant/merchant-dishes/index.json'));

  assert.match(markup, /<merchant-workbench-nav\s+active="dishes"\s*\/>/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  assert.match(markup, /bindinput="bindSearch"/);
  assert.match(markup, /bindtap="selectStatusFilter"/);
  assert.match(markup, /bindtap="openIngredients"[^>]+\{\{ingredientCount\}\}/);
  assert.match(markup, /bindtap="openCreateDish"[^>]+aria-role="button"[^>]+aria-label="新增菜品"/);
  assert.match(markup, /wx:for="\{\{filteredDishRows\}\}"/);
  assert.match(markup, /busyDishMap/);
  assert.doesNotMatch(markup, /<button\b|warm-management-art|hero/);
  assert.match(source, /deriveDishRows/);
  assert.match(source, /bindSearch\s*\(/);
  assert.match(source, /selectStatusFilter\s*\(/);
  assert.match(source, /busyDishMap/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /resolveApiErrorMessage/);
  assert.match(source, /retryLoad\s*\(/);
  assert.match(styles, /\.dish-thumb[\s\S]*?(?:width|height):\s*(?:100|104|108|112)rpx/);
  assert.equal(config.usingComponents['merchant-workbench-nav'], '/components/merchant-workbench-nav/index');
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');
});

test('merchant dish search matches canonical active and inactive status values', () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'merchant-dishes', 'index.js');
  const previousPage = global.Page;
  global.Page = () => {};
  try {
    delete require.cache[require.resolve(pagePath)];
    const { deriveDishRows } = require(pagePath);
    const rows = [
      { id: 1, name: '清蒸鱼', category: '热菜', status: 'active', statusText: '已上架' },
      { id: 2, name: '冬瓜汤', category: '汤品', status: 'inactive', statusText: '已下架' }
    ];
    assert.deepEqual(deriveDishRows(rows, 'active', 'all').map((item) => item.id), [1]);
    assert.deepEqual(deriveDishRows(rows, 'inactive', 'all').map((item) => item.id), [2]);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    global.Page = previousPage;
  }
});

test('merchant dish list does not retain an obsolete full-update payload builder', () => {
  const source = read('pages/merchant/merchant-dishes/index.js');
  assert.doesNotMatch(source, /buildDishUpdatePayload/);
});

test('merchant dish toggle uses only the dedicated status endpoint', () => {
  const source = read('pages/merchant/merchant-dishes/index.js');
  assert.match(source, /updateDishStatus\(dishId, \{ status:/);
  assert.doesNotMatch(source, /getDishDetail\(dishId\)/);
  assert.doesNotMatch(source, /updateDish\(dishId/);
});

test('catalog settings failures are isolated from core page data', () => {
  for (const file of ['pages/merchant/merchant-dishes/index.js', 'pages/merchant/dish-edit/index.js']) {
    assert.match(read(file), /getPublicSettings\(\)\.catch\(\(\) => \(\{\}\)\)/);
  }
});

test('dish payload validation accepts zero price and rejects invalid nested values', () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'dish-edit', 'index.js');
  const previousPage = global.Page;
  global.Page = () => {};
  try {
    delete require.cache[require.resolve(pagePath)];
    const { validateDish, buildDishPayload } = require(pagePath);
    const valid = { name: '粥', categoryId: 2, basePrice: 0, imageUrl: '', description: '', status: 'active', ingredients: [{ name: '米', quantity: 0, unit: '克', calcType: 'FIXED' }], cookingSteps: [{ title: '', content: '煮熟' }] };
    assert.equal(validateDish(valid), '');
    assert.equal(buildDishPayload(valid).basePrice, 0);
    assert.match(validateDish({ ...valid, basePrice: '' }), /价格/);
    assert.match(validateDish({ ...valid, basePrice: null }), /价格/);
    assert.match(validateDish({ ...valid, basePrice: -1 }), /价格/);
    assert.match(validateDish({ ...valid, categoryId: null }), /分类/);
    assert.match(validateDish({ ...valid, ingredients: [{ name: '', quantity: NaN, unit: '', calcType: 'BAD' }] }), /原材料/);
    assert.match(validateDish({ ...valid, cookingSteps: [{ title: '煮', content: '' }] }), /步骤/);
  } finally { delete require.cache[require.resolve(pagePath)]; global.Page = previousPage; }
});

test('catalog mutation messages follow API outcomes rather than cached settings', () => {
  const dishListPath = path.join(root, 'pages', 'merchant', 'merchant-dishes', 'index.js');
  const dishEditPath = path.join(root, 'pages', 'merchant', 'dish-edit', 'index.js');
  const previousPage = global.Page; global.Page = () => {};
  try {
    delete require.cache[require.resolve(dishListPath)]; delete require.cache[require.resolve(dishEditPath)];
    const list = require(dishListPath); const edit = require(dishEditPath);
    assert.equal(list.statusSuccessMessage({ outcome: 'PENDING_REVIEW' }, false), '已提交审核');
    assert.equal(list.statusSuccessMessage({ outcome: 'APPLIED' }, true), '状态已更新');
    assert.equal(edit.saveSuccessMessage({ outcome: 'PENDING_REVIEW' }, false), '已提交审核');
    assert.equal(edit.saveSuccessMessage({ outcome: 'APPLIED' }, true), '菜品已保存');
    assert.equal(edit.saveSuccessMessage({ status: 'PENDING_REVIEW' }, false), '已提交审核');
  } finally { delete require.cache[require.resolve(dishListPath)]; delete require.cache[require.resolve(dishEditPath)]; global.Page = previousPage; }
});

test('dish editor busy guard blocks deferred-save mutations', () => {
  const source = read('pages/merchant/dish-edit/index.js');
  const markup = read('pages/merchant/dish-edit/index.wxml');
  for (const handler of ['bindField', 'bindCategory', 'bindIngredient', 'bindCalculation', 'bindQuantity', 'bindStepTitle', 'bindStepContent', 'addIngredient', 'removeIngredient', 'addCookingStep', 'removeCookingStep', 'toggleStatus', 'openIngredientLibrary']) {
    assert.match(source, new RegExp(`${handler}\\s*\\([^)]*\\)\\s*\\{\\s*if \\(this\\.isMutationBusy\\(\\)\\) return;`));
  }
  assert.match(source, /saveFingerprint/);
  assert.match(markup, /disabled="\{\{saving \|\| uploading\}\}"/);
});

test('ingredient save and delete operations are mutually exclusive', async () => {
  const source = read('pages/merchant/ingredient-edit/index.js');
  const markup = read('pages/merchant/ingredient-edit/index.wxml');
  assert.match(source, /saveIngredient\s*\([^)]*\)\s*\{[\s\S]*?if \(this\.data\.saving \|\| this\.data\.busyIngredientId\) return;/);
  assert.match(source, /removeIngredient\s*\([^)]*\)\s*\{[\s\S]*?if \(this\.data\.saving\) return;/);
  assert.match(markup, /disabled="\{\{saving \|\| busyIngredientId\}\}"/);
});

test('dish editor onShow preserves a dirty form and refreshes only ingredients', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'dish-edit', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const previousRuntime = require.cache[runtimePath]; const previousPageApi = require.cache[pageApiPath]; const previousPage = global.Page; const previousWx = global.wx;
  let definition; let categoryCalls = 0; let ingredientCalls = 0;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant: { getDishCategories: async () => { categoryCalls++; return [{ categoryId: 1, name: '主食' }]; }, getIngredients: async () => { ingredientCalls++; return ingredientCalls === 1 ? [{ ingredientId: 1, name: '米', unit: '克' }] : [{ ingredientId: 2, name: '盐', unit: '克' }, { ingredientId: 1, name: '大米', unit: '克' }]; }, getDishDetail: async () => null }, system: { getPublicSettings: async () => { throw new Error('settings offline'); } } }) } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 2 }), showApiError: () => {}, resolveApiErrorMessage: (e, f) => e.message || f } };
  global.Page = (value) => { definition = value; }; global.wx = { navigateTo() {}, showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update) { for (const [key, value] of Object.entries(update)) { if (key.includes('.')) this.data[key.split('.')[0]][key.split('.')[1]] = value; else this.data[key] = value; } } });
    page.onLoad({}); await page.onShow(); page.setData({ 'dish.name': '未保存的粥' }); page.markDirty(); await page.onShow();
    assert.equal(page.data.dish.name, '未保存的粥'); assert.equal(categoryCalls, 1); assert.equal(ingredientCalls, 2);
    assert.equal(page.data.ingredientIndex, 1); assert.equal(page.data.selectedIngredientName, '大米');
    assert.equal(page.data.calculationIndex, 0); assert.equal(page.data.selectedCalculationLabel, page.data.calculationTypes[0].label);
    assert.equal(page.data.phase, 'ready'); assert.equal(page.data.reviewEnabled, false);
  } finally { delete require.cache[require.resolve(pagePath)]; if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath]; if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath]; global.Page = previousPage; global.wx = previousWx; }
});

test('ingredient save snapshots mode and does not wipe work started after the request', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'ingredient-edit', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const previousRuntime = require.cache[runtimePath]; const previousPageApi = require.cache[pageApiPath]; const previousPage = global.Page; const previousWx = global.wx;
  const pending = deferred(); const payloads = []; const toasts = []; let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant: { updateIngredient: async (id, payload) => { payloads.push([id, payload]); return pending.promise; }, createIngredient: async () => assert.fail('edit snapshot must update'), getIngredients: async () => [] } }) } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 2 }), showApiError: () => {}, resolveApiErrorMessage: (e, f) => e.message || f } };
  global.Page = (value) => { definition = value; }; global.wx = { showToast: (value) => toasts.push(value) };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update) { this.data = { ...this.data, ...update }; } });
    page.setData({ editId: 7, form: { name: '盐', unit: '克', category: '调味' } });
    const save = page.saveIngredient();
    page.setData({ editId: '', form: { name: '糖', unit: '克', category: '调味' } });
    pending.resolve(); await save;
    assert.deepEqual(payloads, [[7, { name: '盐', unit: '克', category: '调味' }]]);
    assert.equal(toasts.at(-1).title, '已保存修改');
    assert.deepEqual(page.data.form, { name: '糖', unit: '克', category: '调味' });
  } finally { delete require.cache[require.resolve(pagePath)]; if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath]; if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath]; global.Page = previousPage; global.wx = previousWx; }
});

test('deleting the currently edited ingredient clears its internal editor state', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'ingredient-edit', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js')); const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const oldRuntime = require.cache[runtimePath]; const oldApi = require.cache[pageApiPath]; const oldPage = global.Page; const oldWx = global.wx; let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant: { deleteIngredient: async () => {}, getIngredients: async () => [] } }) } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 2 }), showApiError: () => {}, resolveApiErrorMessage: (e, f) => e.message || f } };
  global.Page = (value) => { definition = value; }; global.wx = { showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update) { this.data = { ...this.data, ...update }; } });
    page.setData({ editId: 7, ingredients: [{ id: 7, name: '盐', removable: true }], form: { name: '盐', unit: '克', category: '调味', remark: '' } });
    await page.removeIngredient({ currentTarget: { dataset: { id: 7 } } });
    assert.equal(page.data.editId, ''); assert.equal(page.data.form.name, '');
  } finally { delete require.cache[require.resolve(pagePath)]; if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath]; if (oldApi) require.cache[pageApiPath] = oldApi; else delete require.cache[pageApiPath]; global.Page = oldPage; global.wx = oldWx; }
});

test('dish editor uses divided sections, guarded state, upload and safe save action', () => {
  const markup = read('pages/merchant/dish-edit/index.wxml');
  const source = read('pages/merchant/dish-edit/index.js');
  const styles = read('pages/merchant/dish-edit/index.wxss');
  const config = JSON.parse(read('pages/merchant/dish-edit/index.json'));

  assert.doesNotMatch(markup, /merchant-workbench-nav|<button\b|hero/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  for (const heading of ['基本信息', '菜品图片', '原材料', '制作步骤', '上架状态']) assert.match(markup, new RegExp(heading));
  assert.match(markup, /bindtap="chooseDishImage"/);
  assert.match(markup, /bindtap="saveDish"/);
  assert.match(markup, /safe-action-bar/);
  assert.match(source, /phase:\s*'loading'/);
  assert.match(source, /retryLoad\s*\(/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /saving/);
  assert.match(source, /uploading/);
  assert.match(source, /resolveApiErrorMessage/);
  assert.match(source, /missing|不存在|未找到/);
  assert.match(source, /ingredients:\s*\(dish\.ingredients[\s\S]*?ingredientName/);
  assert.match(source, /cookingSteps:\s*\(dish\.cookingSteps[\s\S]*?stepNo/);
  assert.match(styles, /min-height:\s*88rpx/);
  assert.match(styles, /env\(safe-area-inset-bottom\)/);
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');
});

test('ingredient editor exposes references, guarded delete and empty-safe editor', () => {
  const markup = read('pages/merchant/ingredient-edit/index.wxml');
  const source = read('pages/merchant/ingredient-edit/index.js');
  const styles = read('pages/merchant/ingredient-edit/index.wxss');
  const config = JSON.parse(read('pages/merchant/ingredient-edit/index.json'));

  assert.doesNotMatch(markup, /merchant-workbench-nav|<button\b|hero/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  assert.match(markup, /item\.usedByDishCount/);
  assert.match(markup, /item\.usedByDishText/);
  assert.match(markup, /item\.removable/);
  assert.match(markup, /safe-action-bar/);
  assert.match(source, /phase:\s*'loading'/);
  assert.match(source, /retryLoad\s*\(/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /busyIngredientId/);
  assert.match(source, /saving/);
  assert.match(source, /resolveApiErrorMessage/);
  assert.match(source, /if\s*\(!ingredient\s*\|\|\s*!ingredient\.removable\)/);
  assert.match(styles, /min-height:\s*88rpx/);
  assert.match(styles, /env\(safe-area-inset-bottom\)/);
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');
});

test('purchase loads temp items independently and filters selected meals with global rows', async () => {
  const runtime = {
    merchant: { getFamilies: async () => [] },
    purchase: {
      getSummary: async () => [{ ingredientName: '鸡蛋', quantity: 2, unit: '个', sourceStatus: 'CONFIRMED', sources: [{ mealSlotId: 42 }] }],
      getTempItems: async () => [
        { itemId: 1, mealSlotId: null, ingredientName: '全局项', temporary: true },
        { itemId: 2, mealSlotId: 42, ingredientName: '午餐项', temporary: true },
        { itemId: 3, mealSlotId: 73, ingredientName: '晚餐项', temporary: true }
      ],
      getByFamily: async () => assert.fail('no family means no by-family calls')
    }
  };
  await withPurchasePage(runtime, async (page) => {
    await page.load();
    assert.equal(page.data.tempItems.length, 3);
    page.setData({ mealIndex: 1 });
    page.rebuildScene();
    assert.deepEqual(page.data.tempItems.map((item) => item.itemId), [1, 2]);
  });
});

test('merchant reject uses the bodyless endpoint while cancellation keeps its reason', () => {
  const source = read('pages/merchant/merchant-order-detail/index.js');
  const markup = read('pages/merchant/merchant-order-detail/index.wxml');
  assert.match(source, /merchant\.rejectOrder\(this\.data\.id\)/);
  assert.doesNotMatch(source, /rejectOrder\(this\.data\.id,\s*\{/);
  assert.doesNotMatch(source, /请先填写拒单原因/);
  assert.match(source, /merchant\.cancelOrder\(this\.data\.id,\s*\{\s*reason\s*\}\)/);
  assert.doesNotMatch(markup, /拒单或取消时必须填写原因/);
});

test('preparing cancellation blocks a blank reason and submits a nonblank reason', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'merchant-order-detail', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const previousPage = global.Page;
  const previousWx = global.wx;
  const previousRuntime = require.cache[runtimePath];
  const previousPageApi = require.cache[pageApiPath];
  const calls = [];
  const toasts = [];
  let definition;

  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({
    merchant: { cancelOrder: async (id, payload) => calls.push([id, payload]) }
  }) } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    resolveApiErrorMessage: (error, fallback) => (error && error.message) || fallback
  } };
  global.Page = (value) => { definition = value; };
  global.wx = { showToast(value) { toasts.push(value); } };

  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: { ...definition.data, id: '9', scene: { order: { rawStatus: 'PREPARING' } } },
      setData(update) { this.data = { ...this.data, ...update }; },
      async load() {}
    });
    await page.cancelOrder();
    assert.deepEqual(calls, []);
    assert.equal(toasts.at(-1).title, '备餐中取消请填写原因');

    page.setData({ reasonInput: '食材临时缺货' });
    await page.cancelOrder();
    assert.deepEqual(calls, [['9', { reason: '食材临时缺货' }]]);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath];
    if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath];
    global.Page = previousPage;
    global.wx = previousWx;
  }
});

test('merchant families is a restrained root workbench with linear status rows and routes', () => {
  const markup = read('pages/merchant/merchant-families/index.wxml');
  const source = read('pages/merchant/merchant-families/index.js');
  const styles = read('pages/merchant/merchant-families/index.wxss');
  const config = JSON.parse(read('pages/merchant/merchant-families/index.json'));

  assert.match(markup, /<merchant-workbench-nav\s+active="families"\s*\/>/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  assert.match(markup, /wx:for="\{\{families\}\}"/);
  assert.match(markup, /item\.memberCountText/);
  assert.match(markup, /item\.addressCountText/);
  assert.match(markup, /item\.deliveryText/);
  assert.match(markup, /item\.serviceStateText/);
  assert.match(markup, /item\.menuStatusText/);
  assert.match(markup, /bindtap="openFamilyDetail"[^>]+aria-label="查看家庭"/);
  assert.match(markup, /bindtap="openFamilyMenu"[^>]+aria-label="配置菜单"/);
  assert.doesNotMatch(markup, /summary-card|<picker\b|<button\b|hero/);
  assert.match(source, /phase:\s*'loading'/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /resolveApiErrorMessage/);
  assert.match(source, /retryLoad\s*\(/);
  assert.match(source, /activeMenuCount/);
  assert.match(styles, /min-height:\s*88rpx/);
  assert.equal(config.usingComponents['merchant-workbench-nav'], '/components/merchant-workbench-nav/index');
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');
});

test('merchant family detail keeps core state separate from optional ledger menu and order regions', () => {
  const markup = read('pages/merchant/merchant-family-detail/index.wxml');
  const source = read('pages/merchant/merchant-family-detail/index.js');
  const styles = read('pages/merchant/merchant-family-detail/index.wxss');
  const config = JSON.parse(read('pages/merchant/merchant-family-detail/index.json'));

  assert.doesNotMatch(markup, /merchant-workbench-nav|summary-card|<button\b|hero/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  for (const heading of ['家庭概览', '配送规则', '成员与余额', '地址', '菜单预览', '订单预览']) assert.match(markup, new RegExp(heading));
  for (const region of ['ledger', 'menu', 'orders']) {
    assert.match(markup, new RegExp(`optionalStates\\.${region}\\.phase === 'error'`));
  }
  assert.match(markup, /余额偏低/);
  assert.match(markup, /暂无地址/);
  assert.match(markup, /暂无生效菜单/);
  assert.match(source, /Promise\.allSettled/);
  assert.match(source, /phase:\s*'loading'/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /deliveryBusy/);
  assert.match(source, /busyMemberId/);
  assert.match(source, /retryLedger\s*\(/);
  assert.match(source, /retryMenu\s*\(/);
  assert.match(source, /retryOrders\s*\(/);
  assert.match(source, /updateFamilyDeliveryPolicy\(this\.data\.familyId,\s*\{/);
  assert.match(source, /adjustMemberBalance\(memberId,\s*\{/);
  assert.match(styles, /min-height:\s*88rpx/);
  assert.match(styles, /env\(safe-area-inset-bottom\)/);
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');
});

test('family menu exposes explicit family source copy and guarded row editing', () => {
  const markup = read('pages/merchant/family-menu/index.wxml');
  const source = read('pages/merchant/family-menu/index.js');
  const styles = read('pages/merchant/family-menu/index.wxss');
  const config = JSON.parse(read('pages/merchant/family-menu/index.json'));

  assert.doesNotMatch(markup, /merchant-workbench-nav|summary-card|<button\b|hero/);
  assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/);
  assert.match(markup, /当前家庭/);
  assert.match(markup, /来源家庭/);
  assert.match(markup, /wx:for="\{\{menuRows\}\}"/);
  assert.match(markup, /bindtap="copyFromFamily"/);
  assert.match(markup, /bindtap="toggleDish"/);
  assert.match(markup, /data-delta="-1"[^>]+bindtap="changePrice"/);
  assert.match(markup, /data-delta="1"[^>]+bindtap="changePrice"/);
  assert.match(source, /phase:\s*'loading'/);
  assert.match(source, /_loadGeneration/);
  assert.match(source, /rowBusyId/);
  assert.match(source, /retryLoad\s*\(/);
  assert.match(source, /saveFamilyMenu\(this\.data\.familyId,\s*\{\s*items:/s);
  assert.match(source, /copyFamilyMenu\(this\.data\.familyId,\s*\{\s*sourceFamilyId:\s*option\.value/s);
  assert.match(styles, /min-height:\s*88rpx/);
  assert.match(styles, /env\(safe-area-inset-bottom\)/);
  assert.equal(config.usingComponents['page-state'], '/components/page-state/index');
});

test('family menu scene and editable draft preserve a valid zero family price', async () => {
  const { buildApiFamilyMenuScene } = require('../utils/merchant-scenes');
  const scene = buildApiFamilyMenuScene({
    session: { merchantId: 3 },
    currentFamilyId: 8,
    families: [{ familyId: 8, familyName: '零元家庭' }],
    menuItems: [{ dishId: 4, dishName: '赠送汤', basePrice: 12, familyFinalPrice: 0, enabled: true }]
  });
  assert.equal(scene.menuRows[0].finalPrice, 0);
  assert.match(scene.menuRows[0].finalPriceText, /0\.00/);

  const pagePath = path.join(root, 'pages', 'merchant', 'family-menu', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const oldPage = global.Page;
  const oldWx = global.wx;
  const oldRuntime = require.cache[runtimePath];
  const oldPageApi = require.cache[pageApiPath];
  let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({
    baseUrl: '',
    merchant: {
      getFamilies: async () => [{ familyId: 8, familyName: '零元家庭' }],
      getFamilyMenu: async () => [{ dishId: 4, dishName: '赠送汤', basePrice: 12, familyFinalPrice: 0, enabled: true }]
    }
  }) } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 3 }), resolveApiErrorMessage: (error) => error.message } };
  global.Page = (value) => { definition = value; };
  global.wx = { showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); } });
    page.setData({ familyId: '8' });
    await page.load();
    assert.equal(page.menuDraft[0].familyFinalPrice, 0);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi; else delete require.cache[pageApiPath];
    global.Page = oldPage; global.wx = oldWx;
  }
});

test('failed family menu row saves restore the authoritative draft before retry', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'family-menu', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const oldPage = global.Page;
  const oldWx = global.wx;
  const oldRuntime = require.cache[runtimePath];
  const oldPageApi = require.cache[pageApiPath];
  const payloads = [];
  let saveAttempt = 0;
  let definition;
  const runtime = {
    baseUrl: '',
    merchant: {
      getFamilies: async () => [{ familyId: 8, familyName: '测试家庭' }],
      getFamilyMenu: async () => [{ dishId: 4, dishName: '汤', basePrice: 12, familyFinalPrice: 0, enabled: false, sortOrder: 0 }],
      saveFamilyMenu: async (familyId, payload) => {
        payloads.push({ familyId, payload: JSON.parse(JSON.stringify(payload)) });
        saveAttempt += 1;
        if (saveAttempt === 1 || saveAttempt === 3) throw new Error('save failed');
      }
    }
  };
  require.cache[runtimePath] = { exports: { createApiRuntime: () => runtime } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 3 }), resolveApiErrorMessage: (error) => error.message } };
  global.Page = (value) => { definition = value; };
  global.wx = { showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); } });
    page.setData({ familyId: '8' });
    await page.load();
    await page.toggleDish({ currentTarget: { dataset: { id: 4 } } });
    await page.toggleDish({ currentTarget: { dataset: { id: 4 } } });
    await page.changePrice({ currentTarget: { dataset: { id: 4, delta: 1 } } });
    await page.changePrice({ currentTarget: { dataset: { id: 4, delta: 1 } } });

    assert.deepEqual(payloads.map((call) => call.payload.items[0].enabled), [true, true, false, false]);
    assert.deepEqual(payloads.map((call) => call.payload.items[0].familyFinalPrice), [0, 0, 1, 1]);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi; else delete require.cache[pageApiPath];
    global.Page = oldPage; global.wx = oldWx;
  }
});

test('family menu resolves a stale query id before every read and mutation', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'family-menu', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const oldPage = global.Page; const oldWx = global.wx;
  const oldRuntime = require.cache[runtimePath]; const oldPageApi = require.cache[pageApiPath];
  const calls = []; let definition;
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '甲' }, { familyId: 9, familyName: '乙' }],
    getFamilyMenu: async (id) => { calls.push(['read', id]); return [{ dishId: 4, basePrice: 12, enabled: false }]; },
    saveFamilyMenu: async (id) => { calls.push(['save', id]); },
    copyFamilyMenu: async (id, payload) => { calls.push(['copy', id, payload.sourceFamilyId]); }
  } };
  require.cache[runtimePath] = { exports: { createApiRuntime: () => runtime } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 3 }), resolveApiErrorMessage: (error) => error.message } };
  global.Page = (value) => { definition = value; }; global.wx = { showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); } });
    definition.onLoad.call(page, { familyId: '999' });
    await page.load();
    await page.toggleDish({ currentTarget: { dataset: { id: 4 } } });
    await page.copyFromFamily();
    assert.equal(page.data.familyId, 8);
    assert.equal(calls.some((call) => String(call[1]) === '999'), false);
    assert.equal(calls.some((call) => call[0] === 'save' && call[1] === 8), true);
    assert.equal(calls.some((call) => call[0] === 'copy' && call[1] === 8), true);
  } finally {
    delete require.cache[require.resolve(pagePath)]; if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi; else delete require.cache[pageApiPath]; global.Page = oldPage; global.wx = oldWx;
  }
});

test('family detail renders core before deferred optional sections settle', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'merchant-family-detail', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const oldPage = global.Page; const oldWx = global.wx;
  const oldRuntime = require.cache[runtimePath]; const oldPageApi = require.cache[pageApiPath];
  const slowMenu = deferred(); let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant: {
    getFamilyDetail: async () => ({ familyId: 8, familyName: '核心家庭', members: [], addresses: [] }),
    getFamilyMenu: () => slowMenu.promise,
    getOrders: async () => []
  } }) } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 3 }), resolveApiErrorMessage: (error) => error.message } };
  global.Page = (value) => { definition = value; }; global.wx = { showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update) { this.data = { ...this.data, ...update }; } });
    page.setData({ familyId: '8' });
    const loading = page.load();
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(page.data.phase, 'ready');
    assert.equal(page.data.family.name, '核心家庭');
    assert.equal(page.data.optionalStates.menu.phase, 'loading');
    slowMenu.resolve([]);
    await loading;
    assert.equal(page.data.optionalStates.menu.phase, 'ready');
  } finally {
    delete require.cache[require.resolve(pagePath)]; if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi; else delete require.cache[pageApiPath]; global.Page = oldPage; global.wx = oldWx;
  }
});

test('family detail scene preserves zero menu price and chooses the explicit default address', () => {
  const { buildApiMerchantFamilyDetailScene } = require('../utils/merchant-scenes');
  const scene = buildApiMerchantFamilyDetailScene({
    session: { merchantId: 3 },
    familyDetail: { familyId: 8, familyName: '甲', members: [], addresses: [
      { addressId: 1, contactName: '先录入', addressText: '旧址', defaultAddress: false },
      { addressId: 2, contactName: '默认人', addressText: '默认址', defaultAddress: true }
    ] },
    familyMenuItems: [{ dishId: 4, dishName: '赠送汤', familyFinalPrice: 0, basePrice: 12, enabled: true }]
  });
  assert.match(scene.menuPreview[0].priceText, /0\.00/);
  assert.match(scene.family.defaultAddressText, /默认人/);
});

test('family detail rejects non-finite and non-positive balance adjustments before API calls', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'merchant-family-detail', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const oldPage = global.Page; const oldWx = global.wx; const oldRuntime = require.cache[runtimePath]; const oldPageApi = require.cache[pageApiPath];
  let definition; let calls = 0;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant: { adjustMemberBalance: async () => { calls += 1; } } }) } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 3 }), resolveApiErrorMessage: (error) => error.message } };
  global.Page = (value) => { definition = value; }; global.wx = { showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: { ...definition.data, memberAdjustValues: { 7: '-2' } }, setData(update) { this.data = { ...this.data, ...update }; } });
    const event = { currentTarget: { dataset: { memberId: 7, direction: 'increase' } } };
    await page.adjustMemberBalance(event);
    page.data.memberAdjustValues[7] = 'Infinity';
    await page.adjustMemberBalance(event);
    assert.equal(calls, 0);
  } finally {
    delete require.cache[require.resolve(pagePath)]; if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi; else delete require.cache[pageApiPath]; global.Page = oldPage; global.wx = oldWx;
  }
});

test('merchant orders honors a valid family query and offers a custom clear filter', () => {
  const source = read('pages/merchant/merchant-orders/index.js');
  const markup = read('pages/merchant/merchant-orders/index.wxml');
  assert.match(source, /onLoad\s*\(query\)/);
  assert.match(source, /requestedFamilyId/);
  assert.match(source, /clearFamilyFilter\s*\(/);
  assert.match(markup, /家庭筛选/);
  assert.match(markup, /bindtap="clearFamilyFilter"[^>]+aria-role="button"[^>]+aria-label="清除家庭筛选"/);
});

test('merchant orders applies and clears a valid family query filter', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'merchant-orders', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const oldPage = global.Page; const oldWx = global.wx; const oldRuntime = require.cache[runtimePath]; const oldPageApi = require.cache[pageApiPath];
  let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({
    merchant: {
      getFamilies: async () => [{ familyId: 8, familyName: '甲' }, { familyId: 9, familyName: '乙' }],
      getOrders: async () => [
        { orderId: 1, familyId: 8, status: 'PENDING', items: [] },
        { orderId: 2, familyId: 9, status: 'PENDING', items: [] }
      ]
    },
    purchase: { getSummary: async () => [] }
  }) } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 3 }), resolveApiErrorMessage: (error) => error.message } };
  global.Page = (value) => { definition = value; }; global.wx = { navigateTo() {}, showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); } });
    definition.onLoad.call(page, { familyId: '9' });
    await page.load();
    assert.equal(page.data.familyFilterLabel, '乙');
    assert.deepEqual(page.data.visibleOrders.map((item) => item.id), [2]);
    page.clearFamilyFilter();
    assert.deepEqual(page.data.visibleOrders.map((item) => item.id), [1, 2]);
  } finally {
    delete require.cache[require.resolve(pagePath)]; if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi; else delete require.cache[pageApiPath]; global.Page = oldPage; global.wx = oldWx;
  }
});

test('family detail core load and delivery mutation finish while optional reads remain pending', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'merchant-family-detail', 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));
  const oldPage = global.Page; const oldWx = global.wx; const oldRuntime = require.cache[runtimePath]; const oldPageApi = require.cache[pageApiPath];
  const pending = new Promise(() => {}); let definition;
  const runtime = { merchant: {
    getFamilyDetail: async () => ({ familyId: 8, familyName: '甲', deliveryEnabled: true, members: [], addresses: [] }),
    getFamilyMenu: () => pending,
    getOrders: () => pending,
    updateFamilyDeliveryPolicy: async () => {}
  } };
  require.cache[runtimePath] = { exports: { createApiRuntime: () => runtime } };
  require.cache[pageApiPath] = { exports: { requireSession: () => ({ merchantId: 3 }), resolveApiErrorMessage: (error) => error.message } };
  global.Page = (value) => { definition = value; }; global.wx = { showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)]; require(pagePath);
    const page = Object.assign({}, definition, { data: JSON.parse(JSON.stringify(definition.data)), setData(update) { this.data = { ...this.data, ...update }; } });
    page.setData({ familyId: '8' });
    assert.equal(await Promise.race([page.load().then(() => 'resolved'), new Promise((resolve) => setImmediate(() => resolve('pending')))]), 'resolved');
    assert.equal(page.data.phase, 'ready');
    const mutation = page.toggleDeliveryEnabled();
    assert.equal(await Promise.race([mutation.then(() => 'resolved'), new Promise((resolve) => setImmediate(() => resolve('pending')))]), 'resolved');
    assert.equal(page.data.deliveryBusy, false);
    assert.ok(page._optionalLoadPromise);
  } finally {
    delete require.cache[require.resolve(pagePath)]; if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi; else delete require.cache[pageApiPath]; global.Page = oldPage; global.wx = oldWx;
  }
});

test('family detail optional regions distinguish loading from ready-empty', () => {
  const markup = read('pages/merchant/merchant-family-detail/index.wxml');
  assert.match(markup, /optionalStates\.ledger\.phase === 'loading'[\s\S]*?正在加载余额流水/);
  assert.match(markup, /optionalStates\.menu\.phase === 'loading'[\s\S]*?正在加载菜单/);
  assert.match(markup, /optionalStates\.orders\.phase === 'loading'[\s\S]*?正在加载订单/);
  assert.match(markup, /optionalStates\.menu\.phase === 'ready' && !menuPreview\.length/);
  assert.match(markup, /optionalStates\.orders\.phase === 'ready' && !orderPreview\.length/);
});
