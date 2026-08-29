const { toImageUrl } = require('./image-url');

const ORDER_STATUS_LABELS = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  PREPARING: '备菜中',
  READY: '待取餐/待配送',
  DONE: '已完成',
  CANCELLED: '已取消',
  REJECTED: '已拒单'
};

const NOTIFICATION_CATEGORY_LABELS = {
  order: '订单',
  purchase: '采购',
  wallet: '余额'
};

const LEDGER_TYPE_LABELS = {
  ORDER_FREEZE: '下单冻结',
  ORDER_RELEASE: '订单释放',
  ORDER_SETTLE: '订单扣款',
  MANUAL_ADJUST: '余额调整',
  RECHARGE: '余额充值'
};

function formatCurrency(value) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function formatAmountNumber(value) {
  return Number(value || 0).toFixed(2);
}

function formatSignedCurrency(value) {
  const amount = Math.abs(Number(value || 0)).toFixed(2);
  const sign = Number(value || 0) < 0 ? '-' : '+';
  return `${sign}¥${amount}`;
}

function formatDateText(value) {
  if (!value) return '';
  return String(value).replace(/-/g, '.');
}

function formatDateTimeText(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').replace(/:\d{2}$/, '');
}

function mapOrderStatusLabel(status) {
  return ORDER_STATUS_LABELS[status] || status || '';
}

function normalizeAddresses(addresses = []) {
  return addresses.map((item) => ({
    id: item.id || item.addressId,
    contactName: item.contactName,
    phone: item.phone || item.contactPhone,
    address: item.address || item.addressText,
    isDefault: Boolean(item.isDefault || item.defaultAddress),
    tagText: (item.isDefault || item.defaultAddress) ? '默认地址' : '常用地址'
  }));
}

function findDefaultAddress(addresses = []) {
  return addresses.find((item) => item.isDefault) || addresses[0] || null;
}

function resolveMealSlots(homeData, mealSlots = []) {
  if (Array.isArray(mealSlots) && mealSlots.length) {
    return mealSlots;
  }
  return Array.isArray(homeData && homeData.mealSlots) ? homeData.mealSlots : [];
}

function getSelectedMealSlot(mealSlots = []) {
  return mealSlots.find((item) => item.selected) || mealSlots[0] || null;
}

function formatExpectedMealTime(value, fallback = '待选择') {
  if (!value) return fallback;
  const text = String(value);
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
  return match ? `${match[1]}-${match[2]}-${match[3]} ${match[4]}:${match[5]}` : text;
}

function displayText(value, fallback) {
  if (value === undefined || value === null) return fallback;
  const text = String(value).trim();
  return !text || ['null', 'undefined', 'nan'].includes(text.toLowerCase()) ? fallback : text;
}

function buildCrewRoleLabels(crew = {}) {
  const chefName = displayText(crew && crew.chefName, '无主厨');
  const helperName = displayText(crew && crew.helperName, '无帮厨');
  const tasterName = displayText(crew && crew.tasterName, '无试吃员');
  const chefLabel = chefName === '无主厨' ? chefName : chefName + '主厨';
  const helperLabel = helperName === '无帮厨' ? helperName : helperName + '帮厨';
  const tasterLabel = tasterName === '无试吃员' ? tasterName : tasterName + '试吃员';
  return {
    chefName,
    helperName,
    tasterName,
    chefLabel,
    helperLabel,
    tasterLabel,
    chefRecommendationLabel: chefName === '无主厨' ? '今日推荐' : chefLabel + '今日推荐',
    tasterWelcomeLabel: tasterName === '无试吃员' ? '欢迎你' : tasterLabel + '欢迎你'
  };
}

function buildHomeCrewRoleLabels(homeData) {
  const crew = (homeData && homeData.crew) || {};
  return buildCrewRoleLabels({
    ...crew,
    chefName: displayText(crew.chefName, homeData.family.merchantName || null)
  });
}

