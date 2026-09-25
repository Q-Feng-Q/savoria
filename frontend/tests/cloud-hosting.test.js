const test = require('node:test');
const assert = require('node:assert/strict');

const {
  createCloudRequestAdapter,
  createCloudTransferAdapter,
  normalizeContainerPath,
  resolveCloudHostingConfig
} = require('../utils/cloud-hosting');

test('normalizeContainerPath removes the public gateway origin and keeps query strings', () => {
  assert.equal(normalizeContainerPath(
    'https://prod-demo.service.tcloudbase.com/family/cart?page=2',
    'https://prod-demo.service.tcloudbase.com'
  ), '/family/cart?page=2');
  assert.equal(normalizeContainerPath('/public/system-settings', ''), '/public/system-settings');
});

test('cloud request adapter routes existing request options through callContainer', async () => {
  let captured;
  const request = createCloudRequestAdapter({
    env: 'prod-d5g0vleyp9ed264bf',
    service: 'springboot-6dl0',
    assetBaseUrl: 'https://prod-d5g0vleyp9ed264bf.service.tcloudbase.com',
    callContainer: async (options) => {
      captured = options;
      return { statusCode: 200, data: { code: 0, data: 'ok' } };
    }
  });

  const response = await request({
    url: 'https://prod-d5g0vleyp9ed264bf.service.tcloudbase.com/family/cart',
    method: 'POST',
    header: { Authorization: 'Bearer token' },
    data: { dishId: 2 }
  });

  assert.equal(response.data.data, 'ok');
  assert.deepEqual(captured.config, { env: 'prod-d5g0vleyp9ed264bf' });
  assert.equal(captured.path, '/family/cart');
  assert.equal(captured.method, 'POST');
  assert.equal(captured.header.Authorization, 'Bearer token');
  assert.equal(captured.header['X-WX-SERVICE'], 'springboot-6dl0');
  assert.deepEqual(captured.data, { dishId: 2 });
});

test('cloud transfer adapter keeps multipart options and adds the service route header', async () => {
  let captured;
  const upload = createCloudTransferAdapter('uploadFile', {
    service: 'springboot-6dl0',
    transfer: async (options) => {
      captured = options;
      return { statusCode: 200, data: '{}' };
    }
  });

  await upload({
    url: 'https://prod.example/files/images',
    filePath: 'dish.png',
    name: 'file',
    header: { Authorization: 'Bearer token' }
  });

  assert.equal(captured.filePath, 'dish.png');
  assert.equal(captured.name, 'file');
  assert.equal(captured.header.Authorization, 'Bearer token');
  assert.equal(captured.header['X-WX-SERVICE'], 'springboot-6dl0');
});

test('resolveCloudHostingConfig only enables complete configurations', () => {
  assert.deepEqual(resolveCloudHostingConfig({ globalData: { cloudHosting: {
    enabled: true,
    env: 'prod-env',
    service: 'springboot-service',
    assetBaseUrl: 'https://prod-env.service.tcloudbase.com/'
  } } }), {
    enabled: true,
    env: 'prod-env',
    service: 'springboot-service',
    assetBaseUrl: 'https://prod-env.service.tcloudbase.com'
  });
  assert.equal(resolveCloudHostingConfig({ globalData: { cloudHosting: { enabled: true } } }).enabled, false);
});
