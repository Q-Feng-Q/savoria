const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const pagePath = path.join(root, 'pages', 'merchant', 'family-menu', 'index.js');
const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'));
const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'));

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
}

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function loadPage(runtime, wxOverrides = {}) {
  const previous = {
    Page: global.Page,
    wx: global.wx,
    runtime: require.cache[runtimePath],
    pageApi: require.cache[pageApiPath]
  };
  let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => runtime } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    resolveApiErrorMessage: (error, fallback) => (error && error.message) || fallback
  } };
  global.Page = (value) => { definition = value; };
  global.wx = { showToast() {}, ...wxOverrides };
  delete require.cache[require.resolve(pagePath)];
  require(pagePath);
  const page = Object.assign({}, definition, {
    data: clone(definition.data),
    setData(update, callback) {
      this.data = { ...this.data, ...update };
      if (callback) callback();
    }
  });
  return {
    page,
    cleanup() {
      delete require.cache[require.resolve(pagePath)];
      if (previous.runtime) require.cache[runtimePath] = previous.runtime;
      else delete require.cache[runtimePath];
      if (previous.pageApi) require.cache[pageApiPath] = previous.pageApi;
      else delete require.cache[pageApiPath];
      global.Page = previous.Page;
      global.wx = previous.wx;
    }
  };
}

function menuRows() {
  return [
    { dishId: 4, dishName: '赠送汤', categoryName: '汤羹', basePrice: 12, familyFinalPrice: 0, enabled: false, sortOrder: 9 },
    { dishId: 7, dishName: '焖面', categoryName: '主食', basePrice: 18, familyFinalPrice: 18, enabled: false, sortOrder: 2 }
  ];
}

function tap(id) {
  return { currentTarget: { dataset: { id } } };
}

test('empty menu never reports all selected and numeric/string ids share one selection key', async () => {
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '测试家庭' }],
    getFamilyMenu: async () => menuRows()
  } };
  const { page, cleanup } = loadPage(runtime);
  try {
    page.setData({ familyId: 8 });
    await page.load();
    page.toggleSelection(tap(4));
    assert.deepEqual(page.data.selectedDishMap, { 4: true });
    page.toggleSelection(tap('4'));
    assert.deepEqual(page.data.selectedDishMap, {});
    page.menuDraft = [];
    page.setData({ menuRows: [], selectedDishMap: { 4: true } });
    page.refreshSelection();
    assert.equal(page.data.selectedCount, 0);
    assert.equal(page.data.allSelected, false);
  } finally { cleanup(); }
});

test('select all toggles only the current draft', async () => {
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '测试家庭' }],
    getFamilyMenu: async () => menuRows()
  } };
  const { page, cleanup } = loadPage(runtime);
  try {
    page.setData({ familyId: 8 });
    await page.load();
    page.toggleSelectAll();
    assert.deepEqual(page.data.selectedDishMap, { 4: true, 7: true });
    assert.equal(page.data.selectedCount, 2);
    assert.equal(page.data.allSelected, true);
    page.toggleSelectAll();
    assert.deepEqual(page.data.selectedDishMap, {});
    assert.equal(page.data.allSelected, false);
  } finally { cleanup(); }
});

test('selected bulk enable sends one exact draft and refreshes once', async () => {
  const payloads = [];
  let reads = 0;
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '测试家庭' }],
    getFamilyMenu: async () => {
      reads += 1;
      return reads === 1 ? menuRows() : menuRows().map((item) => ({ ...item, enabled: item.dishId === 4 }));
    },
    saveFamilyMenu: async (familyId, payload) => payloads.push({ familyId, payload: clone(payload) })
  } };
  const { page, cleanup } = loadPage(runtime);
  try {
    page.setData({ familyId: 8 });
    await page.load();
    const originalDraft = clone(page.menuDraft);
    const originalRows = clone(page.data.menuRows);
    page.toggleSelection(tap('4'));
    await page.enableSelected();
    assert.equal(payloads.length, 1);
    assert.equal(payloads[0].familyId, 8);
    assert.deepEqual(payloads[0].payload.items, [
      { dishId: 4, enabled: true, sortOrder: 9, familyFinalPrice: 0 },
      { dishId: 7, enabled: false, sortOrder: 2, familyFinalPrice: 18 }
    ]);
    assert.deepEqual(originalDraft, [
      { dishId: 4, enabled: false, sortOrder: 9, familyFinalPrice: 0 },
      { dishId: 7, enabled: false, sortOrder: 2, familyFinalPrice: 18 }
    ]);
    assert.equal(originalRows[0].enabled, false);
    assert.equal(reads, 2);
    assert.deepEqual(page.data.selectedDishMap, {});
  } finally { cleanup(); }
});

