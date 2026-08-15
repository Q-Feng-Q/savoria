const test = require('node:test');
const assert = require('node:assert/strict');

const {
  MERCHANT_ORDER_FILTERS,
  buildApiMerchantScene,
  buildApiPurchaseScene,
  buildPurchaseMealOptions,
  mapMerchantOrder
} = require('../utils/merchant-scenes');
const { buildApiMerchantOrderDetailScene } = require('../utils/merchant-scenes');

test('overview counts every active order before truncating the preview', () => {
  const activeStatuses = [
    'PENDING',
    'CONFIRMED',
    'PREPARING',
    'READY',
    'PENDING',
    'CONFIRMED',
    'READY'
  ];
  const orders = activeStatuses.map((status, index) => ({
    orderId: index + 1,
    familyId: 1,
    mealSlotId: 42,
    status,
    items: []
  }));
  orders.push({ orderId: 99, familyId: 1, mealSlotId: 42, status: 'DONE', items: [] });

  const scene = buildApiMerchantScene({ session: {}, orders });

  assert.equal(scene.todayCommand.activeOrderCount, 7);
  assert.equal(scene.activeOrders.length, 5);
  assert.equal(scene.statCards.find((card) => card.key === 'activeOrders').value, '7');
});

test('merchant order filters expose exact backend status sets', () => {
  assert.deepEqual(MERCHANT_ORDER_FILTERS, {
    pending: { statuses: ['PENDING'] },
    preparing: { statuses: ['CONFIRMED', 'PREPARING'] },
    ready: { statuses: ['READY'] },
    all: { statuses: [] }
  });
  assert.equal(Object.isFrozen(MERCHANT_ORDER_FILTERS), true);
  Object.values(MERCHANT_ORDER_FILTERS).forEach((filter) => {
    assert.equal(Object.isFrozen(filter), true);
    assert.equal(Object.isFrozen(filter.statuses), true);
  });
});

test('merchant orders prefer a response meal name and otherwise show the truthful slot id', () => {
  const named = mapMerchantOrder({
    orderId: 1,
    familyId: 2,
    mealSlotId: 42,
    mealSlotName: '夜宵',
    status: 'PENDING',
    items: []
  });
  const unknown = mapMerchantOrder({
    orderId: 2,
    familyId: 2,
    mealSlotId: 73,
    status: 'PENDING',
    items: []
  });

  assert.equal(named.mealLabel, '夜宵');
  assert.equal(unknown.mealLabel, '餐次 73');
});

test('purchase meal options come only from unique valid response source slots', () => {
  const options = buildPurchaseMealOptions([
    { sources: [{ mealSlotId: 42 }, { mealSlotId: '42' }, { mealSlotId: null }] },
    { sources: [{ mealSlotId: 73 }, { mealSlotId: '' }, { mealSlotId: 'null' }] },
    { sources: [{ mealSlotId: -1 }, { mealSlotId: Number.NaN }] }
  ]);

  assert.deepEqual(options, [
    { key: 'all', label: '全天', mealSlotId: null },
    { key: '42', label: '餐次 42', mealSlotId: 42 },
    { key: '73', label: '餐次 73', mealSlotId: 73 }
  ]);
});

test('purchase scene summarizes actual source statuses as estimated and confirmed', () => {
  const scene = buildApiPurchaseScene({
    session: {},
    summaryItems: [
      { ingredientName: '土豆', quantity: 2, unit: '斤', sourceStatus: 'ESTIMATED', checked: true, sources: [{ mealSlotId: 42 }] },
      { ingredientName: '鸡蛋', quantity: 6, unit: '个', sourceStatus: 'confirmed', checked: false, sources: [{ mealSlotId: 73 }] },
      { ingredientName: '盐', quantity: 1, unit: '袋', sourceStatus: 'UNKNOWN', checked: true, sources: [] }
    ]
  });

  assert.deepEqual(scene.sourceSummary, {
    estimatedCount: 1,
    confirmedCount: 1,
    text: '预估 1 项 · 已确认 1 项'
  });
  assert.deepEqual(scene.mealOptions, [
    { key: 'all', label: '全天', mealSlotId: null },
    { key: '42', label: '餐次 42', mealSlotId: 42 },
    { key: '73', label: '餐次 73', mealSlotId: 73 }
  ]);
});

