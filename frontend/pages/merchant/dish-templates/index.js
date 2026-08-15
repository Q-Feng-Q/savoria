const { createApiRuntime } = require('../../../utils/api-runtime');
const { createDishTemplateSelection } = require('../../../utils/dish-template-selection');
const { toImageUrl } = require('../../../utils/image-url');
const { requireSession, showApiError } = require('../../../utils/page-api');

function mapTemplateRows(runtime, rows, selectedIds) {
  const selected = new Set(selectedIds);
  return (rows || []).map(item => ({
    ...item,
    imageUrl: toImageUrl(runtime.baseUrl, item.imageUrl),
    selected: selected.has(Number(item.templateId)),
    tagsText: (item.tasteTags || []).join(' · '),
    priceText: `¥${Number(item.referencePrice || 0).toFixed(0)}`
  }));
}

Page({
  data: {
    categories: [{ categoryId: '', name: '全部' }], categoryId: '', keyword: '', imported: '',
    items: [], page: 1, pageSize: 20, total: 0, selectedIds: [], loading: true,
    loadingMore: false, refreshing: false, hasMore: true, importing: false, importingAll: false
  },
  selection: createDishTemplateSelection(),
  onLoad() { this.load({ reset: true }); },
  onReachBottom() { return this.loadMore(); },
  onPullDownRefresh() { this.load({ reset: true }).finally(() => wx.stopPullDownRefresh()); },
  async load({ reset = false, silent = false } = {}) {
    if (!requireSession({ merchantOnly: true })) return;
    const generation = (this._listGeneration || 0) + 1;
    this._listGeneration = generation;
    const page = reset ? 1 : this.data.page;
    this.setData({ refreshing: true, loadingMore: false, ...(!silent ? { loading: true } : {}) });
    try {
      const runtime = createApiRuntime();
      const [categoryRows, result] = await Promise.all([
        runtime.merchant.getDishTemplateCategories(),
        runtime.merchant.getDishTemplates({
          categoryId: this.data.categoryId, keyword: this.data.keyword,
          imported: this.data.imported, page, pageSize: this.data.pageSize
        })
      ]);
      if (generation !== this._listGeneration) return;
      const items = mapTemplateRows(runtime, result.items, this.selection.snapshot().selectedIds);
      const total = Number(result.total || 0);
      this.setData({
        categories: [{ categoryId: '', name: '全部' }, ...(categoryRows || [])],
        items,
        page: result.page || page,
        total,
        hasMore: items.length < total,
        refreshing: false,
        loading: false
      });
    } catch (error) {
      if (generation !== this._listGeneration) return;
      this.setData({ refreshing: false, loadingMore: false, ...(!silent ? { loading: false } : {}) });
      showApiError(error, '模板菜品加载失败');
    }
  },
  async loadMore() {
    if (this.data.loading || this.data.refreshing || this.data.loadingMore || !this.data.hasMore
        || this.data.items.length >= this.data.total) return;
    const generation = this._listGeneration || 0;
    const nextPage = this.data.page + 1;
    this.setData({ loadingMore: true });
    try {
      const runtime = createApiRuntime();
      const result = await runtime.merchant.getDishTemplates({
        categoryId: this.data.categoryId,
        keyword: this.data.keyword,
        imported: this.data.imported,
        page: nextPage,
        pageSize: this.data.pageSize
      });
      if (generation !== this._listGeneration) return;
      const nextRows = mapTemplateRows(runtime, result.items, this.selection.snapshot().selectedIds);
      const knownIds = new Set(this.data.items.map(item => Number(item.templateId)));
      const appended = nextRows.filter(item => !knownIds.has(Number(item.templateId)));
      const items = [...this.data.items, ...appended];
      const total = Number(result.total || this.data.total || 0);
      this.setData({
        items,
        page: result.page || nextPage,
        total,
        hasMore: nextRows.length > 0 && items.length < total
      });
    } catch (error) {
      if (generation === this._listGeneration) showApiError(error, '更多模板菜品加载失败');
    } finally {
      if (generation === this._listGeneration) this.setData({ loadingMore: false });
    }
  },
  chooseCategory(event) { this.setData({ categoryId: event.currentTarget.dataset.id }); this.load({ reset: true }); },
  inputKeyword(event) { this.setData({ keyword: event.detail.value }); },
  search() { this.load({ reset: true }); },
  changeImported(event) { this.setData({ imported: event.currentTarget.dataset.value }); this.load({ reset: true }); },
  toggleSelection(event) {
    const row = this.data.items.find(item => Number(item.templateId) === Number(event.currentTarget.dataset.id));
    const state = this.selection.toggle(row);
    this.syncSelection(state);
  },
  selectCurrentPage() { this.syncSelection(this.selection.selectPage(this.data.items)); },
  clearSelection() { this.syncSelection(this.selection.clear()); },
  syncSelection(state) {
    const selected = new Set(state.selectedIds);
    this.setData({ selectedIds: state.selectedIds, items: this.data.items.map(item => ({
      ...item, selected: selected.has(Number(item.templateId))
    })) });
    if (state.limitReached) wx.showToast({ title: '单次最多选择100道', icon: 'none' });
  },
  openDetail(event) { wx.navigateTo({ url: `/pages/merchant/dish-template-detail/index?id=${event.currentTarget.dataset.id}` }); },
  isImportBusy() { return this.data.importing || this.data.importingAll; },
  async importSelected() {
    if (!this.data.selectedIds.length || this.isImportBusy()) return;
    this.setData({ importing: true });
    try {
      const result = await createApiRuntime().merchant.importDishTemplates(this.data.selectedIds);
      wx.showModal({ title: '导入完成', content: `成功 ${result.importedCount || 0} 道，跳过 ${result.skippedCount || 0} 道`, showCancel: false });
      this.syncSelection(this.selection.clear());
      await this.load({ reset: true, silent: true });
    } catch (error) { showApiError(error, '模板导入失败'); }
    finally { this.setData({ importing: false }); }
  },
  async importAll() {
    if (this.isImportBusy() || this._confirmingAll) return;
    this._confirmingAll = true;
    let confirmation;
    try {
      confirmation = await wx.showModal({
        title: '导入全部系统菜品',
        content: '将导入全部未导入菜品；已经导入的菜品会自动跳过，不会覆盖你修改过的内容。',
        confirmText: '确认导入'
      });
    } finally {
      this._confirmingAll = false;
    }
    if (!confirmation || !confirmation.confirm || this.isImportBusy()) return;
    this.setData({ importingAll: true });
    try {
      const result = await createApiRuntime().merchant.importAllDishTemplates();
      this.syncSelection(this.selection.clear());
      await wx.showModal({
        title: '导入完成',
        content: `成功 ${result.importedCount || 0} 道，跳过 ${result.skippedCount || 0} 道`,
        showCancel: false
      });
      await this.load({ reset: true, silent: true });
    } catch (error) {
      showApiError(error, '全部菜品导入失败');
    } finally {
      this.setData({ importingAll: false });
    }
  }
});

module.exports = { mapTemplateRows };