test('one-click enable all preserves order prices and sort then refreshes once', async () => {
  const payloads = [];
  let reads = 0;
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '测试家庭' }],
    getFamilyMenu: async () => { reads += 1; return menuRows(); },
    saveFamilyMenu: async (familyId, payload) => payloads.push({ familyId, payload: clone(payload) })
  } };
  const { page, cleanup } = loadPage(runtime);
  try {
    page.setData({ familyId: 8 });
    await page.load();
    await page.enableAll();
    assert.equal(payloads.length, 1);
    assert.equal(payloads[0].familyId, 8);
    assert.deepEqual(payloads[0].payload.items, [
      { dishId: 4, enabled: true, sortOrder: 9, familyFinalPrice: 0 },
      { dishId: 7, enabled: true, sortOrder: 2, familyFinalPrice: 18 }
    ]);
    assert.equal(reads, 2);
  } finally { cleanup(); }
});

test('failed bulk save performs no refresh and preserves draft rows and selection', async () => {
  let reads = 0;
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '测试家庭' }],
    getFamilyMenu: async () => { reads += 1; return menuRows(); },
    saveFamilyMenu: async () => { throw new Error('保存失败'); }
  } };
  const { page, cleanup } = loadPage(runtime);
  try {
    page.setData({ familyId: 8 });
    await page.load();
    page.toggleSelection(tap(4));
    const before = { draft: clone(page.menuDraft), rows: clone(page.data.menuRows), selection: clone(page.data.selectedDishMap) };
    await page.enableSelected();
    assert.equal(reads, 1);
    assert.deepEqual(page.menuDraft, before.draft);
    assert.deepEqual(page.data.menuRows, before.rows);
    assert.deepEqual(page.data.selectedDishMap, before.selection);
  } finally { cleanup(); }
});

test('empty selected action and already-enabled all action send no request', async () => {
  let saves = 0;
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '测试家庭' }],
    getFamilyMenu: async () => menuRows().map((item) => ({ ...item, enabled: true })),
    saveFamilyMenu: async () => { saves += 1; }
  } };
  const { page, cleanup } = loadPage(runtime);
  try {
    page.setData({ familyId: 8 });
    await page.load();
    await page.enableSelected();
    await page.enableAll();
    assert.equal(saves, 0);
  } finally { cleanup(); }
});

test('manual retry and accepted family switch clear selection separately', async () => {
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '甲' }, { familyId: 9, familyName: '乙' }],
    getFamilyMenu: async () => menuRows()
  } };
  const { page, cleanup } = loadPage(runtime);
  try {
    page.setData({ familyId: 8 });
    await page.load();
    page.toggleSelection(tap(4));
    await page.retryLoad();
    assert.deepEqual(page.data.selectedDishMap, {});
    page.toggleSelection(tap(4));
    await page.bindFamily({ detail: { value: 1 } });
    assert.equal(page.data.familyId, 9);
    assert.deepEqual(page.data.selectedDishMap, {});
  } finally { cleanup(); }
});

test('page-wide lock drops competing writes and family switch then releases', async () => {
  const save = deferred();
  let saves = 0;
  let copies = 0;
  const runtime = { baseUrl: '', merchant: {
    getFamilies: async () => [{ familyId: 8, familyName: '甲' }, { familyId: 9, familyName: '乙' }],
    getFamilyMenu: async () => menuRows(),
    saveFamilyMenu: async () => { saves += 1; if (saves === 1) await save.promise; },
    copyFamilyMenu: async () => { copies += 1; }
  } };
  const { page, cleanup } = loadPage(runtime);
  try {
    page.setData({ familyId: 8 });
    await page.load();
    page.toggleSelection(tap(4));
    const pending = page.enableSelected();
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(page.data.mutationBusy, true);
    await page.toggleDish(tap(7));
    await page.changePrice({ currentTarget: { dataset: { id: 7, delta: 1 } } });
    await page.copyFromFamily();
    page.bindFamily({ detail: { value: 1 } });
    await page.enableAll();
    assert.equal(saves, 1);
    assert.equal(copies, 0);
    assert.equal(page.data.familyId, 8);
    save.resolve();
    await pending;
    assert.equal(page.data.mutationBusy, false);
    await page.toggleDish(tap(7));
    assert.equal(saves, 2);
  } finally { cleanup(); }
});
