const test = require('node:test');
const assert = require('node:assert/strict');
const { evaluateLoginSession, resolveLoginMethod } = require('../utils/login-routing');

test('explicit permission codes take precedence over legacy merchant fields', () => {
  assert.equal(evaluateLoginSession({ merchantId: 2, permissionCodes: ['MERCHANT_ADMIN'] }, 'merchant').allowed, true);
  assert.equal(evaluateLoginSession({ merchantId: 2, roleTemplate: 'merchant_admin', permissionCodes: [] }, 'merchant').allowed, false);
  assert.equal(evaluateLoginSession({ permissionCodes: ['PLATFORM_ADMIN'] }, 'family').allowed, false);
});

test('portal login chooses destination only from returned permissions', () => {
  assert.deepEqual(evaluateLoginSession({ roleTemplate: 'member', memberId: 8, familyId: 3 }),
    { allowed: true, destination: 'family', message: '' });
  assert.deepEqual(evaluateLoginSession({ roleTemplate: 'merchant_admin', merchantId: 2 }),
    { allowed: true, destination: 'merchant', message: '' });
  assert.deepEqual(evaluateLoginSession({ roleTemplate: 'platform_admin', merchantId: null }),
    { allowed: false, destination: '', message: '平台管理员请前往 Web 管理后台登录' });
});

test('family login keeps an unbound user signed in and routes to personal center', () => {
  assert.equal(resolveLoginMethod('family'), 'userLogin');
  assert.deepEqual(evaluateLoginSession({ roleTemplate: 'member', memberId: 8, familyId: 3 }, 'family'),
    { allowed: true, destination: 'family', message: '' });
  assert.deepEqual(evaluateLoginSession({ roleTemplate: 'member', memberId: 8, familyId: null }, 'family'),
    { allowed: true, destination: 'personal', message: '' });
});

test('merchant login requires merchant context and sends platform admins to web', () => {
  assert.equal(resolveLoginMethod('merchant'), 'adminLogin');
  assert.deepEqual(evaluateLoginSession({ roleTemplate: 'merchant_admin', merchantId: 2 }, 'merchant'),
    { allowed: true, destination: 'merchant', message: '' });
  assert.deepEqual(evaluateLoginSession({ roleTemplate: 'platform_admin', backendRoles: ['merchant_admin'], merchantId: null }, 'merchant'),
    { allowed: false, destination: '', message: '平台管理员请前往 Web 管理后台登录' });
});
