const { toImageUrl } = require('./image-url');
const {
  normalizeAddresses,
  mapOrderStatusLabel,
  formatCurrency,
  formatAmountNumber,
  formatDateText,
  formatDateTimeText
} = require('./api-scenes');

const ACTIVE_ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY'];
const MERCHANT_ORDER_FILTERS = Object.freeze({
  pending: Object.freeze({ statuses: Object.freeze(['PENDING']) }),
  preparing: Object.freeze({ statuses: Object.freeze(['CONFIRMED', 'PREPARING']) }),
  ready: Object.freeze({ statuses: Object.freeze(['READY']) }),
  all: Object.freeze({ statuses: Object.freeze([]) })
});

function buildMerchantContext(session, merchantName, family = null, member = null) {
  return {
    merchant: {
      id: session && session.merchantId ? session.merchantId : null,
      name: merchantName || '商户工作台'
    },
    family,
    member
  };
}

function getMerchantName(session, families = [], familyDetail = null) {
  if (familyDetail && familyDetail.merchantName) {
    return familyDetail.merchantName;
  }
  const fromFamily = families.find((item) => item && item.merchantName);
  return (fromFamily && fromFamily.merchantName) || '商户工作台';
}

function buildMerchantOptions(session, merchantName) {
  return [
    {
      value: session && session.merchantId ? session.merchantId : 0,
      label: merchantName || '当前商户'
    }
  ];
}

function nextActionTextByStatus(status) {
  const textMap = {
    PENDING: '确认订单',
    CONFIRMED: '开始备菜',
    PREPARING: '备菜完成',
    READY: '标记完成'
  };
  return textMap[status] || '查看详情';
}

function nextStatusByStatus(status) {
  const statusMap = {
    PENDING: 'CONFIRMED',
    CONFIRMED: 'PREPARING',
    PREPARING: 'READY',
    READY: 'DONE'
  };
  return statusMap[status] || status;
}

function lowerStatus(status) {
  return String(status || '').toLowerCase();
}

function summarizeOrderItems(items = []) {
  return (items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0);
}

function mapMerchantOrder(order, familyNameMap = new Map(), mealSlotMap = new Map()) {
  const familyName = familyNameMap.get(order.familyId) || order.familyName || `家庭 ${order.familyId}`;
  const mealLabel = order.mealSlotName || `餐次 ${order.mealSlotId}`;
  const itemCount = summarizeOrderItems(order.items);

  return {
    id: order.orderId,
    orderNo: `#${order.orderId}`,
    familyId: order.familyId,
    familyName,
    mealLabel,
    serviceDate: order.serviceDate,
    rawStatus: String(order.status || '').toUpperCase(),
    status: lowerStatus(order.status),
    statusLabel: mapOrderStatusLabel(order.status),
    itemCountText: `共 ${itemCount} 份菜品`,
    chargeSummaryText: `合计 ${formatCurrency(order.totalAmount)}`,
    deliveryMode: lowerStatus(order.deliveryMode),
    deliveryFee: Number(order.deliveryFee || 0),
    nextActionText: nextActionTextByStatus(order.status)
  };
}

