const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('brand scene renders local animal mascots through image instead of WXSS url', () => {
  const markup = read('components/brand-scene/index.wxml');
  const styles = read('components/brand-scene/index.wxss');
  assert.match(markup, /<image[^>]+animal-mascot-trio\.webp/);
  assert.doesNotMatch(styles, /background-image\s*:\s*url\(/);
});

test('mini proxy has an explicit development start command', () => {
  const pkg = JSON.parse(read('package.json'));
  assert.equal(pkg.scripts.dev, 'node dev-proxy.js');
});

test('page state also renders its local animal art through image markup', () => {
  const markup = read('components/page-state/index.wxml');
  const styles = read('components/page-state/index.wxss');
  assert.match(markup, /<image[^>]+scenes\/(?:empty-order|network-error)\.webp/);
  assert.doesNotMatch(styles, /background-image\s*:\s*url\(/);
});
