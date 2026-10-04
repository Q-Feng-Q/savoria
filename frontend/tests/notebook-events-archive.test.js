const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');

async function withEventsPage(notebook, run) {
  const pagePath = path.join(root, 'pages/notebook/detail/events/index.js');
  const runtimePath = require.resolve(path.join(root, 'utils/api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils/page-api.js'));
  const identityPath = require.resolve(path.join(root, 'utils/identity-load.js'));
  const originals = [runtimePath, pageApiPath, identityPath].map((name) => require.cache[name]);
  const oldPage = global.Page;
  let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ notebook }) } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ userId: 7 }), showApiError: (error) => { throw error; }
  } };
  require.cache[identityPath] = { exports: { createIdentityLoadGuard: () => ({
    begin: () => 1, isCurrent: () => true
  }) } };
  global.Page = (value) => { definition = value; };
  try {
    delete require.cache[pagePath];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: { ...definition.data },
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
  }
}

test('archive view shows only archived events and returns to active events', async () => {
  const calls = [];
  const allEvents = [{ id: 1, archived: false }, { id: 2, archived: true }];
  await withEventsPage({ listEvents: async (includeArchived) => {
    calls.push(includeArchived);
    return includeArchived ? allEvents : allEvents.filter((item) => !item.archived);
  } }, async (page) => {
    await page.load();
    assert.deepEqual(page.data.events.map((item) => item.id), [1]);
    await page.toggleArchived();
    assert.deepEqual(page.data.events.map((item) => item.id), [2]);
    await page.toggleArchived();
    assert.deepEqual(page.data.events.map((item) => item.id), [1]);
    assert.deepEqual(calls, [false, true, false]);
  });
});

test('archive view labels and empty state describe archived events only', () => {
  const markup = fs.readFileSync(path.join(root, 'pages/notebook/detail/events/index.wxml'), 'utf8');
  assert.match(markup, /查看归档/);
  assert.match(markup, /返回事件/);
  assert.match(markup, /归档中还没有事件/);
});
