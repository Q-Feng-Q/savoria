const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiDishDetailScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { createRequestId } = require('../../../utils/action-request');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { id: '', scene: null, mutationBusy: false },
  onLoad(query) { this.setData({ id: query.id || '' }); },
  onShow() { this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    this.source = null;
    this.setData({ scene: null });
    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const [dishDetail, cart] = await Promise.all([runtime.family.getDishDetail(this.data.id), runtime.cart.getCart()]);
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.source = { runtime, homeData: bundle.homeData, dishDetail, cart };
      this.setData({ scene: buildApiDishDetailScene({ ...this.source, imageBaseUrl: runtime.baseUrl }) });
    } catch (error) {
      if (this.identityLoad.isCurrent(loadToken)) showApiError(error, '菜品详情加载失败');
    }
  },
  async addDish() {
    if (!requireSession() || this.data.mutationBusy || !this.source) return;
    this.setData({ mutationBusy: true });
    const { runtime, cart, dishDetail } = this.source;
    const item = (cart.items || []).find((row) => Number(row.dishId) === Number(dishDetail.dishId));
    try {
      this.source.cart = await runtime.cart.setItemQuantity({ cartId: cart.cartId, cartVersion: cart.version,
        requestId: createRequestId(`dish-${dishDetail.dishId}`), dishId: Number(dishDetail.dishId),
        quantity: Number((item && item.currentMemberQuantity) || 0) + 1, itemRemark: (item && item.currentMemberRemark) || '' });
      this.setData({ scene: buildApiDishDetailScene({ ...this.source, imageBaseUrl: runtime.baseUrl }) });
      wx.showToast({ title: '已加入共享餐篮', icon: 'success' });
    } catch (error) {
      if (error && (error.code === 40931 || error.code === 40932)) await this.load();
      showApiError(error, '加入餐篮失败');
    } finally { this.setData({ mutationBusy: false }); }
  }
});
