const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');

async function mount(pageName, merchant, run) {
  const pagePath = path.join(root, 'pages', 'merchant', pageName, 'index.js');
  const runtimePath = require.resolve(path.join(root, 'utils/api-runtime.js'));
  const apiPath = require.resolve(path.join(root, 'utils/page-api.js'));
  const previous = { page: global.Page, wx: global.wx, runtime: require.cache[runtimePath], api: require.cache[apiPath] };
  const calls = { navigation: [], toasts: [] };
  let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant }) } };
  require.cache[apiPath] = { exports: {
    requireSession: () => ({ merchantId: 2 }),
    resolveApiErrorMessage: (error, fallback) => error.message || fallback,
    showApiError: (error) => calls.toasts.push(error.message)
  } };
  global.Page = (value) => { definition = value; };
  global.wx = {
    navigateTo: (value) => calls.navigation.push(value.url),
    navigateBack: (value) => calls.navigation.push(value || { delta: 1 }),
    showToast: (value) => calls.toasts.push(value),
    showModal: async () => ({ confirm: true })
  };
  try {
    delete require.cache[pagePath];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update) {
        for (const [key, value] of Object.entries(update)) {
          if (key.includes('.')) {
            const [parent, child] = key.split('.');
            this.data[parent][child] = value;
          } else this.data[key] = value;
        }
      }
    });
    return await run(page, calls);
  } finally {
    delete require.cache[pagePath];
    if (previous.runtime) require.cache[runtimePath] = previous.runtime; else delete require.cache[runtimePath];
    if (previous.api) require.cache[apiPath] = previous.api; else delete require.cache[apiPath];
    global.Page = previous.page;
    global.wx = previous.wx;
  }
}

test('ingredient library is searchable and opens dedicated create and edit routes', async () => {
  const app = JSON.parse(read('app.json'));
  const markup = read('pages/merchant/ingredient-edit/index.wxml');
  assert.ok(app.pages.includes('pages/merchant/ingredient-form/index'));
  assert.match(markup, /bindinput="bindSearch"/);
  assert.match(markup, /bindtap="openCreate"/);
  assert.match(markup, /bindtap="openEdit"/);
  assert.doesNotMatch(markup, /bindtap="saveIngredient"/);

  await mount('ingredient-edit', {
    getIngredients: async () => [
      { ingredientId: 7, name: '豆腐干', category: '豆制品', unit: '克', removable: true },
      { ingredientId: 8, name: '大米', category: '谷物', unit: '克', removable: true }
    ]
  }, async (page, calls) => {
    await page.load();
    page.bindSearch({ detail: { value: '豆' } });
    assert.deepEqual(page.data.visibleIngredients.map((item) => item.id), [7]);
    page.openEdit({ currentTarget: { dataset: { id: 7 } } });
    page.openCreate();
    assert.deepEqual(calls.navigation, [
      '/pages/merchant/ingredient-form/index?id=7',
      '/pages/merchant/ingredient-form/index'
    ]);
  });
});

test('ingredient form loads the requested record and reports a missing record', async () => {
  await mount('ingredient-form', {
    getIngredients: async () => [{ ingredientId: 7, name: '豆腐干', category: '豆制品', unit: '克' }]
  }, async (page) => {
    page.onLoad({ id: '7' });
    await page.load();
    assert.equal(page.data.phase, 'ready');
    assert.equal(page.data.form.name, '豆腐干');
    page.onLoad({ id: '999' });
    await page.load();
    assert.equal(page.data.phase, 'error');
    assert.match(page.data.errorMessage, /不存在|未找到/);
  });
});

test('ingredient form saves the selected ingredient and returns to the library', async () => {
  const updates = [];
  await mount('ingredient-form', {
    getIngredients: async () => [{ ingredientId: 7, name: '豆腐干', category: '豆制品', unit: '克' }],
    updateIngredient: async (id, payload) => updates.push([id, payload])
  }, async (page, calls) => {
    page.onLoad({ id: '7' });
    await page.load();
    page.setData({ form: { name: '香干', category: '豆制品', unit: '克' } });
    await page.saveIngredient();
    assert.deepEqual(updates, [['7', { name: '香干', unit: '克', category: '豆制品' }]]);
    assert.deepEqual(calls.navigation.at(-1), { delta: 1 });
  });
});

test('new ingredient saves trimmed fields without fetching a record', async () => {
  const creates = [];
  await mount('ingredient-form', {
    getIngredients: async () => assert.fail('new ingredient should not load the library'),
    createIngredient: async (payload) => creates.push(payload)
  }, async (page, calls) => {
    page.onLoad({});
    await page.onShow();
    assert.equal(page.data.phase, 'ready');
    page.setData({ form: { name: '  山药  ', unit: ' 克 ', category: ' 根茎 ' } });
    await page.saveIngredient();
    assert.deepEqual(creates, [{ name: '山药', unit: '克', category: '根茎' }]);
    assert.deepEqual(calls.navigation.at(-1), { delta: 1 });
  });
});

test('ingredient library refreshes its filtered rows when returning from the form', async () => {
  let reads = 0;
  await mount('ingredient-edit', {
    getIngredients: async () => {
      reads += 1;
      return reads === 1
        ? [{ ingredientId: 1, name: '大米', unit: '克', category: '谷物' }]
        : [{ ingredientId: 1, name: '小米', unit: '克', category: '谷物' }];
    }
  }, async (page) => {
    await page.onShow();
    page.bindSearch({ detail: { value: '米' } });
    await page.onShow();
    assert.equal(reads, 2);
    assert.deepEqual(page.data.visibleIngredients.map((item) => item.name), ['小米']);
    assert.equal(page.data.query, '米');
  });
});

test('ingredient editor keeps an unfinished draft when the app becomes visible again', async () => {
  let reads = 0;
  await mount('ingredient-form', {
    getIngredients: async () => {
      reads += 1;
      return [{ ingredientId: 7, name: '豆腐干', category: '豆制品', unit: '克' }];
    }
  }, async (page) => {
    page.onLoad({ id: '7' });
    await page.onShow();
    page.setData({ 'form.name': '正在编辑的香干' });
    await page.onShow();
    assert.equal(page.data.form.name, '正在编辑的香干');
    assert.equal(reads, 1);
  });
});
