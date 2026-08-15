const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('mini program owns the public registration channel', () => {
  const app = JSON.parse(read('app.json'));
  const entry = read('pages/auth/entry/index.wxml');
  const register = read('pages/auth/register/index.js');
  assert.ok(app.pages.includes('pages/auth/register/index'));
  assert.match(entry, /注册新账户/);
  assert.match(register, /runtime\.auth\.register/);
  assert.match(register, /password\)\.length < 6/);
  assert.match(register, /password\)\.length > 64/);
});

test('auth service posts registration to latest backend endpoint', async () => {
  const calls = [];
  const { createAuthService } = require('../services/auth');
  const auth = createAuthService({ request: async (pathname, options) => {
    calls.push({ pathname, options }); return { code: 0, data: { userId: 1 } };
  }});
  await auth.register({ username: 'new_user', password: '123456', name: '新用户', mobile: '' });
  assert.equal(calls[0].pathname, '/api/auth/register');
  assert.equal(calls[0].options.method, 'POST');
});
