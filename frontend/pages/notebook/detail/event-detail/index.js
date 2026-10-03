const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');
const calendar = require('../../../../utils/notebook-calendar');

function validRange(from, to, maxQueryMonths) {
  const months = (Number(to.slice(0, 4)) - Number(from.slice(0, 4))) * 12
    + Number(to.slice(5, 7)) - Number(from.slice(5, 7)) + 1;
  return from <= to && months >= 1 && months <= maxQueryMonths;
}

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'loading', id: null, event: null, versions: [], errorMessage: '',
    from: '', to: '', maxQueryMonths: 36, records: [], page: 0, hasMore: false,
    recordsBusy: false, recordsError: '' },
  onLoad(options) {
    const today = calendar.dateKey(new Date());
    this.setData({ id: Number(options && options.id),
      from: calendar.monthBounds(today.slice(0, 7)).from, to: today });
  },
  onShow() { return this.load(); },
  async load() {
    const session = requireSession();
    if (!session || !this.data.id) return;
    const ticket = this.identityLoad.begin(session);
    this.setData({ phase: 'loading' });
    try {
      const notebook = createApiRuntime().notebook;
      const [event, versions, config] = await Promise.all([
        notebook.getEvent(this.data.id), notebook.listTemplates(this.data.id), notebook.getConfig()
      ]);
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ event, versions: versions || [], maxQueryMonths: config.maxQueryMonths || 36,
        phase: 'ready' });
      await this.loadRecords(0, ticket);
    } catch (error) {
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '事件加载失败' });
      showApiError(error, '事件加载失败');
    }
  },
  changeRange(event) {
    this.setData({ [event.currentTarget.dataset.field]: event.detail.value });
    this.loadRecords();
  },
  async loadRecords(page = 0, ticket = this.identityLoad.begin(requireSession())) {
    if (!this.data.id || !this.identityLoad.isCurrent(ticket)) return;
    const { from, to, maxQueryMonths } = this.data;
    if (!validRange(from, to, maxQueryMonths)) {
      this.setData({ records: [], hasMore: false,
        recordsError: `单次最多查看 ${maxQueryMonths} 个日历月，请重新选择日期` });
      return;
    }
    this.setData({ recordsBusy: true, recordsError: '' });
    try {
      const result = await createApiRuntime().notebook.listRecords(this.data.id,
        { from, to, timeZone: calendar.timeZone(), page, size: 50 });
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ records: page ? [...this.data.records, ...(result.items || [])] : result.items || [],
        page, hasMore: Boolean(result.hasMore) });
    } catch (error) {
      if (this.identityLoad.isCurrent(ticket)) this.setData({ recordsError: error.message || '历史记录加载失败' });
    } finally { if (this.identityLoad.isCurrent(ticket)) this.setData({ recordsBusy: false }); }
  },
  loadMore() {
    if (!this.data.hasMore || this.data.recordsBusy) return;
    return this.loadRecords(this.data.page + 1);
  },
  openRecord(event) { wx.navigateTo({ url: `/pages/notebook/detail/record-detail/index?id=${event.currentTarget.dataset.id}` }); },
  openExport() {
    if (!validRange(this.data.from, this.data.to, this.data.maxQueryMonths)) return;
    wx.navigateTo({ url: `/pages/notebook/detail/export/index?eventId=${this.data.id}&from=${this.data.from}&to=${this.data.to}` });
  },
  edit() { wx.navigateTo({ url: `/pages/notebook/detail/event-edit/index?id=${this.data.id}` }); },
  addRecord() { wx.navigateTo({ url: `/pages/notebook/detail/record-edit/index?eventId=${this.data.id}` }); },
  share() { wx.navigateTo({ url: `/pages/notebook/detail/sharing/index?eventId=${this.data.id}` }); },
  async toggleFocus() {
    if (!this.data.event) return;
    try {
      await createApiRuntime().notebook.updateEvent(this.data.id,
        { starred: !this.data.event.starred });
      await this.load();
    } catch (error) { showApiError(error, '更新关注失败'); }
  }
});
