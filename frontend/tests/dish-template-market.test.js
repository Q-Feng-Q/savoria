const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {
  createDishTemplateSelection,
  decorateTemplateDetail,
  decorateTemplateRows
} = require('../utils/dish-template-selection');
const { createMerchantService } = require('../services/merchant');

test('template selection ignores imported and incomplete items and caps selection at 100', () => {
  const selection = createDishTemplateSelection();
  const rows = Array.from({ length: 103 }, (_, index) => ({
    templateId: index + 1,
    imported: index === 0,
    importable: index !== 1
  }));
  const result = selection.selectPage(rows);
  assert.equal(result.selectedIds.length, 100);
  assert.equal(result.selectedIds.includes(1), false);
  assert.equal(result.selectedIds.includes(2), false);
  assert.equal(result.limitReached, true);
});

test('template selection toggles one item without retaining imported item', () => {
  const selection = createDishTemplateSelection([2]);
  assert.deepEqual(selection.toggle({ templateId: 2, imported: false }).selectedIds, []);
  assert.deepEqual(selection.toggle({ templateId: 3, imported: true }).selectedIds, []);
});

test('template view models keep null image and price explicit', () => {
  const rows = decorateTemplateRows([{
    templateId: 7,
    imageUrl: null,
    referencePrice: null,
    tasteTags: [],
    sourceType: 'COOK_LIKE_HOC',
    missingSteps: true
  }], [], (value) => value ? `https://api.test${value}` : '');

  assert.equal(rows[0].hasImage, false);
  assert.equal(rows[0].imageUrl, '');
  assert.equal(rows[0].priceText, '价格待完善');
  assert.equal(rows[0].sourceLabel, 'CookLikeHOC');
  assert.equal(rows[0].stepsLabel, '步骤待补充');
  assert.equal(rows[0].importable, false);
  assert.equal(rows[0].readinessLabel, '待完善后导入');

  const detail = decorateTemplateDetail({
    imageUrl: null,
    referencePrice: null,
    sourceType: 'LOCAL_EXTENSION',
    ingredients: [{ quantityStatus: 'MISSING', quantity: null, unit: null }],
    cookingSteps: [{ stepNo: 2, content: '第二步' }, { stepNo: 1, content: '第一步' }]
  }, (value) => value || '');
  assert.equal(detail.hasImage, false);
  assert.equal(detail.priceText, '价格待完善');
  assert.equal(detail.sourceLabel, '本地扩展');
  assert.equal(detail.ingredients[0].quantityText, '用量待完善');
  assert.deepEqual(detail.cookingSteps.map((item) => item.stepNo), [1, 2]);
});

test('merchant service calls real template endpoints', async () => {
  const calls = [];
  const service = createMerchantService({ request: async (url, options) => {
    calls.push([url, options]);
    return { code: 0, data: {} };
  }});
  await service.getDishTemplateCategories();
  await service.getDishTemplates({ page: 2, pageSize: 20, keyword: '鸡' });
  await service.getDishTemplateDetail(7);
  await service.importDishTemplates([7, 8]);
  await service.importAllDishTemplates();
  assert.equal(calls[0][0], '/api/merchant/dish-template-categories');
  assert.match(calls[1][0], /\/api\/merchant\/dish-templates\?/);
  assert.equal(calls[2][0], '/api/merchant/dish-templates/7');
  assert.deepEqual(calls[3], ['/api/merchant/dish-templates/import', { method: 'POST', data: { templateIds: [7, 8] } }]);
  assert.deepEqual(calls[4], ['/api/merchant/dish-templates/import-all', { method: 'POST' }]);
});

test('mini program registers template market and detail pages', () => {
  const app = JSON.parse(fs.readFileSync(path.join(__dirname, '../app.json'), 'utf8'));
  assert.ok(app.pages.includes('pages/merchant/dish-templates/index'));
  assert.ok(app.pages.includes('pages/merchant/dish-template-detail/index'));
});

async function withTemplatePage({ merchant, wxMock, run }) {
  const root = path.resolve(__dirname, '..');
  const pagePath = path.join(root, 'pages/merchant/dish-templates/index.js');
  const runtimePath = require.resolve(path.join(root, 'utils/api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils/page-api.js'));
  const previousPage = global.Page;
  const previousWx = global.wx;
  const previousRuntime = require.cache[runtimePath];
  const previousPageApi = require.cache[pageApiPath];
  let definition;
  const errors = [];
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant }) } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    showApiError: (error, fallback) => errors.push(error.message || fallback)
  } };
  global.Page = (value) => { definition = value; };
  global.wx = wxMock;
  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update) { this.data = { ...this.data, ...update }; }
    });
    return await run(page, errors);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath];
    if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath];
    global.Page = previousPage;
    global.wx = previousWx;
  }
}

