const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');

const STATUS = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '未通过', WITHDRAWN: '已撤回' };
const TYPE = { CREATE: '新增菜品', UPDATE: '修改菜品', STATUS: '状态调整' };

function decorate(rows) {
  return (rows || []).map((item) => ({
    ...item,
    statusLabel: STATUS[String(item.status || '').toUpperCase()] || item.status || '未知状态',
    typeLabel: TYPE[String(item.submissionType || '').toUpperCase()] || item.submissionType || '菜品变更',
    submittedText: String(item.submittedAt || '').replace('T', ' ').slice(0, 16),
    canWithdraw: String(item.status || '').toUpperCase() === 'PENDING'
  }));
}

Page({
  data: { phase: 'loading', rows: [], busyReviewMap: {}, errorMessage: '' },
  onShow() { this.load(); },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()); },
  async load() {
    if (!requireSession({ merchantOnly: true })) return;
    this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const rows = decorate(await createApiRuntime().merchant.getDishReviews());
      this.setData({ rows, phase: rows.length ? 'ready' : 'empty' });
    } catch (error) {
      this.setData({ phase: 'error', errorMessage: error.message || '审核记录加载失败' });
    }
  },
  async withdrawDishReview(event) {
    const id = event.currentTarget.dataset.id;
    if (this.data.busyReviewMap[id]) return;
    this.setData({ [`busyReviewMap.${id}`]: true });
    try {
      await createApiRuntime().merchant.withdrawDishReview(id);
      wx.showToast({ title: '已撤回审核', icon: 'success' });
      await this.load();
    } catch (error) {
      showApiError(error, '撤回审核失败');
    } finally {
      this.setData({ [`busyReviewMap.${id}`]: false });
    }
  },
  retryLoad() { this.load(); }
});
