const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');
const { grantCapabilities, boundedGrantRange, validSharedRange } = require('../../../../utils/notebook-permissions');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'loading', grants: [], selectedId: null, selected: null,
    records: [], page: 0, hasMore: false, loadingMore: false, errorMessage: '',
    maxQueryMonths: 36, rangeFrom: '', rangeTo: '', rangeError: '' },
  onShow() { this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const ticket = this.identityLoad.begin(session);
    this.setData({ phase: 'loading', errorMessage: '', records: [] });
    try {
      const notebook = createApiRuntime().notebook;
      const [grants, config] = await Promise.all([notebook.listShared(), notebook.getConfig()]);
      if (!this.identityLoad.isCurrent(ticket)) return;
      const active = (grants || []).map((grant) => ({ ...grant,
        capabilities: grantCapabilities(grant) })).filter((grant) => grant.capabilities.read);
      const selected = active.find((grant) => grant.id === this.data.selectedId) || active[0] || null;
      const maxQueryMonths = config.maxQueryMonths || 36;
      const range = selected && validSharedRange(selected, this.data.rangeFrom,
        this.data.rangeTo, maxQueryMonths)
        ? { from: this.data.rangeFrom, to: this.data.rangeTo }
        : selected ? boundedGrantRange(selected, maxQueryMonths) : { from: '', to: '' };
      this.setData({ grants: active, selectedId: selected ? selected.id : null,
        selected, maxQueryMonths, rangeFrom: range.from, rangeTo: range.to,
        rangeError: '', phase: 'ready' });
      if (selected) await this.loadRecords(selected, 0, ticket);
    } catch (error) {
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '共享记事加载失败' });
      showApiError(error, '共享记事加载失败');
    }
  },
  async loadRecords(grant, page, ticket) {
    if (!validSharedRange(grant, this.data.rangeFrom, this.data.rangeTo,
        this.data.maxQueryMonths)) {
      this.setData({ records: [], hasMore: false,
        rangeError: `请选择授权范围内、不超过 ${this.data.maxQueryMonths} 个月的日期` });
      return;
    }
    const result = await createApiRuntime().notebook.listRecords(grant.eventId, {
      from: this.data.rangeFrom, to: this.data.rangeTo,
      timeZone: grant.dataTimeZone, page, size: 50
    });
    if (!this.identityLoad.isCurrent(ticket) || this.data.selectedId !== grant.id) return;
    this.setData({ records: page ? [...this.data.records, ...(result.items || [])] : result.items || [],
      page, hasMore: Boolean(result.hasMore) });
  },
  async selectGrant(event) {
    const selected = this.data.grants.find((grant) => grant.id === Number(event.currentTarget.dataset.id));
    if (!selected) return;
    const ticket = this.identityLoad.begin(requireSession());
    const range = boundedGrantRange(selected, this.data.maxQueryMonths);
    this.setData({ selectedId: selected.id, selected, records: [], page: 0, hasMore: false,
      rangeFrom: range.from, rangeTo: range.to, rangeError: '' });
    try { await this.loadRecords(selected, 0, ticket); }
    catch (error) { showApiError(error, '共享记录加载失败'); }
  },
  async changeRange(event) {
    const key = event.currentTarget.dataset.field;
    this.setData({ [key]: event.detail.value, rangeError: '', records: [], hasMore: false });
    const ticket = this.identityLoad.begin(requireSession());
    try { if (this.data.selected) await this.loadRecords(this.data.selected, 0, ticket); }
    catch (error) { showApiError(error, '共享记录加载失败'); }
  },
  async loadMore() {
    if (this.data.loadingMore || !this.data.hasMore || !this.data.selected) return;
    const ticket = this.identityLoad.begin(requireSession());
    this.setData({ loadingMore: true });
    try { await this.loadRecords(this.data.selected, this.data.page + 1, ticket); }
    catch (error) { showApiError(error, '加载更多失败'); }
    finally { this.setData({ loadingMore: false }); }
  },
  openRecord(event) { wx.navigateTo({ url: `/pages/notebook/detail/record-detail/index?id=${event.currentTarget.dataset.id}` }); },
  createRecord() {
    const grant = this.data.selected;
    if (!grant || !grant.capabilities.create) return;
    wx.navigateTo({ url: `/pages/notebook/detail/record-edit/index?eventId=${grant.eventId}&date=${this.data.rangeTo}` });
  },
  exportSelected() {
    const grant = this.data.selected;
    if (!grant || !grant.capabilities.export) return;
    if (!validSharedRange(grant, this.data.rangeFrom, this.data.rangeTo,
        this.data.maxQueryMonths)) return;
    wx.navigateTo({ url: `/pages/notebook/detail/export/index?eventId=${grant.eventId}&from=${this.data.rangeFrom}&to=${this.data.rangeTo}` });
  }
});
