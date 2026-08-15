const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = (file) => fs.readFileSync(path.resolve(__dirname, '..', file), 'utf8');

test('platform system settings exposes account switches and smtp configuration', () => {
  const view = read('src/views/platform/SystemSettingsView.vue');
  assert.match(view, /mobileBindingEnabled/);
  assert.match(view, /emailBindingEnabled/);
  assert.match(view, /wechatBindingEnabled/);
  assert.match(view, /smtpHost/);
  assert.match(view, /smtpPort/);
  assert.match(view, /smtpPassword/);
  assert.match(view, /邮件服务器/);
});
