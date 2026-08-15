const { createApiRuntime } = require('../../../utils/api-runtime');
const { sessionStore } = require('../../../utils/session');
const { redirectBySession, showApiError, showLoginError } = require('../../../utils/page-api');
const { evaluateLoginSession } = require('../../../utils/login-routing');
const { loginWithPermissionFallback } = require('../../../utils/portal-login-flow');

function showPlatformAdminNotice(message) {
  return new Promise((resolve) => wx.showModal({
    title: '平台管理员账号',
    content: message || '平台管理员请前往 Web 管理后台登录。',
    showCancel: false,
    confirmText: '我知道了',
    success: resolve,
    fail: resolve
  }));
}

Page({
  data: { form: { username: '', password: '' }, loading: false, addingAccount: false },
  onLoad(query) {
    let username = '';
    if (query && query.username) {
      try { username = decodeURIComponent(query.username); } catch (error) { username = String(query.username); }
    }
    this.setData({ addingAccount: Boolean(query && query.addAccount), 'form.username': username });
  },
  onShow() {
    const session = sessionStore.getSession();
    if (session && !this.data.addingAccount) { redirectBySession(session); return; }
    this.setData({ loading: false });
  },
  onUsernameInput(event) { this.setData({ 'form.username': event.detail.value }); },
  onPasswordInput(event) { this.setData({ 'form.password': event.detail.value }); },
  openRegister() { wx.navigateTo({ url: '/pages/auth/register/index' }); },
  openPasswordRecovery() { wx.navigateTo({ url: '/pages/auth/password-recovery/index' }); },
  async submitLogin() {
    if (this.data.loading) return;
    const username = String(this.data.form.username || '').trim();
    const password = String(this.data.form.password || '');
    if (!username || !password) { wx.showToast({ title: '请输入账号和密码', icon: 'none' }); return; }
    const previousSession = this.data.addingAccount ? sessionStore.getSession() : null;
    const runtime = createApiRuntime();
    let loginCompleted = false;
    this.setData({ loading: true });
    try {
      const loginSession = await loginWithPermissionFallback(runtime.auth, { username, password });
      loginCompleted = true;
      sessionStore.setSession({ ...loginSession, loginMode: 'api' }, { save: false });
      const context = await runtime.user.getContext();
      const platformRoles = context.platformRoles || [];
      const session = {
        ...loginSession, ...context,
        memberId: loginSession.memberId || context.userId,
        roleTemplate: context.merchantId ? 'merchant_admin' : (context.familyRole || (platformRoles.length ? 'platform_admin' : 'user')).toLowerCase(),
        backendRoles: Array.from(new Set([...(loginSession.backendRoles || []), ...platformRoles])),
        merchantAdminScopes: context.merchantId ? ['merchant'] : (loginSession.merchantAdminScopes || [])
      };
      const evaluation = evaluateLoginSession(session);
      if (!evaluation.allowed) {
        if (previousSession) sessionStore.setSession(previousSession, { save: false }); else sessionStore.clearSession();
        const isPlatformAdmin = session.roleTemplate === 'platform_admin' || (session.backendRoles || []).some((role) => String(role).toLowerCase() === 'platform_admin');
        if (isPlatformAdmin) await showPlatformAdminNotice(evaluation.message); else wx.showToast({ title: evaluation.message, icon: 'none' });
        return;
      }
      const storedSession = sessionStore.setSession({ ...session, username, loginMode: 'api' });
      redirectBySession(storedSession);
    } catch (error) {
      if (previousSession) sessionStore.setSession(previousSession, { save: false }); else sessionStore.clearSession();
      if (loginCompleted) showApiError(error, '登录后账号信息加载失败'); else showLoginError(error);
    } finally { this.setData({ loading: false }); }
  }
});
