const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const source = (name) => fs.readFileSync(path.join(__dirname, '../src', name), 'utf8');
const helper = async () => import(`data:text/javascript;base64,${Buffer.from(source('utils/branding.js')).toString('base64')}`);

test('branding defaults, legacy name and bounded integer sizes', async () => {
  const { normalizeBranding } = await helper();
  assert.equal(normalizeBranding().siteName, '食光栀味');
  assert.equal(normalizeBranding({ siteName: '食光知味' }).siteName, '食光知味');
  assert.equal(normalizeBranding({ siteName: '自己的厨房' }).siteName, '自己的厨房');
  const value = normalizeBranding({ siteLogoSmallSize: 999, siteLogoSize: null, siteLogoLargeSize: 0 });
  assert.deepEqual([value.siteLogoSmallSize, value.siteLogoSize, value.siteLogoLargeSize], [64, 56, 48]);
});
test('safe URL rules and backend root-relative resolution', async () => {
  const { isBrandUrl, resolveBrandUrl, resolveLogo } = await helper();
  for (const url of ['//host/a', '/a\\b', 'http://host/a', 'data:x', 'https://u:p@host/a', '/a b', '/a\u007f', 'https:///a', '/'.repeat(501)]) assert.equal(isBrandUrl(url), false, url);
  assert.equal(isBrandUrl('https://cdn.example/a.png'), true);
  assert.equal(resolveBrandUrl('/uploads/a.png', 'https://api.example/api'), 'https://api.example/uploads/a.png');
  assert.equal(resolveBrandUrl('/uploads/a.png', '/api'), '/api/uploads/a.png');
  assert.equal(resolveLogo({ siteLogoUrl: '/logo.png' }, 'small', '/api').src, '/api/logo.png');
  assert.equal(resolveLogo({}, 'large').fallback, '/brand/logo-256.png');
});
test('brand-only payload validates without leaking settings and reset is image-only', async () => {
  const { brandPayload, resetBrandImages } = await helper();
  const draft = { siteName: '我的厨房', smtpPassword: 'secret', maintenanceEnabled: true };
  assert.equal(brandPayload(draft).smtpPassword, undefined);
  assert.throws(() => brandPayload({ ...draft, siteLogoSize: 1 }), /尺寸/);
  assert.throws(() => brandPayload({ ...draft, siteLogoUrl: 'javascript:x' }), /地址/);
  assert.equal(resetBrandImages(draft).siteName, '我的厨房');
  assert.equal(resetBrandImages(draft).siteLogoSize, 56);
});
test('document title and favicon fail once to local', async () => {
  const { applyBrandDocument } = await helper();
  const link = { href: '' };
  const doc = { querySelector: () => link };
  applyBrandDocument(doc, { siteName: '新厨房', siteFaviconUrl: 'https://cdn.example/x.ico' }, '订单');
  assert.equal(doc.title, '订单 · 新厨房');
  link.onerror();
  assert.equal(link.href, '/favicon.ico');
  assert.equal(link.onerror, null);
});
test('web consumers and independent settings save are wired', () => {
  assert.match(source('components/BrandLogo.vue'), /object-fit: contain/);
  assert.match(source('components/BrandSettings.vue'), /updateBrandingSettings/);
  assert.match(source('components/BrandSettings.vue'), /brandPayload/);
  assert.match(source('views/auth/LoginView.vue'), /BrandLogo variant="large"/);
  assert.match(source('layouts/AdminLayout.vue'), /BrandLogo variant="small"/);
  assert.match(source('stores/branding.js'), /credentials: 'omit'/);
  assert.match(source('main.js'), /loadPublicBranding\(\)/);
});
