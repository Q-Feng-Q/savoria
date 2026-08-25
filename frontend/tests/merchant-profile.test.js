const test = require('node:test');
const assert = require('node:assert/strict');

const {
  createMerchantProfileSnapshot,
  validateMerchantProfile,
  isMerchantAccessError,
  failClosedMerchantSession
} = require('../utils/merchant-profile');

test('merchant profile snapshot trims the editable whitelist', () => {
  assert.deepEqual(createMerchantProfileSnapshot({
    name: ' 暖炉小馆 ', contactName: ' 林女士 ', contactPhone: ' 13800000000 ', merchantId: 7
  }), { name: '暖炉小馆', contactName: '林女士', contactPhone: '13800000000' });
});

test('merchant profile validates name and field limits', () => {
  assert.equal(validateMerchantProfile({ name: '  ' }).valid, false);
  assert.equal(validateMerchantProfile({ name: '暖炉', contactName: '林女士', contactPhone: '13800000000' }).valid, true);
  assert.equal(validateMerchantProfile({ name: '暖炉', contactPhone: '1'.repeat(31) }).valid, false);
});

test('merchant access errors include http and business forbidden codes', () => {
  assert.equal(isMerchantAccessError({ code: 40301 }), true);
  assert.equal(isMerchantAccessError({ statusCode: 403 }), true);
  assert.equal(isMerchantAccessError({ code: 50001 }), false);
});

test('fail closed merchant session removes stale merchant authority', () => {
  const closed = failClosedMerchantSession({
    merchantId: 7,
    merchantRole: 'MERCHANT_ADMIN',
    activeMode: 'merchant',
    familyId: 8,
    roleTemplate: 'merchant_admin',
    permissionCodes: ['FAMILY_MEMBER', 'MERCHANT_ADMIN'],
    backendRoles: ['merchant_admin', 'user'],
    merchantAdminScopes: ['merchant'],
    availableModes: ['family', 'merchant']
  });
  assert.equal(closed.merchantId, null);
  assert.equal(closed.merchantRole, null);
  assert.equal(closed.activeMode, 'family');
  assert.equal(closed.roleTemplate, 'user');
  assert.deepEqual(closed.permissionCodes, ['FAMILY_MEMBER']);
  assert.deepEqual(closed.backendRoles, ['user']);
  assert.deepEqual(closed.merchantAdminScopes, []);
  assert.deepEqual(closed.availableModes, ['family']);
});
