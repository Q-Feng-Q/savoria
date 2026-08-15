const test = require('node:test');
const assert = require('node:assert/strict');

const {
  createApiRuntime,
  isApiSession,
  resolveNotificationScope
} = require('../utils/api-runtime');

test('isApiSession only enables live mode for api sessions', () => {
  assert.equal(isApiSession(null), false);
  assert.equal(isApiSession({ loginMode: 'dev' }), false);
  assert.equal(isApiSession({ loginMode: 'api' }), true);
});

test('resolveNotificationScope follows role template', () => {
  assert.equal(resolveNotificationScope({ roleTemplate: 'member' }), 'account');
  assert.equal(resolveNotificationScope({ roleTemplate: 'merchant_admin' }), 'merchant');
  assert.equal(resolveNotificationScope(null), 'account');
});

test('createApiRuntime wires services to app base url and session headers', async () => {
  let capturedOptions = null;
  const session = {
    loginMode: 'api',
    accessToken: 'token-live',
    userId: 1,
    merchantId: 2,
    familyId: 3,
    memberId: 4,
    roleTemplate: 'member',
    backendRoles: []
  };

  const runtime = createApiRuntime({
    app: {
      globalData: {
        apiBaseUrl: 'http://127.0.0.1:8080',
        sessionStore: {
          getSession: () => session,
          getToken: () => session.accessToken
        }
      }
    },
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

  const result = await runtime.orders.listOrders();

  assert.deepEqual(result, []);
  assert.equal(runtime.baseUrl, 'http://127.0.0.1:8080');
  assert.equal(capturedOptions.url, 'http://127.0.0.1:8080/family/orders');
  assert.equal(capturedOptions.header.Authorization, 'Bearer token-live');
  assert.equal(capturedOptions.header['X-Family-Id'], 3);
  assert.equal(capturedOptions.header['X-Member-Id'], 4);
});

test('createApiRuntime gives file uploads the current token and stable identity headers', async () => {
  let uploadOptions;
  const session = { accessToken: 'upload-token', userId: 9, merchantId: 12, roleTemplate: 'merchant_admin', backendRoles: ['merchant_admin'] };
  const runtime = createApiRuntime({
    baseUrl: 'https://kitchen.test',
    sessionStore: { getSession: () => session, getToken: () => session.accessToken },
    request: async () => ({ statusCode: 200, data: { code: 0, data: [] } }),
    upload: async (options) => {
      uploadOptions = options;
      return { statusCode: 200, data: JSON.stringify({ code: 0, data: { url: '/images/dish.png' } }) };
    }
  });
  await runtime.files.uploadImage('dish.png');
  assert.equal(uploadOptions.header.Authorization, 'Bearer upload-token');
  assert.equal(uploadOptions.header['X-User-Id'], 9);
  assert.equal(uploadOptions.header['X-Merchant-Id'], 12);
});
