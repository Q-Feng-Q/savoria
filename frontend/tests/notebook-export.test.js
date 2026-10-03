const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { prepareExport, writeExportFile } = require('../utils/notebook-export');

const root = path.join(__dirname, '..');
const source = (name) => fs.readFileSync(path.join(root, name), 'utf8');

test('clipboard preparation prompts a file fallback for large JSON', () => {
  const payload = { schemaVersion: 'notebook-export/v1', records: [{ note: 'x'.repeat(300) }] };
  const result = prepareExport(payload, 100);
  assert.equal(result.tooLargeForClipboard, true);
  assert.equal(result.text.includes('notebook-export/v1'), true);
});

test('JSON export writes only under private user-data path', async () => {
  const calls = [];
  const fileSystem = { writeFile(options) { calls.push(options); options.success(); } };
  const filePath = await writeExportFile({ schemaVersion: 'notebook-export/v1', records: [] },
    { fileSystem, userDataPath: 'wxfile://usr', now: 123 });
  assert.equal(filePath, 'wxfile://usr/notebook-export-123.json');
  assert.equal(calls[0].encoding, 'utf8');
  assert.equal(JSON.parse(calls[0].data).schemaVersion, 'notebook-export/v1');
  await assert.rejects(writeExportFile({}, { fileSystem, userDataPath: '../unsafe' }));
});

test('export screen confirms external-data risk and clears payload after use', () => {
  const page = source('pages/notebook/detail/export/index.js');
  assert.match(page, /showModal/);
  assert.match(page, /setClipboardData/);
  assert.match(page, /shareFileMessage/);
  assert.match(page, /writeExportFile/);
  assert.match(page, /payload\s*=\s*null/);
  assert.match(page, /onHide/);
});

test('private image service uses authenticated transfer and rejects failed preview', async () => {
  const { createNotebookImages } = require('../utils/notebook-images');
  const requests = [];
  const images = createNotebookImages({ baseUrl: 'https://example.test',
    getSession: () => ({ accessToken: 'secret', userId: 3 }),
    upload: async (options) => { requests.push(options); return { statusCode: 200,
      data: JSON.stringify({ code: 0, data: { imageId: 4, valueKey: 'private.png' } }) }; },
    download: async () => ({ statusCode: 403, tempFilePath: 'wxfile://tmp/image' }),
    unlink: () => {} });
  assert.deepEqual(await images.stage(7, 'wxfile://tmp/upload'),
    { imageId: 4, valueKey: 'private.png' });
  assert.equal(requests[0].formData.eventId, 7);
  assert.equal(requests[0].header.Authorization, 'Bearer secret');
  await assert.rejects(images.preview(4), /图片读取失败/);
});

test('notebook service chooses staged upload for create and attached upload for edit', async () => {
  const { createNotebookService } = require('../services/notebook');
  const paths = [];
  const images = { stage: async (eventId, filePath) => { paths.push(['stage', eventId, filePath]);
    return { imageId: 1, valueKey: 'a.png' }; },
  upload: async (recordId, filePath) => { paths.push(['upload', recordId, filePath]);
    return { imageId: 2, valueKey: 'b.png' }; },
  preview: async (id) => `wxfile://usr/${id}.png` };
  const notebook = createNotebookService({ request: async () => ({ data: {} }), images });
  assert.deepEqual(await notebook.uploadImage({ eventId: 7, filePath: '/tmp/a.png' }),
    { imageId: 1, valueKey: 'a.png' });
  assert.deepEqual(await notebook.uploadImage({ eventId: 7, recordId: 9, filePath: '/tmp/b.png' }),
    { imageId: 2, valueKey: 'b.png' });
  assert.equal(await notebook.previewImage(2), 'wxfile://usr/2.png');
  assert.deepEqual(paths, [['stage', 7, '/tmp/a.png'], ['upload', 9, '/tmp/b.png']]);
});

test('private image transfer discards bytes and keys when the active account switches', async () => {
  const { createNotebookImages } = require('../utils/notebook-images');
  let session = { userId: 1, accessToken: 'first' };
  const discarded = [];
  const images = createNotebookImages({ baseUrl: 'https://example.test', getSession: () => session,
    upload: async () => { session = { userId: 2, accessToken: 'second' };
      return { statusCode: 200, data: JSON.stringify({ code: 0,
        data: { imageId: 3, valueKey: 'private.png' } }) }; },
    download: async () => { session = { userId: 2, accessToken: 'second' };
      return { statusCode: 200, tempFilePath: 'wxfile://tmp/secret' }; },
    unlink: (path) => discarded.push(path) });
  await assert.rejects(images.stage(7, 'wxfile://tmp/new'), /账号已切换/);
  session = { userId: 1, accessToken: 'first' };
  await assert.rejects(images.preview(3), /账号已切换/);
  assert.deepEqual(discarded, ['wxfile://tmp/secret']);
});