function buildExpectedMealTimeOptions(cart) {
  if (!cart || cart.bookingEnded || !cart.serverDate || !cart.minimumExpectedMealTime) return [];
  const step = Number(cart.timeStepMinutes);
  if (!Number.isInteger(step) || step <= 0) return [];
  const match = String(cart.minimumExpectedMealTime).match(/T(\d{2}):(\d{2})/);
  if (!match) return [];
  let minutes = Number(match[1]) * 60 + Number(match[2]);
  const options = [];
  while (minutes < 24 * 60) {
    const hour = String(Math.floor(minutes / 60)).padStart(2, '0');
    const minute = String(minutes % 60).padStart(2, '0');
    const value = `${cart.serverDate}T${hour}:${minute}:00`;
    options.push({ value, label: `${hour}:${minute}` });
    minutes += step;
  }
  return options;
}

function buildContext(homeData, extra = {}) {
  return {
    merchant: {
      id: extra.merchantId || homeData.family.merchantId || null,
      name: homeData.family.merchantName
    },
    family: {
      id: homeData.family.familyId,
      name: homeData.family.familyName,
      addresses: normalizeAddresses(extra.addresses || [])
    },
    member: {
      id: homeData.member.memberId,
      name: homeData.member.name,
      roleTemplate: homeData.member.roleTemplate
    }
  };
}

function buildApiHomeScene(homeData, { imageBaseUrl = '', windowWidth = 0 } = {}) {
  const selectedMeal = getSelectedMealSlot(resolveMealSlots(homeData));
  const dashboardCards = homeData.dashboardCards || [];
  const cartCard = dashboardCards.find((item) => item.key === 'cart');
  const latestOrder = (homeData.recentOrders || [])[0] || null;
  const featuredSource = Array.isArray(homeData.featuredDishes)
    ? homeData.featuredDishes
    : (homeData.featuredDish ? [homeData.featuredDish] : []);
  const featuredDishes = featuredSource.map((item) => ({
    id: item.dishId ?? item.id,
    name: item.name,
    description: item.description || '今日家庭推荐',
    priceText: formatCurrency(item.price),
    soldText: '今日主推',
    reasonText: '适合今天这一餐',
    imageUrl: toImageUrl(imageBaseUrl, item.imageUrl || item.image)
  }));
  const hasMultipleFeaturedDishes = featuredDishes.length > 1;

  return {
    context: buildContext(homeData),
    crew: buildHomeCrewRoleLabels(homeData),
    heroImageUrl: '/assets/brand/hero-warm-kitchen.webp',
    currentMemberName: homeData.member.name,
    currentDate: formatDateText(homeData.serviceDate),
    dailyQuote: '灶间有烟火，家里有温度',
    featuredDishes,
    featuredDish: featuredDishes[0] || null,
    featuredAutoplay: hasMultipleFeaturedDishes,
    featuredCircular: hasMultipleFeaturedDishes,
    featuredIndicatorDots: hasMultipleFeaturedDishes,
    featuredInterval: hasMultipleFeaturedDishes ? 4000 : 0,
    featuredNextMargin: hasMultipleFeaturedDishes ? (Number(windowWidth) > 0 && Number(windowWidth) <= 320 ? '24rpx' : '40rpx') : '0rpx',
    dashboardCards: dashboardCards.map((item) => ({
      ...item,
      note: item.note || ''
    })),
    primaryAction: { key: 'menu', label: '翻开今日菜单' },
    mealSummary: {
      label: selectedMeal ? selectedMeal.name : '当前餐次',
      time: selectedMeal ? selectedMeal.displayTime : ''
    },
    cartSummary: {
      value: cartCard ? cartCard.value : '0 份',
      label: '家庭餐篮'
    },
    orderSummary: {
      statusLabel: latestOrder ? mapOrderStatusLabel(latestOrder.status) : '还未下单',
      orderId: latestOrder ? latestOrder.orderId : null
    },
    flowCards: [
      { key: 'choose', title: '先选特色菜', note: '从首页推荐或菜单里挑选' },
      { key: 'cart', title: '再确认餐篮', note: '补充备注和配送方式' },
      { key: 'track', title: '最后看进度', note: '订单变化会同步提醒' }
    ],
    quickEntries: [
      { key: 'menu', label: '去点菜', note: '看看今天吃什么' },
      { key: 'cart', label: '看餐篮', note: '已选菜品集中确认' },
      { key: 'orders', label: '订单进度', note: '查看当前订单状态' },
      { key: 'profile', label: '我的', note: '地址、通知和钱包' }
    ]
  };
}

