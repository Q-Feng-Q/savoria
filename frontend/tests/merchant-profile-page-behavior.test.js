const test = require('node:test');
const assert = require('node:assert/strict');
const { createMerchantProfileEditPage } = require('../pages/merchant/merchant-profile-edit/index');

function harness(overrides = {}) {
  const navigations = [];
  const stored = [];
  const session = { userId: 2, merchantId: 7, merchantRole: 'MERCHANT_ADMIN', activeMode: 'merchant', availableModes: ['merchant'], permissionCodes: ['MERCHANT_ADMIN'], accessToken: 'token' };
  const runtime = {
    merchant: {
      getProfile: overrides.getProfile || (async () => ({ name: '暖炉', contactName: '林女士', contactPhone: '138' })),
      updateProfile: overrides.updateProfile || (async (value) => value)
    },
    user: { getContext: async () => ({ userId: 2, merchantId: 7, merchantRole: 'MERCHANT_ADMIN', availableModes: ['merchant'], permissionCodes: ['MERCHANT_ADMIN'] }) }
  };
  const store = {
    getSession: () => stored.at(-1) || session,
    setSession(value) { stored.push(value); return value; },
    getAccount: () => session
  };
  const definition = createMerchantProfileEditPage({
    createRuntime: () => runtime,
    requireSession: () => session,
    sessionStore: store,
    refreshIdentity: overrides.refreshIdentity || (async () => session),
    showApiError: () => {},
    setTimeout: overrides.setTimeout || ((callback) => { callback(); return 1; }),
    clearTimeout: () => {},
    wxApi: {
      showToast() {}, navigateBack() { navigations.push('back'); },
      redirectTo({ url }) { navigations.push(url); }, switchTab({ url }) { navigations.push(url); }
    }
  });
  const page = { ...definition, data: JSON.parse(JSON.stringify(definition.data)), setData(patch) { this.data = { ...this.data, ...patch }; } };
  return { page, stored, navigations };
}

test('ordinary load failure keeps form closed until retry succeeds', async () => {
  let calls = 0;
  const { page } = harness({ getProfile: async () => { calls += 1; if (calls === 1) throw new Error('down'); return { name: '暖炉' }; } });
  await page.load();
  assert.equal(page.data.phase, 'error');
  assert.equal(page.data.editable, false);
  await page.retryLoad();
  assert.equal(page.data.phase, 'ready');
  assert.equal(page.data.form.name, '暖炉');
});

test('successful save locks save and cancel until the scheduled return runs', async () => {
  let scheduled;
  let saves = 0;
  const { page, navigations } = harness({
    updateProfile: async (value) => { saves += 1; return value; },
    setTimeout: (callback) => { scheduled = callback; return 9; }
  });
  await page.load();
  await page.save();
  await page.save();
  page.cancel();
  assert.equal(saves, 1);
  assert.equal(page.data.completed, true);
  assert.equal(page.data.saving, true);
  assert.deepEqual(navigations, []);
  scheduled();
  assert.deepEqual(navigations, ['back']);
});

test('cancel is custom navigation and duplicate save is guarded', async () => {
  let release;
  let saves = 0;
  const pending = new Promise((resolve) => { release = resolve; });
  const { page, navigations } = harness({ updateProfile: async (value) => { saves += 1; await pending; return value; } });
  await page.load();
  page.cancel();
  const first = page.save();
  const second = page.save();
  release();
  await Promise.all([first, second]);
  assert.equal(saves, 1);
  assert.equal(navigations[0], 'back');
});

test('403 remains fail closed even when refresh returns stale merchant mode', async () => {
  const forbidden = Object.assign(new Error('forbidden'), { code: 40301 });
  const { page, stored, navigations } = harness({ getProfile: async () => { throw forbidden; } });
  await page.load();
  assert.equal(stored.at(-1).merchantId, null);
  assert.deepEqual(stored.at(-1).availableModes, []);
  assert.equal(page.data.editable, false);
  assert.ok(navigations.includes('/pages/account/account-management/index'));
});
