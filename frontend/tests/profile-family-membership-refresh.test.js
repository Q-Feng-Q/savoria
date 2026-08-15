const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const profilePagePath = path.join(
  __dirname,
  '..',
  'pages',
  'account',
  'profile',
  'index.js'
);

test('profile membership rendering does not depend on the ordering meal-slot bundle', () => {
  const source = fs.readFileSync(profilePagePath, 'utf8');

  assert.doesNotMatch(source, /loadFamilyBundle/);
  assert.match(source, /runtime\.family\.getHome\(\)/);
});

test('optional address and wallet failures do not hide an existing family', () => {
  const source = fs.readFileSync(profilePagePath, 'utf8');

  assert.match(source, /runtime\.family\.getAddresses\(\)\.catch\(\(\) => \[\]\)/);
  assert.match(source, /runtime\.family\.getWalletLedgers\(\)\.catch\(\(\) => \[\]\)/);
});

test('profile recovers realtime family membership when the legacy context response is stale', () => {
  const source = fs.readFileSync(profilePagePath, 'utf8');
  const homeRequestIndex = source.indexOf('runtime.family.getHome()');
  const unboundDecisionIndex = source.indexOf('if (!effectiveIdentityContext.familyId)');

  assert.ok(homeRequestIndex >= 0);
  assert.ok(unboundDecisionIndex > homeRequestIndex);
  assert.match(source, /familyId:\s*homeData\.family\.familyId/);
});
