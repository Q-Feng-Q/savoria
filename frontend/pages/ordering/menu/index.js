const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMenuScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { createRequestId } = require('../../../utils/action-request');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { searchKeyword: '', activeCategoryKey: 'all', activeCategoryLabel: '全部菜品', categoryOptions: [], menuCards: [], visibleMenuCards: [], cartItemCount: 0,
    currentDate: '', currentMemberName: '', resultSummaryText: '', emptyStateText: '', context: null,
    mutationBusy: false, phase: 'loading', errorMessage: '' },
  onShow() { this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    const runtime = createApiRuntime();
    this.sceneSource = null;
    this.setData({ phase: 'loading', errorMessage: '', context: null, menuCards: [], visibleMenuCards: [] });
    try {
      const bundle = await loadFamilyBundle(runtime);
      const [menuItems, cart] = await Promise.all([runtime.family.getMenuItems(), runtime.cart.getCart()]);
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.sceneSource = { homeData: bundle.homeData, menuItems, cart, runtime };
      this.refreshView({ searchKeyword: this.data.searchKeyword || '' });
      this.setData({ phase: 'ready' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '点菜页加载失败' });
      showApiError(error, '点菜页加载失败');
    }
  },
  refreshView(options = {}) {
    if (!this.sceneSource) return;
    const searchKeyword = Object.prototype.hasOwnProperty.call(options, 'searchKeyword')
      ? options.searchKeyword : this.data.searchKeyword;
    const activeCategoryKey = Object.prototype.hasOwnProperty.call(options, 'activeCategoryKey')
      ? options.activeCategoryKey : this.data.activeCategoryKey;
    this.setData({ ...buildApiMenuScene({ ...this.sceneSource, searchKeyword, activeCategoryKey,
      imageBaseUrl: this.sceneSource.runtime.baseUrl }), searchKeyword });
  },
  handleSearchInput(event) { this.refreshView({ searchKeyword: event.detail.value || '' }); },
  clearSearch() { this.refreshView({ searchKeyword: '' }); },
  selectCategory(event) { this.refreshView({ activeCategoryKey: String(event.currentTarget.dataset.key || 'all') }); },
  noop() {},
  openDishFromRow(event) { wx.navigateTo({ url: `/pages/ordering/dish-detail/index?id=${event.detail.dish.id}` }); },
  async mutateDish(dish, quantity) {
    if (this.data.mutationBusy || !this.sceneSource) return;
    this.setData({ mutationBusy: true });
    const cart = this.sceneSource.cart;
    try {
      this.sceneSource.cart = await this.sceneSource.runtime.cart.setItemQuantity({
        cartId: cart.cartId, cartVersion: cart.version, requestId: createRequestId(`dish-${dish.id}`),
        dishId: Number(dish.id), quantity: Math.max(0, Number(quantity || 0)), itemRemark: ''
      });
      this.refreshView();
    } catch (error) {
      if (error && (error.code === 40931 || error.code === 40932)) {
        await this.load();
        wx.showToast({ title: error.code === 40932 ? '原餐篮已提交，已切换到新餐篮' : '餐篮已被家人更新，请重新操作', icon: 'none' });
      } else showApiError(error, '更新餐篮失败');
    } finally { this.setData({ mutationBusy: false }); }
  },
  changeDishQuantity(event) { const dish = event.detail.dish; return this.mutateDish(dish, event.detail.value); },
  addDish(event) {
    const dish = (this.data.visibleMenuCards || []).find((item) => Number(item.id) === Number(event.currentTarget.dataset.id));
    if (dish) return this.mutateDish(dish, Number(dish.myQuantity || 0) + 1);
  },
  removeDish(event) {
    const dish = (this.data.visibleMenuCards || []).find((item) => Number(item.cartLineId) === Number(event.currentTarget.dataset.lineId));
    if (dish) return this.mutateDish(dish, Number(dish.myQuantity || 0) - 1);
  },
  openDishDetail(event) { wx.navigateTo({ url: `/pages/ordering/dish-detail/index?id=${event.currentTarget.dataset.id}` }); },
  openCart() { wx.navigateTo({ url: '/pages/ordering/cart/index' }); },
  openOrders() { wx.switchTab({ url: '/pages/ordering/orders/index' }); }
});
