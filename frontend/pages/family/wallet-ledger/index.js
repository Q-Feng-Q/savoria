const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiWalletLedgerScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { loadAllPages } = require('../../../utils/pagination');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { actor: 'family', familyId: '', transactions: [], context: null, phase: 'loading', errorMessage: '' },
  onLoad(query) { this.setData({ actor: (query && query.actor) || 'family', familyId: (query && query.familyId) || '' }); },
  onShow() { this.load(); },
  retryLoad() { return this.load(); },
  async load() {
    const session = requireSession();
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    this.setData({ phase: 'loading', errorMessage: '', transactions: [], context: null });
    const runtime = createApiRuntime();
    try {
      if (this.data.actor === 'merchant' && this.data.familyId) {
        const ledgers = await loadAllPages(
          ({ page, pageSize }) => runtime.merchant.getFamilyWalletLedgers(this.data.familyId, { page, pageSize }),
          { pageSize: 100, keyOf: (item) => item.ledgerId }
        );
        const homeData = { family: { familyId: Number(this.data.familyId), familyName: `服务家庭 ${this.data.familyId}` },
          member: { memberId: 0, name: '商户负责人' } };
        if (!this.identityLoad.isCurrent(loadToken)) return;
        this.setData({ ...buildApiWalletLedgerScene({ homeData, ledgers }), phase: 'ready', errorMessage: '' });
      } else {
        const bundle = await loadFamilyBundle(runtime);
        const ledgers = await loadAllPages(
          ({ page, pageSize }) => runtime.family.getWalletLedgers({ page, pageSize }),
          { pageSize: 100, keyOf: (item) => item.ledgerId }
        );
        if (!this.identityLoad.isCurrent(loadToken)) return;
        this.setData({ ...buildApiWalletLedgerScene({ homeData: bundle.homeData, ledgers }), phase: 'ready', errorMessage: '' });
      }
    } catch (error) {
      if (this.identityLoad.isCurrent(loadToken)) {
        this.setData({ phase: 'error', errorMessage: (error && error.message) || '家庭钱包流水加载失败' });
        showApiError(error, '家庭钱包流水加载失败');
      }
    }
  }
});
