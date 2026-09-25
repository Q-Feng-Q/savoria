const test = require('node:test');
const assert = require('node:assert/strict');
const { createFeedbackService } = require('../services/feedback');
const { validateDraft, presentFeedback, createSubmission } = require('../utils/feedback');

test('feedback validates trimmed content and all selected attachments', () => {
  assert.match(validateDraft({ type: 'BUG', content: '  ', images: [] }), /描述/);
  assert.match(validateDraft({ type: 'OTHER', content: '问题', images: [] }), /类型/);
  assert.match(validateDraft({ type: 'BUG', content: '字'.repeat(2001), images: [] }), /2000/);
  assert.match(validateDraft({ type: 'BUG', content: '问题', images: [{ state: 'failed' }] }), /图片/);
  assert.match(validateDraft({ type: 'BUG', content: '问题', images: Array(7).fill({ state: 'ready', imageId: 1 }) }), /6/);
  assert.equal(validateDraft({ type: 'SUGGESTION', content: ' 建议 ', images: [] }), '');
});

test('uncertain submission retries identical payload and requestId', () => {
  const submission = createSubmission(() => 'request-1');
  const first = submission.prepare({ type: 'BUG', content: ' 问题 ', images: [{ state: 'ready', imageId: 2 }] });
  assert.deepEqual(first, { requestId: 'request-1', type: 'BUG', content: '问题', imageIds: [2] });
  assert.deepEqual(submission.prepare({ type: 'SUGGESTION', content: 'changed', images: [] }), first);
  assert.equal(submission.pending(), true);
  submission.clear();
  assert.equal(submission.pending(), false);
});

test('feedback facade uses owner endpoints and authenticated binary transfer', async () => {
  const calls = [];
  const service = createFeedbackService({ baseUrl: 'http://localhost:8080/', getSession: () => ({ userId: 1, accessToken: 'secret' }),
    request: async (...args) => { calls.push(args); return { data: { items: [], feedbackId: 3 } }; },
    upload: async (options) => { calls.push(options); return { statusCode: 200, data: JSON.stringify({ code: 0, data: { imageId: 8 } }) }; },
    download: async (options) => { calls.push(options); return { statusCode: 200, tempFilePath: '/private/temp.png' }; }
  });
  await service.list(2);
  await service.detail(3);
  await service.submit({ requestId: 'abc' });
  assert.equal((await service.uploadImage('/local.png')).imageId, 8);
  assert.equal(await service.downloadImage(8), '/private/temp.png');
  assert.equal(calls[0][0], '/api/feedback?page=2&pageSize=20');
  assert.equal(calls[1][0], '/api/feedback/3');
  assert.equal(calls[2][1].method, 'POST');
  assert.equal(calls[3].header.Authorization, 'Bearer secret');
  assert.equal(calls[4].header.Authorization, 'Bearer secret');
  assert.equal(calls[4].url, 'http://localhost:8080/feedback/images/8');
  assert.equal(calls[4].url.includes('secret'), false);
});

test('binary errors are rejected and never used as preview paths', async () => {
  const service = createFeedbackService({ download: async () => ({ statusCode: 403, tempFilePath: 'bad' }),
    upload: async () => ({ statusCode: 413, data: '{"message":"图片太大"}' }) });
  await assert.rejects(service.downloadImage(1), /图片/);
  await assert.rejects(service.uploadImage('x'), /图片太大/);
});

test('status presentation uses readable labels without invented replies', () => {
  const row = presentFeedback({ type: 'BUG', status: 'OPEN', createdAt: '2026-09-21T12:00:00', content: 'x' });
  assert.equal(row.typeLabel, 'BUG 反馈');
  assert.equal(row.statusLabel, '待处理');
  assert.equal(row.reply, undefined);
});

function page(name) {
  let definition;
  global.Page = (value) => { definition = value; };
  const path = require.resolve(`../pages/account/feedback-${name}/index`);
  delete require.cache[path];
  require(path);
  const instance = { ...definition, data: structuredClone(definition.data), setData(value) { Object.assign(this.data, value); } };
  return instance;
}

test('submission page blocks duplicate requests and retains uncertain retry payload', async () => {
  const instance = page('create');
  let finish;
  const calls = [];
  instance.isCurrent = () => true;
  instance.submission = createSubmission(() => 'stable');
  instance.runtime = { feedback: { submit: (payload) => { calls.push(payload); return new Promise((resolve, reject) => { finish = reject; }); } } };
  instance.setData({ content: '问题', images: [], type: 'BUG' });
  global.wx = { showToast() {} };
  const first = instance.submit();
  await instance.submit();
  assert.equal(calls.length, 1);
  finish(new Error('timeout'));
  await first;
  assert.equal(instance.data.uncertain, true);
  instance.input({ detail: { value: '不可修改' } });
  assert.equal(instance.data.content, '问题');
  assert.equal(instance.data.saving, false);
});

