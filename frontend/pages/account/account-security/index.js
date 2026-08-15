const { createApiRuntime } = require('../../../utils/api-runtime')
const { requireSession, showApiError } = require('../../../utils/page-api')
const { sessionStore } = require('../../../utils/session')

Page({
  data: {
    loading: true,
    saving: false,
    profile: {},
    mobileBindingEnabled: false,
    emailBindingEnabled: false,
    wechatBindingEnabled: false,
    email: '',
    emailCode: '',
    currentPassword: '',
    newPassword: ''
  },
  onShow() { this.load() },
  async load() {
    if (!requireSession()) return
    this.setData({ loading: true })
    try {
      const runtime = createApiRuntime()
      const [profile, settings] = await Promise.all([
        runtime.user.getProfile(),
        runtime.system.getPublicSettings()
      ])
      this.setData({
        loading: false,
        profile,
        mobileBindingEnabled: !!settings.mobileBindingEnabled,
        emailBindingEnabled: !!settings.emailBindingEnabled,
        wechatBindingEnabled: !!settings.wechatBindingEnabled
      })
    } catch (error) {
      this.setData({ loading: false })
      showApiError(error, '账户安全加载失败')
    }
  },
  input(event) { this.setData({ [event.currentTarget.dataset.field]: event.detail.value }) },
  async runSaving(task, fallback) {
    if (this.data.saving) return
    this.setData({ saving: true })
    try { await task() } catch (error) { showApiError(error, fallback) } finally { this.setData({ saving: false }) }
  },
  sendEmailCode() {
    return this.runSaving(async () => {
      await createApiRuntime().user.sendEmailCode({ email: this.data.email })
      wx.showToast({ title: '验证码已发送', icon: 'success' })
    }, '发送失败')
  },
  bindEmail() {
    return this.runSaving(async () => {
      await createApiRuntime().user.bindEmail({ email: this.data.email, verificationCode: this.data.emailCode })
      wx.showToast({ title: '邮箱已绑定', icon: 'success' })
      await this.load()
    }, '邮箱绑定失败')
  },
  bindWechat() {
    if (this.data.saving) return
    wx.login({
      success: ({ code }) => this.runSaving(async () => {
        await createApiRuntime().user.bindWechat({ authorizationCode: code })
        wx.showToast({ title: '微信已绑定', icon: 'success' })
        await this.load()
      }, '微信绑定失败')
    })
  },
  unbindWechat() {
    return this.runSaving(async () => {
      await createApiRuntime().user.unbindWechat()
      await this.load()
    }, '解绑失败')
  },
  changePassword() {
    if (this.data.newPassword.length < 6 || this.data.newPassword.length > 64) return wx.showToast({ title: '新密码需为 6-64 位', icon: 'none' })
    return this.runSaving(async () => {
      await createApiRuntime().user.changePassword({
        currentPassword: this.data.currentPassword,
        newPassword: this.data.newPassword
      })
      sessionStore.clearSession()
      wx.showModal({
        title: '密码已修改',
        content: '请使用新密码重新登录',
        showCancel: false,
        success: () => wx.reLaunch({ url: '/pages/auth/entry/index' })
      })
    }, '密码修改失败')
  }
})
