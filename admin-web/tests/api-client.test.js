const test = require('node:test');
const assert = require('node:assert/strict');

const {
  buildAdminAuthHeaders,
  createAdminApiClient,
  joinApiUrl
} = require('../api-client');

test('buildAdminAuthHeaders maps backend session to merchant headers', () => {
  const headers = buildAdminAuthHeaders({
    accessToken: 'token-merchant',
    userId: 9,
    merchantId: 1,
    familyId: 2,
    memberId: 10,
    roleTemplate: 'merchant_admin',
    backendRoles: ['merchant_admin'],
    merchantAdminScopes: ['merchant']
  });

  assert.equal(headers.Authorization, 'Bearer token-merchant');
  assert.equal(headers['X-User-Id'], 9);
  assert.equal(headers['X-Merchant-Id'], 1);
  assert.equal(headers['X-Family-Id'], 2);
  assert.equal(headers['X-Member-Id'], 10);
  assert.equal(headers['X-Role-Template'], 'merchant_admin');
  assert.equal(headers['X-Backend-Roles'], 'merchant_admin');
  assert.equal(headers['X-Merchant-Admin-Scopes'], 'merchant');
});

test('admin api client unwraps successful backend responses', async () => {
  let capturedUrl = '';
  let capturedOptions = null;
  const client = createAdminApiClient({
    baseUrl: 'http://127.0.0.1:8080',
    getSession: () => ({
      accessToken: 'token-merchant',
      userId: 9,
      merchantId: 1,
      roleTemplate: 'merchant_admin',
      backendRoles: ['merchant_admin'],
      merchantAdminScopes: ['merchant']
    }),
    fetch: async (url, options) => {
      capturedUrl = url;
      capturedOptions = options;
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 0,
          message: 'ok',
          data: [{ orderId: 1 }]
        })
      };
    }
  });

  const data = await client('/api/merchant/orders');

  assert.equal(capturedUrl, 'http://127.0.0.1:8080/api/merchant/orders');
  assert.equal(capturedOptions.headers['X-Backend-Roles'], 'merchant_admin');
  assert.deepEqual(data, [{ orderId: 1 }]);
});

test('admin api client throws backend business errors', async () => {
  const client = createAdminApiClient({
    fetch: async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        code: 40301,
        message: 'No merchant backend access',
        data: null
      })
    })
  });

  await assert.rejects(
    () => client('/api/merchant/orders'),
    (error) => error.code === 40301 && error.message === 'No merchant backend access'
  );
});

test('admin api client reports expired or forbidden sessions', async () => {
  const events = [];
  const client = createAdminApiClient({
    onAuthError: (error) => events.push({ code: error.code, status: error.status }),
    fetch: async () => ({
      ok: false,
      status: 401,
      json: async () => ({ code: 40101, message: 'session expired', data: null })
    })
  });

  await assert.rejects(() => client('/api/merchant/orders'));
  assert.deepEqual(events, [{ code: 40101, status: 401 }]);
});

test('admin api client keeps a valid session on forbidden business responses', async () => {
  const events = [];
  const client = createAdminApiClient({
    onAuthError: (error) => events.push(error),
    fetch: async () => ({
      ok: false,
      status: 403,
      json: async () => ({ code: 40301, message: 'platform admin has no merchant context', data: null })
    })
  });
  await assert.rejects(() => client('/api/merchant/orders'), (error) => error.status === 403);
  assert.deepEqual(events, []);
});

test('joinApiUrl avoids duplicate api prefixes when base url already contains /api', () => {
  assert.equal(joinApiUrl('/api', '/api/auth/admin/login'), '/api/auth/admin/login');
  assert.equal(
    joinApiUrl('http://127.0.0.1:8080/api', '/api/merchant/orders'),
    'http://127.0.0.1:8080/api/merchant/orders'
  );
  assert.equal(
    joinApiUrl('http://127.0.0.1:8080', '/api/merchant/orders'),
    'http://127.0.0.1:8080/api/merchant/orders'
  );
});
