const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { buildApiOrderDetailScene, buildApiOrdersScene } = require('../utils/api-scenes');
const root = path.join(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const homeData = { family: { familyId: 2, familyName: '林家' }, member: { memberId: 10, name: '小林' } };

test('order list uses expected time and aggregate dishes while detail maps member snapshots', () => {
  const order = { orderId: 8, expectedMealTime: '2026-08-28T18:30:00', status: 'PENDING', totalAmount: 114,
    items: [{ dishId: 9, dishName: '番茄牛腩', quantity: 3, price: 38, amount: 114,
      selections: [{ userId: 10, memberName: '小林', quantity: 1 }, { userId: 11, memberName: '阿禾', quantity: 2 }] }] };
  const list = buildApiOrdersScene({ homeData, orders: [order] });
  const detail = buildApiOrderDetailScene({ homeData, order });
  assert.equal(list.orders[0].mealLabel, '2026-08-28 18:30');
  assert.equal(list.orders[0].dishSummaryText, '番茄牛腩 x3');
  assert.equal(detail.items[0].hasSelectionDetails, true);
  assert.deepEqual(detail.items[0].selections.map((item) => `${item.memberName} × ${item.quantity}`), ['小林 × 1', '阿禾 × 2']);
});

test('family and merchant detail markup keeps member attribution collapsed', () => {
  for (const file of ['pages/ordering/order-detail/index.wxml', 'pages/merchant/merchant-order-detail/index.wxml']) {
    const wxml = read(file);
    assert.match(wxml, /toggleSelectionDetails/);
    assert.match(wxml, /expandedDishIds/);
    assert.match(wxml, /item\.memberName.*item\.quantity/s);
  }
});
