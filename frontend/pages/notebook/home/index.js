const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');
const calendar = require('../../../utils/notebook-calendar');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: {
    phase: 'loading', view: 'calendar', month: '', selectedDate: '', days: [],
    events: [], activeEventId: null, records: [], visibleRecords: [], summary: {}, maxQueryMonths: 36,
    errorMessage: '', isPrivate: true
  },
  onShow() { this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    const now = new Date();
    const month = this.data.month || calendar.monthKey(now);
    const selectedDate = this.data.selectedDate || calendar.dateKey(now);
    this.setData({ phase: 'loading', errorMessage: '', month, selectedDate, days: calendar.monthDays(month) });
    try {
      const notebook = createApiRuntime().notebook;
      const [config, events] = await Promise.all([notebook.getConfig(), notebook.listEvents()]);
      if (!this.identityLoad.isCurrent(loadToken)) return;
      const activeEventId = (events || []).some((event) => event.id === this.data.activeEventId)
        ? this.data.activeEventId : ((events || [])[0] || {}).id || null;
      this.setData({ maxQueryMonths: config.maxQueryMonths || 36, events: events || [], activeEventId });
      await this.loadMonth(loadToken);
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ phase: 'ready' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '记事加载失败' });
      showApiError(error, '记事加载失败');
    }
  },
  async loadMonth(loadToken = this.identityLoad.begin(requireSession())) {
    const id = this.data.activeEventId;
    if (!id) { this.setData({ summary: {}, records: [], visibleRecords: [] }); return; }
    const notebook = createApiRuntime().notebook;
    const bounds = calendar.monthBounds(this.data.month);
    const query = { ...bounds, timeZone: calendar.timeZone() };
    const [summary, page] = await Promise.all([
      notebook.getCalendar(id, query), notebook.listRecords(id, { ...query, page: 0, size: 100 })
    ]);
    if (!this.identityLoad.isCurrent(loadToken)) return;
    const counts = {};
    (summary || []).forEach((item) => { counts[item.date] = item.recordCount; });
    this.setData({ summary: counts, records: (page && page.items) || [] }, () => this.syncVisible());
  },
  syncVisible() {
    const target = this.data.view === 'today' ? calendar.dateKey(new Date()) : this.data.selectedDate;
    const source = this.data.records || [];
    const visible = this.data.view === 'timeline' ? source.slice().reverse() : source.filter((record) => {
      const from = calendar.dateKey(new Date(record.occurredFrom));
      const to = calendar.dateKey(new Date(record.occurredTo));
      return from <= target && target <= to;
    });
    this.setData({ visibleRecords: visible });
  },
  async moveMonth(event) {
    const offset = Number(event.currentTarget.dataset.offset || 0);
    this.setData({ month: calendar.shiftMonth(this.data.month, offset) });
    try { await this.loadMonth(); } catch (error) { showApiError(error, '日历加载失败'); }
  },
  selectDate(event) { this.setData({ selectedDate: event.currentTarget.dataset.date }, () => this.syncVisible()); },
  switchView(event) { this.setData({ view: event.currentTarget.dataset.view || 'calendar' }, () => this.syncVisible()); },
  async selectEvent(event) {
    this.setData({ activeEventId: Number(event.currentTarget.dataset.id) });
    try { await this.loadMonth(); } catch (error) { showApiError(error, '记录加载失败'); }
  },
  openEvents() { wx.navigateTo({ url: '/pages/notebook/events/index' }); },
  addRecord() {
    if (!this.data.activeEventId) return this.openEvents();
    wx.navigateTo({ url: `/pages/notebook/record-edit/index?eventId=${this.data.activeEventId}&date=${this.data.selectedDate}` });
  },
  openRecord(event) { wx.navigateTo({ url: `/pages/notebook/record-detail/index?id=${event.currentTarget.dataset.id}` }); }
});