function buildApiMerchantScene({
  session,
  families = [],
  orders = [],
  selectedFamilyId = null,
  familyMenuItems = [],
  purchaseSummary = [],
  mealSlots = []
}) {
  const merchantName = getMerchantName(session, families);
  const context = buildMerchantContext(session, merchantName);
  const merchantOptions = buildMerchantOptions(session, merchantName);
  const familyOptions = families.map((item) => ({
    value: item.familyId,
    label: item.familyName
  }));
  const familyNameMap = new Map(families.map((item) => [item.familyId, item.familyName]));
  const mealSlotMap = new Map(mealSlots.map((item) => [item.mealSlotId, item.name]));
  const allActiveOrders = orders
    .filter((item) => ACTIVE_ORDER_STATUSES.includes(String(item.status || '').toUpperCase()))
    .map((item) => mapMerchantOrder(item, familyNameMap, mealSlotMap));
  const activeOrders = allActiveOrders.slice(0, 5);
  const selectedFamily = families.find((item) => Number(item.familyId) === Number(selectedFamilyId))
    || families[0]
    || null;
  const selectedFamilyMenu = (familyMenuItems || [])
    .filter((item) => item.enabled !== false)
    .slice(0, 6)
    .map((item) => ({
      id: item.dishId,
      name: item.dishName || item.name,
      priceText: formatCurrency(item.familyFinalPrice || item.basePrice),
      baseText: `基础价 ${formatCurrency(item.basePrice)}`
    }));

  return {
    context,
    todayCommand: {
      activeOrderCount: allActiveOrders.length
    },
    merchantOptions,
    merchantIndex: 0,
    familyOptions,
    familyIndex: Math.max(0, familyOptions.findIndex((item) => Number(item.value) === Number(selectedFamily && selectedFamily.familyId))),
    statCards: [
      { key: 'familyCount', label: '服务家庭', value: `${families.length}`, note: '当前商户名下家庭数' },
      {
        key: 'activeOrders',
        label: '待处理订单',
        value: `${allActiveOrders.length}`,
        note: '待确认、备菜中与待完成'
      },
      {
        key: 'purchaseItems',
        label: '采购项',
        value: `${purchaseSummary.length}`,
        note: '按当日汇总生成'
      },
      {
        key: 'selectedFamily',
        label: '当前家庭',
        value: selectedFamily ? selectedFamily.familyName : '-',
        note: '用于菜单预览'
      }
    ],
    families: families.map((item) => ({
      id: item.familyId,
      name: item.familyName,
      memberCountText: `${item.memberCount || 0} 位成员`,
      note: item.note || '暂无家庭备注',
      deliveryText: item.deliverySummary || '未设置配送规则',
      menuCountText: `生效菜品 ${item.activeMenuCount || 0} 道`,
      copySourceText: item.defaultAddressText || '暂无默认地址'
    })),
    activeOrders,
    lowBalanceMembers: [],
    selectedFamilyMenu,
    todayPurchaseItemCount: purchaseSummary.length
  };
}

function buildApiMerchantOrdersScene({ session, families = [], orders = [], mealSlots = [] }) {
  const merchantName = getMerchantName(session, families);
  const familyNameMap = new Map(families.map((item) => [item.familyId, item.familyName]));
  const mealSlotMap = new Map(mealSlots.map((item) => [item.mealSlotId, item.name]));
  const mappedOrders = orders.map((item) => mapMerchantOrder(item, familyNameMap, mealSlotMap));

  return {
    context: buildMerchantContext(session, merchantName),
    merchantOptions: buildMerchantOptions(session, merchantName),
    merchantIndex: 0,
    familyOptions: families.map((item) => ({
      value: item.familyId,
      label: item.familyName
    })),
    summaryCards: [
      { key: 'all', label: '订单总数', value: `${mappedOrders.length}`, note: '当前商户全部订单' },
      {
        key: 'pending',
        label: '待处理',
        value: `${mappedOrders.filter((item) => ACTIVE_ORDER_STATUSES.includes(String(item.rawStatus || '').toUpperCase())).length}`,
        note: '待确认到待完成'
      },
      {
        key: 'delivery',
        label: '配送订单',
        value: `${mappedOrders.filter((item) => item.deliveryMode === 'delivery').length}`,
        note: '含默认配送费冻结'
      }
    ],
    orders: mappedOrders
  };
}