test('feedback pagination appends server pages and ignores stale account results', async () => {
  const instance = page('list');
  let current = true;
  instance.isCurrent = () => current;
  instance.runtime = { feedback: { list: async (number) => ({ items: [{ feedbackId: number, status: 'OPEN' }], total: 2 }) } };
  await instance.load(true);
  await instance.load(false);
  assert.deepEqual(instance.data.items.map((row) => row.feedbackId), [1, 2]);
  current = false;
  await instance.load(true);
  assert.equal(instance.data.items.length, 2);
});

test('feedback entries are available without a family and pages are registered', () => {
  const { buildProfileMenuGroups } = require('../utils/profile-menu');
  assert.ok(buildProfileMenuGroups({}).flatMap((group) => group.items).some((item) => item.key === 'feedback'));
  const config = require('../app.json');
  for (const name of ['create', 'list', 'detail']) assert.ok(config.pages.includes(`pages/account/feedback-${name}/index`));
});

test('upload failure exposes server quota message and can be retried', async () => {
  const instance = page('create');
  instance.isCurrent = () => true;
  let attempt = 0;
  instance.runtime = { feedback: { uploadImage: async () => { if (++attempt === 1) throw new Error('每小时最多上传 30 张图片'); return { imageId: 'uuid' }; } } };
  instance.setData({ images: [{ key: 'a', path: 'local', state: 'uploading' }] });
  global.wx = { showToast() {} };
  await instance.upload('a');
  assert.match(instance.data.images[0].error, /30/);
  await instance.upload('a');
  assert.equal(instance.data.images[0].state, 'ready');
});

test('image retry is serialized and stale downloads are unlinked', async () => {
  const instance = page('detail');
  let current = true;
  instance.isCurrent = () => current;
  instance.setData({ images: [{ imageId: 'one', failed: true, path: '' }] });
  let finish;
  let calls = 0;
  instance.runtime = { feedback: { downloadImage: () => { calls++; return new Promise((resolve) => { finish = resolve; }); } } };
  const removed = [];
  global.wx = { getFileSystemManager: () => ({ unlink: ({ filePath }) => removed.push(filePath) }) };
  const first = instance.download('one', 1);
  await instance.download('one', 1);
  assert.equal(calls, 1);
  current = false;
  finish('/private/stale');
  await first;
  assert.deepEqual(removed, ['/private/stale']);
  assert.equal(instance.data.images[0].path, '');
});

test('in-flight upload cannot populate another account draft', async () => {
  const instance = page('create');
  let current = true;
  instance.isCurrent = () => current;
  instance.setData({ images: [{ key: 'a', path: 'local', state: 'uploading' }] });
  let finish;
  instance.runtime = { feedback: { uploadImage: () => new Promise((resolve) => { finish = resolve; }) } };
  const pending = instance.upload('a');
  current = false;
  instance.setData({ images: [] });
  finish({ imageId: 'old-account' });
  await pending;
  assert.deepEqual(instance.data.images, []);
});

test('compressed originals and unprocessed selected images are removed', async () => {
  const instance = page('create');
  instance.isCurrent = () => true;
  instance.submission = createSubmission();
  const removed = [];
  global.wx = {
    chooseMedia: ({ success }) => success({ tempFiles: [{ tempFilePath: 'original', size: 5 * 1024 * 1024 }, { tempFilePath: 'invalid', size: 1 }, { tempFilePath: 'remaining', size: 1 }] }),
    getImageInfo: ({ src, success }) => success({ type: src === 'invalid' ? 'gif' : 'png', width: 100, height: 100 }),
    compressImage: ({ success }) => success({ tempFilePath: 'compressed' }),
    getFileSystemManager: () => ({ getFileInfo: ({ success }) => success({ size: 100 }), unlink: ({ filePath }) => removed.push(filePath) }),
    showToast() {}
  };
  instance.runtime = { feedback: { uploadImage: async () => ({ imageId: 'one' }) } };
  await instance.chooseImages();
  assert.deepEqual(removed.sort(), ['invalid', 'original', 'remaining']);
  assert.equal(instance.data.images[0].path, 'compressed');
  instance.clearDraft();
  assert.ok(removed.includes('compressed'));
});

test('successful submission stays locked until navigation completes', async () => {
  const instance = page('create');
  instance.isCurrent = () => true;
  instance.submission = createSubmission();
  instance.setData({ content: '问题', images: [] });
  let count = 0;
  instance.runtime = { feedback: { submit: async () => { count++; return { feedbackId: 5 }; } } };
  global.wx = { redirectTo() {} };
  await instance.submit();
  await instance.submit();
  assert.equal(count, 1);
});
