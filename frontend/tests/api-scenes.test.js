const test = require('node:test');
const assert = require('node:assert/strict');

const {
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
  buildApiMerchantDishesScene,
  buildApiMerchantIngredientsScene,
  buildApiFamilyMenuScene,
  buildApiPurchaseScene,
  mapOrderStatusLabel
} = require('../utils/api-scenes');

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
  assert.equal(scene.featuredDish.id, 100);
  assert.equal(scene.featuredDish.priceText, '¥18.00');
  assert.equal(scene.featuredDish.imageUrl, 'http://127.0.0.1:8080/uploads/images/tomato.png');
  assert.equal(scene.quickEntries.length, 4);
  assert.equal(scene.heroImageUrl, '/assets/brand/hero-warm-kitchen.webp');
  assert.deepEqual(scene.primaryAction, { key: 'menu', label: '翻开今日菜单' });
  assert.equal(scene.mealSummary.label, '午餐');
  assert.equal(scene.orderSummary.statusLabel, '待确认');
});

test('buildApiMenuScene merges menu items with cart counts and active meal slot', () => {
  const scene = buildApiMenuScene({
    homeData,
    menuItems: [
      { dishId: 100, categoryId: 3, name: '番茄炒蛋', description: '经典家常', imageUrl: '/uploads/images/tomato.png', price: 18, status: 'ACTIVE' },
      { dishId: 101, categoryId: 4, name: '青菜豆腐汤', description: '清爽', imageUrl: '', price: 22, status: 'ACTIVE' }
    ],
    cart: {
      mealSlotId: 20,
      date: '2026-07-02',
      totalQuantity: 2,
      totalAmount: 36,
      items: [
        { itemId: 901, dishId: 100, dishName: '番茄炒蛋', price: 18, quantity: 2, ownerMemberId: 10, ownerMemberName: '陈梅', editable: true, itemRemark: '少盐' }
      ]
    },
    searchKeyword: '番茄',
    imageBaseUrl: 'http://127.0.0.1:8080'
  });

  assert.equal(scene.cartItemCount, 2);
  assert.equal(scene.activeCategoryLabel, '全部菜品');
  assert.equal(scene.visibleMenuCards.length, 1);
  assert.equal(scene.visibleMenuCards[0].selectedByCurrentMemberCount, 2);
  assert.equal(scene.visibleMenuCards[0].cartLineId, 901);
  assert.deepEqual(scene.visibleMenuCards[0].displayTags, ['分类 3', '经典家常']);
  assert.equal(scene.visibleMenuCards[0].priceText, '¥18.00');
  assert.equal(scene.visibleMenuCards[0].displayImageUrl, 'http://127.0.0.1:8080/uploads/images/tomato.png');
});

test('buildApiDishDetailScene keeps ingredient and selected count info', () => {
  const scene = buildApiDishDetailScene({
    homeData,
    dishDetail: {
      dishId: 100,
      categoryId: 3,
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
  assert.equal(scene.dish.selectedCount, 2);
  assert.equal(scene.dish.ingredients.length, 2);
  assert.equal(scene.dish.cookingSteps.length, 1);
  assert.equal(scene.dish.finalPrice, '18.00');
});

test('buildApiCartScene groups cart items and keeps address selection state', () => {
  const scene = buildApiCartScene({
    homeData,
    mealSlots: homeData.mealSlots,
    cart: {
      mealSlotId: 20,
      date: '2026-07-02',
      remark: '送到门口',
      totalQuantity: 2,
      totalAmount: 36,
      items: [
        { itemId: 901, dishId: 100, dishName: '番茄炒蛋', price: 18, quantity: 2, ownerMemberId: 10, ownerMemberName: '陈梅', editable: true, itemRemark: '少盐' }
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
