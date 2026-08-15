const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiOrdersScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
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

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const orders = await runtime.orders.listOrders();
      const scene = buildApiOrdersScene({
        homeData: bundle.homeData,
        orders,
        mealSlots: bundle.mealSlots
      });
      this.setData(scene);
    } catch (error) {
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

