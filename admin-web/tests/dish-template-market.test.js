const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const read = (file) => fs.readFileSync(path.join(__dirname, '..', file), 'utf8');

test('PC merchant backend exposes template market route and real APIs', () => {
  const router = read('src/router/index.js');
  const api = read('src/api/dishes.js');
  const view = read('src/views/merchant/DishTemplatesView.vue');
  assert.match(router, /path: 'dish-templates'/);
  assert.match(api, /\/api\/merchant\/dish-template-categories/);
  assert.match(api, /\/api\/merchant\/dish-templates\/import/);
  assert.match(view, /已导入/);
  assert.match(view, /全选当前页/);
  assert.match(view, /selectedIds/);
});
