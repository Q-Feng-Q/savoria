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

test('merchant family menu keeps every mutation and selection behind one page guard', () => {
  const js = read('pages/merchant/family-menu/index.js');
  const wxml = read('pages/merchant/family-menu/index.wxml');
  assert.match(js, /mutationBusy:\s*false/);
  assert.match(js, /async runSaving\(task/);
  assert.doesNotMatch(js, /rowBusyMap|rowBusyId/);
  assert.match(wxml, /toggle-action[^>]*mutationBusy[^>]*aria-disabled="\{\{mutationBusy\}\}"[^>]*aria-busy="\{\{mutationBusy\}\}"/);
  assert.match(wxml, /price-action[^>]*mutationBusy[^>]*aria-disabled="\{\{mutationBusy\}\}"[^>]*aria-busy="\{\{mutationBusy\}\}"/);
  assert.match(wxml, /bulk-enable[^>]*aria-disabled="\{\{mutationBusy \|\| selectedCount === 0\}\}"/);
});
