const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, '..');
const read = (name) => fs.readFileSync(path.join(root, name), 'utf8');

test('notebook replaces cart in the native five-tab bar while cart stays registered', () => {
  const app = JSON.parse(read('app.json'));
  assert.equal(app.tabBar.list.length, 5);
  assert.equal(app.tabBar.list[2].pagePath, 'pages/notebook/home/index');
  assert.equal(app.tabBar.list[2].text, '记事');
  assert.equal(app.pages.includes('pages/ordering/cart/index'), true);
  assert.equal(app.pages.includes('pages/notebook/home/index'), true);
});

test('cart opens from menu and family quick entry through navigateTo', () => {
  const menu = read('pages/ordering/menu/index.js');
  const menuView = read('pages/ordering/menu/index.wxml');
  const home = read('pages/family/home/index.js');
  assert.match(menu, /openCart\(\).*wx\.navigateTo\(\{ url: '\/pages\/ordering\/cart\/index' \}\)/);
  assert.match(menuView, /bindtap="openCart"/);
  assert.match(menuView, /cartItemCount/);
  assert.match(home, /if \(key === 'cart'\) wx\.navigateTo\(\{ url \}\)/);
  assert.doesNotMatch(home, /wx\.switchTab\(\{ url: QUICK_ENTRY_ROUTES\.cart \}\)/);
});
