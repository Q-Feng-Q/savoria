const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
function branding() {
  assert.ok(fs.existsSync(path.join(root, 'utils/branding.js')), 'shared branding runtime must exist');
  return require('../utils/branding');
}

test('system-configured names are preserved, only missing names use fallback', () => {
  const { normalizeBranding } = branding();
  const value = normalizeBranding({ siteName: '食光知味' });
  assert.equal(value.siteName, '食光知味');
  assert.deepEqual([value.siteLogoSmallSize, value.siteLogoSize, value.siteLogoLargeSize], [32, 56, 96]);
  assert.equal(normalizeBranding({ siteName: '  祁家小厨  ' }).siteName, '祁家小厨');
});

test('display sizes clamp valid integers and reject malformed values', () => {
  const { normalizeBranding } = branding();
  const value = normalizeBranding({ siteLogoSmallSize: 1000, siteLogoSize: -10, siteLogoLargeSize: 96.3 });
  assert.deepEqual([value.siteLogoSmallSize, value.siteLogoSize, value.siteLogoLargeSize], [64, 24, 96]);
  assert.equal(normalizeBranding({ siteLogoSize: '100px' }).siteLogoSize, 56);
});

test('unsafe URLs are rejected while HTTPS and backend-relative images work', () => {
  const { isSafeBrandUrl, resolveBrandUrl } = branding();
  for (const url of ['javascript:alert(1)', 'data:image/png,x', 'file:///x', '//evil.test/x', '/\\evil.test/x', 'https://u:p@a.test/x', '/x\n', 'http://a.test/x']) {
    assert.equal(isSafeBrandUrl(url), false, url);
  }
  assert.equal(resolveBrandUrl('/uploads/a.png', 'https://food.test/api'), 'https://food.test/uploads/a.png');
  assert.equal(resolveBrandUrl('https://cdn.test/a.png', 'http://127.0.0.1:8080'), 'https://cdn.test/a.png');
  assert.equal(resolveBrandUrl('/uploads/a.png', 'http://127.0.0.1:8080'), 'http://127.0.0.1:8080/uploads/a.png');
});

test('logo selection falls back from variant to standard to local and never stretches', () => {
  const { normalizeBranding, resolveLogo } = branding();
  const brand = normalizeBranding({ siteLogoUrl: '/uploads/standard.png', siteLogoLargeUrl: 'https://cdn.test/large.png' });
  assert.equal(resolveLogo(brand, 'small', 'https://food.test/api').src, 'https://food.test/uploads/standard.png');
  assert.equal(resolveLogo(brand, 'large').src, 'https://cdn.test/large.png');
  assert.equal(resolveLogo(brand, 'large').sizeRpx, 192);
  assert.equal(resolveLogo(normalizeBranding(), 'standard').src, '/assets/brand/logo/logo-128.png');
});

test('brand store deduplicates requests, broadcasts updates and unsubscribes', async () => {
  const { createBrandStore } = branding();
  let calls = 0;
  let now = 0;
  let release;
  const store = createBrandStore({ baseUrl: () => 'https://food.test', now: () => now,
    load: () => { calls++; return new Promise(resolve => { release = resolve; }); } });
  let changes = 0;
  const unsubscribe = store.subscribe(() => { changes++; });
  const first = store.refresh();
  const second = store.refresh();
  await Promise.resolve();
  assert.equal(calls, 1);
  release({ siteName: '新的餐桌' });
  await Promise.all([first, second]);
  assert.equal(store.get().siteName, '新的餐桌');
  assert.equal(changes, 2); // Initial value and successful refresh.
  await store.refresh();
  assert.equal(calls, 1);
  unsubscribe();
  now = 31000;
  const next = store.refresh();
  await Promise.resolve();
  release({ siteName: '另一张餐桌' });
  await next;
  assert.equal(changes, 2);
});

test('failed refresh retains valid cache and does not poison another API origin', async () => {
  const { createBrandStore } = branding();
  let origin = 'https://one.test';
  const storage = new Map();
  const store = createBrandStore({ baseUrl: () => origin, storage: {
    get: key => storage.get(key), set: (key, value) => storage.set(key, value)
  }, load: async () => ({ siteName: '第一家' }) });
  await store.refresh();
  const cached = createBrandStore({ baseUrl: () => origin, storage: {
    get: key => storage.get(key), set() {}
  }, load: async () => { throw new Error('offline'); } });
  assert.equal(cached.get().siteName, '第一家');
  await cached.refresh();
  assert.equal(cached.get().siteName, '第一家');
  origin = 'https://two.test';
  assert.equal(cached.get().siteName, '食光栀味');
});

test('response from previous backend cannot overwrite new origin branding', async () => {
  const { createBrandStore } = branding();
  let origin = 'https://one.test';
  const releases = {};
  const store = createBrandStore({ baseUrl: () => origin, load: base => new Promise(resolve => { releases[base] = resolve; }) });
  const old = store.refresh();
  await Promise.resolve();
  origin = 'https://two.test';
  const current = store.refresh();
  await Promise.resolve();
  releases[origin]({ siteName: '第二家' });
  await current;
  releases['https://one.test']({ siteName: '第一家' });
  await old;
  assert.equal(store.get().siteName, '第二家');
});

