const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const events = [
  { id: 1, name: '生理期', category: '身体', starred: true },
  { id: 2, name: '体重变化', category: '身体', starred: false },
  { id: 3, name: '纪念日', category: '生活', starred: true }
];

async function withHome(notebook, run) {
  const pagePath = path.join(root, 'pages/notebook/home/index.js');
  const runtimePath = require.resolve(path.join(root, 'utils/api-runtime.js'));
  const pageApiPath = require.resolve(path.join(root, 'utils/page-api.js'));
  const identityPath = require.resolve(path.join(root, 'utils/identity-load.js'));
  const originals = [runtimePath, pageApiPath, identityPath].map((name) => require.cache[name]);
  const oldPage = global.Page;
  const oldWx = global.wx;
  let session = { userId: 7, accessToken: 'token-7' };
  let definition;
  const navigation = [];
  const toasts = [];
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
  global.wx = { navigateTo({ url }) { navigation.push(url); },
    showToast(options) { toasts.push(options); } };
  try {
    delete require.cache[pagePath];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update, callback) { this.data = { ...this.data, ...update }; if (callback) callback(); }
    });
    await run(page, navigation, (next) => { session = next; }, toasts);
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

function notebookStub(overrides = {}) {
  return {
    getConfig: async () => ({ maxQueryMonths: 36 }),
    listEvents: async () => events,
    getCalendar: async () => [],
    listRecords: async () => ({ items: [], hasMore: false }),
    ...overrides
  };
}

test('home defaults to all starred events and merges their month data', async () => {
  const calls = [];
  await withHome(notebookStub({
    getCalendar: async (id) => { calls.push(['calendar', id]); return [
      { date: '2026-10-03', recordCount: id }
    ]; },
    listRecords: async (id) => { calls.push(['records', id]); return {
      items: [{ id: id + 10, occurredFrom: `2026-10-03T0${id}:00:00Z`,
        occurredTo: `2026-10-03T0${id}:30:00Z`, title: `记录${id}` }], hasMore: false
    }; }
  }), async (page) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-03' });
    await page.load();
    assert.deepEqual(page.data.selectedEventIds, [1, 3]);
    assert.deepEqual(page.data.visibleSelectedEventIds, [1, 3]);
    assert.deepEqual(calls.filter(([kind]) => kind === 'calendar').map(([, id]) => id).sort(), [1, 3]);
    assert.equal(page.data.summary['2026-10-03'], 4);
    assert.deepEqual(page.data.dayTones['2026-10-03'], [1, 2]);
    assert.match(page.data.calendarHeartSrc['2026-10-03'], /filled-tone-1\.png$/);
    assert.deepEqual(page.data.visibleRecords.map((record) => record.eventName),
      ['生理期', '纪念日']);
  });
});

test('calendar heart colors follow event chips and update when selecting a date', async () => {
  await withHome(notebookStub({
    getCalendar: async (id) => [{ date: id === 1 ? '2026-10-03' : '2026-10-04', recordCount: 1 }]
  }), async (page) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-06' });
    await page.load();
    assert.match(page.data.calendarHeartSrc['2026-10-03'], /outline-tone-1\.png$/);
    assert.match(page.data.calendarHeartSrc['2026-10-04'], /outline-tone-2\.png$/);
    page.selectDate({ currentTarget: { dataset: { date: '2026-10-04' } } });
    assert.match(page.data.calendarHeartSrc['2026-10-04'], /filled-tone-2\.png$/);
  });
});

test('time flow keeps all events from one day together', async () => {
  await withHome(notebookStub({
    listRecords: async (id) => ({ items: id === 1
      ? [{ id: 11, title: '前一天', occurredFrom: '2026-10-03T09:00:00',
        occurredTo: '2026-10-03T10:00:00' },
      { id: 12, title: '当天一', occurredFrom: '2026-10-04T08:00:00',
        occurredTo: '2026-10-04T09:00:00' }]
      : [{ id: 31, title: '当天二', occurredFrom: '2026-10-04T10:00:00',
        occurredTo: '2026-10-04T11:00:00' }], hasMore: false })
  }), async (page) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-04' });
    await page.load();
    await page.switchView({ currentTarget: { dataset: { view: 'timeline' } } });
    assert.deepEqual(page.data.timelineGroups.map((group) =>
      [group.date, group.records.map((record) => record.id)]),
    [['2026-10-04', [31, 12]], ['2026-10-03', [11]]]);
  });
});

test('manual empty selection persists, while untouched selection follows new stars', async () => {
  let currentEvents = events;
  await withHome(notebookStub({ listEvents: async () => currentEvents }), async (page) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-03' });
    await page.load();
    currentEvents = events.map((item) => ({ ...item, starred: item.id === 2 }));
    await page.load();
    assert.deepEqual(page.data.selectedEventIds, [2]);
    await page.selectEvent({ currentTarget: { dataset: { id: 2 } } });
    assert.deepEqual(page.data.selectedEventIds, []);
    await page.load();
    assert.deepEqual(page.data.selectedEventIds, []);
  });
});

test('category filtering keeps selections from other categories', async () => {
  await withHome(notebookStub(), async (page) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-03' });
    await page.load();
    const bodyIndex = page.data.categories.indexOf('身体');
    await page.filterCategory({ detail: { value: bodyIndex } });
    assert.deepEqual(page.data.selectedEventIds, [1, 3]);
    assert.deepEqual(page.data.visibleSelectedEventIds, [1]);
    await page.filterCategory({ detail: { value: 0 } });
    assert.deepEqual(page.data.visibleSelectedEventIds, [1, 3]);
  });
});

