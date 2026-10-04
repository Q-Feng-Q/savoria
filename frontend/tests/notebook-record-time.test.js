const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const calendar = require('../utils/notebook-calendar');

const localIso = (day, hour, minute) => new Date(2026, 9, day, hour, minute).toISOString();

test('record ranges display local date and time instead of raw UTC strings', () => {
  assert.equal(calendar.formatRecordRange(localIso(3, 0, 0), localIso(3, 23, 59)),
    '2026-10-03 00:00—23:59');
  assert.equal(calendar.formatRecordRange(localIso(3, 22, 30), localIso(4, 1, 0)),
    '2026-10-03 22:30—2026-10-04 01:00');
  assert.equal(calendar.formatRecordRange(localIso(3, 12, 0), localIso(3, 12, 0)),
    '2026-10-03 12:00');
  assert.equal(calendar.formatRecordRange('invalid', null), '时间待确认');
  assert.equal(calendar.formatRecordRange(null, null), '时间待确认');
});

test('repeated local clock during daylight saving fallback preserves both instants', () => {
  const output = execFileSync(process.execPath, ['-e',
    "const calendar = require('./utils/notebook-calendar'); process.stdout.write(calendar.formatRecordRange('2026-11-01T05:30:00Z', '2026-11-01T06:30:00Z'));"
  ], {
    cwd: path.resolve(__dirname, '..'),
    env: { ...process.env, TZ: 'America/New_York' },
    encoding: 'utf8'
  });
  assert.equal(output, '2026-11-01 01:30 UTC-04:00—01:30 UTC-05:00');
});

test('notebook record lists and detail render a formatted range', () => {
  const root = path.resolve(__dirname, '..');
  for (const page of ['home', 'detail/event-detail', 'detail/record-detail', 'detail/shared']) {
    const markup = fs.readFileSync(path.join(root,
      `pages/notebook/${page}/index.wxml`), 'utf8');
    assert.match(markup, /displayRange/, page);
    assert.doesNotMatch(markup, /\{\{(?:item|record)\.occurredFrom\}\}/, page);
  }
});

test('timeline view attaches readable time to each visible record', () => {
  const pagePath = path.resolve(__dirname, '../pages/notebook/home/index.js');
  const oldPage = global.Page;
  let definition;
  global.Page = (value) => { definition = value; };
  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: { ...definition.data, view: 'timeline', records: [{ id: 1,
        occurredFrom: localIso(3, 0, 0), occurredTo: localIso(3, 23, 59) }] },
      setData(update) { this.data = { ...this.data, ...update }; }
    });
    page.syncVisible();
    assert.equal(page.data.visibleRecords[0].displayRange, '2026-10-03 00:00—23:59');
  } finally {
    delete require.cache[require.resolve(pagePath)];
    global.Page = oldPage;
  }
});
