const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = (file) => fs.readFileSync(path.join(__dirname, '..', file), 'utf8');

test('dish editor picker shells do not add a second background or inset', () => {
  const markup = read('pages/merchant/dish-edit/index.wxml');
  assert.equal((markup.match(/<picker class="editor-picker"/g) || []).length, 4);
  const css = read('pages/merchant/dish-edit/index.wxss');
  const rule = css.match(/\.merchant-story\.editor-page \.editor-picker\s*\{([^}]+)\}/)?.[1];
  assert.ok(rule);
  for (const value of [/background:\s*transparent/, /padding:\s*0/, /border:\s*0/, /min-height:\s*0/]) assert.match(rule, value);
});

test('merchant dish actions occupy a full-width bottom row at every phone width', () => {
  const css = read('pages/merchant/merchant-dishes/index.wxss');
  const row = css.match(/\.merchant-story\.dishes-page \.row-actions\s*\{([^}]+)\}/)?.[1];
  assert.ok(row);
  assert.match(row, /grid-column:\s*1\s*\/\s*-1/);
  assert.match(row, /flex-direction:\s*row/);
  assert.match(row, /flex-wrap:\s*wrap/);
  const action = css.match(/\.merchant-story\.dishes-page \.row-action\s*\{([^}]+)\}/)?.[1];
  assert.match(action, /flex:\s*1\s+1\s+0/);
  assert.match(action, /min-width:\s*0/);
});