function buildApiMerchantOrderDetailScene({ session, order, familyDetail = null, mealSlots = [] }) {
  const merchantName = getMerchantName(session, [], familyDetail);
  const normalizedAddresses = normalizeAddresses((familyDetail && familyDetail.addresses) || []);
  const defaultAddress = normalizedAddresses.find((item) => item.isDefault) || normalizedAddresses[0] || null;
  const mealSlotMap = new Map(mealSlots.map((item) => [item.mealSlotId, item.name]));
  const familyName = (familyDetail && familyDetail.familyName) || order.familyName || `家庭 ${order.familyId}`;
  const submitter = (familyDetail && familyDetail.members || []).find((item) => item.memberId === order.submitterMemberId);
  const status = String(order.status || '').toUpperCase();

  return {
    context: buildMerchantContext(session, merchantName, {
      id: order.familyId,
      name: familyName
    }),
    order: {
      id: order.orderId,
      orderNo: `#${order.orderId}`,
      familyName,
      rawStatus: status,
      statusLabel: mapOrderStatusLabel(status),
      serviceDate: order.serviceDate,
      mealLabel: mealSlotMap.get(order.mealSlotId) || `餐次 ${order.mealSlotId}`,
      submitterName: (submitter && submitter.name) || '家庭成员',
      deliveryMode: lowerStatus(order.deliveryMode),
      note: order.remark || '',
      addressSnapshot: null,
      deliveryFee: formatAmountNumber(order.deliveryFee)
    },
    familyDeliverySummary: (familyDetail && familyDetail.deliverySummary) || '未设置配送规则',
    currentDefaultAddressText: defaultAddress
      ? `${defaultAddress.contactName} / ${defaultAddress.phone} / ${defaultAddress.address}`
      : '暂无默认地址',
    addressChangedHint: '',
    canAdjustDeliveryFee: status === 'PENDING' && String(order.deliveryMode || '').toUpperCase() === 'DELIVERY',
    canReject: status === 'PENDING',
    canCancel: ['CONFIRMED', 'PREPARING'].includes(status),
    canAdvance: ACTIVE_ORDER_STATUSES.includes(status),
    nextActionText: nextActionTextByStatus(status),
    nextStatus: nextStatusByStatus(status),
    items: (order.items || []).map((item) => ({
      id: `${order.orderId}-${item.dishId}-${item.ownerMemberId}`,
      dishName: item.dishName,
      ownerName: item.ownerName || ((familyDetail && familyDetail.members || []).find((member) => member.memberId === item.ownerMemberId) || {}).name || '家庭成员',
      quantity: Number(item.quantity || 0),
      price: formatAmountNumber(item.price),
      amount: formatAmountNumber(item.amount),
      note: item.itemRemark || ''
    })),
    charges: [
      {
        memberId: order.submitterMemberId,
        memberName: (submitter && submitter.name) || '提交人',
        totalAmount: formatAmountNumber(order.totalAmount),
        dishAmount: formatAmountNumber(order.totalAmount),
        deliveryFeeAmount: formatAmountNumber(order.deliveryFee),
        status: mapOrderStatusLabel(status)
      }
    ],
    timeline: [
      {
        label: mapOrderStatusLabel(status),
        time: formatDateText(order.serviceDate),
        note: order.cancelReason || order.rejectReason || '订单状态已同步'
      }
    ]
  };
}

function buildApiMerchantFamiliesScene({ session, families = [] }) {
  const merchantName = getMerchantName(session, families);

  return {
    context: buildMerchantContext(session, merchantName),
    merchantOptions: buildMerchantOptions(session, merchantName),
    merchantIndex: 0,
    summaryCards: [
      { key: 'familyCount', label: '家庭数', value: `${families.length}`, note: '当前商户服务范围' },
      {
        key: 'deliveryEnabled',
        label: '可配送',
        value: `${families.filter((item) => item.deliveryEnabled).length}`,
        note: '已开启配送家庭'
      },
      {
        key: 'lowBalance',
        label: '低余额提醒',
        value: `${families.reduce((sum, item) => sum + Number(item.lowBalanceMemberCount || 0), 0)}`,
        note: '需关注成员余额'
      }
    ],
    families: families.map((item) => ({
      id: item.familyId,
      name: item.familyName,
      memberCountText: `${item.memberCount || 0} 位成员`,
      note: item.note || '暂无家庭备注',
      deliveryText: item.deliverySummary || '未设置配送规则',
      pendingOrderText: `生效菜品 ${item.activeMenuCount || 0} 道`,
      lowBalanceText: `低余额成员 ${item.lowBalanceMemberCount || 0} 人`,
      defaultAddressText: item.defaultAddressText || '暂无默认地址',
      menuSourceText: `冻结金额 ${formatCurrency(item.frozenBalanceTotal)}`,
      activeClass: ''
    }))
  };
}

