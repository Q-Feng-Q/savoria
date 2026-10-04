const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (name) => fs.readFileSync(path.join(root, name), 'utf8');

async function withHistoryPage(notebook, run) {
  const pagePath = path.join(root, 'pages/notebook/detail/history/index.js');
  const runtimePath = require.resolve(path.join(root, 'utils/api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils/page-api.js'));
  const identityPath = require.resolve(path.join(root, 'utils/identity-load.js'));
  const originals = [runtimePath, pageApiPath, identityPath].map((name) => require.cache[name]);
  const oldPage = global.Page;
  const oldWx = global.wx;
  let session = { userId: 7, accessToken: 'token-7' };
  let definition;
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ notebook }) } };
  require.cache[pageApiPath] = { exports: {
    requireSession: () => session, showApiError: (error) => { throw error; }
  } };
  require.cache[identityPath] = { exports: { createIdentityLoadGuard: () => {
    let generation = 0;
    return {
      begin(current) { generation += 1; return { generation, userId: current && current.userId }; },
      isCurrent(token) { return token && token.generation === generation
        && token.userId === (session && session.userId); }
    };
  } } };
  global.Page = (value) => { definition = value; };
  global.wx = { navigateTo() {} };
  try {
    delete require.cache[pagePath];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); }
    });
    await run(page, (next) => { session = next; });
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

test('history page uses the same heart assets for records, selection, and legend', () => {
  const app = JSON.parse(read('app.json'));
  const notebook = app.subPackages.find((part) => part.root === 'pages/notebook/detail');
  assert.ok(notebook.pages.includes('history/index'));
  assert.match(read('pages/notebook/home/index.wxml'), /bindtap="openHistory"/);
  assert.match(read('pages/notebook/detail/event-detail/index.wxml'), /bindtap="openHistory"/);
  const markup = read('pages/notebook/detail/history/index.wxml');
  const styles = read('pages/notebook/detail/history/index.wxss');
  assert.match(markup, /mode="date"/);
  assert.match(markup, /history-day-heart/);
  assert.match(markup, /history-day-number/);
  assert.match(markup, /day\.hasRecord \|\| day\.key === selectedDate/);
  assert.match(markup, /\/assets\/ui\/notebook-heart-filled\.png/);
  assert.match(markup, /\/assets\/ui\/notebook-heart-outline\.png/);
  assert.match(markup, /class="history-summary-heart"/);
  assert.doesNotMatch(markup, /[♡♥]/);
  assert.match(styles, /grid-template-columns:\s*repeat\(2/);
  assert.match(styles, /\.history-day-heart\s*\{[^}]*width:\s*43rpx;[^}]*height:\s*43rpx/);
  assert.match(styles, /\.history-day\.has-record \.history-day-number,\.history-day\.selected \.history-day-number\s*\{[^}]*transform:\s*translateY\(-3rpx\)/);
  assert.doesNotMatch(styles, /\.history-day\.selected\s*\{[^}]*background:/);
});

test('history query waits for config and enforces live touched-month limit', async () => {
  let resolveConfig;
  const config = new Promise((resolve) => { resolveConfig = resolve; });
  const calls = [];
  await withHistoryPage({
    getConfig: () => config,
    listEvents: async () => [{ id: 7, name: '生理期' }],
    getCalendar: async (id, range) => { calls.push({ id, range }); return []; },
    listRecords: async () => ({ items: [], hasMore: false })
  }, async (page) => {
    page.onLoad({ eventId: '7' });
    page.setData({ from: '2026-10-01', to: '2026-11-30' });
    const loading = page.load();
    await Promise.resolve();
    assert.equal(calls.length, 0);
    resolveConfig({ maxQueryMonths: 2 });
    await loading;
    assert.equal(calls.length, 1);
    assert.equal(calls[0].id, 7);
    assert.equal(calls[0].range.from, '2026-10-01');
    page.changeRange({ currentTarget: { dataset: { field: 'to' } }, detail: { value: '2026-12-01' } });
    await page.search();
    assert.equal(calls.length, 1);
    assert.match(page.data.queryError, /2 个日历月/);
  });
});

test('calendar clips dates, loads selected-day records, and expands month batches', async () => {
  const recordCalls = [];
  await withHistoryPage({
    getConfig: async () => ({ maxQueryMonths: 36 }),
    listEvents: async () => [{ id: 7, name: '生理期' }],
    getCalendar: async () => [{ date: '2026-10-03', recordCount: 1 }],
    listRecords: async (id, query) => {
      recordCalls.push(query);
      return { items: [{ id: 9, title: '记录', occurredFrom: '2026-10-03T00:00:00Z',
        occurredTo: '2026-10-03T01:00:00Z' }], hasMore: false };
    }
  }, async (page) => {
    page.onLoad({ eventId: '7' });
    page.setData({ from: '2026-06-15', to: '2026-11-02' });
    await page.load();
    assert.equal(page.data.months.length, 4);
    assert.equal(page.data.hasMoreMonths, true);
    page.loadMoreMonths();
    assert.equal(page.data.months.length, 6);
    const before = recordCalls.length;
    await page.selectDay({ currentTarget: { dataset: { date: '2026-06-01' } } });
    assert.equal(recordCalls.length, before);
    await page.selectDay({ currentTarget: { dataset: { date: '2026-10-03' } } });
    assert.equal(recordCalls.at(-1).from, '2026-10-03');
    assert.equal(page.data.records[0].displayRange.includes('—'), true);
  });
});

