const test = require('node:test');
const assert = require('node:assert/strict');

const {
  buildCrewRoleLabels,
  buildApiHomeScene,
  buildApiMenuScene,
  buildApiDishDetailScene,
  buildApiOrdersScene,
  buildApiNotificationsScene,
  buildApiCartScene,
  buildApiOrderDetailScene,
  buildApiWalletScene,
  buildApiWalletLedgerScene,
  buildApiProfileScene,
  buildApiAddressBookScene,
  buildApiMerchantScene,
  buildApiMerchantOrdersScene,
  buildApiMerchantOrderDetailScene,
  buildApiMerchantFamiliesScene,
  buildApiMerchantFamilyDetailScene,
  buildApiMerchantIngredientsScene,
  buildApiFamilyMenuScene,
  buildApiPurchaseScene,
  mapOrderStatusLabel
} = require('../utils/api-scenes');
const { buildApiMerchantDishesScene } = require('../utils/merchant-scenes');

const homeData = {
  family: {
    familyId: 2,
    familyName: '陈家晚饭',
    merchantId: 1,
    merchantName: '食光知味'
  },
  member: {
    memberId: 10,
    name: '陈梅',
    roleTemplate: 'member'
  },
  serviceDate: '2026-07-02',
  featuredDish: {
    dishId: 100,
    name: '番茄炒蛋',
    description: '酸甜开胃，适合全家分享',
    price: 18,
    imageUrl: '/uploads/images/tomato.png'
  },
  dashboardCards: [
    { key: 'meal', label: '当前餐次', value: '午餐' },
    { key: 'cart', label: '餐篮份数', value: '2 份' }
  ],
  mealSlots: [
    { mealSlotId: 20, name: '午餐', displayTime: '12:00', selected: true },
    { mealSlotId: 30, name: '晚餐', displayTime: '18:30', selected: false }
  ],
  recentOrders: [
    { orderId: 88, mealSlotName: '午餐', status: 'PENDING', totalAmount: 36 }
  ]
};

test('buildApiHomeScene maps home response to homepage view model', () => {
  const scene = buildApiHomeScene(homeData, { imageBaseUrl: 'http://127.0.0.1:8080' });

  assert.equal(scene.context.family.name, '陈家晚饭');
  assert.equal(scene.currentMemberName, '陈梅');
  assert.deepEqual(scene.crew, {
    chefName: '食光知味',
    helperName: '无帮厨',
    tasterName: '无试吃员',
    chefLabel: '食光知味主厨',
    helperLabel: '无帮厨',
    tasterLabel: '无试吃员',
    chefRecommendationLabel: '食光知味主厨今日推荐',
    tasterWelcomeLabel: '欢迎你'
  });
  assert.equal(scene.featuredDish.id, 100);
  assert.equal(scene.featuredDish.description, '酸甜开胃，适合全家分享');
  assert.equal(scene.featuredDish.priceText, '¥18.00');
  assert.equal(scene.featuredDish.imageUrl, 'http://127.0.0.1:8080/uploads/images/tomato.png');
  assert.equal(scene.quickEntries.length, 4);
  assert.equal(scene.heroImageUrl, '/assets/brand/hero-warm-kitchen.webp');
  assert.deepEqual(scene.primaryAction, { key: 'menu', label: '翻开今日菜单' });
  assert.equal(scene.mealSummary.label, '午餐');
  assert.equal(scene.orderSummary.statusLabel, '待确认');
});

test('buildApiHomeScene maps backend crew names without leaking null text', () => {
  const scene = buildApiHomeScene({
    ...homeData,
    crew: { chefName: '老祁', helperName: '阿禾', tasterName: null }
  });
  assert.deepEqual(scene.crew, {
    chefName: '老祁',
    helperName: '阿禾',
    tasterName: '无试吃员',
    chefLabel: '老祁主厨',
    helperLabel: '阿禾帮厨',
    tasterLabel: '无试吃员',
    chefRecommendationLabel: '老祁主厨今日推荐',
    tasterWelcomeLabel: '欢迎你'
  });
});

