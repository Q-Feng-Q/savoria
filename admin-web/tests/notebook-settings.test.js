const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const view = fs.readFileSync(path.join(root, 'src/views/platform/SystemSettingsView.vue'), 'utf8');

test('platform settings exposes a bounded notebook month control in the common save flow', () => {
  assert.match(view, /记事最长查看时间/);
  assert.match(view, /v-model\.number="form\.notebookMaxQueryMonths"/);
  assert.match(view, /type="number"[^>]*min="1"[^>]*max="36"/);
  assert.match(view, /notebookMaxQueryMonths:36/);
  assert.match(view, /validateNotebookMonths\(form\.notebookMaxQueryMonths\)/);
});

test('notebook month validation accepts only whole months within 1 to 36', async () => {
  const source = fs.readFileSync(path.join(root, 'src/utils/notebook-settings.js'), 'utf8');
  const { validateNotebookMonths } = await import(`data:text/javascript,${encodeURIComponent(source)}`);
  assert.equal(validateNotebookMonths(1), true);
  assert.equal(validateNotebookMonths(36), true);
  for (const value of [0, 37, 2.5, '', null, undefined, '12']) {
    assert.equal(validateNotebookMonths(value), false, String(value));
  }
});
