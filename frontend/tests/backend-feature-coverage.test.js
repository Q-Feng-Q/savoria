const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const { createAuthService } = require('../services/auth');
const { createFamilyService } = require('../services/family');

const root = path.resolve(__dirname, '..');

test('account recovery and family exit use backend endpoints', async () => {
  const calls = [];
  const request = async (pathname, options) => {
    calls.push({ pathname, options });
    return { code: 0, data: null };
  };
  const auth = createAuthService({ request });
  const family = createFamilyService({ request });

  await auth.sendPasswordResetCode({ email: 'family@example.com' });
  await auth.resetPassword({ email: 'family@example.com', code: '123456', newPassword: '654321' });
  await family.exitFamily();

  assert.deepEqual(calls.map((item) => `${item.options.method} ${item.pathname}`), [
    'POST /api/auth/password/reset-code',
    'POST /api/auth/password/reset',
    'POST /api/family/exit'
  ]);
});

test('mini program exposes recovery, family lifecycle and merchant review pages', () => {
  const config = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));
  assert.ok(config.pages.includes('pages/auth/password-recovery/index'));
  assert.ok(config.pages.includes('pages/merchant/dish-reviews/index'));

  const login = fs.readFileSync(path.join(root, 'pages/auth/entry/index.js'), 'utf8');
  const family = fs.readFileSync(path.join(root, 'pages/family/family-management/index.js'), 'utf8');
  const reviews = fs.readFileSync(path.join(root, 'pages/merchant/dish-reviews/index.js'), 'utf8');
  assert.match(login, /openPasswordRecovery/);
  assert.match(family, /exitFamily/);
  assert.match(family, /dissolveFamily/);
  assert.match(reviews, /getDishReviews/);
  assert.match(reviews, /withdrawDishReview/);
});

