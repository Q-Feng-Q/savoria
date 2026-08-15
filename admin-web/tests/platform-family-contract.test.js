const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const src = path.resolve(__dirname, '..', 'src');

test('platform family api exposes options, detail and mutations', () => {
  const source = fs.readFileSync(path.join(src, 'api', 'admin-families.js'), 'utf8');
  assert.match(source, /\/api\/admin\/families\/options/);
  assert.match(source, /updateAdminFamily/);
  assert.match(source, /disableAdminFamily/);
  assert.match(source, /updateAdminFamilyMember/);
  assert.match(source, /getAdminFamilyMenu/);
  assert.match(source, /adjustAdminFamilyMemberBalance/);
  assert.match(source, /updateAdminFamilyAddress/);
});

test('add member form selects a named family instead of typing an id', () => {
  const source = fs.readFileSync(path.join(src, 'views', 'platform', 'FamilyApplicationsView.vue'), 'utf8');
  assert.doesNotMatch(source, /type="number"[^>]*目标家庭ID/);
  assert.match(source, /<select v-model\.number="memberForm\.familyId"/);
  assert.match(source, /familyOptions/);
});

test('platform family center is routed and visible in navigation', () => {
  const router = fs.readFileSync(path.join(src, 'router', 'index.js'), 'utf8');
  const uiConfig = fs.readFileSync(path.resolve(__dirname, '..', 'ui-config.js'), 'utf8');
  assert.match(router, /PlatformFamiliesView/);
  assert.match(uiConfig, /platform-families/);
});
