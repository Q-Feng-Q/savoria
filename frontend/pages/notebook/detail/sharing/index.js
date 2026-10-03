const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');
const { emptyGrant, grantPayload, draftFromGrant } = require('../../../../utils/notebook-permissions');
const { dateKey, timeZone } = require('../../../../utils/notebook-calendar');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'loading', eventId: null, contacts: [], grants: [], contactIndex: 0,
    maxQueryMonths: 36, draft: null, editingId: null, busy: false, errorMessage: '' },
  onLoad(options) { this.setData({ eventId: Number(options.eventId) || null }); },
  onShow() { this.load(); },
  async load() {
    const session = requireSession();
    if (!session || !this.data.eventId) return;
    const ticket = this.identityLoad.begin(session);
    this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const notebook = createApiRuntime().notebook;
      const [contacts, grants, config] = await Promise.all([
        notebook.listContacts(), notebook.listGrants(this.data.eventId), notebook.getConfig()
      ]);
      if (!this.identityLoad.isCurrent(ticket)) return;
      const draft = this.data.draft || emptyGrant(dateKey(new Date()), timeZone());
      this.setData({ contacts: contacts || [], grants: grants || [],
        maxQueryMonths: config.maxQueryMonths || 36, draft, phase: 'ready' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '授权加载失败' });
      showApiError(error, '授权加载失败');
    }
  },
  selectContact(event) {
    const index = Number(event.detail.value);
    const contact = this.data.contacts[index];
    if (!contact) return;
    this.setData({ contactIndex: index, 'draft.granteeUserId': contact.contactUserId });
  },
  changeDate(event) { this.setData({ [`draft.${event.currentTarget.dataset.field}`]: event.detail.value }); },
  changeSwitch(event) { this.setData({ [`draft.${event.currentTarget.dataset.field}`]: Boolean(event.detail.value) }); },
  editGrant(event) {
    const grant = this.data.grants.find((item) => item.id === Number(event.currentTarget.dataset.id));
    if (!grant || grant.status !== 'ACTIVE') return;
    const index = this.data.contacts.findIndex((item) => item.contactUserId === grant.granteeUserId);
    this.setData({ editingId: grant.id, contactIndex: Math.max(index, 0),
      draft: draftFromGrant(grant, timeZone()) });
  },
  resetDraft() { this.setData({ editingId: null, contactIndex: 0,
    draft: emptyGrant(dateKey(new Date()), timeZone()) }); },
  openContacts() { wx.navigateTo({ url: '/pages/notebook/detail/contacts/index' }); },
  save() {
    if (this.data.busy) return;
    let payload;
    try { payload = grantPayload(this.data.draft, this.data.maxQueryMonths); }
    catch (error) { return wx.showToast({ title: error.message, icon: 'none' }); }
    wx.showModal({ title: '确认共享记事？',
      content: '对方可以查看授权范围内的记录；已复制或导出的内容无法远程收回。',
      success: async (result) => {
        if (!result.confirm) return;
        this.setData({ busy: true });
        try {
          const notebook = createApiRuntime().notebook;
          if (this.data.editingId) await notebook.updateGrant(this.data.editingId, payload);
          else await notebook.createGrant(this.data.eventId, payload);
          this.resetDraft();
          await this.load();
        } catch (error) { showApiError(error, '保存授权失败'); }
        finally { this.setData({ busy: false }); }
      } });
  },
  revokeGrant(event) {
    const id = Number(event.currentTarget.dataset.id);
    wx.showModal({ title: '立即撤销授权？', content: '撤销后对方的新请求会失败，已经导出的文件无法收回。',
      success: async (result) => {
        if (!result.confirm) return;
        try { await createApiRuntime().notebook.revokeGrant(id); await this.load(); }
        catch (error) { showApiError(error, '撤销失败'); }
      } });
  }
});
