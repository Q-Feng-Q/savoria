const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const { createMerchantService } = require('../services/merchant');
const {
  buildTemplateSnapshot,
  validateTemplateSnapshot,
  decorateChangeRequest
} = require('../utils/dish-template-change');

test('merchant service exposes the complete template change request workflow', async () => {
  const calls = [];
  const service = createMerchantService({ request: async (pathname, options) => {
    calls.push({ pathname, options });
    return { code: 0, data: {} };
  }});

  const targetSnapshot = { schemaVersion: 2, name: '豆角焖面' };
  await service.submitDishTemplateChange(7, { submitNote: '补充特色菜', targetSnapshot });
  await service.submitImportedDishTemplateChange(31, { submitNote: '同步商户实测用量' });
  await service.getDishTemplateChanges({ status: 'PENDING', keyword: '豆角', page: 2, pageSize: 20 });
  await service.getDishTemplateChangeDetail(9);
  await service.withdrawDishTemplateChange(9);

  assert.deepEqual(calls[0], {
    pathname: '/api/merchant/dish-templates/7/change-requests',
    options: { method: 'POST', data: { submitNote: '补充特色菜', targetSnapshot } }
  });
  assert.deepEqual(calls[1], {
    pathname: '/api/merchant/dishes/31/template-change-requests',
    options: { method: 'POST', data: { submitNote: '同步商户实测用量' } }
  });
  assert.match(calls[2].pathname, /^\/api\/merchant\/dish-template-change-requests\?/);
  assert.match(calls[2].pathname, /status=PENDING/);
  assert.match(calls[2].pathname, /keyword=%E8%B1%86%E8%A7%92/);
  assert.equal(calls[3].pathname, '/api/merchant/dish-template-change-requests/9');
  assert.equal(calls[4].pathname, '/api/merchant/dish-template-change-requests/9/withdraw');
});

test('template snapshot v2 keeps editable data and strips server-owned fields', () => {
  const snapshot = buildTemplateSnapshot({
    categoryId: 3,
    name: '豆角焖面',
    description: '北方家常焖面',
    imageUrl: '/images/dish-templates/dou-jiao-men-mian.jpg',
    imageSourceUrl: 'https://example.com/source',
    imageAuthor: '作者',
    imageLicense: '授权使用',
    referencePrice: '18.50',
    tasteTags: ['咸香', '家常'],
    mealTags: ['LUNCH', 'DINNER'],
    sortOrder: 10,
    enabled: true,
    ingredients: [{ itemId: 'ingredient-1', ingredientName: '豆角', ingredientCategory: '蔬菜', quantityStatus: 'VERIFIED', quantity: '100', unit: '克', calcType: 'FIXED' }],
    cookingSteps: [{ itemId: 'step-1', stepNo: 1, title: '焖制', content: '小火焖熟', durationSeconds: 600, heatLevel: '小火' }]
  });

  assert.deepEqual(snapshot, {
    schemaVersion: 2,
    categoryId: 3,
    name: '豆角焖面',
    description: '北方家常焖面',
    referencePrice: 18.5,
    tasteTags: ['咸香', '家常'],
    mealTags: ['LUNCH', 'DINNER'],
    sortOrder: 10,
    enabled: true,
    ingredients: [{
      itemId: 'ingredient-1', ingredientName: '豆角', ingredientCategory: '蔬菜',
      quantityStatus: 'VERIFIED', quantity: 100, unit: '克', calcType: 'FIXED',
      sourceText: null, sourceQuantityText: null, componentTemplateId: null,
      componentMultiplier: null, sortOrder: 1
    }],
    cookingSteps: [{
      itemId: 'step-1', stepNo: 1, title: '焖制', content: '小火焖熟',
      durationSeconds: 600, temperatureText: null, heatLevel: '小火', componentTemplateId: null
    }]
  });
  assert.equal('imageUrl' in snapshot, false);
  assert.equal('imageSourceUrl' in snapshot, false);
  assert.equal('dataStatus' in snapshot, false);
  assert.equal(validateTemplateSnapshot(snapshot), '');
  assert.equal(validateTemplateSnapshot({ ...snapshot, ingredients: [] }), '模板菜品至少需要 1 项食材');
});

test('template snapshot v2 allows null price and nullable quantity states', () => {
  const snapshot = buildTemplateSnapshot({
    categoryId: 3,
    name: '待完善菜谱',
    description: null,
    referencePrice: null,
    tasteTags: [],
    mealTags: [],
    sortOrder: 0,
    enabled: true,
    ingredients: [{
      itemId: 'missing-1', ingredientName: '调料适量', ingredientCategory: '调味',
      quantityStatus: 'MISSING', quantity: null, unit: null, calcType: null
    }],
    cookingSteps: []
  });
  assert.equal(snapshot.referencePrice, null);
  assert.equal(snapshot.description, null);
  assert.equal(snapshot.ingredients[0].quantity, null);
  assert.equal(snapshot.ingredients[0].unit, null);
  assert.equal(snapshot.ingredients[0].calcType, null);
  assert.equal(validateTemplateSnapshot(snapshot), '');
});

test('change request decoration exposes readable status and stale state', () => {
  assert.deepEqual(decorateChangeRequest({ status: 'PENDING', submittedAt: '2026-08-15T10:20:30', stale: true }), {
    status: 'PENDING',
    submittedAt: '2026-08-15T10:20:30',
    stale: true,
    statusLabel: '待审核',
    statusTone: 'pending',
    submittedText: '2026-08-15 10:20',
    canWithdraw: true,
    staleLabel: '模板已发生变化'
  });
});

test('mini program registers submit, list and detail pages with discoverable entries', () => {
  const root = path.resolve(__dirname, '..');
  const app = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));
  const detail = fs.readFileSync(path.join(root, 'pages/merchant/dish-template-detail/index.wxml'), 'utf8');
  const market = fs.readFileSync(path.join(root, 'pages/merchant/dish-templates/index.wxml'), 'utf8');
  const merchantDishes = fs.readFileSync(path.join(root, 'pages/merchant/merchant-dishes/index.wxml'), 'utf8');

  assert.ok(app.pages.includes('pages/merchant/dish-template-change-edit/index'));
  assert.ok(app.pages.includes('pages/merchant/dish-template-changes/index'));
  assert.ok(app.pages.includes('pages/merchant/dish-template-change-detail/index'));
  assert.match(detail, /申请修改模板/);
  assert.match(market, /修改申请/);
  assert.match(merchantDishes, /同步模板/);
  const editor = fs.readFileSync(path.join(root, 'pages/merchant/dish-template-change-edit/index.wxml'), 'utf8');
  assert.doesNotMatch(editor, /更换图片|图片来源页面|图片授权|form\.imageUrl/);
  assert.match(editor, /制作步骤/);
  const changeDetail = fs.readFileSync(path.join(root, 'pages/merchant/dish-template-change-detail/index.wxml'), 'utf8');
  assert.match(changeDetail, /制作步骤对比/);
  assert.match(changeDetail, /baseSteps/);
  assert.match(changeDetail, /targetSteps/);
});
