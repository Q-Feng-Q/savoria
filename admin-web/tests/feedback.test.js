const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = file => fs.readFileSync(path.join(__dirname, '..', file), 'utf8');
async function moduleUnderTest() {
  const file = path.join(__dirname, '../src/utils/feedback-management.js');
  assert.ok(fs.existsSync(file), 'feedback management controller exists');
  return import(`data:text/javascript;base64,${Buffer.from(fs.readFileSync(file)).toString('base64')}`);
}
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };
async function setup(overrides = {}) {
  const { createFeedbackManagement } = await moduleUnderTest();
  const revoked = [], calls = [];
  const api = { list: async query => { calls.push(query); return { items: [], total: 0 }; }, detail: async id => ({ feedbackId: id, status: 'OPEN', reply: '', version: 2, images: [] }), update: async (id, payload) => ({ feedbackId: id, ...payload, version: 3, images: [] }), image: async () => new Blob(['image']), ...overrides };
  const state = {};
  const controller = createFeedbackManagement({ state, api, createUrl: () => `blob:${calls.push('url')}`, revokeUrl: url => revoked.push(url) });
  return { state, controller, calls, revoked };
}
test('filtered server pagination ignores old responses', async () => {
  const first = deferred(), second = deferred(); let n = 0;
  const { state, controller, calls } = await setup({ list: query => { calls.push(query); return (++n === 1 ? first : second).promise; } });
  const a = controller.loadList(); state.type = 'BUG'; const b = controller.filter();
  second.resolve({ items: [{ feedbackId: 2 }], total: 41 }); await b;
  first.resolve({ items: [{ feedbackId: 1 }], total: 1 }); await a;
  assert.equal(state.items[0].feedbackId, 2); assert.equal(state.total, 41);
  assert.deepEqual(calls[1], { page: 1, pageSize: 20, type: 'BUG', status: '' });
});
test('old detail and private image responses never replace the active detail', async () => {
  const old = deferred(), image = deferred();
  const { state, controller, calls } = await setup({ detail: id => id === 1 ? old.promise : Promise.resolve({ feedbackId: id, status: 'OPEN', images: [{ imageId: 'secret' }] }), image: () => image.promise });
  const a = controller.open(1); const b = controller.open(2);
  await new Promise(resolve => setImmediate(resolve)); controller.close();
  old.resolve({ feedbackId: 1, images: [] }); image.resolve(new Blob(['image'])); await Promise.all([a, b]);
  assert.equal(state.detail, null); assert.equal(calls.includes('url'), false);
});
test('close, account changes and disposal revoke private URLs and clear private data', async () => {
  const { state, controller, revoked } = await setup({ detail: async id => ({ feedbackId: id, status: 'OPEN', images: [{ imageId: 'secret' }] }) });
  await controller.open(1); assert.equal(state.images.length, 1); controller.close(); assert.equal(revoked.length, 1);
  await controller.open(2); state.items = [{ content: 'private' }]; controller.reset();
  assert.equal(revoked.length, 2); assert.deepEqual(state.items, []); assert.equal(state.detail, null);
  await controller.open(3); controller.dispose(); assert.equal(revoked.length, 3);
});
test('account reset invalidates pending list and save responses', async () => {
  const list = deferred(), save = deferred();
  const { state, controller } = await setup({ list: () => list.promise, update: () => save.promise });
  await controller.open(1); const a = controller.loadList(), b = controller.save(); controller.reset();
  list.resolve({ items: [{ content: 'old private' }], total: 1 }); save.resolve({ feedbackId: 1, content: 'old private', images: [] }); await Promise.all([a, b]);
  assert.deepEqual(state.items, []); assert.equal(state.detail, null); assert.equal(state.saving, false);
});
test('reply validation trims, caps length and requires a resolution explanation', async () => {
  const { validateFeedbackUpdate } = await moduleUnderTest();
  assert.throws(() => validateFeedbackUpdate('RESOLVED', '  ', 0), /回复/);
  assert.throws(() => validateFeedbackUpdate('CLOSED', '', 0), /回复/);
  assert.throws(() => validateFeedbackUpdate('OPEN', 'a'.repeat(2001), 0), /2000/);
  assert.throws(() => validateFeedbackUpdate('INVALID', '', 0), /状态/);
  assert.deepEqual(validateFeedbackUpdate('PROCESSING', '  已确认  ', 2), { status: 'PROCESSING', reply: '已确认', version: 2 });
});
test('version conflicts preserve draft and block resave until explicit refresh', async () => {
  let saves = 0;
  const { state, controller } = await setup({ update: async () => { saves++; throw Object.assign(new Error('changed'), { status: 409 }); } });
  await controller.open(1); state.reply = '我的草稿'; await controller.save();
  assert.equal(state.conflict, true); assert.equal(state.reply, '我的草稿'); await controller.save(); assert.equal(saves, 1);
  await controller.refresh(); assert.equal(state.conflict, false); assert.equal(state.reply, '');
});
test('list, detail and image failures are retryable without exposing raw URLs', async () => {
  let fail = true;
  const { state, controller } = await setup({ list: async () => { if (fail) throw new Error('offline'); return { items: [], total: 0 }; }, image: async () => { throw new Error('image offline'); }, detail: async id => ({ feedbackId: id, status: 'OPEN', images: [{ imageId: 'a' }] }) });
  await controller.loadList(); assert.equal(state.listError, 'offline'); fail = false; await controller.loadList(); assert.equal(state.listError, '');
  await controller.open(1); assert.equal(state.images[0].error, 'image offline'); assert.equal(state.images[0].url, '');
});
test('feedback navigation is platform-only and view binds lifecycle and safe plain text', () => {
  const router = read('src/router/index.js');
  assert.match(router, /path: 'platform-feedback'[^}]+requiresPlatformAdmin: true/);
  assert.match(read('src/workspaces.js'), /scope: 'platform'[^\n]+\/platform-feedback/);
  assert.match(read('ui-config.js'), /\/platform-feedback/);
  const view = read('src/views/platform/FeedbackView.vue');
  assert.match(view, /onBeforeUnmount/); assert.match(view, /auth.session/); assert.match(view, /flush: 'sync'/);
  assert.doesNotMatch(view, /v-html|accessToken.*\?/);
});
test('feedback API uses auth headers, private blob bytes and optimistic PUT', () => {
  const api = read('src/api/feedback.js');
  assert.match(api, /buildAdminAuthHeaders/); assert.match(api, /response.blob\(\)/);
  assert.match(api, /cache: 'no-store'/); assert.match(api, /method: 'PUT'/); assert.match(api, /\/feedback\/images\//);
  assert.doesNotMatch(api, /createObjectURL|[?&]token=/);
});

test('late private image 401 does not log out a newly selected account', async () => {
  const pending = deferred();
  let session = { accessToken: 'A', userId: 1 }, invalidated = 0;
  const source = read('src/api/feedback.js').replace(/^import .*;\r?$/gm, '').replace(/export /g, '');
  const image = new Function('window', 'buildAdminAuthHeaders', 'joinApiUrl', 'getApiBaseUrl', 'getStoredSession', 'clearStoredSession', 'emitSessionInvalid', 'request', source + '\nreturn getFeedbackImage;')(
    { fetch: () => pending.promise }, value => ({ Authorization: value.accessToken }), (a,b) => a+b, () => '/api', () => session,
    () => { session = null; }, () => { invalidated++; }, () => {}
  );
  const request = image('private-id');
  session = { accessToken: 'B', userId: 2 };
  pending.resolve({ ok: false, status: 401 });
  await assert.rejects(request, /图片/);
  assert.equal(session.userId, 2);
  assert.equal(invalidated, 0);
});

test('late feedback JSON response cannot invalidate another account', async () => {
  const source = read('src/api/admin-api-client.js');
  const { createAdminApiClient } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
  const pending = deferred(); let session = { accessToken: 'A' }, invalidated = 0;
  const request = createAdminApiClient({ getSession: () => session, fetch: () => pending.promise, onAuthError: () => { invalidated++; } });
  const result = request('/admin/feedback'); session = { accessToken: 'B' };
  pending.resolve({ ok: false, status: 401, json: async () => ({ code: 40101, message: 'expired' }) });
  await assert.rejects(result, /expired/); assert.equal(invalidated, 0);
});
