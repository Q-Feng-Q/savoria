const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('ordering pages remove meal slots and native buttons', () => {
  const sources = ['pages/ordering/menu/index.wxml', 'pages/ordering/dish-detail/index.wxml', 'pages/ordering/cart/index.wxml']
    .map(read).join('\n');
  assert.doesNotMatch(sources, /<button\b/i);
  assert.doesNotMatch(sources, /早餐|午餐|晚餐|mealOptions|selectMeal/);
  assert.match(sources, /预计用餐时间/);
  assert.match(sources, /家庭钱包统一/);
});

test('shared cart mutations are absolute, versioned and refresh stale carts', () => {
  const menu = read('pages/ordering/menu/index.js');
  const cart = read('pages/ordering/cart/index.js');
  assert.match(menu, /cartId:\s*cart\.cartId/);
  assert.match(menu, /cartVersion:\s*cart\.version/);
  assert.match(menu, /setItemQuantity/);
  assert.match(cart, /row\.myQuantity/);
  assert.match(cart, /error\.code === 40931/);
  assert.match(cart, /error\.code === 40932/);
  assert.match(cart, /await this\.load\(\)/);
  assert.doesNotMatch(`${menu}\n${cart}`, /mealSlotId/);
});

test('cart main rows show totals and reveal member attribution only in details', () => {
  const wxml = read('pages/ordering/cart/index.wxml');
  assert.match(wxml, /共 × \{\{row\.totalQuantity\}\}/);
  assert.match(wxml, /我选/);
  assert.match(wxml, /toggleSelectionDetails/);
  assert.match(wxml, /wx:if="\{\{expandedDishIds\[row\.id\]\}\}"/);
  assert.match(wxml, /item\.memberName.*item\.quantity/s);
});
