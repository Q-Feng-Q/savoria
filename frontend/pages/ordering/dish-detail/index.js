const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiDishDetailScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { createRequestId } = require('../../../utils/action-request');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { id: '', scene: null, mutationBusy: false, phase: 'loading', errorMessage: '' },
  onLoad(query) { this.setData({ id: (query && query.id) || '' }); },
  onShow() { this.load(); },
  retryLoad() { return this.load(); },
  async load() {
    const session = requireSession({ familyOnly: true });
    if (!session) return;
    if (!this.data.id) {
      this.setData({ phase: 'error', errorMessage: '缺少菜品编号', scene: null });
      return;
    }
    const loadToken = this.identityLoad.begin(session);
    this.source = null;
    this.setData({ phase: 'loading', errorMessage: '', scene: null });
    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const [dishDetail, cart] = await Promise.all([runtime.family.getDishDetail(this.data.id), runtime.cart.getCart()]);
      if (!this.identityLoad.isCurrent(loadToken)) return;
      if (!dishDetail) throw new Error('菜品不存在或已下架');
      this.source = { runtime, homeData: bundle.homeData, dishDetail, cart };
      this.setData({ scene: buildApiDishDetailScene({ ...this.source, imageBaseUrl: runtime.baseUrl }), phase: 'ready', errorMessage: '' });
    } catch (error) {
      if (this.identityLoad.isCurrent(loadToken)) {
        this.setData({ phase: 'error', errorMessage: (error && error.message) || '菜品详情加载失败' });
        showApiError(error, '菜品详情加载失败');
      }
    }
  },
  async addDish() {
    if (!requireSession({ familyOnly: true }) || this.data.mutationBusy || !this.source) return;
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