test('import all requires confirmation and cancellation sends no request', async () => {
  let imports = 0;
  await withTemplatePage({
    merchant: { importAllDishTemplates: async () => { imports += 1; } },
    wxMock: { showModal: async () => ({ confirm: false }) },
    run: async (page) => {
      page.load = async () => {};
      await page.importAll();
      assert.equal(imports, 0);
      assert.equal(page.data.importingAll, false);
    }
  });
});

test('confirmed import all reports imported and skipped counts then refreshes', async () => {
  let imports = 0;
  let refreshes = 0;
  const modals = [];
  await withTemplatePage({
    merchant: { importAllDishTemplates: async () => {
      imports += 1;
      return { importedCount: 190, skippedCount: 8 };
    } },
    wxMock: { showModal: async (options) => {
      modals.push(options);
      return modals.length === 1 ? { confirm: true } : { confirm: true };
    } },
    run: async (page) => {
      page.load = async (options) => {
        refreshes += 1;
        assert.deepEqual(options, { reset: true, silent: true });
      };
      page.selection.toggle({ templateId: 7, imported: false });
      page.setData({ selectedIds: [7] });
      await page.importAll();
      assert.equal(imports, 1);
      assert.equal(refreshes, 1);
      assert.deepEqual(page.data.selectedIds, []);
      assert.match(modals[1].content, /成功 190 道，跳过 8 道/);
      assert.equal(page.data.importingAll, false);
    }
  });
});

test('selection and import all locks are bidirectional including the confirmation race', async () => {
  let resolveConfirmation;
  let allImports = 0;
  let selectedImports = 0;
  const confirmation = new Promise((resolve) => { resolveConfirmation = resolve; });
  await withTemplatePage({
    merchant: {
      importAllDishTemplates: async () => { allImports += 1; return {}; },
      importDishTemplates: async () => { selectedImports += 1; return {}; }
    },
    wxMock: { showModal: () => confirmation },
    run: async (page) => {
      page.load = async () => {};
      page.setData({ selectedIds: [2] });
      const pendingAll = page.importAll();
      page.setData({ importing: true });
      resolveConfirmation({ confirm: true });
      await pendingAll;
      assert.equal(allImports, 0);

      page.setData({ importing: false, importingAll: true });
      await page.importSelected();
      await page.importSelected();
      assert.equal(selectedImports, 0);
    }
  });
});

test('failed import all preserves selection and rows while releasing its own lock', async () => {
  await withTemplatePage({
    merchant: { importAllDishTemplates: async () => { throw new Error('网络中断'); } },
    wxMock: { showModal: async () => ({ confirm: true }) },
    run: async (page, errors) => {
      page.load = async () => assert.fail('failure must not refresh');
      page.setData({ selectedIds: [5], items: [{ templateId: 5 }] });
      await page.importAll();
      assert.deepEqual(page.data.selectedIds, [5]);
      assert.equal(page.data.items.length, 1);
      assert.equal(page.data.importingAll, false);
      assert.equal(page.data.importing, false);
      assert.deepEqual(errors, ['网络中断']);
    }
  });
});

