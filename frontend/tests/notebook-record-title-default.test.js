const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const root = path.resolve(__dirname, '..');

async function withRecordEditor(notebook, run) {
  const pagePath = path.join(root, 'pages/notebook/detail/record-edit/index.js');
  const runtimePath = require.resolve(path.join(root, 'utils/api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils/page-api.js'));
  const identityPath = require.resolve(path.join(root, 'utils/identity-load.js'));
  const originals = [runtimePath, pageApiPath, identityPath].map((name) => require.cache[name]);
  const oldPage = global.Page;
  const oldWx = global.wx;
  let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ notebook }) } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ userId: 7 }), showApiError: (error) => { throw error; }
  } };
  require.cache[identityPath] = { exports: { createIdentityLoadGuard: () => ({
    begin: () => 1, isCurrent: () => true
  }) } };
  global.Page = (value) => { definition = value; };
  global.wx = { navigateBack() {} };
  try {
    delete require.cache[pagePath];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: { ...definition.data, eventId: 4, occurredDate: '2026-10-04', endDate: '2026-10-04' },
      boundUserId: 7,
      form: { markClean() {}, markDirty() {} },
      setData(update) { this.data = { ...this.data, ...update }; }
    });
    await run(page);
  } finally {
    delete require.cache[pagePath];
    [runtimePath, pageApiPath, identityPath].forEach((name, index) => {
      if (originals[index]) require.cache[name] = originals[index];
      else delete require.cache[name];
    });
    global.Page = oldPage;
    global.wx = oldWx;
  }
}

test('new record defaults its editable title to the selected event name', async () => {
  let created;
  await withRecordEditor({
    getRecordTemplate: async () => ({ name: '体重变化', templateVersion: 1, fields: [] }),
    createRecord: async (eventId, draft) => { created = { eventId, draft }; }
  }, async (page) => {
    await page.load();
    assert.equal(page.data.title, '体重变化');
    page.onText({ currentTarget: { dataset: { key: 'title' } }, detail: { value: '今天的体重' } });
    await page.save();
    assert.equal(created.eventId, 4);
    assert.equal(created.draft.title, '今天的体重');
  });
});

test('editing a record keeps its existing title instead of replacing it with the event name', async () => {
  await withRecordEditor({
    getRecord: async () => ({ id: 8, eventId: 4, ownerUserId: 7, title: '原来的标题',
      note: '', occurredFrom: '2026-10-04T00:00:00Z', occurredTo: '2026-10-04T23:59:00Z',
      fields: [], values: {}, templateVersion: 1, lockVersion: 1 }),
    getRecordTemplate: async () => ({ name: '体重变化', templateVersion: 1, fields: [] }),
    listRecordImages: async () => []
  }, async (page) => {
    page.setData({ recordId: 8 });
    await page.load();
    assert.equal(page.data.title, '原来的标题');
  });
});
