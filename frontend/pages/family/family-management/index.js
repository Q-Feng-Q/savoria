const { createApiRuntime } = require('../../../utils/api-runtime');
const { sessionStore } = require('../../../utils/session');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard, identityKey } = require('../../../utils/identity-load');
const {
  buildOwnerOptions,
  createProfileSnapshot,
  validateFamilyProfile,
  findDefaultAddress,
  currentOwnerLabel,
  failClosedOwnerSession,
  mergeIdentityContext
} = require('../../../utils/family-management');

function defaultConfirm(options) {
  return new Promise((resolve) => wx.showModal({
    ...options,
    success: (result) => resolve(result.confirm),
    fail: () => resolve(false)
  }));
}

function isAuthorizationError(error) {
  return Boolean(error && (
    error.code === 40101
    || error.code === 40301
    || error.statusCode === 401
    || error.statusCode === 403
    || error.status === 401
    || error.status === 403
  ));
}

function createFamilyManagementPage(dependencies = {}) {
  const createRuntime = dependencies.createRuntime || createApiRuntime;
  const getSession = dependencies.requireSession || requireSession;
  const store = dependencies.sessionStore || sessionStore;
  const reportError = dependencies.showApiError || showApiError;
  const askConfirm = dependencies.confirm || defaultConfirm;
    const wxApi = dependencies.wxApi || (typeof wx === 'undefined' ? {} : wx);
    const identityLoad = (dependencies.createIdentityLoadGuard || createIdentityLoadGuard)(() => store.getSession());
    const isCurrentIdentity = (token) => Boolean(token) && identityKey(store.getSession()) === token;

    return {
      identityLoad,
    data: {
      loading: true,
      busy: false,
      saveBusy: false,
      transferBusy: false,
      invitationCode: '',
      userIdentifier: '',
      applications: [],
      invitations: [],
      familyRole: 'MEMBER',
      isAdmin: false,
      isOwner: false,
      info: null,
      profileDraft: { familyName: '', note: '' },
      profileSnapshot: { familyName: '', note: '' },
      editingProfile: false,
      defaultAddress: null,
      addressError: false,
      ownerCandidatesError: false,
      ownerOptions: [],
      ownerIndex: -1,
      selectedOwner: null,
      ownerLabel: ''
    },

    onShow() {
      return this.load();
    },

      async load() {
        const session = getSession();
        if (!session) return;
        const loadToken = this.identityLoad.begin(session);
        this.setData({ loading: true, addressError: false, info: null, applications: [], invitations: [],
          defaultAddress: null, ownerOptions: [], selectedOwner: null });
        try {
          const runtime = createRuntime();
          const context = await runtime.user.getContext();
          if (!this.identityLoad.isCurrent(loadToken)) return;
          const authoritativeSession = mergeIdentityContext(session, context);
        store.setSession(authoritativeSession);
        const familyRole = String(context.familyRole || authoritativeSession.familyRole || 'MEMBER').toUpperCase();
        const isOwner = familyRole === 'OWNER';
        const isAdmin = isOwner || familyRole === 'ADMIN'
          || (context.permissionCodes || []).includes('FAMILY_ADMIN');
        const addressRequest = runtime.family.getAddresses()
          .then((items) => ({ items: items || [], failed: false }))
          .catch(() => ({ items: [], failed: true }));
        const ownerCandidatesRequest = isOwner
          ? runtime.family.getOwnerCandidates()
            .then((items) => ({ items: items || [], failed: false, error: null }))
            .catch((error) => ({ items: [], failed: true, error }))
          : Promise.resolve({ items: [], failed: false, error: null });
          const [info, addressState, invitations, applications, candidateState] = await Promise.all([
          runtime.family.getInfo(),
          addressRequest,
          runtime.family.getMyInvitations().catch(() => []),
          isAdmin ? runtime.family.getJoinApplications().catch(() => []) : Promise.resolve([]),
            ownerCandidatesRequest
          ]);
          if (!this.identityLoad.isCurrent(loadToken)) return;
        let effectiveFamilyRole = familyRole;
        let effectiveIsOwner = isOwner;
        let effectiveIsAdmin = isAdmin;
        if (candidateState.failed && isAuthorizationError(candidateState.error)) {
          const closedSession = failClosedOwnerSession(authoritativeSession);
          store.setSession(closedSession);
          effectiveFamilyRole = 'MEMBER';
          effectiveIsOwner = false;
          effectiveIsAdmin = false;
        }
        const profileSnapshot = createProfileSnapshot(info);
        this.setData({
          loading: false,
          familyRole: effectiveFamilyRole,
          isOwner: effectiveIsOwner,
          isAdmin: effectiveIsAdmin,
          info,
          profileDraft: profileSnapshot,
          profileSnapshot,
          editingProfile: false,
          defaultAddress: findDefaultAddress(addressState.items),
          addressError: addressState.failed,
          ownerCandidatesError: candidateState.failed,
          invitations,
          applications,
          ownerOptions: buildOwnerOptions(candidateState.items),
          ownerIndex: -1,
          selectedOwner: null,
          ownerLabel: currentOwnerLabel(authoritativeSession)
        });
        } catch (error) {
          if (!this.identityLoad.isCurrent(loadToken)) return;
        if (isAuthorizationError(error)) {
          const closedSession = failClosedOwnerSession(store.getSession() || getSession() || {});
          store.setSession(closedSession);
          this.setData({
            loading: false,
            familyRole: 'MEMBER',
            isOwner: false,
            isAdmin: false,
            editingProfile: false,
            ownerOptions: [],
            ownerIndex: -1,
            selectedOwner: null,
            ownerCandidatesError: false
          });
        } else {
          this.setData({ loading: false });
        }
        reportError(error, '家庭管理加载失败');
      }
    },

    input(event) {
      this.setData({ [event.currentTarget.dataset.field]: event.detail.value });
    },

    inputProfile(event) {
      this.setData({
        profileDraft: {
          ...this.data.profileDraft,
          [event.currentTarget.dataset.field]: event.detail.value
        }
      });
    },

    beginProfileEdit() {
      if (!this.data.isAdmin || this.data.saveBusy) return;
      const profileSnapshot = createProfileSnapshot(this.data.info || {});
      this.setData({ editingProfile: true, profileSnapshot, profileDraft: { ...profileSnapshot } });
    },

    cancelProfileEdit() {
      if (this.data.saveBusy) return;
      this.setData({ editingProfile: false, profileDraft: { ...this.data.profileSnapshot } });
    },

    async saveFamilyInfo() {
      if (this.data.saveBusy || !this.data.isAdmin) return;
      const validation = validateFamilyProfile(this.data.profileDraft);
      if (!validation.valid) {
        if (wxApi.showToast) wxApi.showToast({ title: validation.message, icon: 'none' });
        return;
      }
      this.setData({ saveBusy: true });
      try {
        const runtime = createRuntime();
        await runtime.family.updateInfo(validation.payload);
        const submitted = { ...this.data.info, ...validation.payload };
        this.setData({
          info: submitted,
          profileSnapshot: createProfileSnapshot(submitted),
          profileDraft: createProfileSnapshot(submitted),
          editingProfile: false
        });
        try {
          const refreshed = await runtime.family.getInfo();
          this.setData({
            info: refreshed,
            profileSnapshot: createProfileSnapshot(refreshed),
            profileDraft: createProfileSnapshot(refreshed)
          });
        } catch (refreshError) {
          if (wxApi.showToast) wxApi.showToast({ title: '已保存，资料刷新失败', icon: 'none' });
        }
      } catch (error) {
        reportError(error, '家庭资料保存失败');
      } finally {
        this.setData({ saveBusy: false });
      }
    },

    openAddresses() {
      if (wxApi.navigateTo) wxApi.navigateTo({ url: '/pages/family/addresses/index' });
    },

    onOwnerChange(event) {
      const ownerIndex = Number(event.detail.value);
      this.setData({
        ownerIndex,
        selectedOwner: this.data.ownerOptions[ownerIndex] || null
      });
    },

    async transferOwner() {
      if (this.data.transferBusy || !this.data.isOwner || !this.data.selectedOwner) return;
      const target = this.data.selectedOwner;
      const confirmed = await askConfirm({
        title: '移交家庭负责人',
        content: `确认将家庭负责人移交给${target.label}？移交后你将成为普通成员。`,
        confirmText: '确认移交',
        confirmColor: '#b7473d'
      });
      if (!confirmed || this.data.transferBusy || !this.data.isOwner) return;
      const mutationIdentity = identityKey(store.getSession() || getSession());
      this.setData({ transferBusy: true });
      try {
        const runtime = createRuntime();
        await runtime.family.transferOwner({ targetMemberId: target.memberId });
        if (!isCurrentIdentity(mutationIdentity)) return;
        const closedSession = failClosedOwnerSession(store.getSession() || getSession() || {});
        store.setSession(closedSession);
        this.setData({
          isOwner: false,
          isAdmin: false,
          familyRole: 'MEMBER',
          ownerOptions: [],
          ownerCandidatesError: false,
          ownerIndex: -1,
          selectedOwner: null
        });
        try {
          const context = await runtime.user.getContext();
          if (!isCurrentIdentity(mutationIdentity)) return;
          store.setSession(failClosedOwnerSession(mergeIdentityContext(closedSession, context)));
        } catch (refreshError) {
          if (isCurrentIdentity(mutationIdentity) && wxApi.showToast) {
            wxApi.showToast({ title: '已完成移交，请重新进入页面', icon: 'none' });
          }
        }
        try {
          const refreshed = await runtime.family.getInfo();
          if (!isCurrentIdentity(mutationIdentity)) return;
          this.setData({
            info: refreshed,
            profileSnapshot: createProfileSnapshot(refreshed),
            profileDraft: createProfileSnapshot(refreshed)
          });
        } catch (refreshError) {
          if (isCurrentIdentity(mutationIdentity) && wxApi.showToast) {
            wxApi.showToast({ title: '已完成移交，家庭资料刷新失败', icon: 'none' });
          }
        }
      } catch (error) {
        reportError(error, '负责人移交失败');
      } finally {
        this.setData({ transferBusy: false });
      }
    },

    async generateCode() {
      if (this.data.busy) return;
      this.setData({ busy: true });
      try {
        const invitationCode = await createRuntime().family.generateInvitation();
        this.setData({ invitationCode });
      } catch (error) {
        reportError(error, '邀请码生成失败');
      } finally {
        this.setData({ busy: false });
      }
    },

    copyCode() {
      if (this.data.invitationCode && wxApi.setClipboardData) {
        wxApi.setClipboardData({ data: this.data.invitationCode });
      }
    },

    async directInvite() {
      const userIdentifier = String(this.data.userIdentifier || '').trim();
      if (!userIdentifier || this.data.busy) {
        if (!userIdentifier && wxApi.showToast) wxApi.showToast({ title: '请输入账号、手机或邮箱', icon: 'none' });
        return;
      }
      this.setData({ busy: true });
      try {
        await createRuntime().family.directInvite({ userIdentifier });
        if (wxApi.showToast) wxApi.showToast({ title: '邀请已发送', icon: 'success' });
        this.setData({ userIdentifier: '' });
      } catch (error) {
        reportError(error, '邀请发送失败');
      } finally {
        this.setData({ busy: false });
      }
    },

    async approve(event) {
      try {
        await createRuntime().family.approveJoinApplication(event.currentTarget.dataset.id);
        await this.load();
      } catch (error) {
        reportError(error, '审批失败');
      }
    },

    async reject(event) {
      try {
        await createRuntime().family.rejectJoinApplication(
          event.currentTarget.dataset.id, { reason: '家庭管理员拒绝' });
        await this.load();
      } catch (error) {
        reportError(error, '审批失败');
      }
    },

    async acceptInvitation(event) {
      try {
        await createRuntime().family.acceptInvitation(event.currentTarget.dataset.id);
        if (wxApi.showToast) wxApi.showToast({ title: '已加入家庭', icon: 'success' });
        await this.load();
      } catch (error) {
        reportError(error, '接受邀请失败');
      }
    },

    async rejectInvitation(event) {
      try {
        await createRuntime().family.rejectInvitation(
          event.currentTarget.dataset.id, { reason: '暂不加入' });
        await this.load();
      } catch (error) {
        reportError(error, '拒绝邀请失败');
      }
    },

    async exitFamily() {
      if (this.data.busy || this.data.isOwner) return;
      if (!await askConfirm({
        title: '退出当前家庭',
        content: '退出后将无法继续查看该家庭的菜单、订单和钱包。',
        confirmText: '确认退出',
        confirmColor: '#b7473d'
      })) return;
      const mutationIdentity = identityKey(store.getSession() || getSession());
      this.setData({ busy: true });
      try {
        await createRuntime().family.exitFamily();
        await this.finishFamilyChange('已退出家庭', mutationIdentity);
      } catch (error) {
        reportError(error, '退出家庭失败');
      } finally {
        this.setData({ busy: false });
      }
    },

    async dissolveFamily() {
      if (this.data.busy || !this.data.isOwner) return;
      if (!await askConfirm({
        title: '解散当前家庭',
        content: '家庭关系、菜单配置和后续点餐权限将被停用，此操作不可撤销。',
        confirmText: '确认解散',
        confirmColor: '#b7473d'
      })) return;
      const mutationIdentity = identityKey(store.getSession() || getSession());
      this.setData({ busy: true });
      try {
        await createRuntime().family.dissolveFamily();
        await this.finishFamilyChange('家庭已解散', mutationIdentity);
      } catch (error) {
        reportError(error, '解散家庭失败');
      } finally {
        this.setData({ busy: false });
      }
    },

    async finishFamilyChange(message, mutationIdentity) {
      if (!isCurrentIdentity(mutationIdentity)) return false;
      const session = store.getSession();
      let completedIdentity = mutationIdentity;
      if (session) {
        const completedSession = store.setSession({
          ...session,
          familyId: null,
          memberId: null,
          familyRole: null,
          activeMode: 'family',
          roleTemplate: 'user'
        });
        completedIdentity = identityKey(completedSession);
      }
      if (wxApi.showToast) wxApi.showToast({ title: message, icon: 'success' });
      setTimeout(() => {
        if (isCurrentIdentity(completedIdentity) && wxApi.reLaunch) {
          wxApi.reLaunch({ url: '/pages/family/family-start/index' });
        }
      }, 400);
      return true;
    }
  };
}

const pageDefinition = createFamilyManagementPage();
if (typeof Page === 'function') Page(pageDefinition);

module.exports = { createFamilyManagementPage };
