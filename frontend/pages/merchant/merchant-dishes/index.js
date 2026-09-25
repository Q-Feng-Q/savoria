const { PRODUCT_TYPES } = require('../../../utils/nourishment');
const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMerchantDishesScene } = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');

function normalize(value) {
  return String(value || '').trim().toLocaleLowerCase();
}

function deriveDishRows(rows, query, statusFilter, productType = '') {
  const term = normalize(query);
  return (rows || []).filter((row) => {
    const statusMatches = statusFilter === 'all' || row.status === statusFilter;
    const text = normalize([row.name, row.category, row.statusText].join(' '));
    const canonicalStatusMatches = normalize(row.status) === term;
    return (!productType || (row.productType || 'NORMAL') === productType) && statusMatches && (!term || canonicalStatusMatches || text.includes(term));
  });
}

function includesId(ids, id) {
  return (ids || []).some((value) => String(value) === String(id));
}

function toggleVisibleSelection(selectedIds, visibleRows) {
  const selected = selectedIds || [];
  const visibleIds = (visibleRows || []).map((row) => row.id);
  const allVisibleSelected = visibleIds.length > 0 && visibleIds.every((id) => includesId(selected, id));
  if (allVisibleSelected) return selected.filter((id) => !visibleIds.some((visibleId) => String(visibleId) === String(id)));
  return selected.concat(visibleIds.filter((id) => !includesId(selected, id)));
}

function decorateSelection(rows, selectedIds) {
  return (rows || []).map((row) => ({ ...row, selected: includesId(selectedIds, row.id) }));
}

function isPendingReviewResult(result) { return Boolean(result && (result.outcome === 'PENDING_REVIEW' || result.status === 'PENDING_REVIEW')); }
function statusSuccessMessage(result) { return isPendingReviewResult(result) ? '已提交审核' : '状态已更新'; }

