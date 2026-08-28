const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('family wallet pages use shared wallet endpoints and language', () => {
  const walletJs = read('pages/family/wallet/index.js');
  const ledgerJs = read('pages/family/wallet-ledger/index.js');
  const markup = read('pages/family/wallet/index.wxml') + read('pages/family/wallet-ledger/index.wxml');
  assert.match(walletJs, /family\.getWallet\(\)/);
  assert.match(ledgerJs, /getFamilyWalletLedgers/);
  assert.match(markup, /家庭钱包/);
  assert.doesNotMatch(`${walletJs}\n${ledgerJs}`, /getMemberWalletLedgers/);
  assert.doesNotMatch(markup, /谁点的菜扣谁/);
});

test('merchant family detail adjusts one family wallet without member recharge controls', () => {
  const js = read('pages/merchant/merchant-family-detail/index.js');
  const wxml = read('pages/merchant/merchant-family-detail/index.wxml');
  assert.match(js, /adjustFamilyBalance/);
  assert.match(js, /getFamilyWallet/);
  assert.doesNotMatch(js, /adjustMemberBalance|getMemberWalletLedgers/);
  assert.match(wxml, /家庭钱包/);
  assert.doesNotMatch(wxml, /data-member-id=.*充值|成员与余额/);
});
