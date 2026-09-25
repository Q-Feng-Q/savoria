const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const vm = require('node:vm');
const esbuild = require('../node_modules/esbuild');

const bundle = esbuild.buildSync({
  entryPoints: [path.resolve(__dirname, '../src/api/auth.js')],
  bundle: true,
  format: 'cjs',
  platform: 'browser',
  write: false,
  logLevel: 'silent'
});
const sandbox = {
  module: { exports: {} },
  console,
  KitchenAdminUiConfig: require('../ui-config')
};
vm.runInNewContext(bundle.outputFiles[0].text, sandbox);
const { isMerchantAdminSession, isPlatformAdminSession } = sandbox.module.exports;

test('merchant identity supports both clients and uppercase backend roles', () => {
  assert.equal(isMerchantAdminSession({ roleTemplate: 'merchant_admin', merchantId: 9 }), true);
  assert.equal(isMerchantAdminSession({ backendRoles: ['MERCHANT_ADMIN'] }), true);
  assert.equal(isMerchantAdminSession({ actor: { roleTemplate: 'merchant_admin' } }), true);
  assert.equal(isMerchantAdminSession({ merchantAdminScopes: ['merchant'] }), true);
});

test('a family merchant association does not grant console access', () => {
  assert.equal(isMerchantAdminSession({ merchantId: 9, roleTemplate: 'admin' }), false);
  assert.equal(isMerchantAdminSession({ actor: { merchantId: 9, roleTemplate: 'member' } }), false);
  assert.equal(isMerchantAdminSession(null), false);
});

test('platform administrators retain console access', () => {
  assert.equal(isPlatformAdminSession({ backendRoles: ['platform_admin'] }), true);
});
