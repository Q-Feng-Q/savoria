const test = require('node:test');
const assert = require('node:assert/strict');
global.Page = () => {};
const { buildDishPayload, validateDish } = require('../pages/merchant/dish-edit/index');
const { buildTemplateSnapshot } = require('../utils/dish-template-change');
const nourishment = require('../utils/nourishment');

const dish = { name: '银耳羹', categoryId: 1, basePrice: 18, productType: 'NOURISHMENT', nourishmentDescription: '  温润甜羹\n慢炖制作  ', servingAdvice: '', precautions: '注意食材过敏' };
test('dish save carries independent nourishment fields', () => {
  const result = buildDishPayload(dish);
  assert.equal(result.productType, 'NOURISHMENT');
  assert.equal(result.nourishmentDescription, '温润甜羹\n慢炖制作');
  assert.equal(result.servingAdvice, '');
  assert.equal(buildDishPayload({ ...dish, productType: 'NORMAL' }).precautions, dish.precautions);
});
test('legacy dish defaults to normal and long food text is rejected', () => {
  assert.equal(buildDishPayload({ name: '青菜' }).productType, 'NORMAL');
  assert.match(validateDish({ ...dish, precautions: '字'.repeat(1001) }), /1000/);
  assert.match(validateDish({ ...dish, productType: 'OTHER' }), /类型/);
  assert.match(validateDish({ ...dish, productType: '' }), /类型/);
});
test('template snapshots preserve optional nourishment fields and explicit clear', () => {
  const result = buildTemplateSnapshot(dish);
  assert.equal(result.productType, 'NOURISHMENT');
  assert.equal(result.nourishmentDescription, '温润甜羹\n慢炖制作');
  assert.equal(result.servingAdvice, '');
});
test('detail sections omit blanks and hide normal dishes and components', () => {
  assert.equal(typeof nourishment.buildNourishmentSections, 'function');
  const sections = nourishment.buildNourishmentSections(dish);
  assert.deepEqual(sections.map((item) => item.label), ['滋补介绍', '注意事项']);
  assert.equal(sections[0].text, '温润甜羹\n慢炖制作');
  assert.deepEqual(nourishment.buildNourishmentSections({ ...dish, productType: 'NORMAL' }), []);
  assert.deepEqual(nourishment.buildNourishmentSections({ ...dish, templateType: 'COMPONENT' }), []);
  assert.deepEqual(nourishment.buildNourishmentSections({ productType: 'NOURISHMENT', nourishmentDescription: ' \n ', precautions: null }), []);
  assert.equal(nourishment.validateNourishment({ productType: 'NOURISHMENT', nourishmentDescription: '字'.repeat(1000) }), '');
});
test('family type filter uses complete catalog and intersects category and search', () => {
  const { buildApiMenuScene } = require('../utils/api-scenes');
  const scene = buildApiMenuScene({ homeData: { family: {}, member: {} }, productType: 'NOURISHMENT', searchKeyword: '羹',
    menuItems: [
      { dishId: 1, name: '推荐青菜', categoryId: 1, categoryName: '蔬菜', featured: true },
      { dishId: 2, name: '银耳羹', categoryId: 2, categoryName: '甜汤', productType: 'NOURISHMENT' },
      { dishId: 3, name: '炖鸡汤', categoryId: 3, categoryName: '炖汤', productType: 'NOURISHMENT' }
    ] });
  assert.deepEqual(scene.menuCards.map((item) => item.id), [2, 3]);
  assert.deepEqual(scene.menuSections.map((item) => item.key), ['2']);
  assert.equal(scene.visibleMenuCards[0].productType, 'NOURISHMENT');
});
test('merchant filtering defaults legacy dishes and intersects status', () => {
  const { deriveDishRows } = require('../pages/merchant/merchant-dishes/index');
  const rows = [{ id: 1, name: '青菜', status: 'active' }, { id: 2, name: '银耳羹', status: 'active', productType: 'NOURISHMENT' }];
  assert.deepEqual(deriveDishRows(rows, '', 'all', 'NORMAL').map((item) => item.id), [1]);
  assert.deepEqual(deriveDishRows(rows, '银耳', 'inactive', 'NOURISHMENT'), []);
});
test('template comparison labels nourishment changes without leaking enum codes', () => {
  const { buildComparison } = require('../pages/merchant/dish-template-change-detail/index');
  const result = buildComparison({}, dish);
  assert.equal(result.find((item) => item.key === 'productType')?.targetText, '滋补食品');
  assert.equal(result.find((item) => item.key === 'precautions')?.label, '注意事项');
});
test('legacy review omission is not displayed as clearing nourishment information', () => {
  const { buildComparison } = require('../pages/merchant/dish-template-change-detail/index');
  const rows = buildComparison(dish, { name: '银耳羹', nourishmentDescription: '' });
  const type = rows.find((row) => row.key === 'productType');
  assert.equal(type.changed, false);
  assert.match(type.targetText, /保留/);
  const text = rows.find((row) => row.key === 'nourishmentDescription');
  assert.equal(text.changed, true);
  assert.equal(text.targetText, '未填写');
});
