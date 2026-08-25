const test = require('node:test');
const assert = require('node:assert/strict');

const {
  buildOwnerOptions,
  createProfileSnapshot,
  validateFamilyProfile,
  findDefaultAddress,
  currentOwnerLabel,
  failClosedOwnerSession,
  mergeIdentityContext
} = require('../utils/family-management');
const { createFamilyService } = require('../services/family');

test('family service exposes profile and owner endpoints with exact payload', async () => {
  const calls = [];
  const family = createFamilyService({
    request: async (pathname, options) => {
      calls.push({ pathname, options });
      return { code: 0, data: [] };
    }
  });

  await family.getInfo();
  await family.updateInfo({ familyName: '林家', note: null });
  await family.getOwnerCandidates();
  await family.transferOwner({ targetMemberId: 12 });

  assert.deepEqual(calls, [
    { pathname: '/api/family/info', options: { method: 'GET' } },
    { pathname: '/api/family/info', options: { method: 'PUT', data: { familyName: '林家', note: null } } },
    { pathname: '/api/family/owner-candidates', options: { method: 'GET' } },
    { pathname: '/api/family/owner', options: { method: 'PUT', data: { targetMemberId: 12 } } }
  ]);
});

test('profile snapshot and validation normalize only editable fields', () => {
  assert.deepEqual(createProfileSnapshot({ familyName: ' 林家 ', note: null, merchantId: 9 }), {
    familyName: ' 林家 ',
    note: ''
  });
  assert.deepEqual(validateFamilyProfile({ familyName: '  林家新桌 ', note: '  周末聚餐  ' }), {
    valid: true,
    payload: { familyName: '林家新桌', note: '周末聚餐' },
    message: ''
  });
  assert.equal(validateFamilyProfile({ familyName: '   ', note: '' }).valid, false);
});

test('candidate labels use privacy-safe suffixes and stable sequence fallback', () => {
  const options = buildOwnerOptions([
    { memberId: 11, displayName: '小林', phoneSuffix: '2318' },
    { memberId: 12, displayName: '小林', phoneSuffix: '6621' },
    { memberId: 13, displayName: '阿禾', phoneSuffix: null },
    { memberId: 14, displayName: '阿禾', phoneSuffix: null }
  ]);
  assert.deepEqual(options.map((item) => item.label), [
    '小林（尾号 2318）',
    '小林（尾号 6621）',
    '阿禾（成员 1）',
    '阿禾（成员 2）'
  ]);
  assert.ok(options.every((item) => !item.label.includes(String(item.memberId))));
});

test('default address and current owner label reuse existing local data', () => {
  assert.deepEqual(findDefaultAddress([
    { addressId: 1, defaultAddress: false },
    { addressId: 2, defaultAddress: true, contactName: '林女士' }
  ]), { addressId: 2, defaultAddress: true, contactName: '林女士' });
  assert.equal(currentOwnerLabel({ nickname: '小林', username: 'lin' }), '当前负责人：我 · 小林');
  assert.equal(currentOwnerLabel({ username: 'lin' }), '当前负责人：我 · lin');
});

test('owner transfer session downgrade is fail closed and authority merge is complete', () => {
  const session = {
    userId: 2,
    familyId: 8,
    memberId: 2,
    familyRole: 'OWNER',
    roleTemplate: 'owner',
    permissionCodes: ['FAMILY_MEMBER', 'FAMILY_ADMIN'],
    availableModes: ['family']
  };
  const closed = failClosedOwnerSession(session);
  assert.equal(closed.familyRole, 'MEMBER');
  assert.equal(closed.roleTemplate, 'member');
  assert.deepEqual(closed.permissionCodes, ['FAMILY_MEMBER']);

  const merged = mergeIdentityContext(closed, {
    userId: 2,
    familyId: 8,
    familyRole: 'MEMBER',
    merchantId: null,
    availableModes: ['family'],
    permissionCodes: ['FAMILY_MEMBER']
  });
  assert.equal(merged.memberId, 2);
  assert.equal(merged.roleTemplate, 'member');
  assert.deepEqual(merged.permissionCodes, ['FAMILY_MEMBER']);
});