function buildApiMerchantFamilyDetailScene({
  session,
  familyDetail,
  familyMenuItems = [],
  orders = [],
  ledgersByMember = {}
}) {
  const merchantName = getMerchantName(session, [], familyDetail);
  const addresses = normalizeAddresses(familyDetail.addresses || []);
  const menuPreview = (familyMenuItems || [])
    .filter((item) => item.enabled !== false)
    .slice(0, 8)
    .map((item) => ({
      id: item.dishId,
      name: item.dishName || item.name,
      category: item.categoryId ? `分类 ${item.categoryId}` : '家庭菜单',
      badge: item.description || '已启用',
      priceText: formatCurrency(item.familyFinalPrice ?? item.basePrice ?? 0)
    }));
  const orderPreview = orders
    .filter((item) => Number(item.familyId) === Number(familyDetail.familyId))
    .slice(0, 5)
    .map((item) => ({
      id: item.orderId,
      orderNo: `#${item.orderId}`,
      statusLabel: mapOrderStatusLabel(item.status),
      serviceDate: item.serviceDate,
      mealLabel: item.mealSlotName || `餐次 ${item.mealSlotId}`,
      totalText: `合计 ${formatCurrency(item.totalAmount)}`
    }));

  return {
    merchant: buildMerchantContext(session, merchantName).merchant,
    family: {
      id: familyDetail.familyId,
      name: familyDetail.familyName,
      note: familyDetail.note || '暂无家庭备注',
      deliverySummary: familyDetail.deliverySummary || '未设置配送规则',
      deliveryEnabled: Boolean(familyDetail.deliveryEnabled),
      deliveryFeeDefault: Number(familyDetail.deliveryFeeDefault || 0),
      deliveryFeeFree: Boolean(familyDetail.deliveryFree),
      defaultAddressText: (addresses.find((item) => item.isDefault) || addresses[0])
        ? `${(addresses.find((item) => item.isDefault) || addresses[0]).contactName} / ${(addresses.find((item) => item.isDefault) || addresses[0]).address}`
        : '暂无默认地址',
      copySourceText: `${familyDetail.activeMenuCount || 0} 道菜已生效`
    },
    summaryCards: [
      { key: 'memberCount', label: '成员数', value: `${(familyDetail.members || []).length}`, note: '家庭常用成员' },
      { key: 'addressCount', label: '地址数', value: `${addresses.length}`, note: '普通成员也可维护' },
      { key: 'menuCount', label: '生效菜品', value: `${familyDetail.activeMenuCount || 0}`, note: '当前家庭菜单' }
    ],
    members: (familyDetail.members || []).map((item) => {
      const ledgers = ledgersByMember[item.memberId] || [];
      const latest = ledgers[0] || null;
      const available = latest ? latest.balanceAfter : item.availableBalance;
      const frozen = latest ? latest.frozenAfter : item.frozenBalance;

      return {
        id: item.memberId,
        name: item.name,
        roleText: '家庭成员',
        balanceText: `可用 ${formatCurrency(available)}`,
        frozenText: `冻结 ${formatCurrency(frozen)}`,
        warningText: item.lowBalance ? '余额偏低，建议尽快补充' : ''
      };
    }),
    addresses,
    menuPreview,
    orderPreview
  };
}

function buildApiMerchantDishesScene({ session, dishes = [], categories = [], imageBaseUrl = '' }) {
  const merchantName = '商户工作台';
  const categoryMap = new Map(categories.map((item) => [item.categoryId, item.name]));

  return {
    context: buildMerchantContext(session, merchantName),
    merchantOptions: buildMerchantOptions(session, merchantName),
    merchantIndex: 0,
    summaryCards: [
      { key: 'all', label: '菜品数', value: `${dishes.length}`, note: '当前商户全部菜品' },
      { key: 'active', label: '已上架', value: `${dishes.filter((item) => String(item.status || '').toUpperCase() === 'ACTIVE').length}`, note: '可投放家庭菜单' },
      { key: 'inactive', label: '已下架', value: `${dishes.filter((item) => String(item.status || '').toUpperCase() !== 'ACTIVE').length}`, note: '暂不对外展示' }
    ],
    dishRows: dishes.map((item) => ({
      id: item.dishId,
      name: item.name,
      status: String(item.status || '').toLowerCase() === 'active' ? 'active' : 'inactive',
      statusText: String(item.status || '').toUpperCase() === 'ACTIVE' ? '已上架' : '已下架',
      category: categoryMap.get(item.categoryId) || `分类 ${item.categoryId}`,
      badge: item.description || '商户菜品',
      description: item.description || '暂无菜品说明',
      ingredientsText: `${(item.ingredients || []).length} 种原料`,
      stepsText: `${(item.cookingSteps || []).length} 步做法`,
      familyUseText: '家庭菜单可配置',
      soldText: `基础价 ${formatCurrency(item.basePrice || item.price)}`,
      priceText: formatCurrency(item.basePrice || item.price),
      imageUrl: toImageUrl(imageBaseUrl, item.imageUrl)
    }))
  };
}

