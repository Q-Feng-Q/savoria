const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiOrdersScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: {
    orders: [],
    context: null
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    this.setData({ orders: [], context: null });

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const orders = await runtime.orders.listOrders();
      const scene = buildApiOrdersScene({
        homeData: bundle.homeData,
        orders
      });
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData(scene);
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      showApiError(error, '订单列表加载失败');
    }
  },

  openDetail(event) {
    wx.navigateTo({ url: `/pages/ordering/order-detail/index?id=${event.currentTarget.dataset.id}` });
  }
  ,

  openOrderRow(event) {
    wx.navigateTo({ url: `/pages/ordering/order-detail/index?id=${event.detail.order.id}` });
  }
});