function buildApiMenuScene({ homeData, menuItems = [], cart = null, searchKeyword = '', imageBaseUrl = '' }) {
  const normalizedKeyword = String(searchKeyword || '').trim().toLowerCase();
  const cartItems = (cart && cart.items) || [];
  const visibleItems = menuItems.filter((item) => {
    if (!normalizedKeyword) return true;
    return [item.name, item.description]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(normalizedKeyword));
  });

  const menuCards = visibleItems.map((item, index) => {
    const cartItem = cartItems.find((candidate) => candidate.dishId === item.dishId);
    const selectedCount = Number((cartItem && cartItem.quantity) || 0);
    const myQuantity = Number((cartItem && cartItem.currentMemberQuantity) || 0);

    return {
      id: item.dishId,
      name: item.name,
      category: item.categoryId ? `分类 ${item.categoryId}` : '全部菜品',
      badge: item.categoryId ? `分类 ${item.categoryId}` : '家常推荐',
      tasteText: item.description || '今日可点',
      soldText: selectedCount > 0 ? `餐篮已选 ${selectedCount} 份` : '还未加入',
      finalPriceText: formatCurrency(item.price),
      artClass: ['warm', 'soft', 'fresh'][index % 3],
      imageUrl: toImageUrl(imageBaseUrl, item.imageUrl),
      displayImageUrl: toImageUrl(imageBaseUrl, item.imageUrl) || '/assets/brand/dish-placeholder.png',
      displayTags: [item.categoryId ? `分类 ${item.categoryId}` : '家常推荐', item.description || '今日可点'].slice(0, 2),
      priceText: formatCurrency(item.price),
      description: item.description || '今日可点',
      featured: Boolean(item.featured),
      selectedCount,
      selectedByCurrentMemberCount: myQuantity,
      myQuantity,
      cartLineId: cartItem ? cartItem.itemId : null
    };
  });

  return {
    context: buildContext(homeData),
    crew: buildHomeCrewRoleLabels(homeData),
    heroImageUrl: '/assets/ui/kitchen-hero.png',
    currentDate: formatDateText(homeData.serviceDate),
    currentMemberName: homeData.member.name,
    cartItemCount: Number((cart && cart.totalQuantity) || 0),
    activeCategoryKey: 'all',
    activeCategoryLabel: '全部菜品',
    categoryOptions: [{ key: 'all', label: '全部', activeClass: 'active' }],
    expectedMealTimeText: formatExpectedMealTime(cart && cart.expectedMealTime),
    menuCards,
    visibleMenuCards: menuCards,
    searchKeyword,
    resultSummaryText: `共 ${menuCards.length} 道`,
    emptyStateText: normalizedKeyword ? '没有找到匹配菜品' : '当前还没有可点菜品'
  };
}

function buildApiDishDetailScene({ homeData, dishDetail, cart = null, mealSlots = [], imageBaseUrl = '' }) {
  const cartItem = ((cart && cart.items) || []).find((item) => item.dishId === dishDetail.dishId);
  const selectedCount = Number((cartItem && cartItem.quantity) || 0);

  return {
    context: buildContext(homeData),
    dish: {
      id: dishDetail.dishId,
      category: displayText(dishDetail.categoryName, '今日菜单'),
      name: dishDetail.name,
      description: dishDetail.description || '商户维护的菜品详情',
      tasteTags: dishDetail.description ? [dishDetail.description] : ['家常推荐'],
      imageUrl: toImageUrl(imageBaseUrl, dishDetail.imageUrl),
      finalPrice: formatAmountNumber(dishDetail.price),
      familyName: homeData.family.familyName,
      mealLabel: formatExpectedMealTime(cart && cart.expectedMealTime),
      selectedCount,
      myQuantity: Number((cartItem && cartItem.currentMemberQuantity) || 0),
      ingredients: (dishDetail.ingredients || []).map((item, index) => ({
        id: `${dishDetail.dishId}-${index + 1}`,
        name: item.ingredientName,
        quantity: item.quantity,
        unit: item.unit,
        calcType: item.calcType
      })),
      cookingSteps: (dishDetail.cookingSteps || [])
        .slice()
        .sort((left, right) => Number(left.stepNo || 0) - Number(right.stepNo || 0))
    }
  };
}

