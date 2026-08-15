const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMerchantFamilyDetailScene } = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');

const readyState = () => ({ phase: 'ready', message: '' });
const loadingState = () => ({ phase: 'loading', message: '' });
const errorState = (message) => ({ phase: 'error', message });

Page({
  data: {
    phase: 'loading', pageTitle: '正在读取家庭', pageDescription: '请稍候', familyId: '',
    family: null, members: [], addresses: [], menuPreview: [], orderPreview: [],
    memberAdjustValues: {}, feeInput: '', deliveryBusy: false, busyMemberId: null,
    optionalStates: { ledger: loadingState(), menu: loadingState(), orders: loadingState() }
  },

  onLoad(query) { this.setData({ familyId: (query && query.familyId) || '' }); },
  onShow() { this.load(); },

  buildScene(session) {
    return buildApiMerchantFamilyDetailScene({
      session,
      familyDetail: this._familyDetail,
      familyMenuItems: this._familyMenuItems || [],
      orders: this._orders || [],
      ledgersByMember: this._ledgersByMember || {}
    });
  },

  applyScene(session, extra = {}, resetFee = true) {
    const scene = this.buildScene(session);
    const memberAdjustValues = { ...this.data.memberAdjustValues };
    (scene.members || []).forEach((member) => { if (memberAdjustValues[member.id] === undefined) memberAdjustValues[member.id] = '20'; });
    const feeState = resetFee ? { feeInput: `${scene.family.deliveryFeeDefault || 0}` } : {};
    this.setData({ ...scene, memberAdjustValues, ...feeState, ...extra });
  },

  async load({ silent = false } = {}) {
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    if (!silent) this.setData({ phase: 'loading', pageTitle: '正在读取家庭', pageDescription: '请稍候' });
    try {
      const runtime = createApiRuntime();
      const familyDetail = await runtime.merchant.getFamilyDetail(this.data.familyId);
      if (generation !== this._loadGeneration) return;
      if (!familyDetail) {
        this._familyDetail = null;
        this._familyMenuItems = [];
        this._orders = [];
        this._ledgersByMember = {};
        this.setData({
          phase: 'empty',
          pageTitle: '未找到家庭',
          pageDescription: '该家庭不存在，或已不再由当前商户服务。',
          family: null,
          members: [],
          addresses: [],
          menuPreview: [],
          orderPreview: []
        });
        return;
      }
      this._familyDetail = familyDetail;
      this._familyMenuItems = [];
      this._orders = [];
      this._ledgersByMember = {};
      const optionalStates = { ledger: loadingState(), menu: loadingState(), orders: loadingState() };
      this.applyScene(session, { phase: 'ready', pageTitle: '', pageDescription: '', optionalStates });

      const ledgerTask = Promise.allSettled((familyDetail.members || []).map((member) => runtime.merchant.getMemberWalletLedgers(member.memberId)))
        .then((results) => {
          if (generation !== this._loadGeneration) return;
          let failedMessage = '';
          (familyDetail.members || []).forEach((member, index) => {
            const result = results[index];
            if (result && result.status === 'fulfilled') this._ledgersByMember[member.memberId] = result.value;
            if (result && result.status === 'rejected') failedMessage = resolveApiErrorMessage(result.reason, '部分余额流水暂不可用');
          });
          this.applyScene(session, { optionalStates: { ...this.data.optionalStates, ledger: failedMessage ? errorState(failedMessage) : readyState() } }, false);
        });
      const menuTask = runtime.merchant.getFamilyMenu(this.data.familyId)
        .then((items) => {
          if (generation !== this._loadGeneration) return;
          this._familyMenuItems = items || [];
          this.applyScene(session, { optionalStates: { ...this.data.optionalStates, menu: readyState() } }, false);
        })
        .catch((error) => {
          if (generation !== this._loadGeneration) return;
          this.setData({ optionalStates: { ...this.data.optionalStates, menu: errorState(resolveApiErrorMessage(error, '菜单预览加载失败')) } });
        });
      const ordersTask = runtime.merchant.getOrders()
        .then((items) => {
          if (generation !== this._loadGeneration) return;
          this._orders = items || [];
          this.applyScene(session, { optionalStates: { ...this.data.optionalStates, orders: readyState() } }, false);
        })
        .catch((error) => {
          if (generation !== this._loadGeneration) return;
          this.setData({ optionalStates: { ...this.data.optionalStates, orders: errorState(resolveApiErrorMessage(error, '订单预览加载失败')) } });
        });
      this._optionalLoadPromise = Promise.allSettled([ledgerTask, menuTask, ordersTask]);
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ phase: 'error', pageTitle: '家庭详情加载失败', pageDescription: resolveApiErrorMessage(error, '家庭详情加载失败') });
    }
  },

  retryLoad() { return this.load(); },

  async retryOptional(region) {
    const session = requireSession({ merchantOnly: true });
    if (!session || !this._familyDetail) return;
    const generation = this._loadGeneration;
    const optionalStates = { ...this.data.optionalStates, [region]: loadingState() };
    this.setData({ optionalStates });
    try {
      const runtime = createApiRuntime();
      if (region === 'menu') this._familyMenuItems = await runtime.merchant.getFamilyMenu(this.data.familyId);
      if (region === 'orders') this._orders = await runtime.merchant.getOrders();
      if (region === 'ledger') {
        const pairs = await Promise.all((this._familyDetail.members || []).map(async (member) => [member.memberId, await runtime.merchant.getMemberWalletLedgers(member.memberId)]));
        this._ledgersByMember = Object.fromEntries(pairs);
      }
      if (generation !== this._loadGeneration) return;
      this.applyScene(session, { optionalStates: { ...this.data.optionalStates, [region]: readyState() } });
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ optionalStates: { ...this.data.optionalStates, [region]: errorState(resolveApiErrorMessage(error, '加载失败')) } });
    }
  },

  retryLedger() { return this.retryOptional('ledger'); },
  retryMenu() { return this.retryOptional('menu'); },
  retryOrders() { return this.retryOptional('orders'); },
  bindFeeInput(event) { this.setData({ feeInput: event.detail.value }); },

  async updateDeliveryPolicy(patch, successText) {
    if (!this.data.family || this.data.deliveryBusy || this.data.busyMemberId) return;
    this.setData({ deliveryBusy: true });
    try {
      await createApiRuntime().merchant.updateFamilyDeliveryPolicy(this.data.familyId, {
        deliveryEnabled: Object.prototype.hasOwnProperty.call(patch, 'deliveryEnabled') ? patch.deliveryEnabled : this.data.family.deliveryEnabled,
        deliveryFeeDefault: Object.prototype.hasOwnProperty.call(patch, 'deliveryFeeDefault') ? patch.deliveryFeeDefault : Number(this.data.family.deliveryFeeDefault || 0),
        deliveryFree: Object.prototype.hasOwnProperty.call(patch, 'deliveryFree') ? patch.deliveryFree : this.data.family.deliveryFeeFree
      });
      wx.showToast({ title: successText, icon: 'success' });
      await this.load({ silent: true });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, '配送设置保存失败'), icon: 'none' });
    } finally { this.setData({ deliveryBusy: false }); }
  },

  toggleDeliveryEnabled() { return this.updateDeliveryPolicy({ deliveryEnabled: !this.data.family.deliveryEnabled }, '配送能力已更新'); },
  toggleDeliveryFree() { return this.updateDeliveryPolicy({ deliveryFree: !this.data.family.deliveryFeeFree }, '免配送设置已更新'); },
  saveDeliveryFee() { return this.updateDeliveryPolicy({ deliveryFeeDefault: Number(this.data.feeInput || 0) }, '配送费已保存'); },
  bindMemberAdjustValue(event) { this.setData({ [`memberAdjustValues.${event.currentTarget.dataset.memberId}`]: event.detail.value }); },

  async adjustMemberBalance(event) {
    const memberId = event.currentTarget.dataset.memberId;
    const direction = event.currentTarget.dataset.direction;
    const amount = Number(this.data.memberAdjustValues[memberId] || 0);
    if (!Number.isFinite(amount) || amount <= 0 || this.data.busyMemberId || this.data.deliveryBusy) {
      if (!Number.isFinite(amount) || amount <= 0) wx.showToast({ title: '请输入有效金额', icon: 'none' });
      return;
    }
    this.setData({ busyMemberId: memberId });
    try {
      await createApiRuntime().merchant.adjustMemberBalance(memberId, {
        type: direction === 'decrease' ? 'MANUAL_DEBIT' : 'MANUAL_CREDIT',
        amount,
        remark: direction === 'decrease' ? '商户手动扣减' : '商户手动充值'
      });
      wx.showToast({ title: direction === 'decrease' ? '已扣减余额' : '已增加余额', icon: 'success' });
      await this.load({ silent: true });
    } catch (error) { wx.showToast({ title: resolveApiErrorMessage(error, '余额调整失败'), icon: 'none' }); }
    finally { this.setData({ busyMemberId: null }); }
  },

  openMemberLedger(event) { wx.navigateTo({ url: `/pages/family/wallet-ledger/index?actor=merchant&memberId=${event.currentTarget.dataset.memberId}` }); },
  openFamilyMenu() { wx.navigateTo({ url: `/pages/merchant/family-menu/index?familyId=${this.data.familyId}` }); },
  openOrders() { wx.navigateTo({ url: `/pages/merchant/merchant-orders/index?familyId=${this.data.familyId}` }); }
});