test('account switch drops manual selection and uses new account favorites', async () => {
  let currentEvents = events;
  await withHome(notebookStub({ listEvents: async () => currentEvents }), async (page, navigation, setSession) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-03' });
    await page.load();
    await page.selectEvent({ currentTarget: { dataset: { id: 2 } } });
    assert.ok(page.data.selectedEventIds.includes(2));
    setSession({ userId: 8, accessToken: 'token-8' });
    currentEvents = [{ id: 4, name: '新账号事件', starred: true }];
    await page.load();
    assert.deepEqual(page.data.selectedEventIds, [4]);
  });
});

test('sixth event is refused and remaining selected events keep distinct colors', async () => {
  const many = Array.from({ length: 6 }, (_, index) =>
    ({ id: index + 1, name: `事件${index + 1}`, starred: true }));
  await withHome(notebookStub({ listEvents: async () => many }), async (page, navigation, setSession, toasts) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-03' });
    await page.load();
    assert.deepEqual(page.data.selectedEventIds, [1, 2, 3, 4, 5]);
    assert.deepEqual(Object.values(page.data.selectedEventToneMap), [1, 2, 3, 4, 5]);
    await page.selectEvent({ currentTarget: { dataset: { id: 6 } } });
    assert.deepEqual(page.data.selectedEventIds, [1, 2, 3, 4, 5]);
    assert.match(toasts.at(-1).title, /5/);
    await page.selectEvent({ currentTarget: { dataset: { id: 2 } } });
    await page.selectEvent({ currentTarget: { dataset: { id: 6 } } });
    assert.deepEqual(page.data.selectedEventIds, [1, 3, 4, 5, 6]);
    assert.equal(page.data.selectedEventToneMap[6], 2);
    assert.equal(page.data.selectedEventToneMap[3], 3);
  });
});

test('no starred events starts empty without fetching records', async () => {
  const calls = [];
  await withHome(notebookStub({
    listEvents: async () => events.map((item) => ({ ...item, starred: false })),
    getCalendar: async (id) => { calls.push(id); return []; }
  }), async (page) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-03' });
    await page.load();
    assert.deepEqual(page.data.selectedEventIds, []);
    assert.deepEqual(page.data.summary, {});
    assert.deepEqual(calls, []);
  });
});

test('history receives an event only when exactly one is selected', async () => {
  await withHome(notebookStub(), async (page, navigation) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-03' });
    await page.load();
    page.openHistory();
    assert.equal(navigation.at(-1), '/pages/notebook/detail/history/index');
    await page.selectEvent({ currentTarget: { dataset: { id: 1 } } });
    page.openHistory();
    assert.equal(navigation.at(-1), '/pages/notebook/detail/history/index?eventId=3');
  });
});

test('record creation asks for an event with multi or empty selection', async () => {
  await withHome(notebookStub(), async (page, navigation) => {
    page.setData({ month: '2026-10', selectedDate: '2026-10-03' });
    await page.load();
    page.addRecord();
    assert.equal(page.data.showRecordEventPicker, true);
    assert.deepEqual(page.data.recordEventOptions.map((event) => event.id), [1, 3]);
    page.chooseRecordEvent({ currentTarget: { dataset: { id: 3 } } });
    assert.match(navigation.at(-1), /eventId=3/);
    await page.selectEvent({ currentTarget: { dataset: { id: 1 } } });
    page.addRecord();
    assert.match(navigation.at(-1), /eventId=3/);
  });
});

test('home chips and records show focus, multiple selection, and source event', () => {
  const markup = fs.readFileSync(path.join(root, 'pages/notebook/home/index.wxml'), 'utf8');
  const styles = fs.readFileSync(path.join(root, 'pages/notebook/home/index.wxss'), 'utf8');
  assert.match(markup, /item\.starred/);
  assert.match(markup, /selectedEventMap|item\.selected/);
  assert.match(markup, /selectedEventToneMap\[item\.id\]/);
  assert.match(markup, /selectedEventToneMap\[item\.eventId\]/);
  assert.match(markup, /calendarHeartSrc\[item\.key\]/);
  assert.match(markup, /dayTones\[item\.key\]/);
  assert.match(markup, /wx:for="\{\{timelineGroups\}\}"/);
  assert.match(markup, /wx:for="\{\{group\.records\}\}"/);
  assert.match(markup, /item\.eventName/);
  assert.match(markup, /showRecordEventPicker/);
  assert.match(styles, /notebook-event\.starred/);
  for (let tone = 1; tone <= 5; tone++) {
    assert.match(styles, new RegExp(`notebook-event\\.active\\.tone-${tone}`));
    assert.match(styles, new RegExp(`notebook-row__event\\.tone-${tone}`));
  }
  const palette = ['#e43f5a', '#2f7ef7', '#1bae75', '#8957e5', '#f59e0b'];
  palette.forEach((color, index) => {
    assert.match(styles, new RegExp(`notebook-event\\.active\\.tone-${index + 1}\\{[^}]*border-color:${color}`));
    assert.match(styles, new RegExp(`notebook-day-tone\\.tone-${index + 1}\\{[^}]*background:${color}`));
  });
  const generator = fs.readFileSync(path.join(root,
    'scripts/generate-notebook-heart-assets.py'), 'utf8').toLowerCase();
  palette.forEach((color) => assert.ok(generator.includes(color)));
});
