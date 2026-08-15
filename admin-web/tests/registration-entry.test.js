const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const read = (file) => fs.readFileSync(path.resolve(__dirname, '..', file), 'utf8');

test('admin login does not expose public account registration', () => {
  const source = read('src/views/auth/LoginView.vue');
  const router = read('src/router/index.js');
  assert.doesNotMatch(source, /to="\/register"|注册新账户/);
  assert.doesNotMatch(router, /path:\s*['"]\/register['"]/);
});
