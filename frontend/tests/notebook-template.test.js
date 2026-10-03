const test = require('node:test');
const assert = require('node:assert/strict');
const template = require('../utils/notebook-template');

test('field type labels are Chinese while saved field codes stay unchanged', () => {
  assert.deepEqual(template.FIELD_TYPES.map(template.fieldTypeLabel), [
    '文本', '长文本', '数字', '日期', '时间', '日期时间',
    '单选', '多选', '是/否', '评分', '图片'
  ]);
  assert.equal(template.prepareFields([{ type: 'TEXT', label: '备注' }])[0].type, 'TEXT');
  const fs = require('node:fs');
  const path = require('node:path');
  const editor = fs.readFileSync(path.join(__dirname,
    '../pages/notebook/detail/event-edit/index.wxml'), 'utf8');
  assert.match(editor, /range="{{typeLabels}}"/);
  assert.match(editor, /typeLabels\[item\.typeIndex\]/);
  assert.doesNotMatch(editor, /{{item\.type}}/);
});

test('all eleven field types have an editor and required values are checked', () => {
  const types = ['TEXT', 'LONG_TEXT', 'NUMBER', 'DATE', 'TIME', 'DATETIME',
    'SINGLE_SELECT', 'MULTI_SELECT', 'BOOLEAN', 'RATING', 'IMAGE'];
  types.forEach((type) => assert.ok(template.editorFor(type), type));
  assert.equal(template.validateValues([{ key: 'a', type: 'TEXT', required: true }], {}).valid, false);
  assert.equal(template.validateValues([{ key: 'a', type: 'TEXT', required: true }], { a: 'okay' }).valid, true);
});

test('published field edits preserve keys only for unchanged types', () => {
  const old = [{ key: 'stable', type: 'TEXT', label: 'Old', required: false }];
  assert.equal(template.prepareFields([{ ...old[0], label: 'New' }], old)[0].key, 'stable');
  assert.equal(template.prepareFields([{ ...old[0], type: 'NUMBER' }], old)[0].key, undefined);
  assert.equal(template.validateFields([{ type: 'SINGLE_SELECT', label: 'Choice', required: true, options: [] }]).valid, false);
});

test('event editor clears type-specific options and units when a field type changes', () => {
  const source = require('node:fs').readFileSync(require('node:path').join(__dirname,
    '../pages/notebook/detail/event-edit/index.js'), 'utf8');
  assert.match(source, /fieldType[\s\S]*fields\[index\]\.options = \[\]/);
  assert.match(source, /fieldType[\s\S]*fields\[index\]\.unit = null/);
});

test('record values are complete and option, number, and rating types validate', () => {
  const fields = [
    { key: 'n', type: 'NUMBER', required: true },
    { key: 's', type: 'SINGLE_SELECT', required: true, options: ['A'] },
    { key: 'r', type: 'RATING', required: true }
  ];
  assert.equal(template.validateValues(fields, { n: '2.5', s: 'A', r: 4 }).valid, true);
  assert.equal(template.validateValues(fields, { n: 'bad', s: 'B', r: 8 }).valid, false);
  assert.deepEqual(template.normalizeValues(fields, { n: '2.5', s: 'A', r: 4 }), { n: 2.5, s: 'A', r: 4 });
});

test('date-time values require an explicit offset accepted by the backend', () => {
  const fields = [{ key: 'when', type: 'DATETIME', required: true }];
  assert.equal(template.validateValues(fields, { when: '2026-10-03T12:00' }).valid, false);
  assert.equal(template.validateValues(fields, { when: '2026-10-03T12:00:00+08:00' }).valid, true);
  assert.equal(template.validateValues(fields, { when: '2026-10-03T04:00:00.000Z' }).valid, true);
});
