const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');

function collectWxmlFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      return collectWxmlFiles(entryPath);
    }
    return entry.isFile() && entry.name.endsWith('.wxml') ? [entryPath] : [];
  });
}

test('profile templates do not call JavaScript methods inside WXML expressions', () => {
  for (const file of ['pages/account/profile/index.wxml', 'pages/account/profile-edit/index.wxml']) {
    const source = fs.readFileSync(path.join(root, file), 'utf8');
    assert.doesNotMatch(source, /\}\}[^\n]*\.slice\(|\{\{[^}]*\.slice\(/, file);
  }
});

test('profile pages expose precomputed avatar initials', () => {
  const profile = fs.readFileSync(path.join(root, 'pages/account/profile/index.js'), 'utf8');
  const edit = fs.readFileSync(path.join(root, 'pages/account/profile-edit/index.js'), 'utf8');
  assert.match(profile, /avatarInitial/);
  assert.match(edit, /avatarInitial/);
});

test('WXML does not combine conditional fallbacks and loops on one node', () => {
  for (const file of collectWxmlFiles(path.join(root, 'pages'))) {
    const source = fs.readFileSync(file, 'utf8');
    assert.doesNotMatch(
      source,
      /<[^>]*(?:wx:else[^>]*wx:for|wx:for[^>]*wx:else)[^>]*>/,
      path.relative(root, file),
    );
  }
});
