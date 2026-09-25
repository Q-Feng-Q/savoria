const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiWalletScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: {
    balanceCards: [],
    latestTransactions: [],
    context: null,
    phase: 'loading',
    errorMessage: ''
  },

  onShow() {
    this.load();
  },

  retryLoad() { return this.load(); },

  async load() {
    const session = requireSession({ familyOnly: true });
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    this.setData({ phase: 'loading', errorMessage: '', balanceCards: [], latestTransactions: [], context: null });

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const [wallet, ledgers] = await Promise.all([
        runtime.family.getWallet(), runtime.family.getWalletLedgers({ page: 1, pageSize: 20 })
      ]);
      const scene = buildApiWalletScene({
        homeData: bundle.homeData,
        wallet,
        ledgers
      });
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ ...scene, phase: 'ready', errorMessage: '' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ phase: 'error', errorMessage: (error && error.message) || '钱包加载失败' });
      showApiError(error, '钱包加载失败');
    }
  },

  openLedger() {
    wx.navigateTo({ url: '/pages/family/wallet-ledger/index' });
  }
});