test('Logo component binds aspectFit and falls back remote/local/text without a loop', () => {
  branding();
  const file = path.join(root, 'components/brand-logo/index.js');
  assert.ok(fs.existsSync(file), 'brand-logo component must exist');
  let definition;
  let callback;
  let unsubscribed = false;
  const helpers = branding();
  vm.runInNewContext(fs.readFileSync(file, 'utf8'), { Component: value => { definition = value; }, require: () => ({ ...helpers, brandStore: {
    get: () => helpers.normalizeBranding({ siteLogoUrl: 'https://cdn.test/a.png' }),
    baseUrl: () => '', refresh: () => Promise.resolve(),
    subscribe: fn => { callback = fn; fn(helpers.normalizeBranding({ siteLogoUrl: 'https://cdn.test/a.png' })); return () => { unsubscribed = true; }; }
  } }) });
  const instance = { properties: { size: 'standard', showName: true }, data: {}, setData(value) { Object.assign(this.data, value); }, ...definition.methods };
  definition.lifetimes.attached.call(instance);
  assert.equal(instance.data.logoSrc, 'https://cdn.test/a.png');
  instance.onImageError();
  assert.equal(instance.data.logoSrc, '/assets/brand/logo/logo-128.png');
  instance.onImageError();
  assert.equal(instance.data.logoSrc, '');
  instance.onImageError();
  assert.equal(instance.data.logoSrc, '');
  callback(helpers.normalizeBranding({ siteName: '新家', siteLogoUrl: 'https://cdn.test/b.png' }));
  assert.equal(instance.data.logoSrc, 'https://cdn.test/b.png');
  definition.lifetimes.detached.call(instance);
  assert.equal(unsubscribed, true);
  assert.match(fs.readFileSync(path.join(root, 'components/brand-logo/index.wxml'), 'utf8'), /mode="aspectFit"/);
});

test('all approved brand entrances use the shared component and packaged assets', () => {
  const config = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));
  assert.equal(config.window.navigationBarTitleText, branding().BRAND_NAME); // Offline bootstrap only.
  assert.equal(config.usingComponents['brand-logo'], '/components/brand-logo/index');
  for (const [page, size] of [['auth/entry', 'large'], ['auth/register', 'large'], ['family/home', 'standard'], ['ordering/cart', 'standard'], ['account/profile', 'small'], ['merchant', 'small']]) {
    const file = page === 'merchant' ? 'pages/merchant/index.wxml' : `pages/${page}/index.wxml`;
    assert.match(fs.readFileSync(path.join(root, file), 'utf8'), new RegExp(`<brand-logo[^>]+size="${size}"`), page);
  }
  for (const size of [64, 128, 256]) assert.ok(fs.statSync(path.join(root, `assets/brand/logo/logo-${size}.png`)).size > 0);
});

test('public settings URL follows the mini program backend prefix convention without credentials', async () => {
  const { loadPublicBranding } = branding();
  const calls = [];
  global.wx = { request(options) { calls.push(options); options.success({ statusCode: 200, data: { code: 0, data: { siteName: '系统里的名称' } } }); } };
  assert.equal((await loadPublicBranding('http://127.0.0.1:8080')).siteName, '系统里的名称');
  await loadPublicBranding('https://food.test/api');
  assert.deepEqual(calls.map(call => call.url), ['http://127.0.0.1:8080/public/system-settings', 'https://food.test/api/public/system-settings']);
  assert.equal(calls[0].header, undefined);
  delete global.wx;
});

test('page binding uses system name, refreshes on show, preserves lifecycle and stops hidden updates', async () => {
  const { withBranding } = branding();
  let current = { siteName: '配置名称甲' }, listener, refreshes = 0, originalShows = 0;
  const store = { get: () => current, subscribe(fn) { listener = fn; fn(current); return () => { listener = null; }; }, refresh: async () => { refreshes++; } };
  const titles = [];
  global.wx = { setNavigationBarTitle: ({ title }) => titles.push(title) };
  const def = withBranding({ data: { value: 1 }, onShow() { originalShows++; } }, { store, navigationTitle: true });
  const instance = { ...def, data: { ...def.data }, setData(value) { Object.assign(this.data, value); } };
  instance.onShow();
  assert.equal(instance.data.brandName, '配置名称甲');
  current = { siteName: '食光知味' }; listener(current);
  assert.equal(instance.data.brandName, '食光知味');
  assert.equal(titles.at(-1), '食光知味');
  instance.onHide(); assert.equal(listener, null);
  current = { siteName: '配置名称乙' }; instance.onShow();
  assert.equal(instance.data.brandName, '配置名称乙');
  assert.equal(originalShows, 2); assert.equal(refreshes, 2);
  instance.onUnload(); assert.equal(listener, null);
  delete global.wx;
});
