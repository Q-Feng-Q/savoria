const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = (file) => fs.readFileSync(path.resolve(__dirname, '..', file), 'utf8');

test('platform user management is routed and visible in navigation', () => {
  assert.match(read('ui-config.js'), /key:\s*'users'/);
  assert.match(read('src/router/index.js'), /UsersView/);
  assert.match(read('src/workspaces.js'), /'\/users'/);
});

test('user management api uses platform admin user endpoints', () => {
  const source = read('src/api/admin-users.js');
  assert.match(source, /\/api\/admin\/users/);
  assert.match(source, /\/status/);
  assert.match(source, /\/platform-role/);
  assert.match(source, /createAdminUser/);
  assert.match(source, /method:\s*'POST'/);
  assert.match(source, /deleteAdminUser/);
  assert.match(source, /method:\s*'DELETE'/);
});

test('user management selects named statuses and roles without id input', () => {
  const source = read('src/views/platform/UsersView.vue');
  assert.match(source, /v-for="user in/);
  assert.match(source, /<select/);
  assert.match(source, /新增用户/);
  assert.doesNotMatch(source, /placeholder="[^"]*ID/);
  assert.match(source, /role="dialog"/);
  assert.match(source, /<table/);
  assert.match(source, /openCreateDialog/);
  assert.match(source, /confirmAction/);
  assert.match(source, /deleteUser/);
  assert.match(source, /确认删除/);
});

test('platform user creation accepts two-character account names and advertises the same limit', () => {
  const source = read('src/views/platform/UsersView.vue');
  assert.match(source, /\^\[A-Za-z0-9_\]\{2,50\}\$/);
  assert.match(source, /账户名需为 2-50 位/);
  assert.match(source, /placeholder="2-50 位字母、数字或下划线"/);
});
