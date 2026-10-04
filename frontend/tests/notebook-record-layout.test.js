const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('record form places notes after custom fields without a template section heading', () => {
  const markup = fs.readFileSync(path.join(__dirname,
    '../pages/notebook/detail/record-edit/index.wxml'), 'utf8');
  const fieldsAt = markup.indexOf('wx:for="{{fields}}"');
  const lastFieldControlAt = markup.indexOf('＋ 选择私有图片');
  const noteAt = markup.indexOf('class="nb-label">备注');
  assert.ok(fieldsAt >= 0);
  assert.ok(lastFieldControlAt > fieldsAt);
  assert.ok(noteAt > lastFieldControlAt);
  assert.doesNotMatch(markup, /记录字段\s*·\s*模板/);
});

test('record title input prompts for a title', () => {
  const markup = fs.readFileSync(path.join(__dirname,
    '../pages/notebook/detail/record-edit/index.wxml'), 'utf8');
  assert.match(markup, /data-key="title"[^>]*placeholder="请输入标题"/);
});