function buildApiCartScene({ homeData, mealSlots = [], cart, addresses = [], deliveryMode = 'PICKUP', addressId = null }) {
  const normalizedAddresses = normalizeAddresses(addresses);
  const rows = ((cart && cart.items) || []).map((item) => ({
    id: item.itemId,
    dishId: item.dishId,
    dishName: displayText(item.dishName, '未命名菜品'),
    category: '',
    price: formatAmountNumber(item.price),
    quantity: Number(item.quantity || 0),
    totalQuantity: Number(item.quantity || 0),
    myQuantity: Number(item.currentMemberQuantity || 0),
    canEdit: true,
    note: item.currentMemberRemark || '',
    selections: Array.isArray(item.selections) ? item.selections.map((selection) => ({
      ...selection,
      memberName: displayText(selection.memberName, '家庭成员'),
      itemRemark: displayText(selection.itemRemark, '无备注')
    })) : [],
    hasSelectionDetails: Array.isArray(item.selections) && item.selections.length > 0,
    amount: Number(item.price || 0) * Number(item.quantity || 0)
  }));

  const groupedItems = rows.length ? [{ memberId: 'family', ownerName: '全家已选', rows,
    totalAmount: Number((cart && cart.totalAmount) || 0),
    totalAmountText: `合计 ${formatCurrency(cart && cart.totalAmount)}` }] : [];

  const effectiveAddressId = addressId !== null && addressId !== undefined
    ? addressId
    : ((findDefaultAddress(normalizedAddresses) || {}).id || null);
  const currentAddress = normalizedAddresses.find((item) => item.id === effectiveAddressId) || findDefaultAddress(normalizedAddresses);
  const effectiveDeliveryMode = deliveryMode || 'PICKUP';
  const canSubmit = Boolean(cart && cart.items && cart.items.length) && !cart.bookingEnded
    && Boolean(cart.expectedMealTime) && (
    effectiveDeliveryMode !== 'DELIVERY' || Boolean(currentAddress)
  );
  const foundAddressIndex = normalizedAddresses.findIndex((item) => item.id === effectiveAddressId);

  return {
    context: buildContext(homeData, { addresses: normalizedAddresses }),
    cart: {
      note: cart ? cart.remark || '' : ''
    },
    serviceDate: cart ? cart.serverDate : homeData.serviceDate,
    expectedMealTime: cart ? cart.expectedMealTime : null,
    expectedMealTimeText: formatExpectedMealTime(cart && cart.expectedMealTime),
    expectedMealTimeOptions: buildExpectedMealTimeOptions(cart),
    bookingEnded: Boolean(cart && cart.bookingEnded),
    groupedItems,
    chargeLines: [],
    deliveryOptions: [
      { key: 'PICKUP', label: '自取', activeClass: effectiveDeliveryMode === 'PICKUP' ? 'active' : '' },
      { key: 'DELIVERY', label: '配送', activeClass: effectiveDeliveryMode === 'DELIVERY' ? 'active' : '', disabled: !normalizedAddresses.length }
    ],
    addressOptions: normalizedAddresses.map((item) => ({ value: item.id, label: `${item.contactName} · ${item.address}` })),
    addressIndex: foundAddressIndex >= 0 ? foundAddressIndex : 0,
    currentAddress,
    totals: {
      dishTotal: formatAmountNumber(cart ? cart.totalAmount : 0),
      deliveryFee: formatAmountNumber(0),
      totalAmount: formatAmountNumber(cart ? cart.totalAmount : 0)
    },
    warningText: canSubmit ? '' : (cart && cart.bookingEnded ? '今天已停止预约，请明天再来'
      : (!cart || !cart.expectedMealTime ? '请选择今天的预计用餐时间'
        : (effectiveDeliveryMode === 'DELIVERY' && !currentAddress ? '请选择配送地址' : ''))),
    canSubmit
  };
}

