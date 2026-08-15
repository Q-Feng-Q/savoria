const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMenuScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: {
    bannerIndex: 0,
    searchKeyword: '',
    mealOptions: [],
    categoryOptions: [],
    menuCards: [],
    visibleMenuCards: [],
    activeCategoryKey: 'all',
    activeCategoryLabel: '全部菜品',
    cartItemCount: 0,
    currentDate: '',
    currentMemberName: '',
    resultSummaryText: '',
    emptyStateText: '',
    context: null,
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
    const activeMealSlotId = this.data.activeMealSlotId || null;

    try {
      const bundle = await loadFamilyBundle(runtime, { mealSlotId: activeMealSlotId });
      const menuItems = await runtime.family.getMenuItems({
        serviceDate: bundle.serviceDate,
        mealSlotId: bundle.activeMealSlotId
      });
      const cart = await runtime.cart.getCart({
        mealSlotId: bundle.activeMealSlotId,
        date: bundle.serviceDate
      });
      this.sceneSource = {
        homeData: bundle.homeData,
        menuItems,
        cart,
        runtime
      };
      this.refreshView({
        activeMealSlotId: bundle.activeMealSlotId,
        searchKeyword: this.data.searchKeyword || ''
      });
      this.setData({ phase: 'ready' });
    } catch (error) {
      this.setData({ phase: 'error', errorMessage: error.message || '点菜页加载失败' });
      showApiError(error, '点菜页加载失败');
    }
  },

  refreshView(options = {}) {
    if (!this.sceneSource) return;

    const scene = buildApiMenuScene({
      homeData: this.sceneSource.homeData,
      menuItems: this.sceneSource.menuItems,
      cart: this.sceneSource.cart,
      searchKeyword: Object.prototype.hasOwnProperty.call(options, 'searchKeyword')
        ? options.searchKeyword
        : this.data.searchKeyword,
      imageBaseUrl: this.sceneSource.runtime.baseUrl
    });

    this.setData({
      ...scene,
      activeMealSlotId: options.activeMealSlotId || this.data.activeMealSlotId || null,
      searchKeyword: Object.prototype.hasOwnProperty.call(options, 'searchKeyword')
        ? options.searchKeyword
        : this.data.searchKeyword
    });
  },

  async selectMeal(event) {
    this.setData({
      activeMealSlotId: Number(event.currentTarget.dataset.meal || 0)
    });
    await this.load();
  },

  handleSearchInput(event) {
    this.refreshView({
      searchKeyword: event.detail.value || ''
    });
  },

  clearSearch() {
    this.refreshView({
      searchKeyword: ''
    });
  },

  noop() {},

  openDishFromRow(event) {
    wx.navigateTo({
      url: `/pages/ordering/dish-detail/index?id=${event.detail.dish.id}&mealSlotId=${this.data.activeMealSlotId || ''}`
    });
  },

  async changeDishQuantity(event) {
    const dish = event.detail.dish;
    const quantity = Number(event.detail.value || 0);
    const runtime = createApiRuntime();

    try {
      if (dish.cartLineId) {
        if (quantity <= 0) {
          await runtime.cart.deleteItem(Number(dish.cartLineId));
        } else {
          await runtime.cart.updateItem(Number(dish.cartLineId), {
            mealSlotId: this.data.activeMealSlotId,
            date: this.sceneSource.homeData.serviceDate,
            dishId: Number(dish.id),
            quantity
          });
        }
      } else if (quantity > 0) {
        await runtime.cart.addItem({
          mealSlotId: this.data.activeMealSlotId,
          date: this.sceneSource.homeData.serviceDate,
          dishId: Number(dish.id),
          quantity
        });
      }
      await this.load();
    } catch (error) {
      showApiError(error, '更新餐篮失败');
    }
  },

  async addDish(event) {
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    try {
      await runtime.cart.addItem({
        mealSlotId: this.data.activeMealSlotId,
        date: this.sceneSource.homeData.serviceDate,
        dishId: Number(event.currentTarget.dataset.id),
        quantity: 1
      });
      wx.showToast({ title: '已加入餐篮', icon: 'success' });
      await this.load();
    } catch (error) {
      showApiError(error, '加入餐篮失败');
    }
  },

  async removeDish(event) {
    const lineId = Number(event.currentTarget.dataset.lineId || 0);
    if (!lineId) return;

    const currentCard = (this.data.visibleMenuCards || []).find((item) => Number(item.cartLineId) === lineId);
    if (!currentCard || Number(currentCard.selectedByCurrentMemberCount || 0) <= 1) {
      try {
        await createApiRuntime().cart.deleteItem(lineId);
        await this.load();
      } catch (error) {
        showApiError(error, '更新餐篮失败');
      }
      return;
    }

    try {
      await createApiRuntime().cart.updateItem(lineId, {
        mealSlotId: this.data.activeMealSlotId,
        date: this.sceneSource.homeData.serviceDate,
        dishId: Number(currentCard.id),
        quantity: Number(currentCard.selectedByCurrentMemberCount) - 1
      });
      await this.load();
    } catch (error) {
      showApiError(error, '更新餐篮失败');
    }
  },

  openDishDetail(event) {
    wx.navigateTo({
      url: `/pages/ordering/dish-detail/index?id=${event.currentTarget.dataset.id}&mealSlotId=${this.data.activeMealSlotId || ''}`
    });
  },

  openCart() {
    wx.navigateTo({ url: `/pages/ordering/cart/index?mealSlotId=${this.data.activeMealSlotId || ''}` });
  },

  openOrders() {
    wx.switchTab({ url: '/pages/ordering/orders/index' });
  }
});

