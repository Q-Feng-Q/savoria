const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const {
  DEFAULT_API_PROXY_BASE_URL,
  DEFAULT_EXTERNAL_CONFIG_PATH,
  readExternalConfigFile,
  resolveAdminApiBaseUrl,
  resolveAdminProxyTarget
} = require('../runtime-config');

test('resolveAdminApiBaseUrl prefers proxy base url for localhost dev pages', () => {
  assert.equal(
    resolveAdminApiBaseUrl({
      hostname: '127.0.0.1',
      storedBaseUrl: '',
      envBaseUrl: ''
    }),
    DEFAULT_API_PROXY_BASE_URL
  );
});

test('resolveAdminApiBaseUrl keeps explicit configured backend url', () => {
  assert.equal(
    resolveAdminApiBaseUrl({
      hostname: '127.0.0.1',
      storedBaseUrl: 'http://127.0.0.1:8088/',
      envBaseUrl: ''
    }),
    'http://127.0.0.1:8088'
  );
});

test('resolveAdminApiBaseUrl normalizes host values missing http protocol', () => {
  assert.equal(
    resolveAdminApiBaseUrl({
      hostname: 'kitchen.example.com',
      protocol: 'https:',
      storedBaseUrl: '127.0.0.1:8088/api',
      envBaseUrl: ''
    }),
    'http://127.0.0.1:8088/api'
  );
});

test('resolveAdminApiBaseUrl prefers proxy base url for local browser pages even when backend host is configured', () => {
  assert.equal(
    resolveAdminApiBaseUrl({
      hostname: '127.0.0.1',
      protocol: 'http:',
      storedBaseUrl: '',
      envBaseUrl: '',
      externalConfig: {
        apiBaseUrl: 'http://127.0.0.1:8088/',
        devProxyTarget: 'http://127.0.0.1:8088/'
      }
    }),
    DEFAULT_API_PROXY_BASE_URL
  );
});

test('resolveAdminApiBaseUrl falls back to same-origin api in non-dev browser too', () => {
  assert.equal(
    resolveAdminApiBaseUrl({
      hostname: 'kitchen.example.com',
      storedBaseUrl: '',
      envBaseUrl: ''
    }),
    DEFAULT_API_PROXY_BASE_URL
  );
});

test('resolveAdminApiBaseUrl falls back to configured proxy target for local file previews', () => {
  assert.equal(
    resolveAdminApiBaseUrl({
      protocol: 'file:',
      hostname: '',
      storedBaseUrl: '',
      envBaseUrl: '',
      externalConfig: {
        devProxyTarget: 'http://10.0.0.8:8080/'
      }
    }),
    'http://10.0.0.8:8080'
  );
});

test('resolveAdminApiBaseUrl can opt out of proxy mode in local browser pages', () => {
  assert.equal(
    resolveAdminApiBaseUrl({
      hostname: '127.0.0.1',
      protocol: 'http:',
      storedBaseUrl: '',
      envBaseUrl: '',
      externalConfig: {
        apiBaseUrl: 'http://127.0.0.1:8088/',
        preferProxyInDev: false
      }
    }),
    'http://127.0.0.1:8088'
  );
});

test('resolveAdminProxyTarget prefers env target and trims trailing slashes', () => {
  assert.equal(
    resolveAdminProxyTarget('http://10.0.0.8:8080/'),
    'http://10.0.0.8:8080'
  );
  assert.equal(
    resolveAdminProxyTarget('', {
      externalConfig: {
        devProxyTarget: 'http://192.168.1.50:8080/'
      }
    }),
    'http://192.168.1.50:8080'
  );
});

test('readExternalConfigFile loads backend settings from json file', () => {
  const tempPath = path.join(os.tmpdir(), `kitchen-backend-config-${Date.now()}.json`);
  fs.writeFileSync(tempPath, JSON.stringify({
    apiBaseUrl: '/api',
    devProxyTarget: 'http://127.0.0.1:8080/',
    preferProxyInDev: true
  }), 'utf8');

  try {
    assert.deepEqual(readExternalConfigFile(tempPath), {
      apiBaseUrl: '/api',
      devProxyTarget: 'http://127.0.0.1:8080',
      preferProxyInDev: true
    });
  } finally {
    fs.unlinkSync(tempPath);
  }
});

test('default external config path points to the public json config', () => {
  assert.equal(DEFAULT_EXTERNAL_CONFIG_PATH, 'public/backend.config.json');
});
