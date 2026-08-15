const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('admin api exposes latest dish review and system setting endpoints', () => {
  const reviews = read('src/api/dish-reviews.js');
  const settings = read('src/api/system-settings.js');
  assert.match(reviews, /\/api\/admin\/dish-reviews/);
  assert.match(reviews, /approve/);
  assert.match(reviews, /reject/);
  assert.match(settings, /\/api\/admin\/system-settings/);
  assert.match(settings, /method:\s*'PUT'/);
});

test('platform admin navigation includes review and system setting workspaces', () => {
  const config = read('ui-config.js');
  const router = read('src/router/index.js');
  assert.match(config, /dish-reviews/);
  assert.match(config, /system-settings/);
  assert.match(router, /DishReviewsView/);
  assert.match(router, /SystemSettingsView/);
});

test('family settings uses latest membership workflow endpoints', () => {
  const api = read('src/api/family-applications.js');
  assert.match(api, /invitations\/direct/);
  assert.match(api, /invitations\/me/);
  assert.match(api, /join-applications/);
  assert.match(api, /\/owner/);
  assert.match(api, /\/current/);
});