test('buildCrewRoleLabels formats names once and keeps blank fallbacks readable', () => {
  assert.deepEqual(buildCrewRoleLabels({
    chefName: ' 老祁 ',
    helperName: '阿禾',
    tasterName: '小林'
  }), {
    chefName: '老祁',
    helperName: '阿禾',
    tasterName: '小林',
    chefLabel: '老祁主厨',
    helperLabel: '阿禾帮厨',
    tasterLabel: '小林试吃员',
    chefRecommendationLabel: '老祁主厨今日推荐',
    tasterWelcomeLabel: '小林试吃员欢迎你'
  });
  assert.deepEqual(buildCrewRoleLabels({
    chefName: ' ',
    helperName: null,
    tasterName: undefined
  }), {
    chefName: '无主厨',
    helperName: '无帮厨',
    tasterName: '无试吃员',
    chefLabel: '无主厨',
    helperLabel: '无帮厨',
    tasterLabel: '无试吃员',
    chefRecommendationLabel: '今日推荐',
    tasterWelcomeLabel: '欢迎你'
  });
});

test('buildApiHomeScene prefers the featured dish array and enables multi-item swiper behavior', () => {
  const scene = buildApiHomeScene({
    ...homeData,
    featuredDishes: [
      { dishId: 201, name: '糖醋里脊', description: '', price: 28, imageUrl: '/uploads/images/pork.png' },
      { dishId: 202, name: '冬瓜汤', description: '清爽解腻', price: 12, imageUrl: '/uploads/images/soup.png' }
    ]
  }, { imageBaseUrl: 'http://127.0.0.1:8080' });

  assert.deepEqual(scene.featuredDishes.map((item) => item.id), [201, 202]);
  assert.equal(scene.featuredDishes[0].description, '今日家庭推荐');
  assert.equal(scene.featuredDishes[0].priceText, '¥28.00');
  assert.equal(scene.featuredDishes[0].imageUrl, 'http://127.0.0.1:8080/uploads/images/pork.png');
  assert.equal(scene.featuredDish.id, 201);
  assert.equal(scene.featuredAutoplay, true);
  assert.equal(scene.featuredCircular, true);
  assert.equal(scene.featuredIndicatorDots, true);
  assert.equal(scene.featuredInterval, 4000);
  assert.match(String(scene.featuredNextMargin), /^[1-9]\d*rpx$/);
});

test('buildApiHomeScene falls back to the old single featured dish without carousel motion', () => {
  const scene = buildApiHomeScene(homeData);

  assert.deepEqual(scene.featuredDishes.map((item) => item.id), [100]);
  assert.equal(scene.featuredAutoplay, false);
  assert.equal(scene.featuredCircular, false);
  assert.equal(scene.featuredIndicatorDots, false);
  assert.equal(scene.featuredInterval, 0);
  assert.equal(scene.featuredNextMargin, '0rpx');
});

test('buildApiHomeScene treats an explicit empty featured dish array as empty', () => {
  const scene = buildApiHomeScene({ ...homeData, featuredDishes: [] });

  assert.deepEqual(scene.featuredDishes, []);
  assert.equal(scene.featuredDish, null);
  assert.equal(scene.featuredAutoplay, false);
  assert.equal(scene.featuredNextMargin, '0rpx');
});

test('buildApiHomeScene reduces the multi-item peek margin on 320px phones', () => {
  const payload = {
    ...homeData,
    featuredDishes: [homeData.featuredDish, { ...homeData.featuredDish, dishId: 101 }]
  };
  const regular = buildApiHomeScene(payload, { windowWidth: 375 });
  const compact = buildApiHomeScene(payload, { windowWidth: 320 });

  assert.ok(parseInt(compact.featuredNextMargin, 10) < parseInt(regular.featuredNextMargin, 10));
  assert.ok(parseInt(compact.featuredNextMargin, 10) > 0);
});

