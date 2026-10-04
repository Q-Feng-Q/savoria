const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');
const calendar = require('../../../../utils/notebook-calendar');
const { validateRange, projectMonths } = require('../../../../utils/notebook-history');

const MONTH_BATCH = 4;

Page({
  identityLoad: createIdentityLoadGuard(),
  allMonths: [],
  rangeEdited: false,
  data: {
    phase: 'loading', events: [], eventId: null, eventIndex: 0,
    from: '', to: '', maxQueryMonths: 36, queryError: '', queryBusy: false,
    months: [], hasMoreMonths: false, selectedDate: '', records: [],
    recordPage: 0, hasMoreRecords: false, recordsBusy: false, recordsError: '',
    errorMessage: ''
  },
  onLoad(options) {
    const today = calendar.dateKey(new Date());
    const previousMonth = calendar.shiftMonth(today.slice(0, 7), -1);
    this.setData({ eventId: Number(options && options.eventId) || null,
      from: calendar.monthBounds(previousMonth).from, to: today });
    this.rangeEdited = false;
  },
  onShow() { return this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const ticket = this.identityLoad.begin(session);
    this.allMonths = [];
    this.setData({ phase: 'loading', errorMessage: '', queryError: '', queryBusy: false,
      months: [], hasMoreMonths: false, selectedDate: '', records: [],
      recordPage: 0, hasMoreRecords: false, recordsBusy: false, recordsError: '' });
    try {
      const notebook = createApiRuntime().notebook;
      const [config, events] = await Promise.all([notebook.getConfig(), notebook.listEvents(true)]);
      if (!this.identityLoad.isCurrent(ticket)) return;
      const items = events || [];
      const eventIndex = Math.max(0, items.findIndex((item) => item.id === this.data.eventId));
      const maxQueryMonths = config.maxQueryMonths || 36;
      const from = !this.rangeEdited && !validateRange(this.data.from, this.data.to,
        maxQueryMonths).ok ? calendar.monthBounds(this.data.to.slice(0, 7)).from : this.data.from;
      this.setData({ phase: 'ready', events: items, eventIndex,
        eventId: items[eventIndex] ? items[eventIndex].id : null,
        maxQueryMonths, from });
      if (items.length) await this.runSearch(ticket);
    } catch (error) {
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '历史查询加载失败' });
      showApiError(error, '历史查询加载失败');
    }
  },
  changeEvent(event) {
    this.identityLoad.begin(requireSession());
    const eventIndex = Number(event.detail.value);
    const item = this.data.events[eventIndex];
    this.allMonths = [];
    this.setData({ eventIndex, eventId: item ? item.id : null, queryError: '',
      queryBusy: false, months: [], hasMoreMonths: false, selectedDate: '', records: [] });
  },
  changeRange(event) {
    const field = event.currentTarget.dataset.field;
    if (field !== 'from' && field !== 'to') return;
    this.rangeEdited = true;
    this.identityLoad.begin(requireSession());
    this.allMonths = [];
    this.setData({ [field]: event.detail.value, queryError: '', queryBusy: false,
      months: [], hasMoreMonths: false, selectedDate: '', records: [] });
  },
  search() { return this.runSearch(this.identityLoad.begin(requireSession())); },
  async runSearch(ticket) {
    if (!this.identityLoad.isCurrent(ticket) || !this.data.eventId || this.data.phase !== 'ready') return;
    const { from, to, eventId, maxQueryMonths } = this.data;
    const check = validateRange(from, to, maxQueryMonths);
    if (!check.ok) {
      this.setData({ queryError: check.message });
      return;
    }
    this.setData({ queryBusy: true, queryError: '', records: [], recordsError: '' });
    try {
      const summary = await createApiRuntime().notebook.getCalendar(eventId,
        { from, to, timeZone: calendar.timeZone() });
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.allMonths = projectMonths(from, to, summary);
      this.setData({ months: this.allMonths.slice(0, MONTH_BATCH),
        hasMoreMonths: this.allMonths.length > MONTH_BATCH, selectedDate: '',
        recordPage: 0, hasMoreRecords: false, queryBusy: false });
    } catch (error) {
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ queryError: error.message || '历史记录查询失败', queryBusy: false });
    }
  },
  loadMoreMonths() {
    if (!this.data.hasMoreMonths) return;
    const nextCount = this.data.months.length + MONTH_BATCH;
    this.setData({ months: this.allMonths.slice(0, nextCount),
      hasMoreMonths: this.allMonths.length > nextCount });
  },
  async selectDay(event) {
    const date = event.currentTarget.dataset.date;
    if (!date || date < this.data.from || date > this.data.to || this.data.queryBusy) return;
    const ticket = this.identityLoad.begin(requireSession());
    this.setData({ selectedDate: date, records: [], recordPage: 0,
      hasMoreRecords: false, recordsError: '' });
    await this.loadDayRecords(0, ticket);
  },
  closeDay() {
    this.identityLoad.begin(requireSession());
    this.setData({ selectedDate: '', records: [], recordsBusy: false,
      hasMoreRecords: false, recordsError: '' });
  },
  noop() {},
  async loadDayRecords(page, ticket) {
    if (!this.identityLoad.isCurrent(ticket)) return;
    const eventId = this.data.eventId;
    const date = this.data.selectedDate;
    this.setData({ recordsBusy: true, recordsError: '' });
    try {
      const result = await createApiRuntime().notebook.listRecords(eventId,
        { from: date, to: date, timeZone: calendar.timeZone(), page, size: 50 });
      if (!this.identityLoad.isCurrent(ticket) || this.data.selectedDate !== date) return;
      const items = (result.items || []).map((record) => ({ ...record,
        displayRange: calendar.formatRecordRange(record.occurredFrom, record.occurredTo) }));
      this.setData({ records: page ? [...this.data.records, ...items] : items,
        recordPage: page, hasMoreRecords: Boolean(result.hasMore) });
    } catch (error) {
      if (this.identityLoad.isCurrent(ticket)) {
        this.setData({ recordsError: error.message || '当天记录加载失败' });
      }
    } finally {
      if (this.identityLoad.isCurrent(ticket)) this.setData({ recordsBusy: false });
    }
  },
  loadMoreRecords() {
    if (!this.data.hasMoreRecords || this.data.recordsBusy) return;
    const ticket = this.identityLoad.begin(requireSession());
    return this.loadDayRecords(this.data.recordPage + 1, ticket);
  },
  openRecord(event) {
    wx.navigateTo({ url: `/pages/notebook/detail/record-detail/index?id=${event.currentTarget.dataset.id}` });
  }
});
