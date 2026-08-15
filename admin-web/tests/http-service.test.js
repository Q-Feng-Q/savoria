const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const vm = require('node:vm');
const esbuild = require('../node_modules/esbuild');
const runtimeConfig = require('../runtime-config');

async function loadHttpService(options = {}) {
  const result = await esbuild.build({
    entryPoints: [path.resolve(__dirname, '../src/api/http.js')],
    bundle: true,
    format: 'cjs',
    platform: 'browser',
    write: false,
    logLevel: 'silent'
  });

  const storage = options.storage || {
    getItem() {
      return null;
    },
    setItem() {},
    removeItem() {}
  };

  const defaultApiLibrary = {
    createAdminApiClient() {
      return () => ({});
    }
  };

  const sandbox = {
    module: { exports: {} },
    exports: {},
    require,
    console,
    localStorage: storage,
    window: {
      fetch: options.fetch || (async () => ({
        ok: true,
        status: 200,
        json: async () => ({ code: 0, message: 'ok', data: {} })
      })),
      location: options.location || {
        protocol: 'http:',
        hostname: '127.0.0.1'
      }
    },
    globalThis: {
      KitchenAdminRuntimeConfig: runtimeConfig,
      KitchenAdminApi: Object.prototype.hasOwnProperty.call(options, 'apiLibrary')
        ? options.apiLibrary
        : defaultApiLibrary
    }
  };

  vm.runInNewContext(result.outputFiles[0].text, sandbox);
  return {
    service: sandbox.module.exports,
    storage
  };
}

test('getStoredSession clears malformed session payloads instead of crashing the login page', async () => {
  const calls = [];
  const { service } = await loadHttpService({
    storage: {
      getItem(key) {
        return key === 'family_kitchen_admin_session' ? '{bad-json' : null;
      },
      setItem() {},
      removeItem(key) {
        calls.push(key);
      }
    }
  });

  assert.equal(service.getStoredSession(), null);
  assert.deepEqual(calls, ['family_kitchen_admin_session']);
});

test('request works without depending on a root-level global api-client script', async () => {
  const requests = [];
  const { service } = await loadHttpService({
    apiLibrary: undefined,
    fetch: async (url, options) => {
      requests.push({ url, options });
      return {
        ok: true,
        status: 200,
        json: async () => ({ code: 0, message: 'ok', data: { success: true } })
      };
    },
    storage: {
      getItem(key) {
        if (key === 'family_kitchen_admin_session') {
          return JSON.stringify({
            accessToken: 'token-123',
            userId: 9,
            merchantId: 12,
            roleTemplate: 'merchant_admin'
          });
        }
        return null;
      },
      setItem() {},
      removeItem() {}
    },
    location: {
      protocol: 'http:',
      hostname: '127.0.0.1'
    }
  });

  const result = await service.request('/merchant/orders', {
    method: 'GET'
  });

  assert.deepEqual(result, { success: true });
  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, '/api/merchant/orders');
  assert.equal(requests[0].options.headers.Authorization, 'Bearer token-123');
  assert.equal(requests[0].options.headers['X-Merchant-Id'], 12);
});

test('getApiBaseUrl rewrites stale host values without protocol from storage', async () => {
  const { service } = await loadHttpService({
    storage: {
      getItem(key) {
        if (key === 'family_kitchen_admin_base_url') {
          return '127.0.0.1:8080/api';
        }
        return null;
      },
      setItem() {},
      removeItem() {}
    },
    location: {
      protocol: 'https:',
      hostname: 'kitchen.example.com'
    }
  });

  assert.equal(service.getApiBaseUrl(), 'http://127.0.0.1:8080/api');
});
