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

function buildApiHomeScene(homeData, { imageBaseUrl = '' } = {}) {
  const selectedMeal = getSelectedMealSlot(resolveMealSlots(homeData));
  const dashboardCards = homeData.dashboardCards || [];
  const cartCard = dashboardCards.find((item) => item.key === 'cart');
  const latestOrder = (homeData.recentOrders || [])[0] || null;

  return {
    context: buildContext(homeData),
    heroImageUrl: '/assets/brand/hero-warm-kitchen.webp',
    currentMemberName: homeData.member.name,
    currentDate: formatDateText(homeData.serviceDate),
    dailyQuote: '灶间有烟火，家里有温度',
    featuredDish: homeData.featuredDish ? {
      id: homeData.featuredDish.dishId,
      name: homeData.featuredDish.name,
      priceText: formatCurrency(homeData.featuredDish.price),
      soldText: '今日主推',
      reasonText: '适合今天这一餐',
      imageUrl: toImageUrl(imageBaseUrl, homeData.featuredDish.imageUrl)
    } : null,
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
  const mealSlots = resolveMealSlots(homeData);
  const currentMemberId = homeData.member.memberId;
  const normalizedKeyword = String(searchKeyword || '').trim().toLowerCase();
  const cartItems = (cart && cart.items) || [];
  const visibleItems = menuItems.filter((item) => {
    if (!normalizedKeyword) return true;
    return [item.name, item.description]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(normalizedKeyword));
  });

  const menuCards = visibleItems.map((item, index) => {
    const relatedItems = cartItems.filter((cartItem) => cartItem.dishId === item.dishId);
    const currentMemberItem = relatedItems.find((cartItem) => cartItem.ownerMemberId === currentMemberId);
    const selectedCount = relatedItems.reduce((sum, cartItem) => sum + Number(cartItem.quantity || 0), 0);

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
      selectedCount,
      selectedByCurrentMemberCount: currentMemberItem ? Number(currentMemberItem.quantity || 0) : 0,
      cartLineId: currentMemberItem ? currentMemberItem.itemId : null
    };
  });

  return {
    context: buildContext(homeData),
    heroImageUrl: '/assets/ui/kitchen-hero.png',
    currentDate: formatDateText(homeData.serviceDate),
    currentMemberName: homeData.member.name,
    cartItemCount: Number((cart && cart.totalQuantity) || 0),
    activeCategoryKey: 'all',
    activeCategoryLabel: '全部菜品',
    categoryOptions: [{ key: 'all', label: '全部', activeClass: 'active' }],
    mealOptions: mealSlots.map((slot) => ({
      key: slot.mealSlotId,
      label: slot.name,
      time: slot.displayTime,
      statusText: slot.selected ? '当前餐次' : '切换',
      activeClass: slot.selected ? 'active' : ''
    })),
    menuCards,
    visibleMenuCards: menuCards,
    searchKeyword,
    resultSummaryText: `共 ${menuCards.length} 道`,
    emptyStateText: normalizedKeyword ? '没有找到匹配菜品' : '当前还没有可点菜品'
  };
}

