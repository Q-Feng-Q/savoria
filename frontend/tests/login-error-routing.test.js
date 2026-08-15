const test = require('node:test');
const assert = require('node:assert/strict');
const { resolveLoginErrorMessage } = require('../utils/page-api');

test('login authentication failure keeps the backend credential message', () => {
  assert.equal(
    resolveLoginErrorMessage({ code: 40101, statusCode: 401, message: '账号或密码错误' }),
    '账号或密码错误'
  );
});

test('login authentication failure has a specific fallback message', () => {
  assert.equal(resolveLoginErrorMessage({ code: 40101 }), '账号或密码错误');
});
