const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('PC client exposes merchant submit/history and platform review endpoints', () => {
  const api = read('src/api/dish-template-changes.js');
  assert.match(api, /\/api\/merchant\/dish-templates\/\$\{templateId\}\/change-requests/);
  assert.match(api, /\/api\/merchant\/dish-template-change-requests/);
  assert.match(api, /\/api\/admin\/dish-template-change-requests/);
  assert.match(api, /approve/);
  assert.match(api, /reject/);
  assert.match(api, /withdraw/);
});

test('PC routes and navigation expose merchant applications and platform review', () => {
  const router = read('src/router/index.js');
  const config = read('ui-config.js');
  const layout = read('src/layouts/AdminLayout.vue');

  assert.match(router, /DishTemplateChangesView/);
  assert.match(router, /DishTemplateChangeReviewsView/);
  assert.match(router, /path: 'dish-template-changes'/);
  assert.match(router, /path: 'dish-template-change-reviews'/);
  assert.match(config, /模板修改申请/);
  assert.match(config, /模板修改审核/);
  assert.match(layout, /dish-template-change-reviews/);
});

test('merchant template market supports opening a full change request editor', () => {
  const view = read('src/views/merchant/DishTemplatesView.vue');
  const utility = read('src/utils/dish-template-changes.js');
  assert.match(view, /申请修改/);
  assert.match(view, /submitDishTemplateChange/);
  assert.doesNotMatch(view, /imageSourceUrl|imageAuthor|imageLicense|uploadTemplateImage/);
  assert.match(view, /ingredientCategory/);
  assert.match(view, /mealTags/);
  assert.match(view, /cookingSteps/);
  assert.match(view, /quantityStatus/);
  assert.match(utility, /schemaVersion:\s*2/);
  assert.doesNotMatch(utility, /imageUrl:\s*text\(form\.imageUrl\)/);
});

test('merchant dish management can submit an imported dish without building a template snapshot', () => {
  const api = read('src/api/dishes.js');
  const view = read('src/views/merchant/DishesView.vue');

  assert.match(api, /submitImportedDishTemplateChange/);
  assert.match(api, /\/api\/merchant\/dishes\/\$\{dishId\}\/template-change-requests/);
  assert.match(view, /templateImported/);
  assert.match(view, /同步模板/);
  assert.match(view, /submitImportedDishTemplateChange/);
  assert.doesNotMatch(view, /buildTemplateSnapshot/);
});

test('platform review shows both snapshots, stale warning and explicit decisions', () => {
  const view = read('src/views/platform/DishTemplateChangeReviewsView.vue');
  const comparison = read('src/components/SnapshotComparison.vue');
  assert.match(view, /baseSnapshot/);
  assert.match(view, /targetSnapshot/);
  assert.match(view, /stale/);
  assert.match(view, /approveDishTemplateChange/);
  assert.match(view, /rejectDishTemplateChange/);
  assert.match(comparison, /制作步骤对比/);
  assert.match(comparison, /baseSteps/);
  assert.match(comparison, /targetSteps/);
});
