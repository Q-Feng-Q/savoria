const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { decorateChangeRequest } = require('../../../utils/dish-template-change');

const FILTERS = [
  { value: '', label: '全部' }, { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' }, { value: 'REJECTED', label: '已驳回' },
  { value: 'WITHDRAWN', label: '已撤回' }
];

Page({
  data: { phase: 'loading', errorMessage: '', filters: FILTERS, status: '', keyword: '', rows: [], page: 1, pageSize: 20, total: 0, hasMore: false, loadingMore: false },
  onShow() { this.load(true); },
  onPullDownRefresh() { this.load(true).finally(() => wx.stopPullDownRefresh()); },
  onReachBottom() { this.loadMore(); },
  inputKeyword(event) { this.setData({ keyword: event.detail.value }); },
  search() { this.load(true); },
  chooseStatus(event) { this.setData({ status: event.currentTarget.dataset.value }); this.load(true); },
  async load(reset = false) {
    if (!requireSession({ merchantOnly: true })) return;
    const page = reset ? 1 : this.data.page;
    this.setData({ ...(reset ? { phase: 'loading', errorMessage: '' } : {}) });
    try {
      const result = await createApiRuntime().merchant.getDishTemplateChanges({ status: this.data.status, keyword: this.data.keyword, page, pageSize: this.data.pageSize });
      const rows = (result.items || []).map(decorateChangeRequest);
      const total = Number(result.total || 0);
      this.setData({ rows, page: result.page || page, total, hasMore: rows.length < total, phase: rows.length ? 'ready' : 'empty' });
    } catch (error) {
      this.setData({ phase: 'error', errorMessage: error.message || '申请记录加载失败' });
    }
  },
  async loadMore() {
    if (!this.data.hasMore || this.data.loadingMore) return;
    this.setData({ loadingMore: true });
    try {
      const page = this.data.page + 1;
      const result = await createApiRuntime().merchant.getDishTemplateChanges({ status: this.data.status, keyword: this.data.keyword, page, pageSize: this.data.pageSize });
      const next = (result.items || []).map(decorateChangeRequest);
      const rows = [...this.data.rows, ...next];
      this.setData({ rows, page, total: Number(result.total || this.data.total), hasMore: rows.length < Number(result.total || 0) });
    } catch (error) { showApiError(error, '更多申请加载失败'); }
    finally { this.setData({ loadingMore: false }); }
  },
  openDetail(event) { wx.navigateTo({ url: `/pages/merchant/dish-template-change-detail/index?id=${event.currentTarget.dataset.id}` }); },
  retryLoad() { return this.load(true); }
});