test('purchase scene preserves its existing item and breakdown mapping', () => {
  const scene = buildApiPurchaseScene({
    session: {},
    date: '2026-08-02',
    mealSlotId: 42,
    families: [{ familyId: 2, familyName: '陈家' }],
    summaryItems: [{
      ingredientName: '土豆',
      quantity: 3,
      unit: '斤',
      sourceStatus: 'ESTIMATED',
      sources: [{ familyId: 2, mealSlotId: 42, orderId: 901, dishId: 100, serviceDate: '2026-08-02' }]
    }]
  });

  assert.equal(scene.date, '2026-08-02');
  assert.equal(scene.mealLabel, '餐次 42');
  assert.equal(scene.summaryCards[0].value, '1');
  assert.equal(scene.summaryCards[1].value, '1');
  assert.equal(scene.items[0].ingredientName, '土豆');
  assert.equal(scene.items[0].statusText, '预估');
  assert.equal(scene.items[0].familyText, '陈家');
  assert.equal(scene.items[0].mealText, '餐次 42');
  assert.equal(scene.items[0].dishText, '菜品 100');
  assert.deepEqual(scene.items[0].breakdown[0], {
    id: '901-100-2-42',
    familyName: '陈家',
    dishName: '菜品 100',
    quantity: null,
    unit: '斤',
    orderId: 901,
    mealSlotId: 42,
    serviceDate: '2026-08-02',
    status: 'ESTIMATED',
    statusLabel: '预估',
    isEstimated: true
  });
});

test('filtered mixed-meal rows do not claim the all-meals aggregate quantity', () => {
  const scene = buildApiPurchaseScene({
    session: {},
    mealSlotId: 42,
    summaryItems: [{
      ingredientName: '鸡蛋',
      quantity: 12,
      unit: '个',
      sourceStatus: 'CONFIRMED',
      sources: [
        { familyId: 2, mealSlotId: 42, orderId: 901, dishId: 100, serviceDate: '2026-08-02' },
        { familyId: 3, mealSlotId: 73, orderId: 902, dishId: 101, serviceDate: '2026-08-02' }
      ]
    }]
  });

  assert.equal(scene.items[0].totalQuantity, null);
  assert.equal(scene.items[0].quantityText, '按餐次查看来源');
  assert.equal(scene.items[0].quantityScope, 'source-only');
  assert.equal(scene.items[0].mealText, '餐次 42');
  assert.equal(scene.items[0].breakdown.length, 1);
  assert.equal(scene.items[0].breakdown[0].mealSlotId, 42);
});

test('filtered single-meal rows keep their aggregate quantity', () => {
  const scene = buildApiPurchaseScene({
    session: {},
    mealSlotId: 42,
    summaryItems: [{
      ingredientName: '豆腐',
      quantity: 4,
      unit: '块',
      sourceStatus: 'CONFIRMED',
      sources: [
        { familyId: 2, mealSlotId: 42, orderId: 903, dishId: 102, serviceDate: '2026-08-02' }
      ]
    }]
  });

  assert.equal(scene.items[0].totalQuantity, 4);
  assert.equal(scene.items[0].quantityScope, 'aggregate');
  assert.equal(scene.items[0].quantityText, '4块');
});

test('purchase family count includes only source families visible in the selected meal', () => {
  const scene = buildApiPurchaseScene({
    session: {},
    mealSlotId: 42,
    summaryItems: [{
      ingredientName: '青菜',
      quantity: 5,
      unit: '斤',
      sourceStatus: 'CONFIRMED',
      sources: [
        { familyId: 2, mealSlotId: 42, orderId: 904, dishId: 103, serviceDate: '2026-08-02' },
        { familyId: 3, mealSlotId: 73, orderId: 905, dishId: 104, serviceDate: '2026-08-02' }
      ]
    }],
    familyItems: [
      { familyId: 2 },
      { familyId: 3 },
      { familyId: 4 }
    ]
  });

  assert.equal(scene.summaryCards.find((card) => card.key === 'familyCount').value, '1');
});

test('merchant cancellation availability matches the backend state machine', () => {
  const sceneFor = (status) => buildApiMerchantOrderDetailScene({
    session: { merchantId: 3 },
    order: { orderId: 1, familyId: 2, mealSlotId: 42, status, items: [] }
  });

  assert.equal(sceneFor('PENDING').canReject, true);
  assert.equal(sceneFor('PENDING').order.rawStatus, 'PENDING');
  assert.equal(sceneFor('PENDING').canCancel, false);
  assert.equal(sceneFor('CONFIRMED').canCancel, true);
  assert.equal(sceneFor('PREPARING').canCancel, true);
  assert.equal(sceneFor('READY').canCancel, false);
});

test('purchase rows use a stable composite key for duplicate ingredient names', () => {
  const scene = buildApiPurchaseScene({
    session: {},
    summaryItems: [
      { ingredientName: '豆腐', quantity: 2, unit: '块', sourceStatus: 'confirmed', sources: [] },
      { ingredientName: '豆腐', quantity: 1, unit: '盒', sourceStatus: 'ESTIMATED', sources: [] },
      { ingredientName: '豆腐', quantity: 3, unit: '块', sourceStatus: 'UNKNOWN', sources: [] }
    ]
  });

  assert.deepEqual(scene.items.map((item) => item.ingredientId), [
    '豆腐__块__CONFIRMED',
    '豆腐__盒__ESTIMATED',
    '豆腐__块__UNKNOWN'
  ]);
});
