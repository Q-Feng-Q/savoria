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
  assert.match(source, /runtime\.family\.getWallet\(\)\.catch\(\(\) => null\)/);
  assert.match(source, /runtime\.family\.getWalletLedgers\(\)\.catch\(\(\) => \[\]\)/);
});

test('profile only requests family home after the identity context confirms a family', () => {
  const source = fs.readFileSync(profilePagePath, 'utf8');
  const familyDecisionIndex = source.indexOf('identityContext.familyId');
  const homeRequestIndex = source.indexOf('runtime.family.getHome()');
  const unboundDecisionIndex = source.indexOf('if (!effectiveIdentityContext.familyId)');

  assert.ok(familyDecisionIndex >= 0);
  assert.ok(homeRequestIndex >= 0);
  assert.ok(homeRequestIndex > familyDecisionIndex);
  assert.ok(unboundDecisionIndex > homeRequestIndex);
  assert.match(source, /identityContext\.familyId\s*\?\s*await runtime\.family\.getHome\(\)\s*:\s*null/);
  assert.doesNotMatch(source, /catch\s*\(error\)\s*\{\s*if\s*\(identityContext\.familyId\)/);
});

test('profile initializes non-data identity guards during page lifecycle', () => {
  const source = fs.readFileSync(profilePagePath, 'utf8');

  assert.doesNotMatch(source, /Page\(\{\s*identityLoad:/);
  assert.match(source, /onLoad\(\)\s*\{\s*this\.identityLoad\s*=\s*createIdentityLoadGuard\(\)/);
});
