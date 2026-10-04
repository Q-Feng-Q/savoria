const test = require('node:test');
const assert = require('node:assert/strict');
const { createSessionStore } = require('../utils/session');
const { renewSession } = require('../utils/session-renewal');

function store() {
  const values = new Map();
  return createSessionStore({ storage: {
    get: (key) => values.get(key) || null,
    set: (key, value) => values.set(key, value),
    remove: (key) => values.delete(key)
  } });
}

test('concurrent renewal rotates once and keeps the account and selected mode', async () => {
  const sessionStore = store();
  sessionStore.setSession({ userId: 7, accessToken: 'old', refreshToken: 'old-refresh',
    activeMode: 'merchant', availableModes: ['family', 'merchant'] });
  let resolveRefresh;
  let calls = 0;
  const refresh = () => { calls += 1; return new Promise((resolve) => { resolveRefresh = resolve; }); };
  const first = renewSession(sessionStore, refresh);
  const second = renewSession(sessionStore, refresh);
  resolveRefresh({ userId: 7, accessToken: 'new', refreshToken: 'new-refresh' });

  assert.equal(await first, true);
  assert.equal(await second, true);
  assert.equal(calls, 1);
  assert.equal(sessionStore.getSession().accessToken, 'new');
  assert.equal(sessionStore.getSession().activeMode, 'merchant');
});

test('renewal cannot overwrite a different account selected while the request is pending', async () => {
  const sessionStore = store();
  sessionStore.setSession({ userId: 7, accessToken: 'old', refreshToken: 'old-refresh' });
  let resolveRefresh;
  const pending = renewSession(sessionStore, () => new Promise((resolve) => { resolveRefresh = resolve; }));
  sessionStore.setSession({ userId: 8, accessToken: 'other', refreshToken: 'other-refresh' });
  resolveRefresh({ userId: 7, accessToken: 'new', refreshToken: 'new-refresh' });

  assert.equal(await pending, false);
  assert.equal(sessionStore.getSession().userId, 8);
});

test('account B can renew while account A renewal is still pending', async () => {
  const sessionStore = store();
  sessionStore.setSession({ userId: 7, accessToken: 'old-A', refreshToken: 'refresh-A' });
  let resolveA;
  const pendingA = renewSession(sessionStore, () => new Promise((resolve) => { resolveA = resolve; }));
  sessionStore.setSession({ userId: 8, accessToken: 'old-B', refreshToken: 'refresh-B' });
  const pendingB = renewSession(sessionStore, async () => ({
    userId: 8, accessToken: 'new-B', refreshToken: 'new-refresh-B'
  }));
  resolveA({ userId: 7, accessToken: 'new-A', refreshToken: 'new-refresh-A' });
  assert.equal(await pendingA, false);
  assert.equal(await pendingB, true);
  assert.equal(sessionStore.getSession().accessToken, 'new-B');
});
