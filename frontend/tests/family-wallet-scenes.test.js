const test = require('node:test');
const assert = require('node:assert/strict');
const { createFamilyService } = require('../services/family');
const { createMerchantService } = require('../services/merchant');

test('family and merchant wallet services use family-wallet routes', async () => {
  const calls = [];
  const request = async (pathname, options) => { calls.push({ pathname, options }); return { code: 0, data: {} }; };
  const family = createFamilyService({ request });
  const merchant = createMerchantService({ request });
  await family.getWallet();
  await family.getWalletLedgers({ page: 2, pageSize: 10 });
  await merchant.getFamilyWallet(3);
  await merchant.getFamilyWalletLedgers(3, { page: 1, pageSize: 20 });
  await merchant.adjustFamilyBalance(3, { requestId: 'adjust-1', type: 'MANUAL_CREDIT', amount: 20, remark: '充值' });
  assert.deepEqual(calls.map((call) => call.pathname), [
    '/api/family/wallet', '/api/family/wallet/ledgers?page=2&pageSize=10',
    '/api/merchant/families/3/wallet', '/api/merchant/families/3/wallet/ledgers?page=1&pageSize=20',
    '/api/merchant/families/3/wallet/adjust'
  ]);
});
