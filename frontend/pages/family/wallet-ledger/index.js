const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiWalletLedgerScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

function buildMerchantLedgerScene(ledgers = []) {
  const transactions = (ledgers || []).map((item) => ({
    id: item.ledgerId,
    title: item.type || '余额变动',
    note: item.remark || '',
    amountText: `${Number(item.amount || 0) < 0 ? '-' : '+'}楼${Math.abs(Number(item.amount || 0)).toFixed(2)}`,
    amountClass: Number(item.amount || 0) < 0 ? 'negative' : 'positive',
    statusText: `余额 楼${Number(item.balanceAfter || 0).toFixed(2)}`,
    createdAt: String(item.createdAt || '').replace('T', ' ').replace(/:\d{2}$/, '')
  }));

  return {
    context: {
      merchant: { id: 0, name: '商户工作台' },
      family: null,
      member: null
    },
    transactions
  };
}

Page({
  data: {
    actor: 'family',
    memberId: '',
    transactions: [],
    context: null
  },

  onLoad(query) {
    this.setData({
      actor: (query && query.actor) || 'family',
      memberId: (query && query.memberId) || ''
    });
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    try {
      if (this.data.actor === 'merchant' && this.data.memberId) {
        const ledgers = await runtime.merchant.getMemberWalletLedgers(this.data.memberId);
        this.setData(buildMerchantLedgerScene(ledgers));
        return;
      }

      const bundle = await loadFamilyBundle(runtime);
      const ledgers = await runtime.family.getWalletLedgers();
      const scene = buildApiWalletLedgerScene({
        homeData: bundle.homeData,
        ledgers
      });
      this.setData(scene);
    } catch (error) {
      showApiError(error, '流水加载失败');
    }
  }
});
