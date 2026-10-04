const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('event field type picker does not add a second background outside its input', () => {
  const root = path.resolve(__dirname, '..');
  const markup = fs.readFileSync(path.join(root,
    'pages/notebook/detail/event-edit/index.wxml'), 'utf8');
  const styles = fs.readFileSync(path.join(root,
    'pages/notebook/detail/event-edit/index.wxss'), 'utf8');
  assert.match(markup, /<picker range="{{typeLabels}}"[^>]*><view class="nb-input">/);
  assert.match(styles, /\.nb-field>picker\s*\{[^}]*padding:\s*0[^}]*background:\s*transparent/);
});