function buildApiDishDetailScene({ homeData, dishDetail, cart = null, mealSlots = [], imageBaseUrl = '' }) {
  const slots = resolveMealSlots(homeData, mealSlots);
  const selectedMealSlot = getSelectedMealSlot(slots);
  const selectedCount = ((cart && cart.items) || [])
    .filter((item) => item.dishId === dishDetail.dishId)
    .reduce((sum, item) => sum + Number(item.quantity || 0), 0);

  return {
    context: buildContext(homeData),
    dish: {
      id: dishDetail.dishId,
      category: dishDetail.categoryId ? `分类 ${dishDetail.categoryId}` : '今日特色',
      name: dishDetail.name,
      description: dishDetail.description || '商户维护的菜品详情',
      tasteTags: dishDetail.description ? [dishDetail.description] : ['家常推荐'],
      imageUrl: toImageUrl(imageBaseUrl, dishDetail.imageUrl),
      finalPrice: formatAmountNumber(dishDetail.price),
      basePrice: formatAmountNumber(dishDetail.price),
      familyName: homeData.family.familyName,
      mealLabel: selectedMealSlot ? selectedMealSlot.name : '当前餐次',
      selectedCount,
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
    dishName: item.dishName,
    category: '',
    price: formatAmountNumber(item.price),
    quantity: item.quantity,
    ownerMemberId: item.ownerMemberId,
    ownerName: item.ownerMemberName,
    canEdit: Boolean(item.editable),
    note: item.itemRemark || '',
    amount: Number(item.price || 0) * Number(item.quantity || 0)
  }));

  const groupedItems = rows.reduce((groups, row) => {
    const current = groups.find((item) => item.memberId === row.ownerMemberId);
    if (current) {
      current.rows.push(row);
      current.totalAmount += row.amount;
      current.totalAmountText = `小计 ${formatCurrency(current.totalAmount)}`;
      return groups;
    }

    groups.push({
      memberId: row.ownerMemberId,
      ownerName: row.ownerName,
      rows: [row],
      totalAmount: row.amount,
      totalAmountText: `小计 ${formatCurrency(row.amount)}`
    });
    return groups;
  }, []);

  const effectiveAddressId = addressId !== null && addressId !== undefined
    ? addressId
    : ((findDefaultAddress(normalizedAddresses) || {}).id || null);
  const currentAddress = normalizedAddresses.find((item) => item.id === effectiveAddressId) || findDefaultAddress(normalizedAddresses);
  const effectiveDeliveryMode = deliveryMode || 'PICKUP';
  const canSubmit = Boolean(cart && cart.items && cart.items.length) && (
    effectiveDeliveryMode !== 'DELIVERY' || Boolean(currentAddress)
  );
  const foundAddressIndex = normalizedAddresses.findIndex((item) => item.id === effectiveAddressId);

  return {
    context: buildContext(homeData, { addresses: normalizedAddresses }),
    cart: {
      note: cart ? cart.remark || '' : ''
    },
    serviceDate: cart ? cart.date : homeData.serviceDate,
    mealSlotId: cart ? cart.mealSlotId : null,
    mealOptions: resolveMealSlots(homeData, mealSlots).map((slot) => ({
      key: slot.mealSlotId,
      label: slot.name,
      time: slot.displayTime,
      activeClass: cart && slot.mealSlotId === cart.mealSlotId ? 'active' : ''
    })),
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
    warningText: canSubmit ? '' : (effectiveDeliveryMode === 'DELIVERY' && !currentAddress ? '请选择配送地址' : ''),
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
      mealLabel: (mealSlotMap.get(order.mealSlotId) || {}).name || `餐次 ${order.mealSlotId}`,
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
      mealLabel: mealSlot ? mealSlot.name : `餐次 ${order.mealSlotId}`,
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
      note: item.itemRemark || ''
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
    title: LEDGER_TYPE_LABELS[item.type] || item.type || '余额变动',
    note: item.remark || `余额 ${formatCurrency(item.balanceAfter)}`,
    amountText: formatSignedCurrency(amount),
    amountClass: amount < 0 ? 'negative' : 'positive',
    createdAt: formatDateTimeText(item.createdAt),
    statusText: `余额 ${formatCurrency(item.balanceAfter)}`,
    balanceAfterText: `余额 ${formatCurrency(item.balanceAfter)}`,
    frozenAfterText: `冻结 ${formatCurrency(item.frozenAfter)}`
  };
}

function buildApiWalletLedgerScene({ homeData, ledgers = [] }) {
  return {
    context: buildContext(homeData),
    transactions: ledgers.map(mapLedgerItem)
  };
}

function buildApiWalletScene({ homeData, ledgers = [] }) {
  const latest = ledgers[0] || null;

  return {
    context: buildContext(homeData),
    balanceCards: [
      {
        key: 'available',
        label: '当前余额',
        value: formatCurrency(latest ? latest.balanceAfter : 0),
        note: '按最新流水余额展示'
      },
      {
        key: 'frozen',
        label: '冻结金额',
        value: formatCurrency(latest ? latest.frozenAfter : 0),
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

function buildApiProfileScene({ homeData, addresses = [], ledgers = [], session = null }) {
  const normalizedAddresses = normalizeAddresses(addresses);
  const latest = ledgers[0] || null;
  const defaultAddress = findDefaultAddress(normalizedAddresses);

  return {
    context: buildContext(homeData, { addresses: normalizedAddresses }),
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
        value: formatCurrency(latest ? latest.balanceAfter : 0),
        note: '来自最新钱包流水'
      },
      {
        key: 'frozen',
        label: '冻结金额',
        value: formatCurrency(latest ? latest.frozenAfter : 0),
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
