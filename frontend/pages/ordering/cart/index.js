const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiCartScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: {
    mealOptions: [],
    deliveryOptions: [],
    addressOptions: [],
    addressIndex: 0,
    groupedItems: [],
    chargeLines: [],
    totals: {
      dishTotal: 0,
      deliveryFee: 0,
      totalAmount: 0
    },
    warningText: '',
    canSubmit: false,
    cart: {},
    context: null,
    currentAddress: null,
    mealSlotId: null,
    serviceDate: '',
    deliveryMode: 'PICKUP',
    addressId: null
  },

  onLoad(query) {
    this.setData({
      mealSlotId: query && query.mealSlotId ? Number(query.mealSlotId) : null
    });
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime, { mealSlotId: this.data.mealSlotId });
      const [cart, addresses] = await Promise.all([
        runtime.cart.getCart({
          mealSlotId: bundle.activeMealSlotId,
          date: bundle.serviceDate
        }),
        runtime.family.getAddresses()
      ]);
      const deliveryMode = this.data.deliveryMode || 'PICKUP';
      const scene = buildApiCartScene({
        homeData: bundle.homeData,
        mealSlots: bundle.mealSlots,
        cart,
        addresses,
        deliveryMode,
        addressId: this.data.addressId
      });

      this.rawCart = cart;
      this.rawAddresses = addresses;
      this.setData({
        ...scene,
        mealSlotId: bundle.activeMealSlotId,
        serviceDate: bundle.serviceDate,
        deliveryMode,
        addressId: scene.currentAddress ? scene.currentAddress.id : null
      });
    } catch (error) {
      showApiError(error, '餐篮加载失败');
    }
  },

  async selectMeal(event) {
    this.setData({
      mealSlotId: Number(event.currentTarget.dataset.meal || 0)
    });
    await this.load();
  },

  selectDelivery(event) {
    if (event.currentTarget.dataset.disabled) return;
    const mode = event.currentTarget.dataset.mode;
    if (!mode) return;
    this.setData({ deliveryMode: mode }, () => this.load());
  },

  bindAddress(event) {
    const index = Number(event.detail.value || 0);
    const option = this.data.addressOptions[index];
    this.setData({
      addressIndex: index,
      addressId: option ? option.value : null
    }, () => this.load());
  },

  async bindNote(event) {
    const remark = event.detail.value || '';
    try {
      await createApiRuntime().cart.updateRemark({
        mealSlotId: this.data.mealSlotId,
        date: this.data.serviceDate,
        remark
      });
      this.setData({ 'cart.note': remark });
    } catch (error) {
      showApiError(error, '保存备注失败');
    }
  },

  async bindItemNote(event) {
    const lineId = Number(event.currentTarget.dataset.id || 0);
    const remark = event.detail.value || '';
    const row = this.findCartRow(lineId);
    if (!row) return;

    try {
      await createApiRuntime().cart.updateItem(lineId, {
        mealSlotId: this.data.mealSlotId,
        date: this.data.serviceDate,
        dishId: row.dishId,
        quantity: row.quantity,
        itemRemark: remark
      });
      await this.load();
    } catch (error) {
      showApiError(error, '保存菜品备注失败');
    }
  },

  findCartRow(lineId) {
    const groups = this.data.groupedItems || [];
    for (let i = 0; i < groups.length; i += 1) {
      const row = (groups[i].rows || []).find((item) => Number(item.id) === Number(lineId));
      if (row) return row;
    }
    return null;
  },

  async changeCount(event) {
    const lineId = Number(event.currentTarget.dataset.id || 0);
    const delta = Number(event.currentTarget.dataset.delta || 0);
    const row = this.findCartRow(lineId);
    if (!row) return;

    try {
      const nextQuantity = Number(row.quantity || 0) + delta;
      if (nextQuantity <= 0) {
        await createApiRuntime().cart.deleteItem(lineId);
      } else {
        await createApiRuntime().cart.updateItem(lineId, {
          mealSlotId: this.data.mealSlotId,
          date: this.data.serviceDate,
          dishId: row.dishId,
          quantity: nextQuantity,
          itemRemark: row.note || ''
        });
      }
      await this.load();
    } catch (error) {
      showApiError(error, '更新数量失败');
    }
  },

  async submitOrder() {
    if (!this.data.canSubmit) {
      wx.showToast({ title: this.data.warningText || '请先完善下单信息', icon: 'none' });
      return;
    }

    try {
      await createApiRuntime().orders.submitOrder({
        date: this.data.serviceDate,
        mealSlotId: this.data.mealSlotId,
        deliveryMode: this.data.deliveryMode,
        addressId: this.data.deliveryMode === 'DELIVERY' ? this.data.addressId : undefined,
        remark: (this.data.cart && this.data.cart.note) || ''
      });
      wx.showToast({ title: '订单已提交', icon: 'success' });
      setTimeout(() => {
        wx.switchTab({ url: '/pages/ordering/orders/index' });
      }, 250);
    } catch (error) {
      showApiError(error, '提交订单失败');
    }
  }
});