function buildApiOrdersScene({ homeData, orders = [], mealSlots = [] }) {
  const mealSlotMap = new Map(resolveMealSlots(homeData, mealSlots).map((item) => [item.mealSlotId, item]));

  return {
    context: buildContext(homeData),
    orders: orders.map((order) => ({
      id: order.orderId,
      orderNo: `#${order.orderId}`,
      serviceDate: order.serviceDate,
      mealLabel: order.expectedMealTime
        ? formatExpectedMealTime(order.expectedMealTime)
        : `${order.serviceDate || ''} ${(mealSlotMap.get(order.mealSlotId) || {}).name || order.mealSlotName || '历史餐次'}`.trim(),
      statusLabel: mapOrderStatusLabel(order.status),
      dishSummaryText: (order.items || []).map((item) => `${item.dishName} x${item.quantity}`).join('、'),
      chargeSummaryText: `合计 ${formatCurrency(order.totalAmount)}`,
      deliveryMode: String(order.deliveryMode || '').toLowerCase(),
      deliveryFee: Number(order.deliveryFee || 0)
    }))
  };
}

function buildApiOrderDetailScene({ homeData, order, mealSlots = [] }) {
  const mealSlot = resolveMealSlots(homeData, mealSlots).find((item) => item.mealSlotId === order.mealSlotId);
  const submitterName = homeData.member.memberId === order.submitterMemberId ? homeData.member.name : '家庭成员';

  return {
    order: {
      id: order.orderId,
      orderNo: `#${order.orderId}`,
      familyName: homeData.family.familyName,
      statusLabel: mapOrderStatusLabel(order.status),
      serviceDate: order.serviceDate,
      mealLabel: order.expectedMealTime
        ? formatExpectedMealTime(order.expectedMealTime)
        : `${order.serviceDate || ''} ${mealSlot ? mealSlot.name : (order.mealSlotName || '历史餐次')}`.trim(),
      submitterName,
      deliveryMode: String(order.deliveryMode || '').toLowerCase(),
      note: order.remark || '',
      addressSnapshot: null
    },
    items: (order.items || []).map((item) => ({
      id: `${order.orderId}-${item.dishId}`,
      dishName: item.dishName,
      ownerName: item.ownerMemberId === homeData.member.memberId ? homeData.member.name : '家庭成员',
      quantity: item.quantity,
      price: formatAmountNumber(item.price),
      amount: formatAmountNumber(item.amount),
      note: item.itemRemark || '',
      selections: Array.isArray(item.selections) ? item.selections.map((selection) => ({
        memberId: selection.userId,
        memberName: selection.memberName || '家庭成员',
        quantity: Number(selection.quantity || 0),
        itemRemark: selection.itemRemark || ''
      })) : [],
      hasSelectionDetails: Array.isArray(item.selections) && item.selections.length > 0
    })),
    charges: [
      {
        memberId: order.submitterMemberId,
        memberName: submitterName,
        totalAmount: formatAmountNumber(order.totalAmount),
        dishAmount: formatAmountNumber(order.totalAmount),
        deliveryFeeAmount: formatAmountNumber(order.deliveryFee),
        status: mapOrderStatusLabel(order.status)
      }
    ],
    timeline: [
      {
        label: mapOrderStatusLabel(order.status),
        time: formatDateTimeText(order.serviceDate),
        note: order.cancelReason || '订单状态已更新'
      }
    ],
    canCancel: String(order.status || '') === 'PENDING'
  };
}

function buildApiNotificationsScene({ homeData, actorType, pageData }) {
  const mapped = (pageData.items || []).map((item) => ({
    id: item.notificationId,
    title: item.title,
    content: item.content,
    read: item.read,
    createdAt: formatDateTimeText(item.createdAt),
    categoryLabel: NOTIFICATION_CATEGORY_LABELS[item.category] || item.category || '消息'
  }));

  return {
    actorType,
    context: buildContext(homeData),
    unreadCount: mapped.filter((item) => !item.read).length,
    unreadItems: mapped.filter((item) => !item.read),
    readItems: mapped.filter((item) => item.read)
  };
}

