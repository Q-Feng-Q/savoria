const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMerchantIngredientsScene } = require('../../../utils/merchant-scenes');
const { requireSession, showApiError, resolveApiErrorMessage } = require('../../../utils/page-api');

function filterIngredients(ingredients, query) {
  const term = String(query || '').trim().toLocaleLowerCase();
  if (!term) return ingredients || [];
  return (ingredients || []).filter((item) =>
    [item.name, item.category, item.unit].some((value) => String(value || '').toLocaleLowerCase().includes(term))
  );
}

Page({
  data: {
    phase: 'loading',
    errorMessage: '',
    busyIngredientId: '',
    summaryCards: [],
    ingredients: [],
    visibleIngredients: [],
    query: '',
    context: null
  },

  onShow() {
    this.load();
  },

  async load({ silent = false } = {}) {
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    if (!silent) this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const runtime = createApiRuntime();
      const ingredients = await runtime.merchant.getIngredients();
      if (generation !== this._loadGeneration) return;
      const scene = buildApiMerchantIngredientsScene({ session, ingredients });
      this.setData({
        ...scene,
        ingredients: scene.ingredients || [],
        visibleIngredients: filterIngredients(scene.ingredients, this.data.query),
        phase: (scene.ingredients || []).length ? 'ready' : 'empty'
      });
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ phase: 'error', errorMessage: resolveApiErrorMessage(error, '食材列表加载失败') });
    }
  },

  retryLoad() { return this.load(); },

  bindSearch(event) {
    const query = event.detail.value;
    this.setData({ query, visibleIngredients: filterIngredients(this.data.ingredients, query) });
  },

  openCreate() {
    if (this.data.busyIngredientId) return;
    wx.navigateTo({ url: '/pages/merchant/ingredient-form/index' });
  },

  openEdit(event) {
    if (this.data.busyIngredientId) return;
    const ingredient = this.data.ingredients.find((item) => Number(item.id) === Number(event.currentTarget.dataset.id));
    if (!ingredient) return;
    wx.navigateTo({ url: `/pages/merchant/ingredient-form/index?id=${ingredient.id}` });
  },

  async removeIngredient(event) {
    const id = event.currentTarget.dataset.id;
    const ingredient = this.data.ingredients.find((item) => String(item.id) === String(id));
    if (!ingredient || !ingredient.removable) {
      wx.showToast({ title: '该食材已被菜品引用，暂不能删除', icon: 'none' });
      return;
    }
    if (this.data.busyIngredientId) return;
    this.setData({ busyIngredientId: id });
    try {
      const runtime = createApiRuntime();
      await runtime.merchant.deleteIngredient(id);
      wx.showToast({ title: '已删除食材', icon: 'success' });
      await this.load({ silent: true });
    } catch (error) {
      showApiError(error, '删除食材失败');
    } finally {
      this.setData({ busyIngredientId: '' });
    }
  }
});

module.exports = { filterIngredients };
