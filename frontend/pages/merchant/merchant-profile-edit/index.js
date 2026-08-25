const { createApiRuntime } = require('../../../utils/api-runtime');
const { sessionStore } = require('../../../utils/session');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { refreshAccountIdentity, destinationForSession } = require('../../../utils/account-switching');
const {
  createMerchantProfileSnapshot,
  validateMerchantProfile,
  isMerchantAccessError,
  failClosedMerchantSession
} = require('../../../utils/merchant-profile');

function createMerchantProfileEditPage(dependencies = {}) {
  const createRuntime = dependencies.createRuntime || createApiRuntime;
  const getSession = dependencies.requireSession || requireSession;
  const store = dependencies.sessionStore || sessionStore;
  const refreshIdentity = dependencies.refreshIdentity || refreshAccountIdentity;
  const reportError = dependencies.showApiError || showApiError;
  const wxApi = dependencies.wxApi || (typeof wx === 'undefined' ? {} : wx);
  const schedule = dependencies.setTimeout || setTimeout;
  const cancelSchedule = dependencies.clearTimeout || clearTimeout;

  function routeTo(url) {
    if (url === '/pages/family/home/index' || url === '/pages/account/profile/index') wxApi.switchTab({ url });
    else wxApi.redirectTo({ url });
  }

  return {
    data: {
      phase: 'loading',
      pageTitle: '正在读取商户资料',
      pageDescription: '请稍候',
      saving: false,
      completed: false,
      editable: false,
      form: { name: '', contactName: '', contactPhone: '' }
    },

    onLoad() { return this.load(); },

    onUnload() {
      if (this._returnTimer) cancelSchedule(this._returnTimer);
    },

    async load() {
      const session = getSession({ merchantOnly: true });
      if (!session) return;
      this._session = session;
      this._runtime = createRuntime();
      this.setData({ phase: 'loading', pageTitle: '正在读取商户资料', pageDescription: '请稍候', editable: false, completed: false });
      try {
        const profile = await this._runtime.merchant.getProfile();
        this.setData({ phase: 'ready', editable: true, form: createMerchantProfileSnapshot(profile) });
      } catch (error) {
        if (isMerchantAccessError(error)) {
          await this.handleRevokedAccess();
          return;
        }
        this.setData({ phase: 'error', editable: false, pageTitle: '商户资料加载失败', pageDescription: '未加载成功前不会开放编辑，避免覆盖原有联系信息。' });
        reportError(error, '商户资料加载失败');
      }
    },

    retryLoad() { return this.load(); },

    input(event) {
      if (!this.data.editable || this.data.saving) return;
      this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail.value });
    },

    cancel() {
      if (this.data.saving || this.data.completed) return;
      wxApi.navigateBack();
    },

    async save() {
      if (this.data.phase !== 'ready' || this.data.saving || this.data.completed || !this.data.editable) return;
      const validation = validateMerchantProfile(this.data.form);
      if (!validation.valid) {
        wxApi.showToast({ title: validation.message, icon: 'none' });
        return;
      }
      this.setData({ saving: true });
      try {
        const profile = await this._runtime.merchant.updateProfile(validation.value);
        this.setData({ form: createMerchantProfileSnapshot(profile), completed: true });
        wxApi.showToast({ title: '商户资料已保存', icon: 'success' });
        this._returnTimer = schedule(() => wxApi.navigateBack(), 350);
      } catch (error) {
        if (isMerchantAccessError(error)) {
          await this.handleRevokedAccess();
          return;
        }
        reportError(error, '商户资料保存失败');
      } finally {
        if (!this.data.completed) this.setData({ saving: false });
      }
    },

    async handleRevokedAccess() {
      const current = store.getSession() || this._session || {};
      const closed = failClosedMerchantSession(current);
      store.setSession(closed);
      this.setData({ phase: 'error', editable: false, saving: false, completed: false, pageTitle: '商户身份已失效', pageDescription: '请重新选择可用身份。' });
      try {
        const refreshed = await refreshIdentity({ userId: current.userId }, {
          sessionStore: store,
          loadContext: () => createRuntime().user.getContext()
        });
        wxApi.showToast({ title: '商户负责人权限已变更', icon: 'none' });
        const destination = destinationForSession(refreshed);
        if (destination === '/pages/merchant/index') {
          store.setSession(failClosedMerchantSession(refreshed));
          wxApi.redirectTo({ url: '/pages/account/account-management/index' });
          return;
        }
        routeTo(destination);
      } catch (error) {
        store.setSession(closed);
        wxApi.showToast({ title: '商户身份已失效，请重新选择身份', icon: 'none' });
        wxApi.redirectTo({ url: '/pages/account/account-management/index' });
      }
    }
  };
}

const pageDefinition = createMerchantProfileEditPage();
if (typeof Page === 'function') Page(pageDefinition);

module.exports = { createMerchantProfileEditPage };