test('merchant dish management exposes an accessible custom system library entry', () => {
  const root = path.resolve(__dirname, '..');
  const source = fs.readFileSync(path.join(root, 'pages/merchant/merchant-dishes/index.js'), 'utf8');
  const markup = fs.readFileSync(path.join(root, 'pages/merchant/merchant-dishes/index.wxml'), 'utf8');
  assert.match(source, /openDishTemplates\(\)\s*\{\s*wx\.navigateTo\(\{\s*url:\s*'\/pages\/merchant\/dish-templates\/index'/);
  assert.match(markup, /<view class="ui-button secondary-action"[^>]*bindtap="openDishTemplates"[^>]*aria-role="button"[^>]*aria-label="打开系统菜库"[^>]*hover-class="ui-button--pressed"[^>]*>系统菜库<\/view>/);
  assert.match(markup, /<view class="ui-button primary-action"[^>]*aria-role="button"[^>]*hover-class="ui-button--pressed"/);
  assert.doesNotMatch(markup, /<button\b/);
});

test('template market renders a custom import-all action and mutually disabled import controls', () => {
  const root = path.resolve(__dirname, '..');
  const markup = fs.readFileSync(path.join(root, 'pages/merchant/dish-templates/index.wxml'), 'utf8');
  assert.match(markup, /<view class="ui-button import-all-action[^>]*bindtap="importAll"[^>]*aria-role="button"[^>]*aria-label="导入全部系统菜品"[^>]*aria-disabled="\{\{importing \|\| importingAll\}\}"[^>]*aria-busy="\{\{importingAll\}\}"[^>]*hover-class="ui-button--pressed"/);
  assert.match(markup, /import-action[^>]*importing \|\| importingAll[^>]*aria-disabled="\{\{!selectedIds\.length \|\| importing \|\| importingAll\}\}"[^>]*aria-busy="\{\{importing \|\| importingAll\}\}"/);
  assert.doesNotMatch(markup, /<button\b/);
  assert.match(markup, /wx:if="\{\{item\.hasImage\}\}"/);
  assert.match(markup, /class="template-image-placeholder"/);
});

test('template detail renders source, nullable states and ordered cooking steps', () => {
  const root = path.resolve(__dirname, '..');
  const markup = fs.readFileSync(path.join(root, 'pages/merchant/dish-template-detail/index.wxml'), 'utf8');
  assert.match(markup, /detail\.hasImage/);
  assert.match(markup, /detail\.priceText/);
  assert.match(markup, /detail\.sourceLabel/);
  assert.match(markup, /detail\.cookingSteps/);
  assert.match(markup, /制作步骤/);
});

test('merchant dish editing preserves imported cooking step facts', () => {
  const root = path.resolve(__dirname, '..');
  const pagePath = path.join(root, 'pages', 'merchant', 'dish-edit', 'index.js');
  const previousPage = global.Page;
  global.Page = () => {};
  try {
    delete require.cache[require.resolve(pagePath)];
    const { buildDishPayload } = require(pagePath);
    const payload = buildDishPayload({
      name: '豆角焖面', categoryId: 1, basePrice: 18, cookingSteps: [{
        stepNo: 3, title: '焖制', content: '盖盖焖熟', durationSeconds: 600,
        temperatureText: '保持微沸', heatLevel: '小火', componentTemplateId: 9
      }]
    });
    assert.deepEqual(payload.cookingSteps[0], {
      imageUrls: [],
      stepNo: 1, title: '焖制', content: '盖盖焖熟', durationSeconds: 600,
      temperatureText: '保持微沸', heatLevel: '小火', componentTemplateId: 9
    });
    const markup = fs.readFileSync(path.join(root, 'pages', 'merchant', 'dish-edit', 'index.wxml'), 'utf8');
    assert.match(markup, /item\.durationSeconds/);
    assert.match(markup, /item\.temperatureText/);
    assert.match(markup, /item\.heatLevel/);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    global.Page = previousPage;
  }
});

test('template market appends every backend page and stops when total is reached', async () => {
  const calls = [];
  await withTemplatePage({
    merchant: {
      getDishTemplateCategories: async () => [],
      getDishTemplates: async (query) => {
        calls.push(query.page);
        if (query.page === 1) return {
          items: Array.from({ length: 20 }, (_, index) => ({ templateId: index + 1, name: `菜品${index + 1}` })),
          page: 1, total: 25
        };
        return {
          items: Array.from({ length: 5 }, (_, index) => ({ templateId: index + 21, name: `菜品${index + 21}` })),
          page: 2, total: 25
        };
      }
    },
    wxMock: {},
    run: async (page) => {
      await page.load({ reset: true });
      assert.equal(page.data.items.length, 20);
      await page.loadMore();
      assert.equal(page.data.items.length, 25);
      assert.equal(page.data.page, 2);
      await page.loadMore();
      assert.deepEqual(calls, [1, 2]);
      assert.equal(page.data.loadingMore, false);
      assert.equal(page.data.hasMore, false);
    }
  });
});

test('silent template refresh replaces page one without hiding the current list', async () => {
  let resolveRows;
  const pendingRows = new Promise((resolve) => { resolveRows = resolve; });
  await withTemplatePage({
    merchant: {
      getDishTemplateCategories: async () => [],
      getDishTemplates: () => pendingRows
    },
    wxMock: {},
    run: async (page) => {
      const updates = [];
      page.data.loading = false;
      page.data.items = [{ templateId: 99, name: '当前菜品' }];
      page.setData = function setData(update) {
        updates.push(update);
        this.data = { ...this.data, ...update };
      };
      const refresh = page.load({ reset: true, silent: true });
      await new Promise((resolve) => setImmediate(resolve));
      assert.equal(page.data.loading, false);
      assert.equal(page.data.items[0].templateId, 99);
      assert.equal(updates.some((update) => update.loading === true), false);
      resolveRows({ items: [{ templateId: 1, name: '刷新菜品' }], page: 1, total: 1 });
      await refresh;
      assert.equal(page.data.items[0].templateId, 1);
    }
  });
});

test('page reach-bottom delegates to guarded template load more', () => {
  const root = path.resolve(__dirname, '..');
  const source = fs.readFileSync(path.join(root, 'pages/merchant/dish-templates/index.js'), 'utf8');
  const config = JSON.parse(fs.readFileSync(path.join(root, 'pages/merchant/dish-templates/index.json'), 'utf8'));
  assert.match(source, /onReachBottom\(\)\s*\{\s*return this\.loadMore\(\);\s*\}/);
  assert.ok(config.onReachBottomDistance >= 80);
});

test('reset refresh owns the pagination lock and ignores an older load-more result', async () => {
  let resolveMore;
  let resolveReset;
  let pageTwoCalls = 0;
  const pendingMore = new Promise((resolve) => { resolveMore = resolve; });
  const pendingReset = new Promise((resolve) => { resolveReset = resolve; });
  await withTemplatePage({
    merchant: {
      getDishTemplateCategories: async () => [],
      getDishTemplates: ({ page }) => {
        if (page === 2) {
          pageTwoCalls += 1;
          return pendingMore;
        }
        return pendingReset;
      }
    },
    wxMock: {},
    run: async (page) => {
      page._listGeneration = 1;
      page.setData({
        loading: false,
        page: 1,
        total: 40,
        hasMore: true,
        items: Array.from({ length: 20 }, (_, index) => ({ templateId: index + 1 }))
      });
      const loadMore = page.loadMore();
      await new Promise((resolve) => setImmediate(resolve));
      assert.equal(page.data.loadingMore, true);

      const reset = page.load({ reset: true, silent: true });
      assert.equal(page.data.refreshing, true);
      assert.equal(page.data.loadingMore, false);
      await page.loadMore();
      assert.equal(pageTwoCalls, 1);

      resolveReset({ items: [{ templateId: 101, name: '新第一页' }], page: 1, total: 1 });
      await reset;
      resolveMore({ items: [{ templateId: 21, name: '旧第二页' }], page: 2, total: 40 });
      await loadMore;

      assert.deepEqual(page.data.items.map((item) => item.templateId), [101]);
      assert.equal(page.data.refreshing, false);
      assert.equal(page.data.loadingMore, false);
      assert.equal(page.data.hasMore, false);
    }
  });
});

test('successful imports clear row checkmarks before attempting refresh', () => {
  const root = path.resolve(__dirname, '..');
  const source = fs.readFileSync(path.join(root, 'pages/merchant/dish-templates/index.js'), 'utf8');
  assert.equal((source.match(/this\.syncSelection\(this\.selection\.clear\(\)\)/g) || []).length, 4);
});

test('type filter resets selection and is sent for every template page', async () => {
  const calls = [];
  await withTemplatePage({
    merchant: {
      getDishTemplateCategories: async () => [],
      getDishTemplates: async (query) => { calls.push(query); return { page: query.page, total: 2, items: [{ templateId: query.page, productType: 'NOURISHMENT' }] }; }
    },
    wxMock: {},
    run: async (page) => {
      page.selection.selectPage([{ templateId: 8, importable: true }]);
      page.syncSelection(page.selection.snapshot());
      await page.changeProductType({ detail: { value: 2 } });
      assert.deepEqual(page.data.selectedIds, []);
      assert.equal(page.data.page, 1);
      await page.loadMore();
      assert.deepEqual(calls.map((item) => [item.productType, item.page]), [['NOURISHMENT', 1], ['NOURISHMENT', 2]]);
      assert.equal(page.data.items.length, 2);
    }
  });
});
