const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = (file) => fs.readFileSync(path.resolve(__dirname, '..', file), 'utf8');

test('personal center links to conventional profile and account security pages', () => {
  const app = JSON.parse(read('app.json'));
  assert.ok(app.pages.includes('pages/account/profile-edit/index'));
  assert.ok(app.pages.includes('pages/account/account-security/index'));
  const profile = read('pages/account/profile/index.wxml');
  assert.match(profile, /编辑资料/);
  assert.match(profile, /账户与安全/);
});

test('account security honors platform switches and supports password and bindings', () => {
  const page = read('pages/account/account-security/index.wxml');
  const logic = read('pages/account/account-security/index.js');
  assert.match(page, /emailBindingEnabled/);
  assert.match(page, /mobileBindingEnabled/);
  assert.match(page, /wechatBindingEnabled/);
  assert.match(logic, /changePassword/);
  assert.match(logic, /bindEmail/);
  assert.match(logic, /bindWechat/);
});

