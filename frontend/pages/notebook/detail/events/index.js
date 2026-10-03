const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'loading', events: [], includeArchived: false, errorMessage: '', busyId: null },
  onShow() { return this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const ticket = this.identityLoad.begin(session);
    this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const events = await createApiRuntime().notebook.listEvents(this.data.includeArchived);
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ events: events || [], phase: 'ready' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '事件加载失败' });
      showApiError(error, '事件加载失败');
    }
  },
  toggleArchived() { this.setData({ includeArchived: !this.data.includeArchived }); return this.load(); },
  createEvent() { wx.navigateTo({ url: '/pages/notebook/detail/event-edit/index' }); },
  openEvent(event) { wx.navigateTo({ url: `/pages/notebook/detail/event-detail/index?id=${event.currentTarget.dataset.id}` }); },
  editEvent(event) { wx.navigateTo({ url: `/pages/notebook/detail/event-edit/index?id=${event.currentTarget.dataset.id}` }); },
  async changeEvent(event) {
    const id = Number(event.currentTarget.dataset.id);
    const item = this.data.events.find((entry) => entry.id === id);
    if (!item || this.data.busyId) return;
    this.setData({ busyId: id });
    try {
      const kind = event.currentTarget.dataset.kind;
      await createApiRuntime().notebook.updateEvent(id,
        kind === 'starred' ? { starred: !item.starred } : { archived: !item.archived });
      await this.load();
    } catch (error) { showApiError(error, '更新事件失败'); }
    finally { this.setData({ busyId: null }); }
  },
  async moveEvent(event) {
    const id = Number(event.currentTarget.dataset.id);
    const direction = Number(event.currentTarget.dataset.direction);
    const current = this.data.events.slice();
    const index = current.findIndex((item) => item.id === id);
    const other = index + direction;
    if (index < 0 || other < 0 || other >= current.length || this.data.busyId) return;
    [current[index], current[other]] = [current[other], current[index]];
    this.setData({ busyId: id });
    try {
      await createApiRuntime().notebook.orderEvents(current.map((item) => item.id));
      await this.load();
    } catch (error) { showApiError(error, '排序保存失败'); }
    finally { this.setData({ busyId: null }); }
  },
  async deleteEvent(event) {
    const id = Number(event.currentTarget.dataset.id);
    if (this.data.busyId) return;
    this.setData({ busyId: id });
    try {
      const notebook = createApiRuntime().notebook;
      const impact = await notebook.getDeleteImpact(id);
      const first = await wx.showModal({ title: '删除事件',
        content: `会删除 ${impact.recordCount} 条记录、${impact.templateVersionCount} 个模板版本，并撤销 ${impact.grantCount} 个共享授权。`,
        confirmText: '继续', confirmColor: '#b84d3a' });
      if (!first.confirm) return;
      const second = await wx.showModal({ title: '再次确认',
        content: '删除后无法恢复。确定永久删除这个事件吗？', confirmText: '永久删除', confirmColor: '#b84d3a' });
      if (!second.confirm) return;
      await notebook.deleteEvent(id);
      await this.load();
    } catch (error) { showApiError(error, '删除事件失败'); }
    finally { this.setData({ busyId: null }); }
  }
});
