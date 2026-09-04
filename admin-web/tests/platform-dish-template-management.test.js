const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('platform template API covers management and controlled image review', () => {
  const api = read('src/api/admin-dish-templates.js');
  assert.match(api, /\/api\/admin\/dish-templates/);
  assert.match(api, /source-records/);
  assert.match(api, /dish-template-assets\/\$\{assetId\}\/preview/);
  assert.match(api, /image-promotion/);
  assert.match(api, /image-assets\/\$\{assetId\}\/reject/);
});

test('platform template routes and menu are restricted to platform administrators', () => {
  const router = read('src/router/index.js');
  const config = read('ui-config.js');
  const layout = read('src/layouts/AdminLayout.vue');
  assert.match(router, /PlatformDishTemplatesView/);
  assert.match(router, /PlatformDishTemplateDetailView/);
  assert.match(router, /path: 'platform-dish-templates'/);
  assert.match(router, /path: 'platform-dish-templates\/:templateId'/);
  assert.match(router, /requiresPlatformAdmin:\s*true/);
  assert.match(config, /平台菜谱模板/);
  assert.match(layout, /platform-dish-templates/);
});

test('platform list exposes every backend filter and derived status', () => {
  const view = read('src/views/platform/DishTemplatesView.vue');
  for (const field of ['sourceType', 'templateType', 'dataStatus', 'sourceCategory', 'missingImage', 'missingSteps']) {
    assert.match(view, new RegExp(field));
  }
  assert.match(view, /procurementReady/);
  assert.match(view, /imageRightsStatus/);
  assert.match(view, /价格待完善/);
});

test('platform detail uses optimistic updates and controlled asset actions', () => {
  const view = read('src/views/platform/DishTemplateDetailView.vue');
  assert.match(view, /expectedVersion/);
  assert.match(view, /schemaVersion:\s*2/);
  assert.match(view, /internalAssetReviews/);
  assert.match(view, /previewDishTemplateAsset/);
  assert.match(view, /promoteDishTemplateImage/);
  assert.match(view, /rejectDishTemplateImage/);
  assert.match(view, /cookingSteps/);
  assert.match(view, /quantityStatus/);
  assert.doesNotMatch(view, /storageKey|privatePath|filesystemPath/);
});
