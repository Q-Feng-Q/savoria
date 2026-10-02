const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('approved mini program pages bind shared brand copy', () => {
  const expected = {
    'pages/auth/entry/index.wxml': ['brandCopy.brandTagline'],
    'pages/auth/register/index.wxml': ['brandCopy.brandTagline'],
    'pages/family/home/index.wxml': ['brandCopy.homeHeroTagline', 'brandCopy.homeFooterMessage'],
    'pages/ordering/cart/index.wxml': ['brandCopy.cartHeroTagline', 'brandCopy.deliveryMessage', 'brandCopy.pickupMessage', 'brandCopy.cartFooterMessage'],
    'pages/ordering/orders/index.wxml': ['brandCopy.homeFooterMessage'],
    'pages/account/profile/index.wxml': ['brandCopy.profileWelcomeMessage', 'brandCopy.homeFooterMessage']
  };
  for (const [file, bindings] of Object.entries(expected)) {
    const content = read(file);
    for (const binding of bindings) assert.match(content, new RegExp(binding.replace('.', '\\.')), `${file}: ${binding}`);
  }
});

test('approved atmosphere literals are removed while the unbound message stays code-owned', () => {
  const files = [
    'pages/auth/entry/index.wxml', 'pages/auth/register/index.wxml', 'pages/family/home/index.wxml',
    'pages/ordering/cart/index.wxml', 'pages/ordering/orders/index.wxml', 'pages/account/profile/index.wxml'
  ];
  const joined = files.map(read).join('\n');
  for (const literal of [
    '好好吃饭，就是幸福', '让家常菜 · 温暖每一餐\\n就是最好的时光',
    '好好吃饭\\n就是一家人在一起', '美味正在路上，用食物，把温暖送到家',
    '先在一起，好好吃饭，期待您的到来', '把平凡的日子，过成温暖的诗'
  ]) assert.equal(joined.includes(literal), false, literal);
  assert.match(read('pages/account/profile/index.wxml'), /从这里，认识新家/);
});

test('auth entry and registration pages subscribe to shared branding', () => {
  for (const file of ['pages/auth/entry/index.js', 'pages/auth/register/index.js']) {
    const content = read(file);
    assert.match(content, /require\('\.\.\/\.\.\/\.\.\/utils\/branding'\)/);
    assert.match(content, /Page\(withBranding\(\{/);
    assert.match(content, /\}\)\);\s*$/);
  }
});