function buildApiMerchantIngredientsScene({ session, ingredients = [] }) {
  return {
    context: buildMerchantContext(session, '商户工作台'),
    summaryCards: [
      { key: 'all', label: '食材数', value: `${ingredients.length}`, note: '当前商户原料字典' },
      {
        key: 'used',
        label: '已被引用',
        value: `${ingredients.filter((item) => Number(item.referencedDishCount || 0) > 0).length}`,
        note: '被菜品引用时不可误删'
      },
      {
        key: 'removable',
        label: '可删除',
        value: `${ingredients.filter((item) => item.removable).length}`,
        note: '未被引用时可清理'
      }
    ],
    ingredients: ingredients.map((item) => ({
      id: item.ingredientId,
      name: item.name,
      unit: item.unit,
      category: item.category,
      removable: Boolean(item.removable),
      usedByDishCount: Number(item.referencedDishCount || 0),
      usedByDishText: (item.referencedDishNames || []).join('、') || '暂未被菜品引用'
    }))
  };
}

function buildApiFamilyMenuScene({
  session,
  currentFamilyId,
  families = [],
  menuItems = [],
  imageBaseUrl = ''
}) {
  const merchantName = getMerchantName(session, families);
  const currentFamily = families.find((item) => Number(item.familyId) === Number(currentFamilyId)) || families[0] || null;
  const sourceFamilyOptions = families
    .filter((item) => !currentFamily || Number(item.familyId) !== Number(currentFamily.familyId))
    .map((item) => ({
      value: item.familyId,
      label: item.familyName
    }));

  return {
    context: buildMerchantContext(
      session,
      merchantName,
      currentFamily ? { id: currentFamily.familyId, name: currentFamily.familyName } : null
    ),
    merchantOptions: buildMerchantOptions(session, merchantName),
    merchantIndex: 0,
    familyOptions: families.map((item) => ({
      value: item.familyId,
      label: item.familyName
    })),
    familyIndex: Math.max(0, families.findIndex((item) => Number(item.familyId) === Number(currentFamily && currentFamily.familyId))),
    sourceFamilyOptions,
    sourceFamilyIndex: 0,
    menuRows: menuItems.map((item) => ({
      id: item.dishId,
      name: item.dishName || item.name,
      imageUrl: toImageUrl(imageBaseUrl, item.imageUrl),
      category: item.categoryId ? `分类 ${item.categoryId}` : '家庭菜单',
      basePriceText: formatCurrency(item.basePrice),
      enabledText: item.enabled ? '当前已启用' : '当前已停用',
      enabled: Boolean(item.enabled),
      finalPrice: Number(item.familyFinalPrice ?? item.basePrice ?? 0),
      finalPriceText: formatCurrency(item.familyFinalPrice ?? item.basePrice ?? 0)
    }))
  };
}

function buildPurchaseMealOptions(summaryItems = []) {
  const mealSlotIds = [];
  const seen = new Set();

  (summaryItems || []).forEach((item) => {
    (item.sources || []).forEach((source) => {
      const rawId = source && source.mealSlotId;
      if (rawId === null || rawId === undefined || rawId === '' || typeof rawId === 'boolean') return;

      const mealSlotId = Number(rawId);
      if (!Number.isInteger(mealSlotId) || mealSlotId <= 0 || seen.has(mealSlotId)) return;

      seen.add(mealSlotId);
      mealSlotIds.push(mealSlotId);
    });
  });

  return [
    { key: 'all', label: '全天', mealSlotId: null },
    ...mealSlotIds.map((mealSlotId) => ({
      key: String(mealSlotId),
      label: `餐次 ${mealSlotId}`,
      mealSlotId
    }))
  ];
}

