const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, '..');
const cartCss = fs.readFileSync(path.join(root, 'pages/ordering/cart/index.wxss'), 'utf8');
const cartWxml = fs.readFileSync(path.join(root, 'pages/ordering/cart/index.wxml'), 'utf8');

test('shared cart has compact phone and landscape fallbacks', () => {
  assert.match(cartCss, /@media \(max-width: 340px\)/);
  assert.match(cartCss, /@media \(orientation: landscape\)/);
  assert.match(cartCss, /min-width:0/);
  assert.match(cartCss, /overflow-wrap:anywhere/);
});

test('shared cart controls expose semantics and disabled states', () => {
  assert.match(cartWxml, /aria-role="button"/);
  assert.match(cartWxml, /aria-disabled/);
  assert.match(cartWxml, /bottom-action-bar/);
  assert.doesNotMatch(cartWxml, /<button\b/i);
});
