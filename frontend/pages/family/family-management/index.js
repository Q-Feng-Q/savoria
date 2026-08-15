const { createApiRuntime } = require('../../../utils/api-runtime');
const { sessionStore } = require('../../../utils/session');
const { requireSession, showApiError } = require('../../../utils/page-api');

function confirm(options) {
  return new Promise((resolve) => wx.showModal({ ...options, success: (result) => resolve(result.confirm), fail: () => resolve(false) }));
}

Page({
  data: { loading: true, busy: false, invitationCode: '', userIdentifier: '', targetUserId: '', applications: [], invitations: [], familyRole: 'MEMBER', isAdmin: false, isOwner: false },
  onShow() { this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    this.setData({ loading: true });
    try {
      const runtime = createApiRuntime();
      const context = await runtime.user.getContext();
      const familyRole = String(context.familyRole || session.familyRole || session.roleTemplate || 'MEMBER').toUpperCase();
      const isOwner = familyRole === 'OWNER';
      const isAdmin = isOwner || familyRole === 'ADMIN' || (context.permissionCodes || []).includes('FAMILY_ADMIN');
      const [invitations, applications] = await Promise.all([
        runtime.family.getMyInvitations().catch(() => []),
        isAdmin ? runtime.family.getJoinApplications() : Promise.resolve([])
      ]);
      this.setData({ loading: false, familyRole, isOwner, isAdmin, invitations: invitations || [], applications: applications || [] });
    } catch (error) {
      this.setData({ loading: false });
      showApiError(error, '家庭管理加载失败');
    }
  },
  input(event) { this.setData({ [event.currentTarget.dataset.field]: event.detail.value }); },
  async generateCode() { if(this.data.busy)return; this.setData({busy:true});try{const invitationCode=await createApiRuntime().family.generateInvitation();this.setData({invitationCode});}catch(e){showApiError(e,'邀请码生成失败');}finally{this.setData({busy:false});} },
  copyCode() { if (this.data.invitationCode) wx.setClipboardData({ data: this.data.invitationCode }); },
  async directInvite() { const userIdentifier=String(this.data.userIdentifier||'').trim();if(!userIdentifier)return wx.showToast({title:'请输入账号、手机或邮箱',icon:'none'});this.setData({busy:true});try{await createApiRuntime().family.directInvite({userIdentifier});wx.showToast({title:'邀请已发送',icon:'success'});this.setData({userIdentifier:''});}catch(e){showApiError(e,'邀请发送失败');}finally{this.setData({busy:false});} },
  async approve(event) { try { await createApiRuntime().family.approveJoinApplication(event.currentTarget.dataset.id); await this.load(); } catch (error) { showApiError(error, '审批失败'); } },
  async reject(event) { try { await createApiRuntime().family.rejectJoinApplication(event.currentTarget.dataset.id, { reason: '家庭管理员拒绝' }); await this.load(); } catch (error) { showApiError(error, '审批失败'); } },
  async acceptInvitation(event) { try { await createApiRuntime().family.acceptInvitation(event.currentTarget.dataset.id); wx.showToast({title:'已加入家庭',icon:'success'}); await this.load(); } catch(error) { showApiError(error,'接受邀请失败'); } },
  async rejectInvitation(event) { try { await createApiRuntime().family.rejectInvitation(event.currentTarget.dataset.id,{reason:'暂不加入'}); await this.load(); } catch(error) { showApiError(error,'拒绝邀请失败'); } },
  async exitFamily() {
    if (this.data.busy || this.data.isOwner) return;
    if (!await confirm({ title: '退出当前家庭', content: '退出后将无法继续查看该家庭的菜单、订单和钱包。', confirmText: '确认退出', confirmColor: '#b7473d' })) return;
    this.setData({ busy: true });
    try { await createApiRuntime().family.exitFamily(); await this.finishFamilyChange('已退出家庭'); } catch (error) { showApiError(error, '退出家庭失败'); } finally { this.setData({ busy: false }); }
  },
  async transferOwner() {
    const targetUserId = Number(this.data.targetUserId);
    if (!Number.isInteger(targetUserId) || targetUserId <= 0) return wx.showToast({ title: '请输入新负责人用户 ID', icon: 'none' });
    if (!await confirm({ title: '移交家庭负责人', content: `确认将家庭负责人移交给用户 #${targetUserId}？`, confirmText: '确认移交' })) return;
    this.setData({ busy: true });
    try { await createApiRuntime().family.transferOwner({ targetUserId }); wx.showToast({ title: '已完成移交', icon: 'success' }); this.setData({ targetUserId: '' }); await this.load(); } catch(error) { showApiError(error,'负责人移交失败'); } finally { this.setData({busy:false}); }
  },
  async dissolveFamily() {
    if (this.data.busy || !this.data.isOwner) return;
    if (!await confirm({ title: '解散当前家庭', content: '家庭关系、菜单配置和后续点餐权限将被停用，此操作不可撤销。', confirmText: '确认解散', confirmColor: '#b7473d' })) return;
    this.setData({busy:true});
    try { await createApiRuntime().family.dissolveFamily(); await this.finishFamilyChange('家庭已解散'); } catch(error) { showApiError(error,'解散家庭失败'); } finally { this.setData({busy:false}); }
  },
  async finishFamilyChange(message) {
    const session = sessionStore.getSession();
    if (session) sessionStore.setSession({ ...session, familyId: null, memberId: null, familyRole: null, activeMode: 'family', roleTemplate: 'user' });
    wx.showToast({ title: message, icon: 'success' });
    setTimeout(() => wx.reLaunch({ url: '/pages/family/family-start/index' }), 400);
  }
});
