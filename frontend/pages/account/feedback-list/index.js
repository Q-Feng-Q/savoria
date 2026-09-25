const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession } = require('../../../utils/page-api');
const { identityKey } = require('../../../utils/identity-load');
const { presentFeedback } = require('../../../utils/feedback');
Page({
  currentSession() { const store = this.runtime.sessionStore; return store.getSession(); },
  data: { items: [], total: 0, page: 0, loading: false, error: '' },
  onLoad() { this.runtime = createApiRuntime(); this.alive = true; this._loadGeneration = 0; },
  onShow() {
    const session = requireSession();
    this.owner = identityKey(session);
    this._loadGeneration += 1;
    this.setData({ items: [], total: 0, page: 0, loading: false, error: '' });
    if (session) this.load(true);
  },
  isCurrent() { return this.alive && this.owner === identityKey(this.currentSession()); },
  onUnload() { this.alive = false; },
  onReachBottom() { this.load(false); },
  retry() { this.load(this.data.page === 0); },
  async load(reset = false) {
    if (!this.isCurrent() || this.data.loading || (!reset && this.data.items.length >= this.data.total)) return;
    const generation = this._loadGeneration;
    const page = reset ? 1 : this.data.page + 1;
    this.setData({ loading: true, error: '' });
    try {
      const result = await this.runtime.feedback.list(page);
      if (!this.isCurrent() || generation !== this._loadGeneration) return;
      this.setData({ items: [...(reset ? [] : this.data.items), ...result.items.map(presentFeedback)], page, total: result.total });
    } catch (error) {
      if (this.isCurrent() && generation === this._loadGeneration) this.setData({ error: error.message || '反馈记录读取失败' });
    } finally { if (this.isCurrent() && generation === this._loadGeneration) this.setData({ loading: false }); }
  },
  create() { wx.navigateTo({ url: '/pages/account/feedback-create/index' }); },
  open(event) { wx.navigateTo({ url: `/pages/account/feedback-detail/index?id=${event.currentTarget.dataset.id}` }); }
});
