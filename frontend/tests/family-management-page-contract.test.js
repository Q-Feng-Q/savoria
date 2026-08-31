const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const pageModule = require('../pages/family/family-management/index');

function harness(overrides = {}) {
  const stored = [];
  const session = {
    userId: 2,
    username: 'lin',
    nickname: '小林',
    familyId: 8,
    memberId: 2,
    familyRole: 'OWNER',
    roleTemplate: 'owner',
    permissionCodes: ['FAMILY_MEMBER', 'FAMILY_ADMIN'],
    availableModes: ['family']
  };
  let currentSession = { ...session };
  const relaunches = [];
  let transferCalls = 0;
  let updateCalls = 0;
  const runtime = {
    user: {
      getContext: async () => ({
        userId: 2, familyId: 8, familyRole: 'OWNER', merchantId: null,
        availableModes: ['family'], permissionCodes: ['FAMILY_MEMBER', 'FAMILY_ADMIN']
      })
    },
    family: {
      getInfo: async () => ({ familyName: '林家', note: '晚餐少辣', merchantName: '老祁', deliveryEnabled: true, deliveryFeeDefault: 6, deliveryFree: false }),
      updateInfo: async () => { updateCalls += 1; },
      getAddresses: async () => [{ addressId: 1, defaultAddress: true, contactName: '林女士', contactPhone: '13800000000', addressText: '春风路 1 号' }],
      getOwnerCandidates: async () => [{ memberId: 11, displayName: '阿禾', phoneSuffix: '2318' }],
      transferOwner: async () => { transferCalls += 1; },
      exitFamily: async () => {},
      dissolveFamily: async () => {},
      getMyInvitations: async () => [],
      getJoinApplications: async () => []
    }
  };
  Object.assign(runtime.user, overrides.user || {});
  Object.assign(runtime.family, overrides.family || {});
  const definition = pageModule.createFamilyManagementPage({
    createRuntime: () => runtime,
    requireSession: () => currentSession,
    sessionStore: {
      getSession: () => currentSession,
      setSession: (value) => { currentSession = value; stored.push(value); return value; }
    },
    showApiError: () => {},
    confirm: overrides.confirm || (async () => true),
    wxApi: { showToast() {}, navigateTo() {}, reLaunch(options) { relaunches.push(options); }, setClipboardData() {} }
  });
  const page = {
    ...definition,
    data: JSON.parse(JSON.stringify(definition.data)),
    setData(patch) { this.data = { ...this.data, ...patch }; }
  };
  return {
    page,
    runtime,
    stored,
    relaunches,
    setActiveSession(value) { currentSession = value; },
    getActiveSession() { return currentSession; },
    calls: () => ({ transferCalls, updateCalls })
  };
}

test('template uses picker and custom controls without owner id input', () => {
  const markup = fs.readFileSync(path.join(root, 'pages/family/family-management/index.wxml'), 'utf8');
  const styles = fs.readFileSync(path.join(root, 'pages/family/family-management/index.wxss'), 'utf8');
  assert.doesNotMatch(markup, /targetUserId|新负责人用户 ID|type="number"/);
  assert.match(markup, /<picker[^>]+bindchange="onOwnerChange"/);
  assert.match(markup, /bindtap="beginProfileEdit"/);
  assert.match(markup, /bindtap="saveFamilyInfo"/);
  assert.match(markup, /bindtap="cancelProfileEdit"/);
  assert.match(markup, /bindtap="openAddresses"/);
  assert.match(markup, /aria-role="button"/);
  assert.match(styles, /min-height:88rpx/);
  assert.match(styles, /env\(safe-area-inset-bottom\)/);
  assert.match(styles, /@media\s*\(max-width:360px\)/);
  assert.match(styles, /overflow-x:hidden/);
  assert.match(styles, /--fm-primary:\s*#9f4f3d/i);
  assert.match(styles, /--fm-positive:\s*#55705c/i);
  assert.doesNotMatch(styles, /--sk-coral|--sk-ink/);
  assert.match(styles, /\.profile-action--primary[^}]*background:var\(--fm-primary\)[^}]*color:#fff/i);
  assert.match(styles, /\.mini-action--primary[^}]*background:var\(--fm-positive\)[^}]*color:#fff/i);
  assert.match(styles, /\.mini-action\{[^}]*border:2rpx solid var\(--fm-secondary-border\)[^}]*background:#fffdf7[^}]*color:var\(--fm-ink\)/i);
  assert.match(styles, /\.profile-action\{[^}]*border:2rpx solid var\(--fm-secondary-border\)[^}]*color:var\(--fm-ink\)/i);
  assert.match(styles, /\.danger-action[^}]*background:#fff1ed[^}]*color:var\(--fm-danger\)/i);
  assert.match(styles, /\.invitation-code[^}]*min-height:88rpx/);
  assert.match(styles, /\.is-disabled\{opacity:\.58\}/);
  assert.doesNotMatch(styles, /\.ui-button--pressed\{opacity:/);
  assert.match(markup, /block-action \{\{busy \? 'is-disabled' : ''\}\}/);
  assert.match(markup, /danger-action \{\{busy \? 'is-disabled' : ''\}\}/);
});

