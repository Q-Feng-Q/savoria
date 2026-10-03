const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'loading', contacts: [], invites: [], identifier: '', token: '',
    issuedToken: '', busy: false, errorMessage: '' },
  onShow() { this.load(); },
  onHide() { this.setData({ token: '', issuedToken: '' }); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const ticket = this.identityLoad.begin(session);
    this.setData({ phase: 'loading', errorMessage: '', issuedToken: '' });
    try {
      const notebook = createApiRuntime().notebook;
      const [contacts, invites] = await Promise.all([notebook.listContacts(), notebook.listInvites()]);
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ contacts: contacts || [], invites: invites || [], phase: 'ready' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '联系人加载失败' });
      showApiError(error, '联系人加载失败');
    }
  },
  changeIdentifier(event) { this.setData({ identifier: event.detail.value }); },
  changeToken(event) { this.setData({ token: event.detail.value }); },
  async invite() {
    const identifier = this.data.identifier.trim();
    if (!identifier || this.data.busy) return;
    this.setData({ busy: true, issuedToken: '' });
    try {
      const created = await createApiRuntime().notebook.inviteContact(identifier);
      this.setData({ identifier: '', issuedToken: created.token || '' });
      wx.showToast({ title: '邀请已创建，请通过可信渠道交给对方', icon: 'none' });
      await this.load();
      if (created.token) this.setData({ issuedToken: created.token });
    } catch (error) { showApiError(error, '邀请失败'); }
    finally { this.setData({ busy: false }); }
  },
  copyToken() {
    if (!this.data.issuedToken) return;
    wx.setClipboardData({ data: this.data.issuedToken });
  },
  async respond(event) {
    if (this.data.busy) return;
    const id = Number(event.currentTarget.dataset.id);
    const token = this.data.token.trim();
    if (!id || !token) return wx.showToast({ title: '请输入邀请令牌', icon: 'none' });
    this.setData({ busy: true });
    try {
      const notebook = createApiRuntime().notebook;
      if (event.currentTarget.dataset.action === 'accept') await notebook.acceptInvite(id, token);
      else await notebook.rejectInvite(id, token);
      this.setData({ token: '' });
      await this.load();
    } catch (error) { showApiError(error, '处理邀请失败'); }
    finally { this.setData({ busy: false }); }
  },
  removeContact(event) {
    const id = Number(event.currentTarget.dataset.id);
    wx.showModal({ title: '移除联系人？', content: '双方的记事授权将同时撤销。',
      success: async (result) => {
        if (!result.confirm) return;
        try { await createApiRuntime().notebook.removeContact(id); await this.load(); }
        catch (error) { showApiError(error, '移除失败'); }
      } });
  }
});
