const test = require('node:test');
const assert = require('node:assert/strict');

const { buildAuthHeaders, createApiClient } = require('../utils/api');

test('api client attaches token and returns backend data payload', async () => {
  let capturedOptions = null;

  const request = createApiClient({
    baseUrl: 'http://127.0.0.1:3100',
    getToken: () => 'token-1',
    request: async (options) => {
      capturedOptions = options;
      return {
        statusCode: 200,
        data: {
          code: 0,
          message: 'ok',
          data: {
            status: 'ok'
          }
        }
      };
    }
  });

  const response = await request('/health', { method: 'GET' });

  assert.equal(capturedOptions.url, 'http://127.0.0.1:3100/health');
  assert.equal(capturedOptions.header.Authorization, 'Bearer token-1');
  assert.deepEqual(response, {
    code: 0,
    message: 'ok',
    data: {
      status: 'ok'
    }
  });
});

test('api client attaches stable backend identity headers from session', async () => {
  let capturedOptions = null;

  const request = createApiClient({
    baseUrl: 'http://127.0.0.1:8080',
    getSession: () => ({
      accessToken: 'token-2',
      userId: 9,
      merchantId: 1,
      familyId: 2,
      memberId: 10,
      roleTemplate: 'merchant_admin',
      backendRoles: ['merchant_admin'],
      merchantAdminScopes: ['merchant']
    }),
    request: async (options) => {
      capturedOptions = options;
      return {
        statusCode: 200,
        data: {
          code: 0,
          message: 'ok',
          data: []
        }
      };
    }
  });

  await request('/api/merchant/orders', { method: 'GET' });

  assert.equal(capturedOptions.url, 'http://127.0.0.1:8080/merchant/orders');
  assert.equal(capturedOptions.header.Authorization, 'Bearer token-2');
  assert.equal(capturedOptions.header['X-User-Id'], 9);
  assert.equal(capturedOptions.header['X-Merchant-Id'], 1);
  assert.equal(capturedOptions.header['X-Family-Id'], 2);
  assert.equal(capturedOptions.header['X-Member-Id'], 10);
  assert.equal(capturedOptions.header['X-Role-Template'], 'merchant_admin');
  assert.equal(capturedOptions.header['X-Backend-Roles'], 'merchant_admin');
  assert.equal(capturedOptions.header['X-Merchant-Admin-Scopes'], 'merchant');
});

test('buildAuthHeaders supports existing mini program demo actor sessions', () => {
  const headers = buildAuthHeaders({
    accessToken: 'token-3',
    roleTemplate: 'member',
    actor: {
      userId: 'user-demo',
      merchantId: 'merchant-hearth',
      familyId: 'family-chen',
      memberId: 'member-chen-mei'
    }
  });

  assert.equal(headers.Authorization, 'Bearer token-3');
  assert.equal(headers['X-User-Id'], 'user-demo');
  assert.equal(headers['X-Merchant-Id'], 'merchant-hearth');
  assert.equal(headers['X-Family-Id'], 'family-chen');
  assert.equal(headers['X-Member-Id'], 'member-chen-mei');
  assert.equal(headers['X-Role-Template'], 'member');
});

test('api client throws normalized errors for non-zero backend code', async () => {
  const request = createApiClient({
    getToken: () => '',
    request: async () => ({
      statusCode: 200,
      data: {
        code: 4001,
        message: 'invalid state',
        data: null
      }
    })
  });

  await assert.rejects(
    () => request('/demo', { method: 'GET' }),
    (error) => error.code === 4001 && error.message === 'invalid state'
  );
});
