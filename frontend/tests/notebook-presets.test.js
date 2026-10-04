const test = require('node:test');
const assert = require('node:assert/strict');
const { listPresets, getPreset, copyPresetToEvent } = require('../utils/notebook-presets');
const { validateFields } = require('../utils/notebook-template');

test('the local catalog contains twenty-one valid Chinese notebook templates', () => {
  const presets = listPresets();
  assert.equal(presets.length, 21);
  assert.equal(new Set(presets.map((item) => item.id)).size, 21);
  assert.ok(presets.every((item) => item.name && item.category && item.description));
  for (const preset of presets) {
    assert.equal(validateFields(preset.fields).valid, true, preset.name);
    assert.ok(preset.fields.length > 0, preset.name);
  }
  assert.equal(getPreset('period').name, '生理期');
  assert.deepEqual(copyPresetToEvent('general').fields, [{
    label: '今日内容', type: 'LONG_TEXT', required: false, options: [], unit: null
  }]);
  assert.equal(getPreset('missing'), null);
});

test('importing a preset produces an independent event draft without private data', () => {
  const first = copyPresetToEvent('period');
  const second = copyPresetToEvent('period');
  assert.deepEqual(Object.keys(first).sort(), ['category', 'description', 'fields', 'name']);
  assert.deepEqual(Object.keys(first.fields[0]).sort(), ['label', 'options', 'required', 'type', 'unit']);
  first.fields[0].options.push('自定义');
  first.fields[0].label = '修改后';
  assert.notDeepEqual(first.fields, second.fields);
  assert.equal(getPreset('period').fields[0].label, '经量');
  assert.equal(copyPresetToEvent('missing'), null);
});
