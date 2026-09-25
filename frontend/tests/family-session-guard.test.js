const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const sessionModulePath = path.join(root, 'utils', 'session.js');
const pageApiModulePath = path.join(root, 'utils', 'page-api.js');

test('family-only guard redirects a merchant-only identity before family APIs run', () => {
  const previousWx = global.wx;
  const sessionModule = require(sessionModulePath);
  const originalGetSession = sessionModule.sessionStore.getSession;
  const redirects = [];

  sessionModule.sessionStore.getSession = () => ({
    userId: 1,
    accessToken: 'valid-token',
    activeMode: 'merchant',
    familyId: null,
    merchantId: 1,
    permissionCodes: ['MERCHANT_ADMIN']
  });
  global.wx = {
    redirectTo(options) { redirects.push(options); },
    switchTab() { assert.fail('merchant-only identity should return to merchant workbench'); },
    reLaunch() { assert.fail('valid session should not be relaunched'); }
  };

  try {
    delete require.cache[require.resolve(pageApiModulePath)];
    const { requireSession } = require(pageApiModulePath);
    assert.equal(requireSession({ familyOnly: true }), null);
    assert.deepEqual(redirects, [{ url: '/pages/merchant/index' }]);
  } finally {
    sessionModule.sessionStore.getSession = originalGetSession;
    delete require.cache[require.resolve(pageApiModulePath)];
    global.wx = previousWx;
  }
});

test('every family business page declares the family-only session guard', () => {
  const guardedPages = [
    'pages/family/home/index.js',
    'pages/family/addresses/index.js',
    'pages/family/address-edit/index.js',
    'pages/family/wallet/index.js',
    'pages/ordering/menu/index.js',
    'pages/ordering/cart/index.js',
    'pages/ordering/dish-detail/index.js',
    'pages/ordering/orders/index.js',
    'pages/ordering/order-detail/index.js'
  ];

  guardedPages.forEach((relativePath) => {
    const source = fs.readFileSync(path.join(root, relativePath), 'utf8');
    assert.match(source, /requireSession\(\{\s*familyOnly:\s*true\s*\}\)/, relativePath);
  });
});
