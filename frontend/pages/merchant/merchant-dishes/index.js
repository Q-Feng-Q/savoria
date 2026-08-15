const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMerchantDishesScene } = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');

function normalize(value) {
  return String(value || '').trim().toLocaleLowerCase();
}

function deriveDishRows(rows, query, statusFilter) {
  const term = normalize(query);
  return (rows || []).filter((row) => {
    const statusMatches = statusFilter === 'all' || row.status === statusFilter;
    const text = normalize([row.name, row.category, row.statusText].join(' '));
    const canonicalStatusMatches = normalize(row.status) === term;
    return statusMatches && (!term || canonicalStatusMatches || text.includes(term));
  });
}

function isPendingReviewResult(result) { return Boolean(result && (result.outcome === 'PENDING_REVIEW' || result.status === 'PENDING_REVIEW')); }
function statusSuccessMessage(result) { return isPendingReviewResult(result) ? '已提交审核' : '状态已更新'; }

Page({
  data: {
    phase: 'loading',
    errorMessage: '',
    dishRows: [],
    filteredDishRows: [],
    query: '',
    statusFilter: 'all',
    ingredientCount: 0,
    busyDishMap: {}
    ,reviewEnabled: false
  },

  onShow() { this.load(); },

  async load({ silent = false } = {}) {
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    if (!silent) this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const runtime = createApiRuntime();
      const [dishes, categories, ingredients, settings] = await Promise.all([
        runtime.merchant.getDishes(),
        runtime.merchant.getDishCategories(),
        runtime.merchant.getIngredients(),
        runtime.system && runtime.system.getPublicSettings ? runtime.system.getPublicSettings().catch(() => ({})) : Promise.resolve({})
      ]);
      if (generation !== this._loadGeneration) return;
      const scene = buildApiMerchantDishesScene({ session, dishes, categories, imageBaseUrl: runtime.baseUrl });
      const dishRows = (scene.dishRows || []).map((row) => ({
        ...row,
        imageUrl: row.imageUrl || '/assets/brand/dish-placeholder.png',
        toggleLabel: row.status === 'active' ? '下架' : '上架'
      }));
      this.setData({
        ...scene,
        dishRows,
        filteredDishRows: deriveDishRows(dishRows, this.data.query, this.data.statusFilter),
        ingredientCount: (ingredients || []).length,
        reviewEnabled: Boolean(settings && settings.dishReviewEnabled),
        phase: dishRows.length ? 'ready' : 'empty'
      });
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ phase: 'error', errorMessage: resolveApiErrorMessage(error, '菜品列表加载失败') });
    }
  },

  retryLoad() { return this.load(); },

  refreshFilteredRows(next = {}) {
    const query = Object.prototype.hasOwnProperty.call(next, 'query') ? next.query : this.data.query;
    const statusFilter = next.statusFilter || this.data.statusFilter;
    this.setData({ ...next, filteredDishRows: deriveDishRows(this.data.dishRows, query, statusFilter) });
  },

  bindSearch(event) { this.refreshFilteredRows({ query: event.detail.value }); },
  selectStatusFilter(event) { this.refreshFilteredRows({ statusFilter: event.currentTarget.dataset.status }); },
  openDishTemplates() { wx.navigateTo({ url: '/pages/merchant/dish-templates/index' }); },
  openCreateDish() { wx.navigateTo({ url: '/pages/merchant/dish-edit/index' }); },
  openEditDish(event) { wx.navigateTo({ url: `/pages/merchant/dish-edit/index?id=${event.currentTarget.dataset.id}` }); },
  openIngredients() { wx.navigateTo({ url: '/pages/merchant/ingredient-edit/index' }); },
  openReviews() { wx.navigateTo({ url: '/pages/merchant/dish-reviews/index' }); },

  async toggleStatus(event) {
    const dishId = event.currentTarget.dataset.id;
    if (!dishId || this.data.busyDishMap[dishId]) return;
    this.setData({ busyDishMap: { ...this.data.busyDishMap, [dishId]: true } });
    try {
      const runtime = createApiRuntime();
      const row = this.data.dishRows.find((item) => String(item.id) === String(dishId));
      const result = await runtime.merchant.updateDishStatus(dishId, { status: row && row.status === 'active' ? 'INACTIVE' : 'ACTIVE' });
      wx.showToast({ title: statusSuccessMessage(result), icon: 'success' });
      await this.load({ silent: true });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, '状态更新失败'), icon: 'none' });
    } finally {
      const busyDishMap = { ...this.data.busyDishMap };
      delete busyDishMap[dishId];
      this.setData({ busyDishMap });
    }
  }
});

module.exports = { deriveDishRows, statusSuccessMessage, isPendingReviewResult };
