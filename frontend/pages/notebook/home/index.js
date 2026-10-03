const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');
const calendar = require('../../../utils/notebook-calendar');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: {
    phase: 'loading', view: 'calendar', month: '', selectedDate: '', days: [],
    events: [], displayEvents: [], categories: ['全部类别'], categoryIndex: 0,
    selectedCategory: '', activeEventId: null, records: [], visibleRecords: [], summary: {}, maxQueryMonths: 36,
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
      const categories = ['全部类别', ...new Set((events || []).map((event) => event.category).filter(Boolean))];
      const selectedCategory = categories.includes(this.data.selectedCategory)
        ? this.data.selectedCategory : '';
      const displayEvents = (events || []).filter((event) => !selectedCategory
        || event.category === selectedCategory);
      const activeEventId = displayEvents.some((event) => event.id === this.data.activeEventId)
        ? this.data.activeEventId : (displayEvents[0] || {}).id || null;
      this.setData({ maxQueryMonths: config.maxQueryMonths || 36, events: events || [],
        displayEvents, categories, selectedCategory,
        categoryIndex: Math.max(0, categories.indexOf(selectedCategory)), activeEventId });
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
    const summaryPromise = notebook.getCalendar(id, query);
    const records = [];
    let page = 0;
    let hasMore = true;
    while (hasMore) {
      const batch = await notebook.listRecords(id, { ...query, page, size: 100 });
      if (!this.identityLoad.isCurrent(loadToken)) return;
      records.push(...((batch && batch.items) || []));
      hasMore = Boolean(batch && batch.hasMore);
      page += 1;
    }
    const summary = await summaryPromise;
    if (!this.identityLoad.isCurrent(loadToken)) return;
    const counts = {};
    (summary || []).forEach((item) => { counts[item.date] = item.recordCount; });
    this.setData({ summary: counts, records }, () => this.syncVisible());
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
    const month = calendar.shiftMonth(this.data.month, offset);
    this.setData({ month, selectedDate: `${month}-01`, days: calendar.monthDays(month) });
    try { await this.loadMonth(); } catch (error) { showApiError(error, '日历加载失败'); }
  },
  selectDate(event) { this.setData({ selectedDate: event.currentTarget.dataset.date }, () => this.syncVisible()); },
  async switchView(event) {
    const view = event.currentTarget.dataset.view || 'calendar';
    if (view === 'today') {
      const today = new Date();
      this.setData({ view, month: calendar.monthKey(new Date()), selectedDate: calendar.dateKey(today),
        days: calendar.monthDays(calendar.monthKey(today)) });
      try { await this.loadMonth(); } catch (error) { showApiError(error, '今天的记录加载失败'); }
    } else this.setData({ view }, () => this.syncVisible());
  },
  async selectEvent(event) {
    this.setData({ activeEventId: Number(event.currentTarget.dataset.id) });
    try { await this.loadMonth(); } catch (error) { showApiError(error, '记录加载失败'); }
  },
  async filterCategory(event) {
    const categoryIndex = Number(event.detail.value);
    const selectedCategory = categoryIndex ? this.data.categories[categoryIndex] : '';
    const displayEvents = this.data.events.filter((item) => !selectedCategory
      || item.category === selectedCategory);
    this.setData({ categoryIndex, selectedCategory, displayEvents,
      activeEventId: (displayEvents[0] || {}).id || null });
    try { await this.loadMonth(); } catch (error) { showApiError(error, '类别加载失败'); }
  },
  openEvents() { wx.navigateTo({ url: '/pages/notebook/detail/events/index' }); },
  openContacts() { wx.navigateTo({ url: '/pages/notebook/detail/contacts/index' }); },
  openShared() { wx.navigateTo({ url: '/pages/notebook/detail/shared/index' }); },
  addRecord() {
    if (!this.data.activeEventId) return this.openEvents();
    wx.navigateTo({ url: `/pages/notebook/detail/record-edit/index?eventId=${this.data.activeEventId}&date=${this.data.selectedDate}` });
  },
  openRecord(event) { wx.navigateTo({ url: `/pages/notebook/detail/record-detail/index?id=${event.currentTarget.dataset.id}` }); }
});
