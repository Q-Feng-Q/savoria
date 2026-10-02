const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const {
  createCloudImageResolver,
  isCloudContainerAsset,
  normalizeCloudAssetPath
} = require('../utils/cloud-image');

test('cloud image path detection only captures backend-owned image paths', () => {
  const base = 'https://prod-env.service.tcloudbase.com';
  assert.equal(isCloudContainerAsset('/images/dish-templates/dish-001.jpg', base), true);
  assert.equal(isCloudContainerAsset('/uploads/images/dish.jpg', base), true);
  assert.equal(isCloudContainerAsset(`${base}/images/dish-templates/dish-001.jpg`, base), true);
  assert.equal(isCloudContainerAsset('/assets/brand/dish-placeholder.png', base), false);
  assert.equal(isCloudContainerAsset('https://images.example/dish.jpg', base), false);
  assert.equal(normalizeCloudAssetPath(`${base}/images/dish.jpg`, base), '/images/dish.jpg');
});

test('cloud image resolver downloads through callContainer and reuses the local cache', async () => {
  const calls = [];
  const writes = [];
  const directories = [];
  const resolver = createCloudImageResolver({
    env: 'prod-env',
    service: 'springboot-service',
    assetBaseUrl: 'https://prod-env.service.tcloudbase.com',
    userDataPath: '/user-data',
    callContainer: async (options) => {
      calls.push(options);
      return {
        statusCode: 200,
        data: new Uint8Array([1, 2, 3]).buffer,
        header: { 'content-type': 'image/jpeg' }
      };
    },
    fileSystem: {
      mkdir: ({ dirPath, success }) => { directories.push(dirPath); success(); },
      writeFile: ({ filePath, data, success }) => { writes.push({ filePath, data }); success(); }
    }
  });

  const first = await resolver('/images/dish-templates/dish-001.jpg');
  const second = await resolver('/images/dish-templates/dish-001.jpg');

  assert.equal(first, second);
  assert.match(first, /^\/user-data\/cloud-images\/[a-f0-9]+\.jpg$/);
  assert.deepEqual(directories, ['/user-data/cloud-images']);
  assert.equal(writes.length, 1);
  assert.equal(calls.length, 1);
  assert.equal(calls[0].path, '/images/dish-templates/dish-001.jpg');
  assert.equal(calls[0].responseType, 'arraybuffer');
  assert.equal(calls[0].header['X-WX-SERVICE'], 'springboot-service');
});

test('cloud image component is globally registered and replaces backend image surfaces', () => {
  const app = JSON.parse(read('app.json'));
  assert.equal(app.usingComponents['cloud-image'], '/components/cloud-image/index');
  assert.match(read('components/cloud-image/index.wxml'), /warm-cloud-image/);
  assert.match(read('components/cloud-image/index.js'), /createCloudImageResolver/);

  for (const file of [
    'components/brand-logo/index.wxml',
    'components/dish-row/index.wxml',
    'components/order-row/index.wxml',
    'components/step-images/index.wxml',
    'pages/family/home/index.wxml',
    'pages/ordering/dish-detail/index.wxml',
    'pages/merchant/dish-templates/index.wxml',
    'pages/merchant/dish-template-detail/index.wxml',
    'pages/merchant/merchant-dishes/index.wxml',
    'pages/merchant/dish-edit/index.wxml',
    'pages/merchant/dish-template-change-edit/index.wxml'
  ]) {
    assert.match(read(file), /<cloud-image\b/, `${file} must use cloud-image`);
  }
});
