const test = require('node:test');
const assert = require('node:assert/strict');
const { createNotebookService } = require('../services/notebook');
const { createApiRuntime } = require('../utils/api-runtime');
const calendar = require('../utils/notebook-calendar');

test('notebook service uses only account endpoints and carries IANA calendar range', async () => {
  const calls = [];
  const notebook = createNotebookService({ request: async (path, options) => {
    calls.push({ path, options }); return { data: path === '/api/notebook/config'
      ? { maxQueryMonths: 24 } : [] };
  } });
  assert.deepEqual(await notebook.getConfig(), { maxQueryMonths: 24 });
  await notebook.getCalendar(7, { from: '2026-01-01', to: '2026-01-31', timeZone: 'Asia/Shanghai' });
  assert.equal(calls[1].path, '/api/notebook/events/7/calendar?from=2026-01-01&to=2026-01-31&timeZone=Asia%2FShanghai');
  assert.equal(calls.every((item) => !item.path.includes('/family/')), true);
});

test('runtime exposes notebook for an account without family ownership', async () => {
  const calls = [];
  const runtime = createApiRuntime({ baseUrl: 'https://example.test',
    sessionStore: { getSession: () => ({ userId: 9, accessToken: 'token', loginMode: 'api' }), getToken: () => 'token' },
    request: async (options) => { calls.push(options); return { statusCode: 200,
      data: { code: 0, data: { maxQueryMonths: 36 } } }; } });
  assert.deepEqual(await runtime.notebook.getConfig(), { maxQueryMonths: 36 });
  assert.equal(calls[0].url, 'https://example.test/notebook/config');
  assert.equal(calls[0].header['X-User-Id'], 9);
});

test('calendar maps local month and dates without exceeding one month', () => {
  assert.deepEqual(calendar.monthBounds('2026-02'), { from: '2026-02-01', to: '2026-02-28' });
  assert.equal(calendar.shiftMonth('2026-12', 1), '2027-01');
  assert.equal(calendar.monthDays('2026-02').filter((day) => !day.blank).length, 28);
});

test('record template and private image metadata use notebook account routes', async () => {
  const calls = [];
  const notebook = createNotebookService({ request: async (path) => {
    calls.push(path); return { data: {} };
  } });
  await notebook.getRecordTemplate(7);
  await notebook.listRecordImages(9);
  assert.deepEqual(calls, ['/api/notebook/events/7/record-template',
    '/api/notebook/images/records/9']);
});
