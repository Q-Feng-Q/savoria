const test = require('node:test');
const assert = require('node:assert/strict');
const {
  decoratePurchaseItems,
  togglePurchaseCheck,
  addTemporaryPurchaseItem
} = require('../utils/purchase-state');

test('decorates purchase items with persisted checked state', () => {
  const items = [{ ingredientId: 'egg', unit: '个', ingredientName: '鸡蛋' }];

  assert.equal(decoratePurchaseItems(items, { egg__个: true })[0].isChecked, true);
});

test('togglePurchaseCheck flips an item state', () => {
  assert.deepEqual(togglePurchaseCheck({ egg__个: true }, 'egg__个'), { egg__个: false });
});

test('addTemporaryPurchaseItem appends valid manual item', () => {
  const items = addTemporaryPurchaseItem([], { name: '餐巾纸', quantity: '1', unit: '包' });

  assert.equal(items[0].name, '餐巾纸');
  assert.equal(items[0].isTemp, true);
});
