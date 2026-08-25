const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');

test('merchant profile edit page is registered and uses custom responsive controls', () => {
  const app = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));
  const markup = fs.readFileSync(path.join(root, 'pages/merchant/merchant-profile-edit/index.wxml'), 'utf8');
  const styles = fs.readFileSync(path.join(root, 'pages/merchant/merchant-profile-edit/index.wxss'), 'utf8');
  assert.ok(app.pages.includes('pages/merchant/merchant-profile-edit/index'));
  assert.doesNotMatch(markup, /<button\b/i);
  assert.match(markup, /data-field="name"/);
  assert.match(markup, /data-field="contactName"/);
  assert.match(markup, /data-field="contactPhone"/);
  assert.match(markup, /bindtap="save"/);
  assert.match(markup, /bindtap="cancel"/);
  assert.match(markup, /aria-disabled="\{\{saving \|\| completed/);
  assert.match(markup, /aria-busy="\{\{saving\}\}"/);
  assert.match(markup, /bind:retry="retryLoad"/);
  assert.match(styles, /min-height:\s*88rpx/);
  assert.match(styles, /@media\s*\(max-width:\s*360px\)/);
  assert.match(styles, /merchant-profile-cancel[^}]*min-height:\s*88rpx/);
});

test('merchant workbench exposes merchant-only profile editing entry', () => {
  const merchant = fs.readFileSync(path.join(root, 'pages/merchant/index.wxml'), 'utf8');
  const family = fs.readFileSync(path.join(root, 'pages/family/family-management/index.wxml'), 'utf8');
  assert.match(merchant, /bindtap="openMerchantProfile"/);
  assert.doesNotMatch(family, /merchant-profile-edit/);
});
