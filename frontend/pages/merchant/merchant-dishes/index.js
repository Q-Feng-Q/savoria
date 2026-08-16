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
    featuredCount: 0,
    ingredientCount: 0,
    busyDishMap: {},
    syncBusyDishMap: {},
    reviewEnabled: false
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
      const errorMessage = resolveApiErrorMessage(error, silent ? '菜品列表刷新失败' : '菜品列表加载失败');
      if (silent) {
        wx.showToast({ title: errorMessage, icon: 'none' });
        return;
      }
      this.setData({ phase: 'error', errorMessage });
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

  async syncToTemplate(event) {
    const dishId = event.currentTarget.dataset.id;
    if (!dishId || this.data.syncBusyDishMap[dishId]) return;
    const row = this.data.dishRows.find((item) => String(item.id) === String(dishId));
    if (!row || !row.templateImported) return;
    const confirmation = await wx.showModal({
      title: `同步“${row.name}”到模板`,
      content: '提交后进入平台审核，不会立即修改系统菜库。可填写本次调整说明。',
      editable: true,
      placeholderText: '申请说明（选填）',
      confirmText: '提交审核'
    });
    if (!confirmation.confirm) return;
    this.setData({ syncBusyDishMap: { ...this.data.syncBusyDishMap, [dishId]: true } });
    try {
      const runtime = createApiRuntime();
      const result = await runtime.merchant.submitImportedDishTemplateChange(dishId, {
        submitNote: String(confirmation.content || '').trim() || null
      });
      wx.showToast({ title: '已提交模板审核', icon: 'success' });
      wx.navigateTo({ url: `/pages/merchant/dish-template-change-detail/index?id=${result.requestId}` });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, '模板同步申请失败'), icon: 'none' });
    } finally {
      const syncBusyDishMap = { ...this.data.syncBusyDishMap };
      delete syncBusyDishMap[dishId];
      this.setData({ syncBusyDishMap });
    }
  },

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
  },

  async toggleFeatured(event) {
    const dishId = event.currentTarget.dataset.id;
    if (!dishId || this.data.busyDishMap[dishId]) return;
    const row = this.data.dishRows.find((item) => String(item.id) === String(dishId));
    if (!row || (!row.featured && (row.status !== 'active' || this.data.featuredCount >= 5))) return;
    this.setData({ busyDishMap: { ...this.data.busyDishMap, [dishId]: true } });
    try {
      const runtime = createApiRuntime();
      await runtime.merchant.setDishFeatured(dishId, !row.featured);
      wx.showToast({ title: row.featured ? '已取消推荐' : '已设为推荐', icon: 'success' });
      await this.load({ silent: true });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, '推荐状态更新失败'), icon: 'none' });
    } finally {
      const busyDishMap = { ...this.data.busyDishMap };
      delete busyDishMap[dishId];
      this.setData({ busyDishMap });
    }
  }
});

module.exports = { deriveDishRows, statusSuccessMessage, isPendingReviewResult };
