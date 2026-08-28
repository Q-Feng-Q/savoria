const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiOrderDetailScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: {
    id: '',
    scene: null,
    expandedDishIds: {}
  },

  onLoad(query) {
    this.setData({ id: query.id || '' });
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
      const order = await runtime.orders.getOrderDetail(this.data.id);
      const scene = buildApiOrderDetailScene({
        homeData: bundle.homeData,
        order
      });
      this.setData({ scene });
    } catch (error) {
      showApiError(error, '订单详情加载失败');
    }
  },

  async cancelOrder() {
    try {
      await createApiRuntime().orders.cancelOrder(this.data.id, {
        reason: '家庭成员取消订单'
      });
      wx.showToast({ title: '订单已取消', icon: 'success' });
      await this.load();
    } catch (error) {
      showApiError(error, '取消订单失败');
    }
  },
  toggleSelectionDetails(event) {
    const id = String(event.currentTarget.dataset.id);
    this.setData({ [`expandedDishIds.${id}`]: !this.data.expandedDishIds[id] });
  }
});
