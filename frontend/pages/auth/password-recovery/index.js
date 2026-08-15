const { createApiRuntime } = require('../../../utils/api-runtime');
const { showApiError } = require('../../../utils/page-api');

Page({
  data: {
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: '',
    sending: false,
    saving: false,
    codeSent: false
  },
  input(event) { this.setData({ [event.currentTarget.dataset.field]: event.detail.value }); },
  async sendCode() {
    if (this.data.sending) return;
    const email = String(this.data.email || '').trim();
    if (!email) return wx.showToast({ title: '请输入绑定邮箱', icon: 'none' });
    this.setData({ sending: true });
    try {
      await createApiRuntime().auth.sendPasswordResetCode({ email });
      this.setData({ codeSent: true });
      wx.showToast({ title: '验证码已发送', icon: 'success' });
    } catch (error) {
      showApiError(error, '验证码发送失败');
    } finally {
      this.setData({ sending: false });
    }
  },
  async submit() {
    if (this.data.saving) return;
    const email = String(this.data.email || '').trim();
    const code = String(this.data.code || '').trim();
    const newPassword = String(this.data.newPassword || '');
    if (!email || !code) return wx.showToast({ title: '请填写邮箱和验证码', icon: 'none' });
    if (newPassword.length < 6) return wx.showToast({ title: '新密码至少 6 位', icon: 'none' });
    if (newPassword !== this.data.confirmPassword) return wx.showToast({ title: '两次密码不一致', icon: 'none' });
    this.setData({ saving: true });
    try {
      await createApiRuntime().auth.resetPassword({ email, code, newPassword });
      await new Promise((resolve) => wx.showModal({ title: '密码已更新', content: '请使用新密码重新登录。', showCancel: false, success: resolve, fail: resolve }));
      wx.navigateBack();
    } catch (error) {
      showApiError(error, '密码重置失败');
    } finally {
      this.setData({ saving: false });
    }
  }
});
