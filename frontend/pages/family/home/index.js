const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiHomeScene } = require('../../../utils/api-scenes');
const { requireSession, showApiError } = require('../../../utils/page-api');

const QUICK_ENTRY_ROUTES = {
  menu: '/pages/ordering/menu/index',
  cart: '/pages/ordering/cart/index',
  orders: '/pages/ordering/orders/index',
  profile: '/pages/account/profile/index'
};

Page({
  data: {
    bannerIndex: 0,
    dashboardCards: [],
    featuredDish: null,
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
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const homeData = await runtime.family.getHome();
      const scene = buildApiHomeScene(homeData, { imageBaseUrl: runtime.baseUrl });
      this.setData({ ...scene, phase: 'ready' });
    } catch (error) {
      this.setData({ phase: 'error', errorMessage: error.message || '首页加载失败' });
      showApiError(error, '首页加载失败');
    }
  },

  openMenu() {
    wx.switchTab({ url: QUICK_ENTRY_ROUTES.menu });
  },

  openDishDetail(event) {
    wx.navigateTo({ url: `/pages/ordering/dish-detail/index?id=${event.currentTarget.dataset.id}` });
  },

  openQuickEntry(event) {
    const key = event.currentTarget.dataset.key;
    const url = QUICK_ENTRY_ROUTES[key];
    if (!url) return;

    if (key === 'cart') {
      wx.navigateTo({ url });
      return;
    }

    wx.switchTab({ url });
  }
});
