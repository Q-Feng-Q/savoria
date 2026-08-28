const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiCartScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { createRequestId } = require('../../../utils/action-request');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: { deliveryOptions: [], addressOptions: [], addressIndex: 0, groupedItems: [], totals: {}, warningText: '',
    canSubmit: false, cart: {}, context: null, currentAddress: null, serviceDate: '', deliveryMode: 'PICKUP',
    addressId: null, expectedMealTimeOptions: [], expectedMealTimeIndex: 0, expandedDishIds: {}, mutationBusy: false,
    phase: 'loading', errorMessage: '' },
  onShow() { this.load(); },
  async load() {
    if (!requireSession()) return;
    const runtime = createApiRuntime();
    this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const bundle = await loadFamilyBundle(runtime);
      const [cart, addresses] = await Promise.all([runtime.cart.getCart(), runtime.family.getAddresses()]);
      this.source = { runtime, homeData: bundle.homeData, cart, addresses };
      this.renderCart();
      this.setData({ phase: 'ready' });
    } catch (error) {
      this.setData({ phase: 'error', errorMessage: error.message || '餐篮加载失败' });
      showApiError(error, '餐篮加载失败');
    }
  },
  renderCart() {
    if (!this.source) return;
    const scene = buildApiCartScene({ homeData: this.source.homeData, cart: this.source.cart,
      addresses: this.source.addresses, deliveryMode: this.data.deliveryMode || 'PICKUP', addressId: this.data.addressId });
    const expectedMealTimeIndex = Math.max(0, scene.expectedMealTimeOptions
      .findIndex((option) => option.value === scene.expectedMealTime));
    this.setData({ ...scene, expectedMealTimeIndex, deliveryMode: this.data.deliveryMode || 'PICKUP',
      addressId: scene.currentAddress ? scene.currentAddress.id : null });
  },
  async mutate(method, payload, fallback) {
    if (this.data.mutationBusy || !this.source) return null;
    this.setData({ mutationBusy: true });
    const cart = this.source.cart;
    try {
      const next = await this.source.runtime.cart[method]({ cartId: cart.cartId, cartVersion: cart.version,
        requestId: createRequestId(method), ...payload });
      this.source.cart = next;
      this.renderCart();
      return next;
    } catch (error) {
      if (error && (error.code === 40931 || error.code === 40932)) {
        await this.load();
        wx.showToast({ title: error.code === 40932 ? '原餐篮已提交，已打开新餐篮' : '家人刚更新了餐篮，请重新确认', icon: 'none' });
      } else showApiError(error, fallback);
      return null;
    } finally { this.setData({ mutationBusy: false }); }
  },
  bindExpectedMealTime(event) {
    const option = this.data.expectedMealTimeOptions[Number(event.detail.value || 0)];
    if (option) this.mutate('updateExpectedMealTime', { expectedMealTime: option.value }, '保存用餐时间失败');
  },
  selectDelivery(event) {
    if (event.currentTarget.dataset.disabled) return;
    const mode = event.currentTarget.dataset.mode;
    if (mode) this.setData({ deliveryMode: mode }, () => this.renderCart());
  },
  bindAddress(event) {
    const index = Number(event.detail.value || 0); const option = this.data.addressOptions[index];
    this.setData({ addressIndex: index, addressId: option ? option.value : null }, () => this.renderCart());
  },
  bindNote(event) { return this.mutate('updateRemark', { remark: event.detail.value || '' }, '保存备注失败'); },
  findCartRow(lineId) {
    for (const group of (this.data.groupedItems || [])) {
      const row = (group.rows || []).find((item) => Number(item.id) === Number(lineId)); if (row) return row;
    }
    return null;
  },
  bindItemNote(event) {
    const row = this.findCartRow(event.currentTarget.dataset.id); if (!row) return;
    return this.mutate('setItemQuantity', { dishId: row.dishId, quantity: row.myQuantity,
      itemRemark: event.detail.value || '' }, '保存菜品备注失败');
  },
  changeCount(event) {
    const row = this.findCartRow(event.currentTarget.dataset.id); if (!row) return;
    const target = Math.max(0, Number(row.myQuantity || 0) + Number(event.currentTarget.dataset.delta || 0));
    return this.mutate('setItemQuantity', { dishId: row.dishId, quantity: target, itemRemark: row.note || '' }, '更新数量失败');
  },
  toggleSelectionDetails(event) {
    const id = String(event.currentTarget.dataset.id); this.setData({ [`expandedDishIds.${id}`]: !this.data.expandedDishIds[id] });
  },
  async submitOrder() {
    if (!this.data.canSubmit || this.data.mutationBusy || !this.source) {
      wx.showToast({ title: this.data.warningText || '请先完善下单信息', icon: 'none' }); return;
    }
    this.setData({ mutationBusy: true });
    const cart = this.source.cart;
    try {
      await this.source.runtime.orders.submitOrder({ cartId: cart.cartId, cartVersion: cart.version,
        requestId: createRequestId('submit'), deliveryMode: this.data.deliveryMode,
        addressId: this.data.deliveryMode === 'DELIVERY' ? this.data.addressId : null, remark: cart.remark || '' });
      wx.showToast({ title: '订单已提交，可继续点新餐篮', icon: 'success' });
      await this.load();
      setTimeout(() => wx.switchTab({ url: '/pages/ordering/orders/index' }), 300);
    } catch (error) {
      if (error && (error.code === 40931 || error.code === 40932)) await this.load();
      showApiError(error, '提交订单失败');
    } finally { this.setData({ mutationBusy: false }); }
  }
});
