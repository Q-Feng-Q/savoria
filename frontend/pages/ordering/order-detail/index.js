const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiOrderDetailScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: {
    id: '',
    scene: null,
    expandedDishIds: {},
    phase: 'loading',
    errorMessage: '',
    mutationBusy: false
  },

  onLoad(query) {
    this.setData({ id: (query && query.id) || '' });
  },

  onShow() {
    this.load();
  },

  retryLoad() { return this.load(); },

  async load({ silent = false } = {}) {
    const session = requireSession({ familyOnly: true });
    if (!session) return;
    if (!this.data.id) {
      this.setData({ phase: 'error', errorMessage: '缺少订单编号', scene: null });
      return;
    }
    const loadToken = this.identityLoad.begin(session);
    if (!silent) this.setData({ phase: 'loading', errorMessage: '', scene: null, expandedDishIds: {} });

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const order = await runtime.orders.getOrderDetail(this.data.id);
      if (!order) throw new Error('订单不存在或已无权查看');
      const scene = buildApiOrderDetailScene({
        homeData: bundle.homeData,
        order
      });
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ scene, phase: 'ready', errorMessage: '' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      if (!silent) this.setData({ phase: 'error', errorMessage: (error && error.message) || '订单详情加载失败' });
      showApiError(error, '订单详情加载失败');
    }
  },

  async cancelOrder() {
    if (this.data.mutationBusy) return;
    this.setData({ mutationBusy: true });
    try {
      await createApiRuntime().orders.cancelOrder(this.data.id, {
        reason: '家庭成员取消订单'
      });
      wx.showToast({ title: '订单已取消', icon: 'success' });
      await this.load({ silent: true });
    } catch (error) {
      showApiError(error, '取消订单失败');
    } finally { this.setData({ mutationBusy: false }); }
  },
  toggleSelectionDetails(event) {
    const id = String(event.currentTarget.dataset.id);
    this.setData({ [`expandedDishIds.${id}`]: !this.data.expandedDishIds[id] });
  }
});
