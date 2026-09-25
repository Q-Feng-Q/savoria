const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiHomeScene } = require('../../../utils/api-scenes');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

const QUICK_ENTRY_ROUTES = {
  menu: '/pages/ordering/menu/index',
  cart: '/pages/ordering/cart/index',
  orders: '/pages/ordering/orders/index',
  profile: '/pages/account/profile/index'
};

const { withBranding } = require('../../../utils/branding'); Page(withBranding({
  identityLoad: createIdentityLoadGuard(),
  data: {
    bannerIndex: 0,
    dashboardCards: [],
    featuredDishes: [],
    featuredDish: null,
    featuredAutoplay: false,
    featuredCircular: false,
    featuredIndicatorDots: false,
    featuredInterval: 0,
    featuredNextMargin: '0rpx',
    flowCards: [],
    quickEntries: [],
    currentDate: '',
    currentMemberName: '',
    dailyQuote: '',
    context: null,
    heroImageUrl: '',
    phase: 'loading',
    errorMessage: ''
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession({ familyOnly: true });
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);

    const runtime = createApiRuntime();
    this.setData({ phase: 'loading', errorMessage: '', context: null });
    try {
      const [homeData, orders, cart] = await Promise.all([
        runtime.family.getHome(), runtime.orders.listOrders(), runtime.cart.getCart()
      ]);
      const windowInfo = typeof wx.getWindowInfo === 'function' ? wx.getWindowInfo() : {};
      const scene = buildApiHomeScene(homeData, {
        imageBaseUrl: runtime.baseUrl, orders, cart,
        windowWidth: windowInfo.windowWidth
      });
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ ...scene, phase: 'ready' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '首页加载失败' });
      showApiError(error, '首页加载失败');
    }
  },

  openMenu() {
    wx.switchTab({ url: QUICK_ENTRY_ROUTES.menu });
  },

  openDishDetail(event) {
    const dishId = event.currentTarget.dataset.id;
    if (!dishId) return;
    wx.navigateTo({ url: `/pages/ordering/dish-detail/index?id=${dishId}` });
  },

  openQuickEntry(event) {
    const key = event.currentTarget.dataset.key;
    const url = QUICK_ENTRY_ROUTES[key];
    if (!url) return;
    wx.switchTab({ url });
  }
}));
