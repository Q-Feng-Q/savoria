const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiProfileScene } = require('../../../utils/api-scenes');
const { sessionStore } = require('../../../utils/session');
const { isMerchantSession, requireSession, showApiError } = require('../../../utils/page-api');
const { buildProfileMenuGroups } = require('../../../utils/profile-menu');
const { switchAccountIdentity } = require('../../../utils/account-switching');

Page({
  data: {
    summaryCards: [],
    memberCards: [],
    defaultAddress: null,
    context: null,
    accountProfile: null,
    identityContext: null,
    isUnbound: false,
    sessionLabel: '',
    avatarInitial: '用',
    showDemoSwitchers: false,
    menuGroups: [{
      title: '常用服务',
      items: [
        { key: 'addresses', label: '地址簿', note: '维护配送地址' },
        { key: 'wallet', label: '我的余额', note: '查看可用与冻结' },
        { key: 'notifications', label: '通知中心', note: '订单和余额提醒' },
        { key: 'merchant', label: '商户工作台', note: '进入商户端管理页' }
      ]
    }]
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    try {
      const [accountProfile, identityContext] = await Promise.all([
        runtime.user.getProfile(),
        runtime.user.getContext()
      ]);
      let homeData = null;
      try {
        homeData = await runtime.family.getHome();
      } catch (error) {
        if (identityContext.familyId) throw error;
      }
      const effectiveIdentityContext = homeData && homeData.family
        ? {
          ...identityContext,
          familyId: homeData.family.familyId,
          familyRole: (homeData.member && homeData.member.roleTemplate) || identityContext.familyRole
        }
        : identityContext;
      const hasExplicitPermissions = Array.isArray(effectiveIdentityContext.permissionCodes);
      const hasMerchantAccess = Boolean(effectiveIdentityContext.merchantId && (hasExplicitPermissions
        ? effectiveIdentityContext.permissionCodes.includes('MERCHANT_ADMIN')
        : effectiveIdentityContext.merchantRole));
      const merchantMode = session.activeMode === 'merchant' && hasMerchantAccess;
      const refreshedSession = {
        ...session,
        ...effectiveIdentityContext,
        username: accountProfile.username,
        nickname: accountProfile.nickname || '',
        familyId: effectiveIdentityContext.familyId || null,
        memberId: effectiveIdentityContext.familyId ? effectiveIdentityContext.userId : null,
        merchantId: effectiveIdentityContext.merchantId || null,
        availableModes: effectiveIdentityContext.availableModes,
        permissionCodes: effectiveIdentityContext.permissionCodes,
        roleTemplate: merchantMode
          ? 'merchant_admin'
          : String(effectiveIdentityContext.familyRole || 'user').toLowerCase(),
        merchantAdminScopes: hasMerchantAccess ? ['merchant'] : [],
        loginMode: 'api'
      };
      sessionStore.setSession(refreshedSession);
      if (!effectiveIdentityContext.familyId) {
        this.setData({
          accountProfile,
          avatarInitial: (accountProfile.nickname || accountProfile.username || '用').slice(0, 1),
          identityContext: effectiveIdentityContext,
          isUnbound: true,
          context: { member: { name: accountProfile.nickname || accountProfile.username }, family: { name: effectiveIdentityContext.merchantId ? '商户工作台' : '个人账号' } },
          summaryCards: [
            { key: 'username', label: '用户名', value: accountProfile.username },
            { key: 'email', label: '邮箱', value: accountProfile.maskedEmail || '未绑定' }
          ],
          sessionLabel: effectiveIdentityContext.merchantId ? '商户管理身份' : '个人身份',
          menuGroups: buildProfileMenuGroups(effectiveIdentityContext)
        });
        return;
      }
      const [addresses, ledgers] = await Promise.all([
        runtime.family.getAddresses().catch(() => []),
        runtime.family.getWalletLedgers().catch(() => [])
      ]);
      const scene = buildApiProfileScene({
        homeData,
        addresses,
        ledgers,
        session: refreshedSession
      });
      this.setData({
        ...scene,
        isUnbound: false,
        accountProfile,
        avatarInitial: (accountProfile.nickname || accountProfile.username || '用').slice(0, 1),
        identityContext: effectiveIdentityContext,
        sessionLabel: isMerchantSession(refreshedSession) ? '家庭与商户负责人' : '家庭成员',
        menuGroups: buildProfileMenuGroups(effectiveIdentityContext)
      });
    } catch (error) {
      showApiError(error, '个人页加载失败');
    }
  },

  openAddresses() {
    wx.navigateTo({ url: '/pages/family/addresses/index' });
  },

  openWallet() {
    wx.navigateTo({ url: '/pages/family/wallet/index' });
  },

  openNotifications() {
    wx.navigateTo({ url: '/pages/account/notifications/index' });
  },

  async openMerchant() {
    const session = sessionStore.getSession();
    if (!session) return;
    try {
      const result = await switchAccountIdentity({ userId: session.userId, mode: 'merchant' }, {
        sessionStore,
        loadContext: () => createApiRuntime().user.getContext()
      });
      wx.redirectTo({ url: result.destination });
    } catch (error) {
      showApiError(error, '商户身份不可用');
    }
  },

  openFamilyStart() {
    wx.navigateTo({ url: '/pages/family/family-start/index' });
  },
  openHome(){wx.switchTab({url:'/pages/family/home/index'});},
  openOrders(){wx.switchTab({url:'/pages/ordering/orders/index'});},
  openFamilyManagement(){wx.navigateTo({url:'/pages/family/family-management/index'});},

  openProfileEdit() { wx.navigateTo({ url: '/pages/account/profile-edit/index' }); },
  openAccountSecurity() { wx.navigateTo({ url: '/pages/account/account-security/index' }); },

  selectMenu(event) {
    const actions = {
      addresses: () => this.openAddresses(),
      wallet: () => this.openWallet(),
      notifications: () => this.openNotifications(),
      merchant: () => this.openMerchant(),
      home:()=>this.openHome(),
      orders:()=>this.openOrders(),
      familyManagement:()=>this.openFamilyManagement()
    };
    const action = actions[event.detail.item.key];
    if (action) action();
  },

  switchIdentity() {
    wx.navigateTo({ url: '/pages/account/account-management/index' });
  },

  async logoutExperience() {
    try {
      await createApiRuntime().auth.logout();
    } catch (error) {
      // Local logout must still succeed when the session has already expired.
    } finally {
      sessionStore.clearSession();
      wx.reLaunch({ url: '/pages/auth/entry/index' });
    }
  }
});