test('buildApiMenuScene merges menu items with shared cart counts', () => {
  const scene = buildApiMenuScene({
    homeData,
    menuItems: [
      { dishId: 100, categoryId: 3, name: '番茄炒蛋', description: '经典家常', imageUrl: '/uploads/images/tomato.png', price: 18, status: 'ACTIVE' },
      { dishId: 101, categoryId: 4, name: '青菜豆腐汤', description: '清爽', imageUrl: '', price: 22, status: 'ACTIVE' }
    ],
    cart: {
      expectedMealTime: '2026-07-02T18:30:00',
      totalQuantity: 2,
      totalAmount: 36,
      items: [
        { itemId: 901, dishId: 100, dishName: '番茄炒蛋', price: 18, quantity: 2, currentMemberQuantity: 2, currentMemberRemark: '少盐' }
      ]
    },
    searchKeyword: '番茄',
    imageBaseUrl: 'http://127.0.0.1:8080'
  });

  assert.equal(scene.cartItemCount, 2);
  assert.equal(scene.crew.chefLabel, '食光知味主厨');
  assert.equal(scene.crew.helperLabel, '无帮厨');
  assert.equal(scene.activeCategoryLabel, '全部菜品');
  assert.equal(scene.visibleMenuCards.length, 1);
  assert.equal(scene.visibleMenuCards[0].selectedByCurrentMemberCount, 2);
  assert.equal(scene.visibleMenuCards[0].cartLineId, 901);
  assert.deepEqual(scene.visibleMenuCards[0].displayTags, ['分类 3', '经典家常']);
  assert.equal(scene.visibleMenuCards[0].priceText, '¥18.00');
  assert.equal(scene.visibleMenuCards[0].displayImageUrl, 'http://127.0.0.1:8080/uploads/images/tomato.png');
});

test('buildApiMenuScene maps featured state without reordering the backend menu', () => {
  const scene = buildApiMenuScene({
    homeData,
    menuItems: [
      { dishId: 101, name: '普通菜', price: 12, featured: false },
      { dishId: 100, name: '主厨推荐菜', price: 18, featured: true }
    ]
  });

  assert.deepEqual(scene.menuCards.map((item) => item.id), [101, 100]);
  assert.deepEqual(scene.menuCards.map((item) => item.featured), [false, true]);
});

test('buildApiDishDetailScene keeps ingredient and selected count info', () => {
  const scene = buildApiDishDetailScene({
    homeData,
    dishDetail: {
      dishId: 100,
      categoryId: 3,
      categoryName: '家常菜',
      name: '番茄炒蛋',
      description: '酸甜开胃',
      imageUrl: '/uploads/images/tomato.png',
      price: 18,
      ingredients: [
        { ingredientName: '番茄', quantity: 2, unit: '个', calcType: 'FIXED' },
        { ingredientName: '鸡蛋', quantity: 3, unit: '个', calcType: 'FIXED' }
      ],
      cookingSteps: [
        { stepNo: 1, title: '打蛋', content: '鸡蛋打散备用' }
      ]
    },
    cart: {
      items: [
        { dishId: 100, quantity: 2 }
      ]
    },
    imageBaseUrl: 'http://127.0.0.1:8080'
  });

  assert.equal(scene.dish.name, '番茄炒蛋');
  assert.equal(scene.dish.category, '家常菜');
  assert.equal(scene.dish.selectedCount, 2);
  assert.equal(scene.dish.ingredients.length, 2);
  assert.equal(scene.dish.cookingSteps.length, 1);
  assert.equal(scene.dish.finalPrice, '18.00');
  assert.equal(Object.hasOwn(scene.dish, 'basePrice'), false);
});

test('buildApiDishDetailScene hides internal category ids behind readable copy', () => {
  const scene = buildApiDishDetailScene({
    homeData,
    dishDetail: { dishId: 100, categoryId: 14, name: '番茄炒蛋', price: 18 }
  });

  assert.equal(scene.dish.category, '今日菜单');
  assert.doesNotMatch(scene.dish.category, /14|分类/);
});

test('buildApiMerchantDishesScene marks only imported template dishes as syncable', () => {
  const scene = buildApiMerchantDishesScene({
    session: { merchantId: 1 },
    categories: [{ categoryId: 3, name: '家常菜' }],
    dishes: [
      { dishId: 100, categoryId: 3, name: '豆角焖面', price: 18, status: 'ACTIVE', sourceTemplateId: 7 },
      { dishId: 101, categoryId: 3, name: '自创汤', price: 12, status: 'ACTIVE', templateImported: false }
    ]
  });

  assert.equal(scene.dishRows[0].sourceTemplateId, 7);
  assert.equal(scene.dishRows[0].templateImported, true);
  assert.equal(scene.dishRows[1].templateImported, false);
});

