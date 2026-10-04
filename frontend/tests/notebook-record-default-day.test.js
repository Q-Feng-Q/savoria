const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

test('new record defaults to the full selected calendar day', () => {
  const root = path.resolve(__dirname, '..');
  const pagePath = path.join(root, 'pages/notebook/detail/record-edit/index.js');
  const pageApiPath = require.resolve(path.join(root, 'utils/page-api.js'));
  const oldPageApi = require.cache[pageApiPath];
  const oldPage = global.Page;
  const oldWx = global.wx;
  let definition;
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ userId: 7 }), showApiError() {}
  } };
  global.Page = (value) => { definition = value; };
  global.wx = {};
  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update) { this.data = { ...this.data, ...update }; },
      load() {}
    });
    page.onLoad({ eventId: '4', date: '2026-10-03' });
    assert.equal(page.data.occurredDate, '2026-10-03');
    assert.equal(page.data.endDate, '2026-10-03');
    assert.equal(page.data.startTime, '00:00');
    assert.equal(page.data.endTime, '23:59');
  } finally {
    delete require.cache[require.resolve(pagePath)];
    if (oldPageApi) require.cache[pageApiPath] = oldPageApi;
    else delete require.cache[pageApiPath];
    global.Page = oldPage;
    global.wx = oldWx;
  }
});