function buildApiPurchaseScene({
  session,
  date,
  mealSlotId,
  mealSlots = [],
  families = [],
  summaryItems = [],
  familyItems = []
}) {
  const merchantName = getMerchantName(session, families);
  const mealSlotMap = new Map(mealSlots.map((item) => [item.mealSlotId, item.name]));
  const familyMap = new Map(families.map((item) => [item.familyId, item.familyName]));
  const visibleSummary = mealSlotId
    ? summaryItems.filter((item) => (item.sources || []).some((source) => Number(source.mealSlotId) === Number(mealSlotId)))
    : summaryItems;
  const estimatedCount = visibleSummary.filter((item) => String(item.sourceStatus || '').toUpperCase() === 'ESTIMATED').length;
  const confirmedCount = visibleSummary.filter((item) => String(item.sourceStatus || '').toUpperCase() === 'CONFIRMED').length;
  const visibleFamilyIds = new Set();
  visibleSummary.forEach((item) => {
    (item.sources || [])
      .filter((source) => !mealSlotId || Number(source.mealSlotId) === Number(mealSlotId))
      .forEach((source) => {
        if (source.familyId !== null && source.familyId !== undefined) {
          visibleFamilyIds.add(String(source.familyId));
        }
      });
  });

  return {
    context: buildMerchantContext(session, merchantName),
    merchantOptions: buildMerchantOptions(session, merchantName),
    merchantIndex: 0,
    date: date || '',
    mealLabel: mealSlotId ? (mealSlotMap.get(mealSlotId) || `餐次 ${mealSlotId}`) : '全天',
    mealOptions: buildPurchaseMealOptions(summaryItems),
    sourceSummary: {
      estimatedCount,
      confirmedCount,
      text: `预估 ${estimatedCount} 项 · 已确认 ${confirmedCount} 项`
    },
    summaryCards: [
      { key: 'itemCount', label: '采购项', value: `${visibleSummary.length}`, note: '按食材汇总' },
      {
        key: 'estimated',
        label: '预估项',
        value: `${visibleSummary.filter((item) => String(item.sourceStatus || '').toUpperCase() === 'ESTIMATED').length}`,
        note: '待确认订单会标记预估'
      },
      {
        key: 'familyCount',
        label: '涉及家庭',
        value: `${visibleFamilyIds.size}`,
        note: '来源家庭数量'
      }
    ],
    items: visibleSummary.map((item) => {
      const allSources = item.sources || [];
      const filteredSources = allSources
        .filter((source) => !mealSlotId || Number(source.mealSlotId) === Number(mealSlotId))
      const sourceStatus = String(item.sourceStatus || '').toUpperCase();
      const isEstimated = sourceStatus === 'ESTIMATED';
      const breakdown = filteredSources.map((source) => ({
        id: `${source.orderId}-${source.dishId}-${source.familyId}-${source.mealSlotId}`,
        familyName: familyMap.get(source.familyId) || `家庭 ${source.familyId}`,
        dishName: `菜品 ${source.dishId}`,
        quantity: null,
        unit: item.unit,
        orderId: source.orderId,
        mealSlotId: source.mealSlotId,
        serviceDate: source.serviceDate,
        status: sourceStatus,
        statusLabel: isEstimated ? '预估' : '已确认',
        isEstimated
      }));
      const hasMixedMealSources = Boolean(mealSlotId) && filteredSources.length < allSources.length;

      return {
        ingredientId: `${item.ingredientName}__${item.unit}__${sourceStatus}`,
        ingredientName: item.ingredientName,
        totalQuantity: hasMixedMealSources ? null : item.quantity,
        quantityText: hasMixedMealSources ? '按餐次查看来源' : `${item.quantity}${item.unit}`,
        quantityScope: hasMixedMealSources ? 'source-only' : 'aggregate',
        unit: item.unit,
        statusText: isEstimated ? '预估' : '已确认',
        familyText: Array.from(new Set(breakdown.map((source) => source.familyName))).join('、') || '暂无',
        mealText: Array.from(new Set(filteredSources.map((source) => mealSlotMap.get(source.mealSlotId) || `餐次 ${source.mealSlotId}`))).join('、') || '全天',
        dishText: Array.from(new Set(breakdown.map((source) => source.dishName))).join('、') || '暂无',
        breakdown
      };
    })
  };
}

module.exports = {
  MERCHANT_ORDER_FILTERS,
  buildApiMerchantScene,
  buildApiMerchantOrdersScene,
  buildApiMerchantOrderDetailScene,
  buildApiMerchantFamiliesScene,
  buildApiMerchantFamilyDetailScene,
  buildApiMerchantDishesScene,
  buildApiMerchantIngredientsScene,
  buildApiFamilyMenuScene,
  buildApiPurchaseScene,
  buildPurchaseMealOptions,
  mapMerchantOrder,
  nextStatusByStatus
};
