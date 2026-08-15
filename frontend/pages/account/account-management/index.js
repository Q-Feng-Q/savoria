const { createApiRuntime } = require('../../../utils/api-runtime');
const { sessionStore } = require('../../../utils/session');
const { refreshAccountIdentity, switchAccountIdentity } = require('../../../utils/account-switching');

function decorate(accounts, current) {
  return accounts.map((item) => {
    const currentAccount = Boolean(current && String(current.userId) === String(item.userId));
    const modes = item.availableModes || [];
    return {
      ...item,
      displayName: item.nickname || item.username || `用户 ${item.userId}`,
      roleLabel: [modes.includes('family') ? '家庭用户' : '', modes.includes('merchant') ? '商户管理' : '']
        .filter(Boolean).join(' · '),
      hasFamilyMode: modes.includes('family'),
      hasMerchantMode: modes.includes('merchant'),
      familyUsing: currentAccount && current.activeMode === 'family',
      merchantUsing: currentAccount && current.activeMode === 'merchant',
      familyKey: `${item.userId}:family`,
      merchantKey: `${item.userId}:merchant`,
      isCurrent: currentAccount,
      avatarInitial: String(item.nickname || item.username || '用').slice(0, 1)
    };
  });
}

function navigateDestination(destination) {
  if (destination === '/pages/merchant/index') {
    wx.redirectTo({ url: destination });
  } else {
    wx.switchTab({ url: destination });
  }
}

Page({
  data: { accounts: [], switchingKey: '', currentUserId: '', syncError: '' },
  onShow() { this.refreshIdentity(); },
  async refreshIdentity() {
    const current = sessionStore.getSession();
    this.refresh();
    if (!current || current.requiresLogin || !current.accessToken) return;
    this.setData({ syncError: '' });
    try {
      await refreshAccountIdentity({ userId: current.userId }, {
        sessionStore,
        loadContext: () => createApiRuntime().user.getContext()
      });
    } catch (error) {
      this.setData({ syncError: (error && error.message) || '身份权限同步失败，请稍后重试' });
      // 列表仍显示本地账号；具体失效原因在用户主动切换时提示。
    }
    this.refresh();
  },
  refresh() {
    const current = sessionStore.getSession();
    this.setData({
      accounts: decorate(sessionStore.listAccounts(), current),
      currentUserId: current && current.userId != null ? String(current.userId) : ''
    });
  },
  switchAccount(event) {
    if (this.data.switchingKey) return;
    const account = sessionStore.getAccount(event.currentTarget.dataset.userId);
    if (!account) return;
    this.startSwitch(account.userId, account.activeMode || account.availableModes[0]);
  },
  switchIdentity(event) {
    if (this.data.switchingKey) return;
    this.startSwitch(event.currentTarget.dataset.userId, event.currentTarget.dataset.mode);
  },
  async startSwitch(userId, mode) {
    if (this.data.switchingKey) return;
    const account = sessionStore.getAccount(userId);
    if (!account) return;
    if (account.requiresLogin || !account.accessToken) {
      this.openLogin(account.username);
      return;
    }
    const switchingKey = `${userId}:${mode}`;
    this.setData({ switchingKey });
    try {
      const runtime = createApiRuntime();
      const result = await switchAccountIdentity({ userId, mode }, {
        sessionStore,
        loadContext: () => runtime.user.getContext()
      });
      navigateDestination(result.destination);
    } catch (error) {
      if (error && error.platformAdmin) {
        wx.showModal({
          title: '平台管理员账号',
          content: '平台管理员请前往 Web 管理后台登录。',
          showCancel: false,
          confirmText: '我知道了'
        });
      } else if (error && error.requiresLogin) {
        wx.showToast({ title: '登录已失效，请重新验证', icon: 'none' });
        this.refresh();
      } else {
        wx.showToast({ title: (error && error.message) || '账号切换失败', icon: 'none' });
      }
    } finally {
      this.setData({ switchingKey: '' });
    }
  },
  openLogin(username) {
    const encoded = encodeURIComponent(username || '');
    wx.navigateTo({ url: `/pages/auth/entry/index?addAccount=1&username=${encoded}` });
  },
  addAccount() { this.openLogin(''); },
  removeAccount(event) {
    const userId = event.currentTarget.dataset.userId;
    wx.showModal({
      title: '移除账号',
      content: '仅移除本机保存的登录状态，不会注销账号。',
      confirmText: '移除',
      confirmColor: '#b7473d',
      success: (result) => {
        if (!result.confirm) return;
        sessionStore.removeAccount(userId);
        if (!sessionStore.getSession()) {
          wx.reLaunch({ url: '/pages/auth/entry/index' });
          return;
        }
        this.refresh();
      }
    });
  }
});
