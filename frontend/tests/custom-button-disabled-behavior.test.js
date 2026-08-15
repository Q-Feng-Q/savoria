const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('cart rejects disabled delivery controls after native button removal', () => {
  const js = read('pages/ordering/cart/index.js');
  const wxml = read('pages/ordering/cart/index.wxml');
  assert.match(js, /selectDelivery\(event\)\s*\{[\s\S]*event\.currentTarget\.dataset\.disabled[\s\S]*return/);
  assert.match(wxml, /delivery-option[^>]*is-disabled[^>]*data-disabled="\{\{item\.disabled\}\}"[^>]*aria-disabled=/);
});

test('merchant family menu keeps page and row mutations behind scoped busy guards', () => {
  const js = read('pages/merchant/family-menu/index.js');
  const wxml = read('pages/merchant/family-menu/index.wxml');
  assert.match(js, /saving:\s*false/);
  assert.match(js, /async runSaving\(task/);
  assert.match(js, /rowBusyMap:\s*\{\}/);
  assert.match(js, /rowBusyId !== null && this\.data\.rowBusyMap\[rowBusyId\]/);
  assert.match(wxml, /toggle-action[^>]*rowBusyMap\[item\.id\][^>]*aria-disabled="\{\{rowBusyMap\[item\.id\]\}\}"[^>]*aria-busy=/);
});
