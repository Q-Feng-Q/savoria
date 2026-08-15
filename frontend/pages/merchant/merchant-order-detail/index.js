const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMerchantOrderDetailScene, nextStatusByStatus } = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');

Page({
  data: {
    id: '',
    phase: 'loading',
    pageTitle: '正在读取订单',
    pageDescription: '请稍候',
    scene: null,
    feeInput: '',
    reasonInput: '',
    mutationBusy: false
  },

  onLoad(query) { this.setData({ id: (query && query.id) || '' }); },
  onShow() { this.load(); },

  async load({ silent = false } = {}) {
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    if (!this.data.id) {
      this.setData({ phase: 'error', pageTitle: '订单不存在', pageDescription: '缺少订单编号，无法读取详情。', scene: null });
      return;
    }
    if (!silent) this.setData({ phase: 'loading', pageTitle: '正在读取订单', pageDescription: '请稍候' });
    try {
      const runtime = createApiRuntime();
      const order = await runtime.merchant.getOrderDetail(this.data.id);
      if (!order) throw new Error('没有找到这笔订单');
      const familyDetail = order.familyId ? await runtime.merchant.getFamilyDetail(order.familyId) : null;
      if (generation !== this._loadGeneration) return;
      const scene = buildApiMerchantOrderDetailScene({ session, order, familyDetail });
      this.setData({
        scene,
        feeInput: scene.order.deliveryFee || '',
        reasonInput: '',
        phase: 'ready',
        pageTitle: '',
        pageDescription: ''
      });
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({
        phase: 'error',
        pageTitle: '订单详情加载失败',
        pageDescription: resolveApiErrorMessage(error, '订单详情加载失败'),
        scene: null
      });
    }
  },

  retryLoad() { return this.load(); },
  bindFeeInput(event) { this.setData({ feeInput: event.detail.value }); },
  bindReasonInput(event) { this.setData({ reasonInput: event.detail.value }); },

  async runMutation(operation, successTitle, fallbackMessage) {
    if (this.data.mutationBusy) return;
    this.setData({ mutationBusy: true });
    try {
      await operation(createApiRuntime().merchant);
      wx.showToast({ title: successTitle, icon: 'success' });
      await this.load({ silent: true });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, fallbackMessage), icon: 'none' });
    } finally {
      this.setData({ mutationBusy: false });
    }
  },

  saveFee() {
    return this.runMutation(
      (merchant) => merchant.updateDeliveryFee(this.data.id, { deliveryFee: Number(this.data.feeInput || 0) }),
      '配送费已更新',
      '更新配送费失败'
    );
  },

  rejectOrder() {
    return this.runMutation((merchant) => merchant.rejectOrder(this.data.id), '已拒单', '拒单失败');
  },

  cancelOrder() {
    const reason = String(this.data.reasonInput || '').trim();
    if (this.data.scene && this.data.scene.order.rawStatus === 'PREPARING' && !reason) {
      wx.showToast({ title: '备餐中取消请填写原因', icon: 'none' });
      return;
    }
    return this.runMutation((merchant) => merchant.cancelOrder(this.data.id, { reason }), '订单已取消', '取消订单失败');
  },

  advanceOrder() {
    if (!this.data.scene) return;
    return this.runMutation(async (merchant) => {
      if (this.data.scene.nextStatus === 'CONFIRMED') await merchant.confirmOrder(this.data.id);
      else await merchant.advanceOrderStatus(this.data.id, {
        status: this.data.scene.nextStatus || nextStatusByStatus(this.data.scene.order.rawStatus),
        reason: String(this.data.reasonInput || '').trim()
      });
    }, '订单状态已更新', '更新状态失败');
  }
});
