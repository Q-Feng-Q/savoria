const test = require('node:test');
const assert = require('node:assert/strict');
const { getDishDetailView } = require('../utils/dish');

const dish = {
  id: 'tomato-egg',
  name: '番茄炒蛋',
  category: '家常菜',
  description: '酸甜开胃。',
  baseServings: 2,
  tasteTags: ['正常'],
  status: 'on',
  ingredients: [{ id: 'egg', name: '鸡蛋' }],
  cookingSteps: [
    { title: '备菜', content: '番茄切块，鸡蛋打散。' },
    { title: '炒制', content: '先炒鸡蛋，再下番茄。' }
  ]
};

test('hides cooking steps from normal users', () => {
  const detail = getDishDetailView(dish, 'user');

  assert.equal(detail.canViewCookingSteps, false);
  assert.equal(Object.hasOwn(detail, 'cookingSteps'), false);
});

test('shows cooking steps to merchants', () => {
  const detail = getDishDetailView(dish, 'merchant');

  assert.equal(detail.canViewCookingSteps, true);
  assert.deepEqual(detail.cookingSteps, dish.cookingSteps);
});
