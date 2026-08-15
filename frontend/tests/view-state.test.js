const test = require('node:test');
const assert = require('node:assert/strict');
const {
  createViewState,
  beginMutation,
  failMutation,
  finishMutation
} = require('../utils/view-state');

test('local mutation keeps ready content visible', () => {
  const ready = createViewState('ready', { rows: [1] });
  const saving = beginMutation(ready, 'save-address');

  assert.equal(saving.phase, 'ready');
  assert.deepEqual(saving.data, { rows: [1] });
  assert.equal(saving.submittingAction, 'save-address');
});

test('mutation failure keeps data and stores local error', () => {
  const failed = failMutation(
    beginMutation(createViewState('ready', { rows: [1] }), 'submit'),
    '保存失败'
  );

  assert.equal(failed.phase, 'ready');
  assert.deepEqual(failed.data, { rows: [1] });
  assert.equal(failed.submittingAction, '');
  assert.equal(failed.mutationError, '保存失败');
});

test('finish mutation clears transient flags and exposes success feedback', () => {
  const done = finishMutation(beginMutation(createViewState('ready'), 'submit'));

  assert.equal(done.submittingAction, '');
  assert.equal(done.mutationError, '');
  assert.equal(done.successFeedback, true);
});
