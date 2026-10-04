const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const pagePath = path.join(root, 'pages/notebook/detail/event-edit/index.js');

async function withPage({ modal = async () => ({ confirm: true }), run }) {
  const runtimePath = require.resolve(path.join(root, 'utils/api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils/page-api.js'));
  const identityPath = require.resolve(path.join(root, 'utils/identity-load.js'));
  const oldRuntime = require.cache[runtimePath];
  const oldPageApi = require.cache[pageApiPath];
  const oldIdentity = require.cache[identityPath];
  const oldPage = global.Page;
  const oldWx = global.wx;
  const created = [];
  let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ notebook: {
    createEvent: async (draft) => { created.push(draft); }
  } }) } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ userId: 7 }), showApiError: (error) => { throw error; }
  } };
  require.cache[identityPath] = { exports: { createIdentityLoadGuard: () => ({
    begin: () => 1, isCurrent: () => true
  }) } };
  global.Page = (value) => { definition = value; };
  global.wx = { showModal: modal, navigateBack() {}, showToast() {},
    enableAlertBeforeUnload() {}, disableAlertBeforeUnload() {} };
  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update) { this.data = { ...this.data, ...update }; }
    });
    page.onLoad({});
    await run(page, created);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (oldRuntime) require.cache[runtimePath] = oldRuntime; else delete require.cache[runtimePath];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi; else delete require.cache[pageApiPath];
    if (oldIdentity) require.cache[identityPath] = oldIdentity; else delete require.cache[identityPath];
    global.Page = oldPage;
    global.wx = oldWx;
  }
}

test('new event editor browses local templates by category and previews fields', async () => {
  await withPage({ run: async (page) => {
    page.openPresetPicker();
    assert.equal(page.data.showPresetPicker, true);
    assert.equal(page.data.visiblePresets.length, 21);
    assert.equal(page.data.presetCount, 21);
    page.choosePresetCategory({ currentTarget: { dataset: { category: '身体' } } });
    assert.ok(page.data.visiblePresets.every((item) => item.category === '身体'));
    page.selectPreset({ currentTarget: { dataset: { id: 'period' } } });
    assert.equal(page.data.selectedPreset.name, '生理期');
    assert.equal(page.data.selectedPresetFields[0].typeLabel, '单选');
    assert.equal(page.data.selectedPresetFields[0].optionsText, '少、适中、多、不确定');
    page.choosePresetCategory({ currentTarget: { dataset: { category: '学习' } } });
    assert.equal(page.data.selectedPreset, null);
    assert.deepEqual(page.data.selectedPresetFields, []);
    page.closePresetPicker();
    assert.equal(page.data.showPresetPicker, false);
    page.setData({ editing: true });
    page.openPresetPicker();
    assert.equal(page.data.showPresetPicker, false);
  } });
});

test('import fills an unsaved event and save keeps existing backend field codes', async () => {
  await withPage({ run: async (page, created) => {
    page.openPresetPicker();
    page.selectPreset({ currentTarget: { dataset: { id: 'period' } } });
    await page.importPreset();
    assert.equal(page.data.name, '生理期');
    assert.equal(page.data.category, '身体');
    assert.equal(page.data.fields[0].typeIndex, 6);
    assert.equal(page.data.fields[0].optionsText, '少，适中，多，不确定');
    assert.equal(page.data.showPresetPicker, false);
    assert.equal(page.form.isDirty(), true);
    assert.equal(created.length, 0);
    await page.save();
    assert.equal(created.length, 1);
    assert.equal(created[0].fields[0].type, 'SINGLE_SELECT');
    assert.equal(created[0].fields[0].typeIndex, undefined);
    assert.equal(created[0].fields[0].optionsText, undefined);
  } });
});

test('import asks before replacing edits and cancellation preserves them', async () => {
  let confirm = false;
  let asks = 0;
  await withPage({ modal: async () => { asks++; return { confirm }; }, run: async (page) => {
    page.onText({ currentTarget: { dataset: { key: 'name' } }, detail: { value: '我的事件' } });
    page.openPresetPicker();
    page.selectPreset({ currentTarget: { dataset: { id: 'travel' } } });
    await page.importPreset();
    assert.equal(asks, 1);
    assert.equal(page.data.name, '我的事件');
    assert.equal(page.data.showPresetPicker, true);
    confirm = true;
    await page.importPreset();
    assert.equal(page.data.name, '旅行足迹');
    assert.equal(page.data.fields[0].label, '地点');
  } });
});

test('new event markup exposes a template browser and hides it while editing', () => {
  const markup = fs.readFileSync(path.join(root,
    'pages/notebook/detail/event-edit/index.wxml'), 'utf8');
  assert.match(markup, /导入模板/);
  assert.match(markup, /{{presetCount}} 个常用模板/);
  assert.match(markup, /wx:if="{{!editing}}"/);
  assert.match(markup, /selectedPresetFields/);
  assert.match(markup, /item\.optionsText/);
  assert.match(markup, /bindtap="importPreset"/);
});

test('template category strip avoids intrinsic sizing unsupported by older WXSS engines', () => {
  const styles = fs.readFileSync(path.join(root,
    'pages/notebook/detail/event-edit/index.wxss'), 'utf8');
  assert.match(styles, /\.nb-preset-category-row\{display:inline-flex/);
  assert.doesNotMatch(styles, /width:max-content/);
});
