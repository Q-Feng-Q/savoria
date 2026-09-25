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
  assert.doesNotMatch(sources, /今天已选|谁点谁扣|分别冻结|个人钱包|\bNULL\b/i);
});

test('menu shows expected meal time and uses 38rpx visual stepper buttons', () => {
  const menu = read('pages/ordering/menu/index.wxml');
  const stepperMarkup = read('components/quantity-stepper/index.wxml');
  const stepper = read('components/quantity-stepper/index.wxss');
  assert.match(menu, /预计用餐时间/);
  assert.match(menu, /expectedMealTimeText/);
  assert.match(stepperMarkup, /quantity-stepper__glyph/);
  assert.match(stepper, /\.quantity-stepper__glyph\s*\{[^}]*width:\s*38rpx[^}]*height:\s*38rpx/s);
});

test('homepage role cards display dynamic crew names', () => {
  const home = read('pages/family/home/index.wxml');
  const menu = read('pages/ordering/menu/index.wxml');
  const profile = read('pages/account/profile/index.wxml');
  assert.match(home, /crew\.chefRecommendationLabel/);
  assert.match(home, /crew\.helperLabel/);
  assert.match(home, /crew\.tasterLabel/);
  assert.match(menu, /activeCategoryLabel/);
  assert.match(menu, /crew\.helperLabel/);
  assert.match(profile, /crew\.tasterWelcomeLabel/);
  assert.doesNotMatch([home, menu, profile].join('\n'), /无主厨(?:今天|今日)推荐|无试吃员欢迎你/);
  assert.doesNotMatch([home, menu, profile].join('\n'), /小熊|兔子帮厨|橘猫试吃员/);
});

test('dish detail separates labels from values and documents family wallet settlement', () => {
  const detail = read('pages/ordering/dish-detail/index.wxml');
  const styles = read('pages/ordering/dish-detail/index.wxss');
  assert.match(detail, /detail-fact__label/);
  assert.match(detail, /detail-fact__value/);
  assert.match(detail, /家庭钱包统一冻结并扣款/);
  assert.match(detail, /detail-section__heading[^>]*>点餐信息</);
  assert.match(detail, /detail-section__heading[^>]*>菜品说明</);
  assert.match(detail, /detail-section__heading[^>]*>主要食材</);
  assert.match(detail, /detail-section__body/);
  assert.equal((detail.match(/class="meta-value"/g) || []).length, 1);
  assert.doesNotMatch(detail, /商户价格|商户基础价|basePrice/);
  assert.match(styles, /\.detail-fact\s*\{/);
  assert.match(styles, /\.detail-section\s*\{[^}]*margin-top:/s);
  assert.match(styles, /\.detail-section__head\s*\{[^}]*padding-bottom:[^}]*border-bottom:/s);
});

test('dish row keeps the 38rpx stepper in a non-overlapping bottom action row', () => {
  const markup = read('components/dish-row/index.wxml');
  const styles = read('components/dish-row/index.wxss');
  const stepper = read('components/quantity-stepper/index.wxss');
  const actionRowRule = styles.match(/\.dish-row__action-row\s*\{([^}]*)\}/)?.[1];
  const priceRule = styles.match(/\.dish-row__price\s*\{([^}]*)\}/)?.[1];
  const stepperRule = styles.match(/\.dish-row__stepper\s*\{([^}]*)\}/)?.[1];
  const compactActionRowRule = styles.match(/@media\s*\(max-width:\s*360px\)[\s\S]*?\.dish-row__action-row\s*\{([^}]*)\}/)?.[1];
  assert.equal((markup.match(/dish-row__price/g) || []).length, 1);
  assert.match(markup, /<view class="dish-row__action-row"(?![^>]*catchtap)[^>]*>\s*<text class="dish-row__price"[^>]*>[^<]*<\/text>\s*<view class="dish-row__stepper" catchtap="noop">\s*<quantity-stepper\b[^>]*\/>\s*<\/view>/);
  assert.ok(actionRowRule);
  assert.match(actionRowRule, /grid-column:\s*2/);
  assert.match(actionRowRule, /display:\s*flex/);
  assert.match(actionRowRule, /justify-content:\s*space-between/);
  assert.match(actionRowRule, /align-items:\s*center/);
  assert.match(actionRowRule, /margin-top:\s*14rpx/);
  assert.ok(priceRule);
  assert.match(priceRule, /min-width:\s*0/);
  assert.match(priceRule, /overflow:\s*hidden/);
  assert.match(priceRule, /text-overflow:\s*ellipsis/);
  assert.match(priceRule, /white-space:\s*nowrap/);
  assert.ok(stepperRule);
  assert.match(stepperRule, /flex:\s*0\s+0\s+auto/);
  assert.doesNotMatch(styles, /\.dish-row__action-row\s*\{[^}]*position:\s*absolute/s);
  assert.match(styles, /@media\s*\(max-width:\s*360px\)[\s\S]*\.dish-row__action-row\s*\{/);
  assert.ok(compactActionRowRule);
  assert.match(compactActionRowRule, /width:\s*100%/);
  assert.match(compactActionRowRule, /justify-content:\s*space-between/);
  assert.match(stepper, /\.quantity-stepper__glyph\s*\{[^}]*width:\s*38rpx[^}]*height:\s*38rpx/s);
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

test('unavailable cart rows keep decrement active and disable only increment and checkout', () => {
  const js = read('pages/ordering/cart/index.js');
  const wxml = read('pages/ordering/cart/index.wxml');
  const wxss = read('pages/ordering/cart/index.wxss');
  assert.match(js, /delta\s*>\s*0\s*&&\s*!row\.available/);
  assert.match(wxml, /wx:if="\{\{!row\.available\}\}"[^>]*class="cart-item-warning"[^>]*>\{\{row\.unavailableReason\}\}/);
  assert.match(wxml, /data-delta="1"[^>]*is-disabled[^>]*aria-disabled="\{\{!row\.available\}\}"/);
  assert.match(wxss, /\.cart-item-warning\s*\{[^}]*color:\s*var\(--sk-danger\)/s);
});
