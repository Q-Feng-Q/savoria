const { createApiRuntime } = require('../../../utils/api-runtime');
const {
  MERCHANT_ORDER_FILTERS,
  buildApiMerchantOrdersScene,
  buildApiPurchaseScene,
  nextStatusByStatus
} = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');
const { todayText } = require('../../../utils/date');

const statusOptions = [
  { value: 'pending', label: '待确认' },
  { value: 'preparing', label: '备餐中' },
  { value: 'ready', label: '待完成' },
  { value: 'all', label: '全部' }
];

function groupOrdersByServiceDate(orders = []) {
  const groups = [];
  const byDate = new Map();
  orders.forEach((order) => {
    const date = order.serviceDate || '日期待定';
    if (!byDate.has(date)) {
      const group = { serviceDate: date, orders: [] };
      byDate.set(date, group);
      groups.push(group);
    }
    byDate.get(date).orders.push(order);
  });
  return groups;
}

Page({
  data: {
    phase: 'loading',
    pageTitle: '正在读取订单',
    pageDescription: '请稍候',
    familyOptions: [{ value: 'all', label: '全部家庭' }],
    familyIndex: 0,
    requestedFamilyId: '',
    familyFilterActive: false,
    familyFilterLabel: '',
    statusOptions,
    statusIndex: 0,
    orders: [],
    visibleOrders: [],
    orderGroups: [],
    purchaseItemCount: 0,
    purchaseSourceText: '预估 0 项 · 已确认 0 项',
    context: null,
    busyOrderMap: {}
  },

  onLoad(query) { this.setData({ requestedFamilyId: (query && query.familyId) || '' }); },

  onShow() { this.load(); },

  async load({ silent = false } = {}) {
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    if (!silent) this.setData({ phase: 'loading', pageTitle: '正在读取订单', pageDescription: '请稍候' });

    try {
      const runtime = createApiRuntime();
      const [families, orders, purchaseItems] = await Promise.all([
        runtime.merchant.getFamilies(),
        runtime.merchant.getOrders(),
        runtime.purchase.getSummary({ date: todayText(), includePending: true })
      ]);
      if (generation !== this._loadGeneration) return;
      const scene = buildApiMerchantOrdersScene({ session, families, orders });
      const purchaseScene = buildApiPurchaseScene({ session, families, summaryItems: purchaseItems });
      const familyOptions = [{ value: 'all', label: '全部家庭' }].concat(scene.familyOptions || []);
      const requestedFamilyIndex = familyOptions.findIndex((option) => option.value !== 'all'
        && Number(option.value) === Number(this.data.requestedFamilyId));
      const familyIndex = requestedFamilyIndex >= 0 ? requestedFamilyIndex : 0;
      this.setData({
        ...scene,
        familyOptions,
        familyIndex,
        familyFilterActive: familyIndex > 0,
        familyFilterLabel: familyIndex > 0 ? familyOptions[familyIndex].label : '',
        purchaseItemCount: (purchaseItems || []).length,
        purchaseSourceText: purchaseScene.sourceSummary.text,
        phase: scene.orders.length ? 'ready' : 'empty',
        pageTitle: scene.orders.length ? '' : '暂无订单',
        pageDescription: scene.orders.length ? '' : '新订单提交后会出现在这里。'
      }, () => this.applyFilters());
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({
        phase: 'error',
        pageTitle: '订单加载失败',
        pageDescription: resolveApiErrorMessage(error, '订单列表加载失败')
      });
    }
  },

  retryLoad() { return this.load(); },

  applyFilters() {
    const familyOption = this.data.familyOptions[this.data.familyIndex];
    const statusOption = this.data.statusOptions[this.data.statusIndex] || this.data.statusOptions[0];
    const statuses = (MERCHANT_ORDER_FILTERS[statusOption.value] || MERCHANT_ORDER_FILTERS.all).statuses;
    const visibleOrders = (this.data.orders || []).filter((item) => {
      const familyMatch = !familyOption || familyOption.value === 'all' || Number(item.familyId) === Number(familyOption.value);
      return familyMatch && (!statuses.length || statuses.includes(item.rawStatus));
    });
    this.setData({ visibleOrders, orderGroups: groupOrdersByServiceDate(visibleOrders) });
  },

  bindFamily(event) {
    const familyIndex = Number(event.detail.value || 0);
    const option = this.data.familyOptions[familyIndex];
    this.setData({
      familyIndex,
      requestedFamilyId: option && option.value !== 'all' ? option.value : '',
      familyFilterActive: familyIndex > 0,
      familyFilterLabel: familyIndex > 0 && option ? option.label : ''
    }, () => this.applyFilters());
  },

  clearFamilyFilter() {
    this.setData({ familyIndex: 0, requestedFamilyId: '', familyFilterActive: false, familyFilterLabel: '' }, () => this.applyFilters());
  },

  bindStatus(event) {
    const value = event.currentTarget && event.currentTarget.dataset && event.currentTarget.dataset.index !== undefined
      ? event.currentTarget.dataset.index
      : event.detail.value;
    this.setData({ statusIndex: Number(value || 0) }, () => this.applyFilters());
  },

  openDetail(event) {
    wx.navigateTo({ url: `/pages/merchant/merchant-order-detail/index?id=${event.currentTarget.dataset.id}` });
  },

  openPurchase() {
    wx.navigateTo({ url: `/pages/merchant/purchase/index?date=${todayText()}` });
  },

  async advanceOrder(event) {
    const orderId = Number(event.currentTarget.dataset.id || 0);
    if (!orderId || this.data.busyOrderMap[orderId]) return;
    const order = (this.data.orders || []).find((item) => Number(item.id) === orderId);
    if (!order) return;
    this.setData({ busyOrderMap: { ...this.data.busyOrderMap, [orderId]: true } });
    try {
      const runtime = createApiRuntime();
      if (order.rawStatus === 'PENDING') await runtime.merchant.confirmOrder(orderId);
      else await runtime.merchant.advanceOrderStatus(orderId, { status: nextStatusByStatus(order.rawStatus), reason: '' });
      await this.load({ silent: true });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, '推进订单失败'), icon: 'none' });
    } finally {
      const busyOrderMap = { ...this.data.busyOrderMap };
      delete busyOrderMap[orderId];
      this.setData({ busyOrderMap });
    }
  }
});
