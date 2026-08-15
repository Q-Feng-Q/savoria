const test = require('node:test');
const assert = require('node:assert/strict');
const { DEFAULT_DISH_CATEGORIES, mergeDishCategories, findCategoryIndex } = require('../utils/categories');

test('dish categories are predefined and keep existing custom categories compatible', () => {
  const categories = mergeDishCategories(DEFAULT_DISH_CATEGORIES, [
    { category: '家常菜' },
    { category: '节日菜' }
  ]);

  assert.deepEqual(categories.slice(0, 4), ['早餐', '家常菜', '主食', '汤']);
  assert.equal(categories.includes('节日菜'), true);
});

test('unknown category falls back to other category index', () => {
  const categories = ['早餐', '其他'];

  assert.equal(findCategoryIndex(categories, '不存在'), 1);
});
