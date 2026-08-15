const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');

function firstCharacter(value) {
  return (value || '用').slice(0, 1);
}

Page({
  data: {
    loading: true,
    saving: false,
    mobileBindingEnabled: true,
    avatarInitial: '用',
    form: { nickname: '', avatarUrl: '', mobile: '' }
  },
  onLoad() { this.load(); },
  async load() {
    if (!requireSession()) return;
    try {
      const runtime = createApiRuntime();
      const [profile, settings] = await Promise.all([
        runtime.user.getProfile(),
        runtime.system.getPublicSettings()
      ]);
      const nickname = profile.nickname || '';
      this.setData({
        loading: false,
        mobileBindingEnabled: settings.mobileBindingEnabled !== false,
        avatarInitial: firstCharacter(nickname || profile.username),
        form: { nickname, avatarUrl: profile.avatarUrl || '', mobile: profile.mobile || '' }
      });
    } catch (error) {
      showApiError(error, '资料加载失败');
    }
  },
  input(event) {
    const field = event.currentTarget.dataset.field;
    const value = event.detail.value;
    const patch = { [`form.${field}`]: value };
    if (field === 'nickname') patch.avatarInitial = firstCharacter(value);
    this.setData(patch);
  },
  async save() {
    if (!this.data.form.nickname.trim()) {
      wx.showToast({ title: '请填写昵称', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    try {
      await createApiRuntime().user.updateProfile(this.data.form);
      wx.showToast({ title: '资料已保存', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 400);
    } catch (error) {
      showApiError(error, '资料保存失败');
    } finally {
      this.setData({ saving: false });
    }
  }
});

