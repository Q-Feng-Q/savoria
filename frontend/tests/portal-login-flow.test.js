const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { loginWithPermissionFallback } = require('../utils/portal-login-flow');

test('unified portal never retries the admin endpoint after unauthorized login', async () => {
  const calls = [];
  const expected = Object.assign(new Error('unauthorized'), { statusCode: 401 });
  const auth = {
    portalLogin: async () => { calls.push('portal'); throw expected; },
    adminLogin: async () => { calls.push('admin'); return { roleTemplate: 'platform_admin' }; }
  };
  await assert.rejects(() => loginWithPermissionFallback(auth, { username: 'user', password: 'wrong' }), expected);
  assert.deepEqual(calls, ['portal']);
});

test('unified portal does not mask non-authentication failures', async () => {
  let adminCalled = false;
  const expected = Object.assign(new Error('offline'), { statusCode: 502 });
  await assert.rejects(() => loginWithPermissionFallback({
    portalLogin: async () => { throw expected; },
    adminLogin: async () => { adminCalled = true; }
  }, {}), expected);
  assert.equal(adminCalled, false);
});

test('platform admin notice is a confirm-only modal', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/auth/entry/index.js'), 'utf8');
  assert.match(source, /wx\.showModal/);
  assert.match(source, /showCancel:\s*false/);
  assert.match(source, /confirmText:\s*['"]我知道了['"]/);
});

test('component WXSS avoids forbidden tag and attribute selectors', () => {
  const order = fs.readFileSync(path.resolve(__dirname, '../components/order-row/index.wxss'), 'utf8');
  const action = fs.readFileSync(path.resolve(__dirname, '../components/action-button/index.wxss'), 'utf8');
  assert.doesNotMatch(order, /__date\s+text/);
  assert.doesNotMatch(action, /\[disabled\]/);
});

