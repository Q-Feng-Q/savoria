const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('mini login has one permission-driven entry without account type controls', () => {
  const page = fs.readFileSync(path.resolve(__dirname, '..', 'pages', 'auth', 'entry', 'index.js'), 'utf8');
  const template = fs.readFileSync(path.resolve(__dirname, '..', 'pages', 'auth', 'entry', 'index.wxml'), 'utf8');
  assert.match(page, /loginWithPermissionFallback\(runtime\.auth/);
  assert.doesNotMatch(`${page}\n${template}`, /accountType|家庭账号|商户账号/);
});
