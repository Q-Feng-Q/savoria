const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = (file) => fs.readFileSync(path.resolve(__dirname, '..', file), 'utf8');

test('mini program registers a dedicated family start page', () => {
  const app = JSON.parse(read('app.json'));
  assert.ok(app.pages.includes('pages/family/family-start/index'));
  assert.match(read('pages/account/profile/index.wxml'), /开始设置家庭/);
  assert.match(read('pages/account/profile/index.js'), /openFamilyStart/);
});

test('family start page uses a private merchant invitation or joint merchant creation without exposing merchant list', () => {
  const page = read('pages/family/family-start/index.wxml');
  const logic = read('pages/family/family-start/index.js');
  assert.match(page, /申请创建家庭/);
  assert.match(page, /加入已有家庭/);
  assert.doesNotMatch(page, /<picker/);
  assert.doesNotMatch(logic, /merchantOptions/);
  assert.match(page, /商户邀请码/);
  assert.match(page, /创建新私厨/);
  assert.match(logic, /merchantMode/);
  assert.match(logic, /JOINT_CREATE/);
  assert.match(logic, /newMerchantName/);
  assert.match(logic, /getOnboarding/);
  assert.match(logic, /applyFamily/);
  assert.match(logic, /joinFamily/);
});
