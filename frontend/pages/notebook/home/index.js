const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');
const calendar = require('../../../utils/notebook-calendar');
const { MAX_SELECTED_EVENTS, reconcileSelection, assignSelectionTones,
  visibleSelectedEvents, mergeEventMonths, calendarHeartSources, groupTimelineRecords } =
  require('../../../utils/notebook-overview');

Page({
  identityLoad: createIdentityLoadGuard(),
  selectionUserId: null,
  hasManualSelection: false,
  data: {
    phase: 'loading', view: 'calendar', month: '', selectedDate: '', days: [],
    events: [], displayEvents: [], categories: ['全部类别'], categoryIndex: 0,
    selectedCategory: '', selectedEventIds: [], selectedEventMap: {},
    selectedEventToneMap: {}, visibleSelectedEventIds: [],
    records: [], visibleRecords: [], timelineGroups: [], summary: {}, dayTones: {},
    calendarHeartSrc: {},
    maxQueryMonths: 36,
    showRecordEventPicker: false, recordEventOptions: [],
    errorMessage: '', isPrivate: true
  },
  onShow() { this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    if (this.selectionUserId !== session.userId) {
      this.selectionUserId = session.userId;
      this.hasManualSelection = false;
      this.setData({ selectedEventIds: [], selectedEventMap: {},
        selectedEventToneMap: {}, selectedCategory: '' });
    }
    const now = new Date();
    const month = this.data.month || calendar.monthKey(now);
    const selectedDate = this.data.selectedDate || calendar.dateKey(now);
    this.setData({ phase: 'loading', errorMessage: '', month, selectedDate,
      days: calendar.monthDays(month), records: [], visibleRecords: [], timelineGroups: [],
      summary: {}, dayTones: {},
      calendarHeartSrc: calendarHeartSources({}, {}, selectedDate),
      showRecordEventPicker: false });
    try {
      const notebook = createApiRuntime().notebook;
      const [config, events] = await Promise.all([notebook.getConfig(), notebook.listEvents()]);
      if (!this.identityLoad.isCurrent(loadToken)) return;
      const categories = ['全部类别', ...new Set((events || []).map((event) => event.category).filter(Boolean))];
      const selectedCategory = categories.includes(this.data.selectedCategory)
        ? this.data.selectedCategory : '';
      const displayEvents = (events || []).filter((event) => !selectedCategory
        || event.category === selectedCategory);
      const selectedEventIds = reconcileSelection(events, this.data.selectedEventIds,
        this.hasManualSelection);
      const visibleSelectedEventIds = visibleSelectedEvents(displayEvents,
        selectedEventIds).map((event) => event.id);
      this.setData({ maxQueryMonths: config.maxQueryMonths || 36, events: events || [],
        displayEvents, categories, selectedCategory,
        categoryIndex: Math.max(0, categories.indexOf(selectedCategory)),
        selectedEventIds, selectedEventMap: Object.fromEntries(selectedEventIds.map((id) => [id, true])),
        selectedEventToneMap: assignSelectionTones(selectedEventIds, this.data.selectedEventToneMap),
        visibleSelectedEventIds });
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
    const selected = visibleSelectedEvents(this.data.displayEvents, this.data.selectedEventIds);
    if (!selected.length) {
      this.setData({ visibleSelectedEventIds: [], summary: {}, dayTones: {},
        calendarHeartSrc: calendarHeartSources({}, {}, this.data.selectedDate),
        records: [], visibleRecords: [], timelineGroups: [] });
      return;
    }
    const notebook = createApiRuntime().notebook;
    const bounds = calendar.monthBounds(this.data.month);
    const query = { ...bounds, timeZone: calendar.timeZone() };
    const results = [];
    for (let index = 0; index < selected.length; index += 3) {
      const batch = await Promise.all(selected.slice(index, index + 3).map((event) =>
        this.loadEventMonth(notebook, event, query, loadToken)));
      if (!this.identityLoad.isCurrent(loadToken)) return;
      results.push(...batch);
    }
    const combined = mergeEventMonths(results, this.data.selectedEventToneMap);
    this.setData({ visibleSelectedEventIds: selected.map((event) => event.id),
      summary: combined.summary, dayTones: combined.dayTones,
      calendarHeartSrc: calendarHeartSources(combined.summary, combined.dayTones,
        this.data.selectedDate), records: combined.records }, () => this.syncVisible());
  },
  async loadEventMonth(notebook, event, query, loadToken) {
    const summaryPromise = notebook.getCalendar(event.id, query);
    const recordsPromise = (async () => {
      const records = [];
      let page = 0;
      let hasMore = true;
      while (hasMore) {
        const batch = await notebook.listRecords(event.id, { ...query, page, size: 100 });
        if (!this.identityLoad.isCurrent(loadToken)) return records;
        records.push(...((batch && batch.items) || []));
        hasMore = Boolean(batch && batch.hasMore);
        page += 1;
      }
      return records;
    })();
    const [summary, records] = await Promise.all([summaryPromise, recordsPromise]);
    return { event, summary, records };
  },
  syncVisible() {
    const target = this.data.view === 'today' ? calendar.dateKey(new Date()) : this.data.selectedDate;
    const source = this.data.records || [];
    const visible = this.data.view === 'timeline' ? source.slice().reverse() : source.filter((record) => {
      const from = calendar.dateKey(new Date(record.occurredFrom));
      const to = calendar.dateKey(new Date(record.occurredTo));
      return from <= target && target <= to;
    });
    const visibleRecords = visible.map((record) => ({ ...record,
      displayRange: calendar.formatRecordRange(record.occurredFrom, record.occurredTo) }));
    this.setData({ visibleRecords, timelineGroups: this.data.view === 'timeline'
      ? groupTimelineRecords(visibleRecords) : [] });
  },
  async moveMonth(event) {
    const offset = Number(event.currentTarget.dataset.offset || 0);
    const month = calendar.shiftMonth(this.data.month, offset);
    const selectedDate = `${month}-01`;
    this.setData({ month, selectedDate, days: calendar.monthDays(month), summary: {},
      dayTones: {}, calendarHeartSrc: calendarHeartSources({}, {}, selectedDate),
      records: [], visibleRecords: [], timelineGroups: [] });
    try { await this.loadMonth(); } catch (error) { showApiError(error, '日历加载失败'); }
  },
  selectDate(event) {
    const selectedDate = event.currentTarget.dataset.date;
    this.setData({ selectedDate,
      calendarHeartSrc: calendarHeartSources(this.data.summary, this.data.dayTones,
        selectedDate) }, () => this.syncVisible());
  },
  async switchView(event) {
    const view = event.currentTarget.dataset.view || 'calendar';
    if (view === 'today') {
      const today = new Date();
      const month = calendar.monthKey(new Date());
      const selectedDate = calendar.dateKey(today);
      this.setData({ view, month, selectedDate, days: calendar.monthDays(month),
        calendarHeartSrc: calendarHeartSources(this.data.summary, this.data.dayTones,
          selectedDate) });
      try { await this.loadMonth(); } catch (error) { showApiError(error, '今天的记录加载失败'); }
    } else this.setData({ view }, () => this.syncVisible());
  },
  async selectEvent(event) {
    const id = Number(event.currentTarget.dataset.id);
    if (!this.data.displayEvents.some((item) => item.id === id)) return;
    if (!this.data.selectedEventIds.includes(id)
      && this.data.selectedEventIds.length >= MAX_SELECTED_EVENTS) {
      wx.showToast({ title: `最多同时选择 ${MAX_SELECTED_EVENTS} 个事件`, icon: 'none' });
      return;
    }
    this.hasManualSelection = true;
    const selectedEventIds = this.data.selectedEventIds.includes(id)
      ? this.data.selectedEventIds.filter((item) => item !== id)
      : [...this.data.selectedEventIds, id];
    this.setData({ selectedEventIds,
      selectedEventMap: Object.fromEntries(selectedEventIds.map((item) => [item, true])),
      selectedEventToneMap: assignSelectionTones(selectedEventIds, this.data.selectedEventToneMap),
      visibleSelectedEventIds: visibleSelectedEvents(this.data.displayEvents,
        selectedEventIds).map((item) => item.id) });
    try { await this.loadMonth(); } catch (error) { showApiError(error, '记录加载失败'); }
  },
  async filterCategory(event) {
    const categoryIndex = Number(event.detail.value);
    const selectedCategory = categoryIndex ? this.data.categories[categoryIndex] : '';
    const displayEvents = this.data.events.filter((item) => !selectedCategory
      || item.category === selectedCategory);
    this.setData({ categoryIndex, selectedCategory, displayEvents,
      visibleSelectedEventIds: visibleSelectedEvents(displayEvents,
        this.data.selectedEventIds).map((item) => item.id) });
    try { await this.loadMonth(); } catch (error) { showApiError(error, '类别加载失败'); }
  },
  openEvents() { wx.navigateTo({ url: '/pages/notebook/detail/events/index' }); },
  openHistory() {
    const selected = visibleSelectedEvents(this.data.displayEvents, this.data.selectedEventIds);
    const eventId = selected.length === 1 ? selected[0].id : null;
    wx.navigateTo({ url: `/pages/notebook/detail/history/index${eventId ? `?eventId=${eventId}` : ''}` });
  },
  openContacts() { wx.navigateTo({ url: '/pages/notebook/detail/contacts/index' }); },
  openShared() { wx.navigateTo({ url: '/pages/notebook/detail/shared/index' }); },
  addRecord() {
    const selected = visibleSelectedEvents(this.data.displayEvents, this.data.selectedEventIds);
    if (selected.length === 1) return this.navigateToRecordEditor(selected[0].id);
    const recordEventOptions = selected.length ? selected : this.data.displayEvents;
    if (!recordEventOptions.length) return this.openEvents();
    this.setData({ showRecordEventPicker: true, recordEventOptions });
  },
  closeRecordEventPicker() { this.setData({ showRecordEventPicker: false }); },
  chooseRecordEvent(event) {
    const id = Number(event.currentTarget.dataset.id);
    if (!this.data.recordEventOptions.some((item) => item.id === id)) return;
    this.setData({ showRecordEventPicker: false });
    this.navigateToRecordEditor(id);
  },
  navigateToRecordEditor(eventId) {
    wx.navigateTo({ url: `/pages/notebook/detail/record-edit/index?eventId=${eventId}&date=${this.data.selectedDate}` });
  },
  openRecord(event) { wx.navigateTo({ url: `/pages/notebook/detail/record-detail/index?id=${event.currentTarget.dataset.id}` }); }
});
