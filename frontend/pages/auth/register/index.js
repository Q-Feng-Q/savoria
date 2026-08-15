const { createApiRuntime } = require('../../../utils/api-runtime');
const { sessionStore } = require('../../../utils/session');
const { showApiError } = require('../../../utils/page-api');

Page({
  data: { form: { username: '', password: '', name: '', mobile: '' }, loading: false },
  onUsernameInput(event) { this.setData({ 'form.username': event.detail.value }); },
  onPasswordInput(event) { this.setData({ 'form.password': event.detail.value }); },
  onNameInput(event) { this.setData({ 'form.name': event.detail.value }); },
  onMobileInput(event) { this.setData({ 'form.mobile': event.detail.value }); },
  async submitRegister() {
    if (this.data.loading) return;
    const form = { ...this.data.form, username: String(this.data.form.username || '').trim(), name: String(this.data.form.name || '').trim(), mobile: String(this.data.form.mobile || '').trim() };
    if (!/^[A-Za-z0-9_]{3,50}$/.test(form.username)) return wx.showToast({ title: '账号名需为3-50位字母、数字或下划线', icon: 'none' });
    if (String(form.password).length < 6 || String(form.password).length > 64) return wx.showToast({ title: '密码长度需为6-64位', icon: 'none' });
    if (!form.name) return wx.showToast({ title: '请输入姓名', icon: 'none' });
    const runtime = createApiRuntime(); this.setData({ loading: true });
    try {
      const session = await runtime.auth.register(form);
      sessionStore.setSession({ ...session, loginMode: 'api' }, { save: false });
      try { await runtime.auth.logout(); } catch (error) { /* Registration remains successful. */ }
      sessionStore.clearSession();
      await new Promise((resolve) => wx.showModal({ title: '注册成功', content: '个人账户已创建。家庭或后台权限需提交申请并等待管理员审核。', showCancel: false, confirmText: '去登录', success: resolve, fail: resolve }));
      wx.reLaunch({ url: '/pages/auth/entry/index' });
    } catch (error) {
      sessionStore.clearSession();
      showApiError(error, '注册失败');
    } finally { this.setData({ loading: false }); }
  }
});