test('address failure degrades only address block', async () => {
  const { page } = harness({ family: { getAddresses: async () => { throw new Error('address down'); } } });
  await page.load();
  assert.equal(page.data.loading, false);
  assert.equal(page.data.info.familyName, '林家');
  assert.equal(page.data.addressError, true);
  assert.equal(page.data.ownerOptions.length, 1);
});

test('owner candidate failure is distinct from an empty list and authorization failure closes controls', async () => {
  const unavailable = harness({
    family: { getOwnerCandidates: async () => { throw new Error('candidate down'); } }
  });
  await unavailable.page.load();
  assert.equal(unavailable.page.data.isOwner, true);
  assert.equal(unavailable.page.data.ownerCandidatesError, true);
  assert.equal(unavailable.page.data.ownerOptions.length, 0);

  const forbidden = harness({
    family: {
      getOwnerCandidates: async () => {
        const error = new Error('forbidden');
        error.code = 40301;
        throw error;
      }
    }
  });
  await forbidden.page.load();
  assert.equal(forbidden.page.data.isOwner, false);
  assert.equal(forbidden.page.data.isAdmin, false);
  assert.equal(forbidden.page.data.ownerCandidatesError, true);
  assert.equal(forbidden.stored.at(-1).familyRole, 'MEMBER');
});

test('page-level authorization failure clears stale owner privileges', async () => {
  const unauthorized = harness({
    user: {
      getContext: async () => {
        const error = new Error('expired');
        error.code = 40101;
        throw error;
      }
    }
  });
  unauthorized.page.setData({
    isOwner: true,
    isAdmin: true,
    familyRole: 'OWNER',
    editingProfile: true,
    ownerOptions: [{ memberId: 11, label: '阿禾' }],
    selectedOwner: { memberId: 11, label: '阿禾' }
  });

  await unauthorized.page.load();

  assert.equal(unauthorized.page.data.isOwner, false);
  assert.equal(unauthorized.page.data.isAdmin, false);
  assert.equal(unauthorized.page.data.familyRole, 'MEMBER');
  assert.equal(unauthorized.page.data.editingProfile, false);
  assert.equal(unauthorized.page.data.ownerOptions.length, 0);
  assert.equal(unauthorized.stored.at(-1).familyRole, 'MEMBER');
  assert.ok(!unauthorized.stored.at(-1).permissionCodes.includes('FAMILY_ADMIN'));
});

test('double save sends one request and refresh failure keeps submitted view', async () => {
  let release;
  let saveCalls = 0;
  const pending = new Promise((resolve) => { release = resolve; });
  const { page } = harness({
    family: {
      updateInfo: async () => { saveCalls += 1; await pending; },
      getInfo: (() => {
        let calls = 0;
        return async () => {
          calls += 1;
          if (calls === 1) return { familyName: '林家', note: '', merchantName: '老祁' };
          throw new Error('refresh down');
        };
      })()
    }
  });
  await page.load();
  page.beginProfileEdit();
  page.setData({ profileDraft: { familyName: '林家新桌', note: '周末' } });
  const first = page.saveFamilyInfo();
  const second = page.saveFamilyInfo();
  release();
  await Promise.all([first, second]);
  assert.equal(saveCalls, 1);
  assert.equal(page.data.info.familyName, '林家新桌');
  assert.equal(page.data.loading, false);
});

test('transfer confirmation and busy lock prevent invalid duplicate requests', async () => {
  const cancelled = harness({ confirm: async () => false });
  await cancelled.page.load();
  cancelled.page.setData({ selectedOwner: cancelled.page.data.ownerOptions[0] });
  await cancelled.page.transferOwner();
  assert.equal(cancelled.calls().transferCalls, 0);

  let release;
  let transferCalls = 0;
  const pending = new Promise((resolve) => { release = resolve; });
  const active = harness({ family: { transferOwner: async () => { transferCalls += 1; await pending; } } });
  await active.page.load();
  active.page.setData({ selectedOwner: active.page.data.ownerOptions[0] });
  const first = active.page.transferOwner();
  const second = active.page.transferOwner();
  release();
  await Promise.all([first, second]);
  assert.equal(transferCalls, 1);

  const empty = harness({ family: { getOwnerCandidates: async () => [] } });
  await empty.page.load();
  await empty.page.transferOwner();
  assert.equal(empty.calls().transferCalls, 0);
});

