const test = require('node:test');
const assert = require('node:assert/strict');
const { createDirtyForm } = require('../utils/dirty-form');

test('dirty form enables and clears native leave alert', () => {
  const calls = [];
  const form = createDirtyForm({
    enableAlertBeforeUnload(options) { calls.push(['enable', options.message]); },
    disableAlertBeforeUnload() { calls.push(['disable']); }
  });

  form.markDirty();
  form.markDirty();
  form.markClean();

  assert.equal(form.isDirty(), false);
  assert.deepEqual(calls, [
    ['enable', '内容尚未保存，确定离开吗？'],
    ['disable']
  ]);
});