test('buildApiMerchantDishesScene maps and orders featured dishes with timestamp and id fallbacks', () => {
  const scene = buildApiMerchantDishesScene({
    session: { merchantId: 1 },
    dishes: [
      { dishId: 1, name: '原顺序一', status: 'ACTIVE', featured: false },
      { dishId: 8, name: '同时间低 ID', status: 'ACTIVE', featured: true, featuredAt: '2026-08-15T10:00:00Z' },
      { dishId: 3, name: '原顺序二', status: 'INACTIVE', featured: false },
      { dishId: 9, name: '同时间高 ID', status: 'ACTIVE', featured: true, featuredAt: '2026-08-15T10:00:00Z' },
      { dishId: 7, name: '较早推荐', status: 'ACTIVE', featured: true, featuredAt: '2026-08-14T10:00:00Z' }
    ]
  });

  assert.equal(scene.featuredCount, 3);
  assert.deepEqual(scene.dishRows.map((row) => row.id), [9, 8, 7, 1, 3]);
  assert.deepEqual(scene.dishRows.slice(0, 3).map((row) => [row.featured, row.featuredAt]), [
    [true, '2026-08-15T10:00:00Z'],
    [true, '2026-08-15T10:00:00Z'],
    [true, '2026-08-14T10:00:00Z']
  ]);
});

test('buildApiMerchantDishesScene derives featured labels and disabled state from the full list', () => {
  const dishes = Array.from({ length: 6 }, (_, index) => ({
    dishId: index + 1,
    name: `菜品${index + 1}`,
    status: index === 5 ? 'INACTIVE' : 'ACTIVE',
    featured: index < 5,
    featuredAt: index < 5 ? `2026-08-${String(index + 10).padStart(2, '0')}T10:00:00Z` : null
  }));
  const scene = buildApiMerchantDishesScene({ session: { merchantId: 1 }, dishes });

  assert.equal(scene.featuredCount, 5);
  assert.equal(scene.dishRows.find((row) => row.id === 1).featuredLabel, '已推荐');
  assert.equal(scene.dishRows.find((row) => row.id === 1).featuredDisabled, false);
  assert.equal(scene.dishRows.find((row) => row.id === 6).featuredLabel, '推荐已满');
  assert.equal(scene.dishRows.find((row) => row.id === 6).featuredDisabled, true);

  const available = buildApiMerchantDishesScene({
    session: { merchantId: 1 },
    dishes: [{ dishId: 10, name: '可推荐', status: 'ACTIVE', featured: false }]
  });
  assert.equal(available.dishRows[0].featuredLabel, '推荐');
  assert.equal(available.dishRows[0].featuredDisabled, false);

  const inactive = buildApiMerchantDishesScene({
    session: { merchantId: 1 },
    dishes: [{ dishId: 11, name: '已下架', status: 'INACTIVE', featured: false }]
  });
  assert.equal(inactive.dishRows[0].featuredLabel, '推荐');
  assert.equal(inactive.dishRows[0].featuredDisabled, true);
});

test('buildApiCartScene groups cart items and keeps address selection state', () => {
  const scene = buildApiCartScene({
    homeData,
    cart: {
      serverDate: '2026-07-02',
      minimumExpectedMealTime: '2026-07-02T18:00:00',
      timeStepMinutes: 15,
      bookingEnded: false,
      expectedMealTime: '2026-07-02T18:30:00',
      remark: '送到门口',
      totalQuantity: 2,
      totalAmount: 36,
      items: [
        { itemId: 901, dishId: 100, dishName: '番茄炒蛋', price: 18, quantity: 2, currentMemberQuantity: 2, currentMemberRemark: '少盐', selections: [] }
      ]
    },
    addresses: [
      { addressId: 1, contactName: '陈梅', contactPhone: '13800000000', addressText: '星河路 18 号', defaultAddress: true }
    ],
    deliveryMode: 'DELIVERY',
    addressId: 1
  });

  assert.equal(scene.groupedItems.length, 1);
  assert.equal(scene.groupedItems[0].rows[0].note, '少盐');
  assert.equal(scene.currentAddress.contactName, '陈梅');
  assert.equal(scene.totals.totalAmount, '36.00');
  assert.equal(scene.canSubmit, true);
});

