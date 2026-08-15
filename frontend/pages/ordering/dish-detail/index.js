const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiDishDetailScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: {
    id: '',
    mealSlotId: null,
    scene: null
  },

  onLoad(query) {
    this.setData({
      id: query.id || '',
      mealSlotId: query.mealSlotId ? Number(query.mealSlotId) : null
    });
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime, { mealSlotId: this.data.mealSlotId });
      const dishDetail = await runtime.family.getDishDetail(this.data.id);
      const cart = await runtime.cart.getCart({
        mealSlotId: bundle.activeMealSlotId,
        date: bundle.serviceDate
      });
      const scene = buildApiDishDetailScene({
        homeData: bundle.homeData,
        dishDetail,
        cart,
        mealSlots: bundle.mealSlots,
        imageBaseUrl: runtime.baseUrl
      });

      this.setData({
        scene,
        mealSlotId: bundle.activeMealSlotId
      });
    } catch (error) {
      showApiError(error, '菜品详情加载失败');
    }
  },

  async addDish() {
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime, { mealSlotId: this.data.mealSlotId });
      await runtime.cart.addItem({
        mealSlotId: bundle.activeMealSlotId,
        date: bundle.serviceDate,
        dishId: Number(this.data.id),
        quantity: 1
      });
      wx.showToast({ title: '已加入餐篮', icon: 'success' });
      await this.load();
    } catch (error) {
      showApiError(error, '加入餐篮失败');
    }
  }
});

