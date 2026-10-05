const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const page = (extension) => fs.readFileSync(path.join(__dirname, '..', 'pages/merchant/dish-templates', `index.${extension}`), 'utf8');

test('template type picker centers its label and arrow in a narrow white control', () => {
  const markup = page('wxml');
  const styles = page('wxss');
  const picker = styles.match(/\.merchant-story\.template-page \.product-type-filter\s*\{([^}]+)\}/)?.[1];
  const control = styles.match(/\.product-type-filter__control\s*\{([^}]+)\}/)?.[1];
  const filterItem = styles.match(/\.filter-row \.filter-item\s*\{([^}]+)\}/)?.[1];

  assert.match(markup, /<picker class="filter-item product-type-filter"[^>]+bindchange="changeProductType"/);
  assert.match(markup, /<view class="product-type-filter__control">\s*<text class="product-type-filter__value">\{\{productTypes\[productTypeIndex\]\.label\}\}<\/text>\s*<view class="product-type-filter__arrow"><\/view>/);
  assert.ok(picker, 'type picker should override the global full-width picker style');
  assert.match(picker, /width:\s*1\d\drpx/);
  assert.match(picker, /background:\s*transparent/);
  assert.match(picker, /padding:\s*10rpx\s+0/);
  assert.match(control || '', /align-items:\s*center/);
  assert.match(control || '', /background:\s*#fff\b/);
  assert.match(styles, /\.product-type-filter__value\s*\{[^}]*white-space:\s*nowrap/);
  assert.match(styles, /\.product-type-filter__arrow\s*\{[^}]*rotate\(45deg\)/);
  assert.match(filterItem || '', /flex:\s*none/);
  assert.match(filterItem || '', /white-space:\s*nowrap/);
});