test('buildApiOrdersScene and detail scene map uppercase statuses', () => {
  const order = {
    orderId: 88,
    merchantId: 1,
    familyId: 2,
    submitterMemberId: 10,
    mealSlotId: 20,
    serviceDate: '2026-07-02',
    deliveryMode: 'DELIVERY',
    deliveryFee: 6,
    status: 'PENDING',
    totalAmount: 42,
    remark: '不要香菜',
    cancelReason: null,
    items: [
      { dishId: 100, dishName: '番茄炒蛋', ownerMemberId: 10, price: 18, quantity: 2, amount: 36, itemRemark: '少盐' }
    ]
  };

  const listScene = buildApiOrdersScene({ homeData, orders: [order], mealSlots: homeData.mealSlots });
  const detailScene = buildApiOrderDetailScene({ homeData, order, mealSlots: homeData.mealSlots });

  assert.equal(listScene.orders[0].statusLabel, '待确认');
  assert.equal(detailScene.order.statusLabel, '待确认');
  assert.equal(detailScene.items[0].note, '少盐');
  assert.equal(detailScene.canCancel, true);
});

test('buildApiNotificationsScene splits unread and read notifications', () => {
  const scene = buildApiNotificationsScene({
    homeData,
    actorType: 'family',
    pageData: {
      items: [
        { notificationId: 1, category: 'order', title: '订单已提交', content: '请等待商户确认', read: false, createdAt: '2026-07-02T10:20:00' },
        { notificationId: 2, category: 'wallet', title: '余额冻结', content: '已冻结 42 元', read: true, createdAt: '2026-07-02T10:21:00' }
      ]
    }
  });

  assert.equal(scene.unreadCount, 1);
  assert.equal(scene.unreadItems.length, 1);
  assert.equal(scene.readItems.length, 1);
});

test('wallet and profile scenes keep backend fields usable by pages', () => {
  const ledgers = [
    {
      ledgerId: 1,
      type: 'ORDER_FREEZE',
      amount: -42,
      balanceAfter: 88,
      frozenAfter: 42,
      remark: '午餐订单冻结',
      createdAt: '2026-07-02T10:30:00'
    }
  ];
  const addresses = [
    { addressId: 1, contactName: '陈梅', contactPhone: '13800000000', addressText: '星河路 18 号', defaultAddress: true }
  ];
  const walletScene = buildApiWalletScene({ homeData, ledgers });
  const walletLedgerScene = buildApiWalletLedgerScene({ homeData, ledgers });
  const profileScene = buildApiProfileScene({
    homeData,
    addresses,
    ledgers,
    session: { roleTemplate: 'member' }
  });

  assert.equal(walletScene.balanceCards[0].value, '¥88.00');
  assert.equal(walletScene.latestTransactions[0].amountText, '-¥42.00');
  assert.equal(walletLedgerScene.transactions[0].note, '午餐订单冻结');
  assert.equal(profileScene.defaultAddress.contactName, '陈梅');
  assert.equal(profileScene.crew.tasterLabel, '无试吃员');
  assert.equal(profileScene.memberCards[0].balanceText, '可用 ¥88.00');
  assert.equal(profileScene.showDemoSwitchers, false);
});

test('address scenes normalize backend address fields', () => {
  const addressScene = buildApiAddressBookScene({
    homeData,
    addresses: [
      { addressId: 1, contactName: '陈梅', contactPhone: '13800000000', addressText: '星河路 18 号', defaultAddress: true }
    ]
  });

  assert.equal(addressScene.addresses[0].id, 1);
  assert.equal(addressScene.addresses[0].isDefault, true);
});

test('mapOrderStatusLabel falls back gracefully', () => {
  assert.equal(mapOrderStatusLabel('DONE'), '已完成');
  assert.equal(mapOrderStatusLabel('UNKNOWN_STATUS'), 'UNKNOWN_STATUS');
});
