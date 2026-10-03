const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');

test('event and record workflow pages are registered with working controls', () => {
  const app = JSON.parse(read('app.json'));
  const notebookPackage = app.subPackages.find((part) => part.root === 'pages/notebook/detail');
  assert.ok(notebookPackage);
  for (const name of ['events', 'event-edit', 'event-detail', 'record-edit', 'record-detail']) {
    const page = `pages/notebook/detail/${name}/index`;
    assert.ok(notebookPackage.pages.includes(`${name}/index`), page);
    assert.ok(read(`${page}.wxml`).includes('bindtap='), name);
    assert.ok(read(`${page}.wxss`).includes('safe-area-inset-bottom'), name);
  }
  assert.match(read('pages/notebook/detail/events/index.js'), /getDeleteImpact/);
  assert.match(read('pages/notebook/detail/events/index.js'), /deleteEvent/);
  assert.match(read('pages/notebook/detail/event-edit/index.js'), /\bmoveField\s*\(/);
  assert.match(read('pages/notebook/detail/record-edit/index.js'), /upgradeRecord/);
  assert.match(read('pages/notebook/detail/record-edit/index.js'), /createDirtyForm/);
  assert.match(read('pages/notebook/detail/record-detail/index.wxml'), /templateVersion/);
});

test('home page exhausts monthly pages and Today changes back to current month', () => {
  const home = read('pages/notebook/home/index.js');
  assert.match(home, /while\s*\(.*hasMore|for\s*\(.*hasMore/);
  assert.match(home, /switchView[\s\S]*monthKey\(new Date\(\)\)/);
});

test('home and event detail expose category and bounded historical browsing', () => {
  const home = read('pages/notebook/home/index.js');
  const detail = read('pages/notebook/detail/event-detail/index.js');
  assert.match(home, /selectedCategory/);
  assert.match(home, /filterCategory/);
  assert.match(detail, /maxQueryMonths/);
  assert.match(detail, /listRecords/);
  assert.match(detail, /loadMore/);
  assert.match(detail, /openExport/);
  assert.match(read('pages/notebook/detail/event-detail/index.wxml'), /mode="date"/);
});

test('record details authorize edit controls and resolve private image previews', () => {
  const detail = read('pages/notebook/detail/record-detail/index.js');
  assert.match(detail, /listRecordImages/);
  assert.match(detail, /previewImage/);
  assert.match(detail, /recordWithinGrant/);
  assert.match(read('pages/notebook/detail/record-detail/index.wxml'), /wx:if="{{canEdit}}"/);
});

test('private event and record forms refuse a switched account before saving', () => {
  for (const name of ['event-edit', 'record-edit']) {
    const form = read(`pages/notebook/detail/${name}/index.js`);
    assert.match(form, /boundUserId/);
    assert.match(form, /onShow\s*\(/);
    assert.match(form, /createIdentityLoadGuard/);
  }
});

test('record date-time fields have picker controls and upgrade does not discard metadata edits', () => {
  const page = read('pages/notebook/detail/record-edit/index.js');
  const view = read('pages/notebook/detail/record-edit/index.wxml');
  assert.match(page, /onFieldDatetime\s*\(/);
  assert.match(view, /item\.editor === 'datetime'/);
  assert.match(view, /bindchange="onFieldDatetime"/);
  assert.match(page, /upgradeMode[\s\S]*originalMetadata/);
  assert.match(page, /cancelUpgrade\s*\(/);
  assert.match(view, /bindtap="cancelUpgrade"/);
});
