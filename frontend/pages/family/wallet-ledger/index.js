const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiWalletLedgerScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: { actor: 'family', familyId: '', transactions: [], context: null },
  onLoad(query) { this.setData({ actor: (query && query.actor) || 'family', familyId: (query && query.familyId) || '' }); },
  onShow() { this.load(); },
  async load() {
    if (!requireSession()) return;
    const runtime = createApiRuntime();
    try {
      if (this.data.actor === 'merchant' && this.data.familyId) {
        const ledgers = await runtime.merchant.getFamilyWalletLedgers(this.data.familyId, { page: 1, pageSize: 100 });
        const homeData = { family: { familyId: Number(this.data.familyId), familyName: `服务家庭 ${this.data.familyId}` },
          member: { memberId: 0, name: '商户负责人' } };
        this.setData(buildApiWalletLedgerScene({ homeData, ledgers }));
      } else {
        const bundle = await loadFamilyBundle(runtime);
        const ledgers = await runtime.family.getWalletLedgers({ page: 1, pageSize: 100 });
        this.setData(buildApiWalletLedgerScene({ homeData: bundle.homeData, ledgers }));
      }
    } catch (error) { showApiError(error, '家庭钱包流水加载失败'); }
  }
});