Page({
  data: {
    productType: '', productTypeIndex: 0, productTypes: [{ value: '', label: '全部类型' }, ...PRODUCT_TYPES],
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
    reviewEnabled: false,
    scope: 'available',
    selectedDishIds: [],
    allVisibleSelected: false,
    mutationBusy: false
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
        runtime.merchant.getDishes({ scope: this.data.scope, productType: this.data.productType || undefined }),
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
        filteredDishRows: decorateSelection(deriveDishRows(dishRows, this.data.query, this.data.statusFilter, this.data.productType), []),
        selectedDishIds: [],
        allVisibleSelected: false,
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
    const selectedDishIds = Object.prototype.hasOwnProperty.call(next, 'selectedDishIds') ? next.selectedDishIds : this.data.selectedDishIds;
    const filteredDishRows = decorateSelection(deriveDishRows(this.data.dishRows, query, statusFilter, this.data.productType), selectedDishIds);
    const allVisibleSelected = filteredDishRows.length > 0 && filteredDishRows.every((row) => row.selected);
    this.setData({ ...next, selectedDishIds, filteredDishRows, allVisibleSelected });
  },

  changeProductType(event) {
    if (this.data.mutationBusy) return;
    const index = Number(event.detail.value), option = this.data.productTypes[index];
    if (!option) return;
    this.setData({ productType: option.value, productTypeIndex: index, selectedDishIds: [], allVisibleSelected: false });
    return this.load();
  },
  bindSearch(event) { this.refreshFilteredRows({ query: event.detail.value, selectedDishIds: [] }); },
  selectStatusFilter(event) { this.refreshFilteredRows({ statusFilter: event.currentTarget.dataset.status, selectedDishIds: [] }); },
  selectScope(event) {
    if (this.data.mutationBusy) return;
    const scope = event.currentTarget.dataset.scope;
    if (!scope || scope === this.data.scope) return;
    this.setData({ scope, statusFilter: 'all', selectedDishIds: [], allVisibleSelected: false });
    return this.load();
  },
  toggleSelectDish(event) {
    if (this.data.mutationBusy) return;
    const dishId = event.currentTarget.dataset.id;
    const selectedDishIds = includesId(this.data.selectedDishIds, dishId)
      ? this.data.selectedDishIds.filter((id) => String(id) !== String(dishId))
      : this.data.selectedDishIds.concat([dishId]);
    this.refreshFilteredRows({ selectedDishIds });
  },
  toggleSelectAllVisible() {
    if (this.data.mutationBusy) return;
    this.refreshFilteredRows({ selectedDishIds: toggleVisibleSelection(this.data.selectedDishIds, this.data.filteredDishRows) });
  },
  openDishTemplates() { wx.navigateTo({ url: '/pages/merchant/dish-templates/index' }); },
  openCreateDish() { if (this.data.mutationBusy) return; wx.navigateTo({ url: '/pages/merchant/dish-edit/index' }); },
  openEditDish(event) { if (this.data.mutationBusy) return; wx.navigateTo({ url: `/pages/merchant/dish-edit/index?id=${event.currentTarget.dataset.id}` }); },
  openIngredients() { wx.navigateTo({ url: '/pages/merchant/ingredient-edit/index' }); },
  openReviews() { wx.navigateTo({ url: '/pages/merchant/dish-reviews/index' }); },

  async runBatchMutation(kind) {
    if (this.data.mutationBusy || !this.data.selectedDishIds.length) return;
    const seen = new Set();
    const dishIds = this.data.selectedDishIds.filter((id) => {
      const key = String(id);
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
    if (dishIds.length > 100) {
      wx.showToast({ title: '一次最多操作 100 道菜', icon: 'none' });
      return;
    }
    const restoring = kind === 'restore';
    this.setData({ mutationBusy: true });
    try {
      const confirmation = await wx.showModal({
        title: restoring ? `恢复 ${dishIds.length} 道菜` : `删除 ${dishIds.length} 道菜`,
        content: restoring
          ? '恢复后菜品保持下架状态，需要重新上架并配置到家庭菜单。'
          : '删除后菜品会从商户列表和家庭菜单消失，历史订单仍保留，可在回收站恢复。',
        confirmText: restoring ? '确认恢复' : '确认删除',
        confirmColor: restoring ? '#668269' : '#d76552'
      });
      if (!confirmation.confirm) return;
      const runtime = createApiRuntime();
      const result = restoring
        ? await runtime.merchant.batchRestoreDishes({ dishIds })
        : await runtime.merchant.batchDeleteDishes({ dishIds });
      wx.showToast({ title: `${restoring ? '已恢复' : '已删除'} ${result.changedCount || 0} 道菜`, icon: 'success' });
      await this.load({ silent: true });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, restoring ? '批量恢复失败' : '批量删除失败'), icon: 'none' });
    } finally {
      this.setData({ mutationBusy: false });
    }
  },
  batchDeleteSelected() { return this.runBatchMutation('delete'); },
  batchRestoreSelected() { return this.runBatchMutation('restore'); },

  async syncToTemplate(event) {
    const dishId = event.currentTarget.dataset.id;
    if (!dishId || this.data.mutationBusy || this.data.syncBusyDishMap[dishId]) return;
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
    this.setData({ mutationBusy: true, syncBusyDishMap: { ...this.data.syncBusyDishMap, [dishId]: true } });
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
      this.setData({ mutationBusy: false, syncBusyDishMap });
    }
  },

  async toggleStatus(event) {
    const dishId = event.currentTarget.dataset.id;
    if (!dishId || this.data.mutationBusy || this.data.busyDishMap[dishId]) return;
    this.setData({ mutationBusy: true, busyDishMap: { ...this.data.busyDishMap, [dishId]: true } });
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
      this.setData({ mutationBusy: false, busyDishMap });
    }
  },

  async toggleFeatured(event) {
    const dishId = event.currentTarget.dataset.id;
    if (!dishId || this.data.mutationBusy || this.data.busyDishMap[dishId]) return;
    const row = this.data.dishRows.find((item) => String(item.id) === String(dishId));
    if (!row || (!row.featured && (row.status !== 'active' || this.data.featuredCount >= 5))) return;
    this.setData({ mutationBusy: true, busyDishMap: { ...this.data.busyDishMap, [dishId]: true } });
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
      this.setData({ mutationBusy: false, busyDishMap });
    }
  }
});

module.exports = { deriveDishRows, toggleVisibleSelection, statusSuccessMessage, isPendingReviewResult };