function mapLedgerItem(item) {
  const amount = Number(item.amount || 0);

  return {
    id: item.ledgerId,
    title: LEDGER_TYPE_LABELS[item.businessType || item.type] || item.businessType || item.type || '余额变动',
    note: item.remark || `可用 ${formatCurrency(item.availableAfter ?? item.balanceAfter)}`,
    amountText: formatSignedCurrency(amount),
    amountClass: amount < 0 ? 'negative' : 'positive',
    createdAt: formatDateTimeText(item.createdAt),
    statusText: `可用 ${formatCurrency(item.availableAfter ?? item.balanceAfter)}`,
    balanceAfterText: `可用 ${formatCurrency(item.availableAfter ?? item.balanceAfter)}`,
    frozenAfterText: `冻结 ${formatCurrency(item.frozenAfter)}`
  };
}

function buildApiWalletLedgerScene({ homeData, ledgers = [] }) {
  return {
    context: buildContext(homeData),
    transactions: ledgers.map(mapLedgerItem)
  };
}

function buildApiWalletScene({ homeData, wallet = null, ledgers = [] }) {
  const latest = ledgers[0] || null;
  const available = wallet ? wallet.availableAmount : (latest ? latest.availableAfter ?? latest.balanceAfter : 0);
  const frozen = wallet ? wallet.frozenAmount : (latest ? latest.frozenAfter : 0);

  return {
    context: buildContext(homeData),
    balanceCards: [
      {
        key: 'available',
        label: '当前余额',
        value: formatCurrency(available),
        note: '家庭成员共同使用'
      },
      {
        key: 'frozen',
        label: '冻结金额',
        value: formatCurrency(frozen),
        note: '待商户确认后结算'
      },
      {
        key: 'ledgerCount',
        label: '流水条数',
        value: `${ledgers.length}`,
        note: '可进入流水页查看全部'
      }
    ],
    latestTransactions: ledgers.slice(0, 3).map(mapLedgerItem)
  };
}

function buildApiProfileScene({ homeData, addresses = [], wallet = null, ledgers = [], session = null }) {
  const normalizedAddresses = normalizeAddresses(addresses);
  const latest = ledgers[0] || null;
  const defaultAddress = findDefaultAddress(normalizedAddresses);

  return {
    context: buildContext(homeData, { addresses: normalizedAddresses }),
    crew: buildHomeCrewRoleLabels(homeData),
    summaryCards: [
      {
        key: 'addressCount',
        label: '地址数量',
        value: `${normalizedAddresses.length}`,
        note: '普通成员也可维护地址'
      },
      {
        key: 'balance',
        label: '当前余额',
        value: formatCurrency(wallet ? wallet.availableAmount : (latest ? latest.availableAfter ?? latest.balanceAfter : 0)),
        note: '家庭钱包可用余额'
      },
      {
        key: 'frozen',
        label: '冻结金额',
        value: formatCurrency(wallet ? wallet.frozenAmount : (latest ? latest.frozenAfter : 0)),
        note: '待确认订单会先冻结'
      }
    ],
    memberCards: [
      {
        id: homeData.member.memberId,
        name: homeData.member.name,
        roleText: homeData.member.roleTemplate === 'merchant_admin' ? '商户管理员' : '家庭成员',
        balanceText: `可用 ${formatCurrency(latest ? latest.balanceAfter : 0)}`,
        frozenText: `冻结 ${formatCurrency(latest ? latest.frozenAfter : 0)}`,
        activeClass: 'active'
      }
    ],
    defaultAddress,
    sessionLabel: session && session.roleTemplate === 'merchant_admin' ? '商户工作台身份' : '家庭点餐身份',
    showDemoSwitchers: false,
    merchantOptions: [],
    merchantIndex: 0,
    familyOptions: [],
    familyIndex: 0,
    memberOptions: [],
    memberIndex: 0
  };
}

function buildApiAddressBookScene({ homeData, addresses = [] }) {
  return {
    context: buildContext(homeData, { addresses }),
    addresses: normalizeAddresses(addresses)
  };
}

module.exports = {
  buildCrewRoleLabels,
  formatExpectedMealTime,
  buildExpectedMealTimeOptions,
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
  normalizeAddresses,
  getSelectedMealSlot,
  mapOrderStatusLabel,
  formatCurrency,
  formatAmountNumber,
  formatDateText,
  formatDateTimeText,
  buildContext
};
