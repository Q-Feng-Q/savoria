const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');

test('merchant dish review history has an API module, route and navigation entry', () => {
  const api = fs.readFileSync(path.join(root, 'src/api/merchant-dish-reviews.js'), 'utf8');
  const router = fs.readFileSync(path.join(root, 'src/router/index.js'), 'utf8');
  const config = fs.readFileSync(path.join(root, 'ui-config.js'), 'utf8');
  const view = fs.readFileSync(path.join(root, 'src/views/merchant/DishReviewsView.vue'), 'utf8');

  assert.match(api, /\/api\/merchant\/dish-reviews/);
  assert.match(api, /withdraw/);
  assert.match(router, /merchant-dish-reviews/);
  assert.match(config, /merchant-dish-reviews/);
  assert.match(view, /withdrawDishReview/);
});

