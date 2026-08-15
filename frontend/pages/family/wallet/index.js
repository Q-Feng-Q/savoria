const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiWalletScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: {
    balanceCards: [],
    latestTransactions: [],
    context: null
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const ledgers = await runtime.family.getWalletLedgers();
      const scene = buildApiWalletScene({
        homeData: bundle.homeData,
        ledgers
      });
      this.setData(scene);
    } catch (error) {
      showApiError(error, '钱包加载失败');
    }
  },

  openLedger() {
    wx.navigateTo({ url: '/pages/family/wallet-ledger/index' });
  }
});
