const test = require('node:test');
const assert = require('node:assert/strict');
const { buildDishList, buildDishSections, pickRandomDish } = require('../utils/dish-list');

const dishes = [
  {
    id: 'tomato-egg',
    name: '番茄炒蛋',
    category: '家常菜',
    status: 'on',
    ingredients: [{ name: '番茄' }, { name: '鸡蛋' }]
  },
  {
    id: 'rice-roll',
    name: '牛肉饭',
    category: '主食',
    status: 'on',
    ingredients: [{ name: '牛肉' }, { name: '米饭' }]
  },
  {
    id: 'hidden-soup',
    name: '隐藏汤',
    category: '汤',
    status: 'off',
    ingredients: [{ name: '排骨' }]
  }
];

test('buildDishList filters by category and ingredient keyword', () => {
  const result = buildDishList({
    dishes,
    category: '家常菜',
    keyword: '鸡蛋',
    activeMealType: 'lunch',
    getSelectedCount: () => 0
  });

  assert.deepEqual(result.map((dish) => dish.id), ['tomato-egg']);
});

test('buildDishList decorates selected dish state for the active meal', () => {
  const result = buildDishList({
    dishes,
    category: '全部',
    keyword: '',
    activeMealType: 'dinner',
    getSelectedCount: (dishId, mealType) => dishId === 'rice-roll' && mealType === 'dinner' ? 2 : 0
  });
  const selected = result.find((dish) => dish.id === 'rice-roll');

  assert.equal(selected.hasSelected, true);
  assert.equal(selected.selectedCount, 2);
  assert.equal(selected.selectedText, '已选 2 份');
});

test('buildDishSections groups filtered dishes by visible category order', () => {
  const list = buildDishList({
    dishes,
    category: '全部',
    keyword: '',
    activeMealType: 'lunch',
    getSelectedCount: () => 0,
    getSoldCount: (dishId) => dishId === 'tomato-egg' ? 5 : 0
  });

  const sections = buildDishSections(list, ['家常菜', '主食', '汤']);

  assert.deepEqual(sections.map((section) => section.name), ['家常菜', '主食']);
  assert.equal(sections[0].dishes[0].soldText, '销量 5');
});

test('pickRandomDish returns a deterministic dish with injected random source', () => {
  const list = buildDishList({
    dishes,
    category: '全部',
    keyword: '',
    activeMealType: 'lunch',
    getSelectedCount: () => 0
  });

  const dish = pickRandomDish(list, () => 0.75);

  assert.equal(dish.id, 'rice-roll');
});
