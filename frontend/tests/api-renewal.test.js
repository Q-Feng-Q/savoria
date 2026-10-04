const test = require('node:test');
const assert = require('node:assert/strict');
const { createApiClient } = require('../utils/api');

test('api client renews once after unauthorized response and retries with the new token', async () => {
  let token = 'old';
  const seen = [];
  let renewals = 0;
  const request = createApiClient({
    getToken: () => token,
    request: async (options) => {
      seen.push(options.header.Authorization);
      return options.header.Authorization === 'Bearer old'
        ? { statusCode: 401, data: { code: 40101, message: 'expired' } }
        : { statusCode: 200, data: { code: 0, data: { ok: true } } };
    },
    refreshSession: async () => { renewals += 1; token = 'new'; return true; }
  });

  const result = await request('/api/notebook/events');

  assert.deepEqual(result.data, { ok: true });
  assert.deepEqual(seen, ['Bearer old', 'Bearer new']);
  assert.equal(renewals, 1);
});

test('api client never renews login failures or loops after a second unauthorized response', async () => {
  let calls = 0;
  let renewals = 0;
  const request = createApiClient({
    getToken: () => 'old',
    request: async () => { calls += 1; return { statusCode: 401, data: { code: 40101 } }; },
    refreshSession: async () => { renewals += 1; return true; }
  });

  await assert.rejects(() => request('/api/auth/login', { method: 'POST' }));
  assert.equal(renewals, 0);
  await assert.rejects(() => request('/api/notebook/events'));
  assert.equal(renewals, 1);
  assert.equal(calls, 3);
});

test('late unauthorized response cannot replay account A operation as account B', async () => {
  let session = { userId: 1, activeMode: 'family', accessToken: 'A' };
  let respond;
  let started;
  const requestStarted = new Promise((resolve) => { started = resolve; });
  const seen = [];
  let renewals = 0;
  const request = createApiClient({
    getSession: () => session,
    getToken: () => session.accessToken,
    request: async (options) => {
      seen.push(options.header.Authorization);
      if (seen.length === 1) return new Promise((resolve) => { respond = resolve; started(); });
      return { statusCode: 200, data: { code: 0, data: null } };
    },
    refreshSession: async () => { renewals += 1; return true; }
  });
  const pending = request('/api/notebook/events/1/records', { method: 'POST', data: { title: 'A' } });
  await requestStarted;
  session = { userId: 2, activeMode: 'family', accessToken: 'B' };
  respond({ statusCode: 401, data: { code: 40101, message: 'expired' } });

  await assert.rejects(pending, (error) => error.code === 'ACCOUNT_SWITCHED');
  assert.deepEqual(seen, ['Bearer A']);
  assert.equal(renewals, 0);
});

test('late successful response from account A is not returned to account B', async () => {
  let session = { userId: 1, activeMode: 'family', accessToken: 'A' };
  let respond;
  let started;
  const requestStarted = new Promise((resolve) => { started = resolve; });
  const request = createApiClient({
    getSession: () => session,
    getToken: () => session.accessToken,
    request: () => new Promise((resolve) => { respond = resolve; started(); })
  });
  const pending = request('/api/notebook/events');
  await requestStarted;
  session = { userId: 2, activeMode: 'family', accessToken: 'B' };
  respond({ statusCode: 200, data: { code: 0, data: ['private A'] } });
  await assert.rejects(pending, (error) => error.code === 'ACCOUNT_SWITCHED');
});

test('old request cannot replay after the same account logs out and back in', async () => {
  let session = { userId: 1, activeMode: 'family', accessToken: 'A',
    refreshToken: 'session-A.secret-1' };
  let respond;
  let started;
  let calls = 0;
  const requestStarted = new Promise((resolve) => { started = resolve; });
  const request = createApiClient({
    getSession: () => session,
    getToken: () => session.accessToken,
    request: () => {
      calls += 1;
      if (calls === 1) return new Promise((resolve) => { respond = resolve; started(); });
      return { statusCode: 200, data: { code: 0, data: null } };
    },
    refreshSession: async () => true
  });
  const pending = request('/api/notebook/events/1/records', { method: 'POST' });
  await requestStarted;
  session = { userId: 1, activeMode: 'family', accessToken: 'B',
    refreshToken: 'session-B.secret-2' };
  respond({ statusCode: 401, data: { code: 40101 } });
  await assert.rejects(pending, (error) => error.code === 'ACCOUNT_SWITCHED');
  assert.equal(calls, 1);
});

test('temporary refresh failure remains retryable instead of expiring the session', async () => {
  const refreshError = new Error('network unavailable');
  const request = createApiClient({
    getToken: () => 'old',
    request: async () => ({ statusCode: 401, data: { code: 40101 } }),
    refreshSession: async () => { throw refreshError; }
  });
  await assert.rejects(() => request('/api/notebook/events'), (error) => error === refreshError);
});

test('expired access token can renew before logout revokes the server session', async () => {
  let token = 'old';
  const seen = [];
  const request = createApiClient({
    getToken: () => token,
    request: async (options) => {
      seen.push(options.header.Authorization);
      return token === 'old'
        ? { statusCode: 401, data: { code: 40101 } }
        : { statusCode: 200, data: { code: 0, data: null } };
    },
    refreshSession: async () => { token = 'new'; return true; }
  });

  await request('/api/auth/logout', { method: 'POST' });
  assert.deepEqual(seen, ['Bearer old', 'Bearer new']);
});
