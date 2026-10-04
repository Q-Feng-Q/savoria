const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const root = path.resolve(__dirname, '..');

async function withEntry(suppressed, run, options = {}) {
  const pagePath = path.join(root, 'pages/auth/entry/index.js');
  const paths = [
    'utils/api-runtime.js', 'utils/session.js', 'utils/page-api.js', 'utils/branding.js'
  ].map((name) => require.resolve(path.join(root, name)));
  const originals = paths.map((name) => require.cache[name]);
  const oldPage = global.Page;
  const oldWx = global.wx;
  let definition;
  let current = options.current || null;
  const calls = [];
  const store = {
    getSession: () => current,
    setSession: (session) => { current = session; return session; },
    clearSession: () => { current = null; },
    getAutoLoginTarget: () => options.target || null,
    setAutoLoginTarget: () => {},
    isAutoLoginSuppressed: () => suppressed,
    setAutoLoginSuppressed: (value) => { suppressed = value; }
  };
  require.cache[paths[0]] = { exports: { createApiRuntime: () => ({
    auth: { wechatLogin: async ({ code }) => { calls.push(['wechat', code]); return {
      accessToken: 'token', refreshToken: 'refresh', userId: options.loginUserId || 7, roleTemplate: 'member'
    }; }, logout: async () => { calls.push(['logout']); } },
    user: { getContext: async () => ({ userId: 7, familyId: 3, familyRole: 'member', platformRoles: [] }) }
  }) } };
  require.cache[paths[1]] = { exports: { sessionStore: store } };
  require.cache[paths[2]] = { exports: {
    redirectBySession: (session) => calls.push(['redirect', session.userId]),
    showApiError() {}, showLoginError() {}
  } };
  require.cache[paths[3]] = { exports: { withBranding: (page) => page } };
  global.Page = (value) => { definition = value; };
  global.wx = { login: ({ success }) => { calls.push(['wx.login']); success({ code: 'wx-code' }); } };
  try {
    delete require.cache[pagePath];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: { ...definition.data },
      setData(update) { this.data = { ...this.data, ...update }; }
    });
    await run(page, calls, store);
  } finally {
    delete require.cache[pagePath];
    paths.forEach((name, index) => {
      if (originals[index]) require.cache[name] = originals[index];
      else delete require.cache[name];
    });
    global.Page = oldPage;
    global.wx = oldWx;
  }
}

test('entry automatically signs in an already-bound WeChat account', async () => {
  await withEntry(false, async (page, calls, store) => {
    await page.onShow();
    assert.deepEqual(calls, [['wx.login'], ['wechat', 'wx-code'], ['redirect', 7]]);
    assert.equal(store.getSession().refreshToken, 'refresh');
  });
});

test('entry does not automatically sign in after explicit logout', async () => {
  await withEntry(true, async (page, calls) => {
    await page.onShow();
    assert.deepEqual(calls, []);
  });
});

test('automatic WeChat login cannot switch a pending account to another user', async () => {
  await withEntry(false, async (page, calls, store) => {
    await page.onShow();
    assert.equal(calls.some(([kind]) => kind === 'redirect'), false);
    assert.notEqual(store.getSession() && store.getSession().userId, 8);
  }, { current: { userId: 7, requiresLogin: true }, loginUserId: 8 });
});
