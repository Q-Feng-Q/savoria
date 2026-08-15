const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('action button renders a custom accessible control with loading copy', () => {
  const wxml = read('components/action-button/index.wxml');
  const js = read('components/action-button/index.js');
  assert.match(wxml, /<view[^>]+class="[^"]*ui-button/);
  assert.match(wxml, /aria-role="button"/);
  assert.match(wxml, /aria-busy="\{\{loading\}\}"/);
  assert.match(wxml, /loading \? loadingLabel : label/);
  assert.match(js, /loadingLabel:\s*\{[^}]*value:\s*'处理中…'/s);
  assert.match(js, /if \(this\.properties\.disabled \|\| this\.properties\.loading\) return/);
});

test('bottom action bar and quantity stepper use custom controls with guards', () => {
  const barWxml = read('components/bottom-action-bar/index.wxml');
  const barJs = read('components/bottom-action-bar/index.js');
  const stepperWxml = read('components/quantity-stepper/index.wxml');
  const stepperJs = read('components/quantity-stepper/index.js');
  assert.doesNotMatch(barWxml, /<\/?button\b/i);
  assert.match(barWxml, /aria-disabled="\{\{disabled \|\| loading\}\}"/);
  assert.match(barJs, /if \(this\.properties\.disabled \|\| this\.properties\.loading\) return/);
  assert.doesNotMatch(stepperWxml, /<\/?button\b/i);
  assert.match(stepperJs, /if \(this\.properties\.disabled\) return/);
});

test('custom controls keep an 88rpx minimum touch target and are never scaled down', () => {
  const foundation = read('styles/warm-kitchen-foundation.wxss');
  const stepper = read('components/quantity-stepper/index.wxss');
  const dishRow = read('components/dish-row/index.wxss');
  assert.match(foundation, /\.ui-button\s*\{[^}]*min-height:\s*88rpx/s);
  assert.match(stepper, /\.quantity-stepper__button\s*\{[^}]*min-width:\s*88rpx[^}]*min-height:\s*88rpx/s);
  assert.doesNotMatch(dishRow, /transform:\s*scale\(/);
});
