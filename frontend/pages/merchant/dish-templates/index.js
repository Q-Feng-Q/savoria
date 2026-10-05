const { PRODUCT_TYPES } = require('../../../utils/nourishment');
const { createApiRuntime } = require('../../../utils/api-runtime');
const {
  createDishTemplateSelection,
  decorateTemplateRows
} = require('../../../utils/dish-template-selection');
const { toImageUrl } = require('../../../utils/image-url');
const { requireSession, showApiError } = require('../../../utils/page-api');

function mapTemplateRows(runtime, rows, selectedIds) {
  return decorateTemplateRows(rows, selectedIds,
    (value) => toImageUrl(runtime.baseUrl, value));
}

Page({
  data: {
    productType: '', productTypeIndex: 0, productTypes: [{ value: '', label: '全部类型' }, ...PRODUCT_TYPES],
    categories: [{ categoryId: '', name: '全部' }], categoryId: '', keyword: '', imported: '',
    items: [], page: 1, pageSize: 20, total: 0, selectedIds: [], loading: true,
    loadingMore: false, refreshing: false, hasMore: true, importing: false, importingAll: false,
    phase: 'loading', errorMessage: ''
  },
  selection: createDishTemplateSelection(),
  onShow() { this.load({ reset: true }); },
  onReachBottom() { return this.loadMore(); },
  onPullDownRefresh() { this.load({ reset: true }).finally(() => wx.stopPullDownRefresh()); },
  retryLoad() { return this.load({ reset: true }); },
  async load({ reset = false, silent = false } = {}) {
    if (!requireSession({ merchantOnly: true })) return;
    const generation = (this._listGeneration || 0) + 1;
    this._listGeneration = generation;
    const page = reset ? 1 : this.data.page;
    this.setData({ refreshing: true, loadingMore: false, ...(!silent ? { loading: true, phase: 'loading', errorMessage: '' } : {}) });
    try {
      const runtime = createApiRuntime();
      const [categoryRows, result] = await Promise.all([
        runtime.merchant.getDishTemplateCategories(),
        runtime.merchant.getDishTemplates({
          productType: this.data.productType || undefined, categoryId: this.data.categoryId, keyword: this.data.keyword,
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
        loading: false,
        phase: 'ready',
        errorMessage: ''
      });
    } catch (error) {
      if (generation !== this._listGeneration) return;
      this.setData({ refreshing: false, loadingMore: false,
        ...(!silent ? { loading: false, phase: 'error', errorMessage: (error && error.message) || '模板菜品加载失败' } : {}) });
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
        productType: this.data.productType || undefined, categoryId: this.data.categoryId,
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
  changeProductType(event) {
    if (this.isImportBusy()) return;
    const index = Number(event.detail.value), option = this.data.productTypes[index];
    if (!option) return;
    this.syncSelection(this.selection.clear());
    this.setData({ productType: option.value, productTypeIndex: index });
    return this.load({ reset: true, silent: true });
  },
  chooseCategory(event) { this.setData({ categoryId: event.currentTarget.dataset.id }); return this.load({ reset: true, silent: true }); },
  inputKeyword(event) { this.setData({ keyword: event.detail.value }); },
  search() { return this.load({ reset: true, silent: true }); },
  changeImported(event) { this.setData({ imported: event.currentTarget.dataset.value }); return this.load({ reset: true, silent: true }); },
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
  openChangeRequests() { wx.navigateTo({ url: '/pages/merchant/dish-template-changes/index' }); },
  isImportBusy() { return this.data.importing || this.data.importingAll
    || this._confirmingSelected || this._confirmingAll; },
  async importSelected() {
    if (!this.data.selectedIds.length || this.isImportBusy()) return;
    const selected = new Set(this.data.selectedIds.map(Number));
    const hasUnseen = this.data.items.filter((item) => selected.has(Number(item.templateId))).length
      < selected.size;
    const incomplete = this.data.items.filter((item) => selected.has(Number(item.templateId))
      && item.completionHints && item.completionHints.length);
    if (incomplete.length || hasUnseen) {
      this._confirmingSelected = true;
      let confirmation;
      try {
        const hints = [...new Set(incomplete.flatMap((item) => item.completionHints))];
        const warning = hints.length ? `待补：${hints.join('、')}。` : '所选模板可能含有待完善资料。';
        confirmation = await wx.showModal({ title: '模板资料待完善',
          content: `${warning}导入后请补充；缺少价格的菜品会自动下架。仍要导入吗？`,
          confirmText: '继续导入' });
      } finally { this._confirmingSelected = false; }
      if (!confirmation || !confirmation.confirm || this.isImportBusy()) return;
    }
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
        content: '将导入全部未导入菜品；待完善的模板也会导入，请之后补齐资料。缺少价格的菜品会自动下架；已导入菜品不会覆盖。',
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
