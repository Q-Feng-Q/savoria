const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('record field pickers do not draw a second paper background around their input', () => {
  const root = path.resolve(__dirname, '..');
  const globalStyles = fs.readFileSync(path.join(root,
    'styles/warm-kitchen-foundation.wxss'), 'utf8');
  const recordStyles = fs.readFileSync(path.join(root,
    'pages/notebook/detail/record-edit/index.wxss'), 'utf8');
  assert.match(globalStyles, /input, textarea, picker, \.picker-text\s*\{[^}]*background:/);
  assert.match(recordStyles, /\.nb-field>picker\s*\{[^}]*padding:\s*0[^}]*background:\s*transparent/);
  assert.match(recordStyles, /\.nb-input\s*\{[^}]*background:\s*#fffdf8/);
});