test('context refresh failure remains fail closed after successful transfer', async () => {
  const { page, stored } = harness({
    user: { getContext: (() => {
      let calls = 0;
      return async () => {
        calls += 1;
        if (calls === 1) return {
          userId: 2, familyId: 8, familyRole: 'OWNER', availableModes: ['family'],
          permissionCodes: ['FAMILY_MEMBER', 'FAMILY_ADMIN']
        };
        throw new Error('context down');
      };
    })() }
  });
  await page.load();
  page.setData({ selectedOwner: page.data.ownerOptions[0] });
  await page.transferOwner();
  const last = stored.at(-1);
  assert.equal(last.roleTemplate, 'member');
  assert.equal(last.familyRole, 'MEMBER');
  assert.ok(!last.permissionCodes.includes('FAMILY_ADMIN'));
  assert.equal(page.data.isOwner, false);
  assert.equal(page.data.isAdmin, false);
});

test('successful transfer refreshes family info without restoring owner controls', async () => {
  let infoCalls = 0;
  const { page, stored } = harness({
    family: {
      getInfo: async () => {
        infoCalls += 1;
        return infoCalls === 1
          ? { familyName: '林家', note: '旧资料' }
          : { familyName: '林家新桌', note: '移交后资料' };
      }
    },
    user: {
      getContext: (() => {
        let calls = 0;
        return async () => {
          calls += 1;
          return calls === 1
            ? { userId: 2, familyId: 8, familyRole: 'OWNER', availableModes: ['family'], permissionCodes: ['FAMILY_MEMBER', 'FAMILY_ADMIN'] }
            : { userId: 2, familyId: 8, familyRole: 'MEMBER', availableModes: ['family'], permissionCodes: ['FAMILY_MEMBER'] };
        };
      })()
    }
  });
  await page.load();
  page.setData({ selectedOwner: page.data.ownerOptions[0] });
  await page.transferOwner();

  assert.equal(infoCalls, 2);
  assert.equal(page.data.info.familyName, '林家新桌');
  assert.equal(page.data.isOwner, false);
  assert.equal(page.data.isAdmin, false);
  assert.equal(stored.at(-1).familyRole, 'MEMBER');
});

test('late owner transfer completion never rewrites a newly selected account', async () => {
  let releaseTransfer;
  let markStarted;
  const pending = new Promise((resolve) => { releaseTransfer = resolve; });
  const started = new Promise((resolve) => { markStarted = resolve; });
  const active = harness({
    family: { transferOwner: async () => { markStarted(); await pending; } }
  });
  await active.page.load();
  active.page.setData({ selectedOwner: active.page.data.ownerOptions[0] });

  const request = active.page.transferOwner();
  await started;
  const accountB = {
    userId: 9, accessToken: 'account-b', activeMode: 'family', familyId: 20,
    memberId: 90, familyRole: 'OWNER', roleTemplate: 'owner',
    permissionCodes: ['FAMILY_MEMBER', 'FAMILY_ADMIN'], availableModes: ['family']
  };
  active.setActiveSession(accountB);
  releaseTransfer();
  await request;

  assert.deepEqual(active.getActiveSession(), accountB);
  assert.equal(active.page.data.isOwner, true);
});

test('late exit and dissolve completion never clear a newly selected account', async () => {
  for (const operation of ['exitFamily', 'dissolveFamily']) {
    let releaseMutation;
    let markStarted;
    const pending = new Promise((resolve) => { releaseMutation = resolve; });
    const started = new Promise((resolve) => { markStarted = resolve; });
    const active = harness({
      family: { [operation]: async () => { markStarted(); await pending; } }
    });
    await active.page.load();
    active.page.setData({ isOwner: operation === 'dissolveFamily' });

    const request = active.page[operation]();
    await started;
    const accountB = {
      userId: 9, accessToken: 'account-b', activeMode: 'family', familyId: 20,
      memberId: 90, familyRole: 'OWNER', roleTemplate: 'owner',
      permissionCodes: ['FAMILY_MEMBER', 'FAMILY_ADMIN'], availableModes: ['family']
    };
    active.setActiveSession(accountB);
    releaseMutation();
    await request;

    assert.deepEqual(active.getActiveSession(), accountB, operation);
    assert.equal(active.relaunches.length, 0, operation);
  }
});
