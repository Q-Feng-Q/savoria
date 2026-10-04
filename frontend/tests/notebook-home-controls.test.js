const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '../pages/notebook/home');
const markup = fs.readFileSync(path.join(root, 'index.wxml'), 'utf8');
const styles = fs.readFileSync(path.join(root, 'index.wxss'), 'utf8');

test('category filter stays compact without pushing notebook links offscreen', () => {
  assert.match(markup, /<picker[^>]*class="notebook-category-picker"/);
  assert.match(styles, /\.notebook-category-picker\s*\{[^}]*width:\s*auto/);
  assert.match(styles, /\.notebook-category-picker\s*\{[^}]*background:\s*transparent/);
  assert.match(styles, /\.notebook-tools\s*\{[^}]*flex-wrap:\s*wrap/);
});

test('recorded and selected dates use the same heart silhouette at the same size', () => {
  assert.match(markup, /class="notebook-day[^\"]*has-record/);
  assert.doesNotMatch(markup, /class="notebook-dot"/);
  assert.match(markup, /wx:if="\{\{summary\[item\.key\] \|\| item\.key === selectedDate\}\}" class="notebook-day-heart"/);
  assert.match(markup, /src="\{\{calendarHeartSrc\[item\.key\]\}\}"/);
  const overview = fs.readFileSync(path.resolve(root, '../../../utils/notebook-overview.js'), 'utf8');
  assert.match(overview, /\/assets\/ui\/notebook-heart-filled\.png/);
  assert.match(overview, /\/assets\/ui\/notebook-heart-outline\.png/);
  assert.doesNotMatch(markup, /[♥♡]/);
  assert.match(markup, /<text class="notebook-day-number">\{\{item\.day\}\}<\/text>/);
  assert.match(styles, /\.notebook-day-heart\s*\{[^}]*top:\s*4rpx;[^}]*width:\s*65rpx;[^}]*height:\s*65rpx/);
  const assets = ['outline', 'filled'].map((kind) => fs.readFileSync(
    path.resolve(root, `../../../assets/ui/notebook-heart-${kind}.png`)));
  for (const image of assets) assert.equal(image.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
  assert.deepEqual(assets[0].subarray(16, 24), assets[1].subarray(16, 24));
  const source = fs.readFileSync(path.resolve(root,
    '../../../scripts/generate-notebook-heart-assets.py'), 'utf8');
  assert.match(source, /HORIZONTAL_SCALE = 0\.925/);
  assert.match(styles, /\.notebook-day\.has-record \.notebook-day-number,\.notebook-day\.selected \.notebook-day-number\s*\{[^}]*transform:\s*translateY\(-4rpx\)/);
  assert.doesNotMatch(styles, /\.notebook-day\.selected\s*\{[^}]*background:/);
});

test('notebook-wide actions sit above calendar view tabs', () => {
  const normalized = markup.replace(/\r\n/g, '\n');
  const headerEnd = normalized.indexOf('\n  </view>\n  <view wx:if=');
  const actions = normalized.indexOf('class="notebook-tools"');
  const tabs = normalized.indexOf('class="notebook-view-switch"');
  assert.ok(headerEnd > 0);
  assert.ok(actions > 0 && actions < headerEnd);
  assert.ok(tabs > headerEnd && actions < tabs);
});
