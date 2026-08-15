const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const runtimeConfig = require('../runtime-config');

test('vite config proxies /api to real backend to avoid browser cors during dev', async () => {
  const viteConfigModule = await import('../vite.config.mjs');
  const viteConfig = viteConfigModule.default || viteConfigModule;
  const proxy = viteConfig.server && viteConfig.server.proxy ? viteConfig.server.proxy : {};
  const externalConfig = runtimeConfig.readExternalConfigFile(
    path.resolve(__dirname, '../public/backend.config.json')
  );

  assert.equal(proxy['/api'].target, externalConfig.devProxyTarget);
  assert.equal(proxy['/api'].changeOrigin, true);
  assert.equal(typeof proxy['/api'].rewrite, 'function');
  assert.equal(proxy['/api'].rewrite('/api/auth/admin/login'), '/auth/admin/login');
});