test('account switch discards an in-flight calendar response', async () => {
  let resolveCalendar;
  const calendar = new Promise((resolve) => { resolveCalendar = resolve; });
  await withHistoryPage({
    getConfig: async () => ({ maxQueryMonths: 36 }),
    listEvents: async () => [{ id: 7, name: '生理期' }],
    getCalendar: () => calendar,
    listRecords: async () => ({ items: [], hasMore: false })
  }, async (page, setSession) => {
    page.onLoad({ eventId: '7' });
    const loading = page.load();
    for (let i = 0; i < 8 && page.data.phase !== 'ready'; i++) await Promise.resolve();
    setSession({ userId: 8, accessToken: 'token-8' });
    resolveCalendar([{ date: '2026-10-03', recordCount: 1 }]);
    await loading;
    assert.deepEqual(page.data.months, []);
    assert.deepEqual(page.data.records, []);
  });
});

test('tapping query button starts a fresh request after changing dates', async () => {
  const calls = [];
  await withHistoryPage({
    getConfig: async () => ({ maxQueryMonths: 36 }),
    listEvents: async () => [{ id: 7, name: '生理期' }],
    getCalendar: async (id, range) => { calls.push(range); return []; },
    listRecords: async () => ({ items: [], hasMore: false })
  }, async (page) => {
    page.onLoad({ eventId: '7' });
    await page.load();
    page.changeRange({ currentTarget: { dataset: { field: 'from' } },
      detail: { value: '2026-09-01' } });
    assert.deepEqual(page.data.months, []);
    await page.search({ type: 'tap' });
    assert.equal(calls.length, 2);
    assert.equal(calls[1].from, '2026-09-01');
  });
});

test('rapid day changes ignore stale records and selected-day pagination appends', async () => {
  let resolveOldDay;
  const oldDay = new Promise((resolve) => { resolveOldDay = resolve; });
  await withHistoryPage({
    getConfig: async () => ({ maxQueryMonths: 36 }),
    listEvents: async () => [{ id: 7, name: '生理期' }],
    getCalendar: async () => [
      { date: '2026-10-03', recordCount: 1 }, { date: '2026-10-04', recordCount: 2 }
    ],
    listRecords: async (id, query) => {
      if (query.from === '2026-10-03') return oldDay;
      return query.page === 0
        ? { items: [{ id: 10, title: '第一条' }], hasMore: true }
        : { items: [{ id: 11, title: '第二条' }], hasMore: false };
    }
  }, async (page) => {
    page.onLoad({ eventId: '7' });
    page.setData({ from: '2026-10-01', to: '2026-10-31' });
    await page.load();
    const first = page.selectDay({ currentTarget: { dataset: { date: '2026-10-03' } } });
    await page.selectDay({ currentTarget: { dataset: { date: '2026-10-04' } } });
    resolveOldDay({ items: [{ id: 9, title: '过期结果' }], hasMore: false });
    await first;
    assert.deepEqual(page.data.records.map((record) => record.id), [10]);
    await page.loadMoreRecords();
    assert.deepEqual(page.data.records.map((record) => record.id), [10, 11]);
    assert.equal(page.data.hasMoreRecords, false);
  });
});

test('account reload clears prior account calendar and open day sheet immediately', async () => {
  let events = [{ id: 7, name: '生理期' }];
  await withHistoryPage({
    getConfig: async () => ({ maxQueryMonths: 36 }),
    listEvents: async () => events,
    getCalendar: async () => [{ date: '2026-10-03', recordCount: 1 }],
    listRecords: async () => ({ items: [{ id: 9, title: '旧账号记录' }], hasMore: false })
  }, async (page, setSession) => {
    page.onLoad({ eventId: '7' });
    page.setData({ from: '2026-10-01', to: '2026-10-31' });
    await page.load();
    await page.selectDay({ currentTarget: { dataset: { date: '2026-10-03' } } });
    assert.equal(page.data.records.length, 1);
    setSession({ userId: 8, accessToken: 'token-8' });
    events = [];
    const reloading = page.load();
    assert.deepEqual(page.data.months, []);
    assert.deepEqual(page.data.records, []);
    assert.equal(page.data.selectedDate, '');
    await reloading;
    assert.equal(page.data.eventId, null);
  });
});

test('single-month platform limit clamps the untouched default range', async () => {
  const calls = [];
  await withHistoryPage({
    getConfig: async () => ({ maxQueryMonths: 1 }),
    listEvents: async () => [{ id: 7, name: '生理期' }],
    getCalendar: async (id, range) => { calls.push(range); return []; },
    listRecords: async () => ({ items: [], hasMore: false })
  }, async (page) => {
    page.onLoad({ eventId: '7' });
    await page.load();
    assert.equal(calls.length, 1);
    assert.equal(calls[0].from.slice(0, 7), calls[0].to.slice(0, 7));
    assert.equal(page.data.queryError, '');
  });
});

test('history can open an archived event passed from its detail page', async () => {
  let includeArchived;
  let queriedEvent;
  await withHistoryPage({
    getConfig: async () => ({ maxQueryMonths: 36 }),
    listEvents: async (include) => {
      includeArchived = include;
      return include ? [{ id: 7, name: '现用事件' }, { id: 8, name: '归档事件', archived: true }]
        : [{ id: 7, name: '现用事件' }];
    },
    getCalendar: async (id) => { queriedEvent = id; return []; },
    listRecords: async () => ({ items: [], hasMore: false })
  }, async (page) => {
    page.onLoad({ eventId: '8' });
    await page.load();
    assert.equal(includeArchived, true);
    assert.equal(page.data.eventId, 8);
    assert.equal(queriedEvent, 8);
  });
});
