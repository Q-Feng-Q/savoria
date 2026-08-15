const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const sessionModulePath = path.join(root, 'utils', 'session.js');
const pageApiModulePath = path.join(root, 'utils', 'page-api.js');

test('merchant-only pages explain denial and redirect to account management', () => {
  const previousWx = global.wx;
  const sessionModule = require(sessionModulePath);
  const originalGetSession = sessionModule.sessionStore.getSession;
  const calls = { toasts: [], redirects: [], switchTabs: [] };

  sessionModule.sessionStore.getSession = () => ({
    activeMode: 'family',
    familyId: 12,
    merchantId: null,
    permissionCodes: []
  });
  global.wx = {
    showToast(options) { calls.toasts.push(options); },
    redirectTo(options) { calls.redirects.push(options); },
    switchTab(options) { calls.switchTabs.push(options); },
    reLaunch() { assert.fail('an authenticated unauthorized user should not be relaunched'); }
  };

  try {
    delete require.cache[require.resolve(pageApiModulePath)];
    const { requireSession } = require(pageApiModulePath);
    assert.equal(requireSession({ merchantOnly: true }), null);
    assert.equal(calls.toasts.length, 1);
    assert.equal(calls.toasts[0].icon, 'none');
    assert.ok(calls.toasts[0].title.length > 0, 'permission denial should be explained');
    assert.deepEqual(calls.redirects, [
      { url: '/pages/account/account-management/index' }
    ]);
    assert.deepEqual(calls.switchTabs, []);
  } finally {
    sessionModule.sessionStore.getSession = originalGetSession;
    delete require.cache[require.resolve(pageApiModulePath)];
    global.wx = previousWx;
  }
});

test('legacy merchant role without a merchant identity is not an authorized merchant session', () => {
  const { isMerchantSession } = require(pageApiModulePath);
  assert.equal(isMerchantSession({ roleTemplate: 'merchant_admin', merchantId: null }), false);
  assert.equal(isMerchantSession({ backendRoles: ['merchant_admin'], merchantId: null }), false);
  assert.equal(isMerchantSession({ merchantAdminScopes: ['merchant'], merchantId: null }), false);
  assert.equal(isMerchantSession({ roleTemplate: 'merchant_admin', merchantId: 2 }), true);
});
