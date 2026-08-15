const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiPurchaseScene, buildPurchaseMealOptions } = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');
const { todayText } = require('../../../utils/date');

function appendTemporaryItems(copyText, tempItems) {
  const lines = [];
  const normalizedCopy = typeof copyText === 'string' ? copyText.trim() : '';
  if (normalizedCopy) lines.push(normalizedCopy);
  (tempItems || []).forEach((item) => {
    const ingredientName = String(item.ingredientName || '').trim();
    if (!ingredientName) return;
    const quantityText = [item.quantity, item.unit].filter((value) => value !== null && value !== undefined && value !== '').join(' ');
    lines.push(`${ingredientName}${quantityText ? ` ${quantityText}` : ''} / 临时补录`);
  });
  return lines.length ? lines.join('\n') : '当前没有采购项';
}

Page({
  data: {
    phase: 'loading',
    pageTitle: '正在生成采购清单',
    pageDescription: '请稍候',
    date: '',
    mealTypes: [{ key: 'all', label: '全天', mealSlotId: null }],
    mealIndex: 0,
    summaryCards: [],
    sourceSummary: { estimatedCount: 0, confirmedCount: 0, text: '预估 0 项 · 已确认 0 项' },
    items: [],
    tempItems: [],
    context: null,
    mealLabel: '全天',
    tempIngredientName: '',
    tempQuantity: '',
    tempUnit: '',
    tempRemark: '',
    purchaseBusy: false,
    busyItemMap: {}
  },

  onLoad(query) { this.setData({ date: (query && query.date) || todayText() }); },
  onShow() { this.load(); },

  async load({ silent = false } = {}) {
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    if (!silent) this.setData({ phase: 'loading', pageTitle: '正在生成采购清单', pageDescription: '请稍候' });
    try {
      const runtime = createApiRuntime();
      const date = this.data.date || todayText();
      const [families, summaryItems, tempItemResponse] = await Promise.all([
        runtime.merchant.getFamilies(),
        runtime.purchase.getSummary({ date, includePending: true }),
        runtime.purchase.getTempItems({ date })
      ]);
      if (generation !== this._loadGeneration) return;
      const mealTypes = buildPurchaseMealOptions(summaryItems);
      const tempItemMap = new Map();
      (tempItemResponse || []).forEach((item) => {
        if (item && item.temporary === true && item.itemId !== null && item.itemId !== undefined) {
          tempItemMap.set(String(item.itemId), item);
        }
      });
      const tempItems = Array.from(tempItemMap.values());
      const selectedKey = (this.data.mealTypes[this.data.mealIndex] || {}).key;
      const mealIndex = Math.max(0, mealTypes.findIndex((option) => option.key === selectedKey));
      this._purchaseSource = { session, date, families, summaryItems };
      this._allTempItems = tempItems;
      this.setData({ mealTypes, mealIndex }, () => this.rebuildScene(generation));
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({
        phase: 'error',
        pageTitle: '采购清单加载失败',
        pageDescription: resolveApiErrorMessage(error, '采购清单加载失败')
      });
    }
  },

  retryLoad() { return this.load(); },

  rebuildScene(generation = this._loadGeneration) {
    if (generation !== this._loadGeneration || !this._purchaseSource) return;
    const mealOption = this.data.mealTypes[this.data.mealIndex] || this.data.mealTypes[0];
    const scene = buildApiPurchaseScene({ ...this._purchaseSource, mealSlotId: mealOption.mealSlotId });
    const tempItems = (this._allTempItems || []).filter((item) => (
      !mealOption.mealSlotId
      || item.mealSlotId === null
      || item.mealSlotId === undefined
      || Number(item.mealSlotId) === Number(mealOption.mealSlotId)
    ));
    this.setData({
      ...scene,
      tempItems,
      mealTypes: this.data.mealTypes,
      mealIndex: this.data.mealIndex,
      mealLabel: mealOption.label,
      phase: scene.items.length || tempItems.length ? 'ready' : 'empty',
      pageTitle: scene.items.length || tempItems.length ? '' : '暂无采购项',
      pageDescription: scene.items.length || tempItems.length ? '' : '当前日期与餐次没有需要采购的食材。'
    });
  },

  bindDate(event) { this.setData({ date: event.detail.value, mealIndex: 0 }, () => this.load()); },
  bindMeal(event) { this.setData({ mealIndex: Number(event.detail.value || 0) }, () => this.rebuildScene()); },
  bindTempIngredientName(event) { this.setData({ tempIngredientName: event.detail.value }); },
  bindTempQuantity(event) { this.setData({ tempQuantity: event.detail.value }); },
  bindTempUnit(event) { this.setData({ tempUnit: event.detail.value }); },
  bindTempRemark(event) { this.setData({ tempRemark: event.detail.value }); },

  async runPurchaseMutation(operation, fallbackMessage, itemId = null) {
    if (itemId !== null && this.data.busyItemMap[itemId]) return;
    if (itemId === null && this.data.purchaseBusy) return;
    if (itemId === null) this.setData({ purchaseBusy: true });
    else this.setData({ busyItemMap: { ...this.data.busyItemMap, [itemId]: true } });
    try {
      await operation(createApiRuntime().purchase);
      await this.load({ silent: true });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, fallbackMessage), icon: 'none' });
    } finally {
      if (itemId === null) this.setData({ purchaseBusy: false });
      else {
        const busyItemMap = { ...this.data.busyItemMap };
        delete busyItemMap[itemId];
        this.setData({ busyItemMap });
      }
    }
  },

  toggleTempItem(event) {
    const itemId = Number(event.currentTarget.dataset.id || 0);
    if (!itemId) return;
    const checked = event.currentTarget.dataset.checked === true || event.currentTarget.dataset.checked === 'true';
    return this.runPurchaseMutation(
      (purchase) => purchase.toggleChecked(itemId, { checked: !checked }),
      '更新采购项失败',
      itemId
    );
  },

  deleteTempItem(event) {
    const itemId = Number(event.currentTarget.dataset.id || 0);
    if (!itemId) return;
    return this.runPurchaseMutation((purchase) => purchase.deleteTempItem(itemId), '删除临时采购项失败', itemId);
  },

  createTempItem() {
    const ingredientName = String(this.data.tempIngredientName || '').trim();
    const quantity = Number(this.data.tempQuantity);
    const unit = String(this.data.tempUnit || '').trim();
    const remark = String(this.data.tempRemark || '').trim();
    if (!ingredientName || !Number.isFinite(quantity) || quantity <= 0 || !unit) {
      wx.showToast({ title: '请填写食材、数量和单位', icon: 'none' });
      return;
    }
    const mealOption = this.data.mealTypes[this.data.mealIndex] || this.data.mealTypes[0];
    const payload = {
      date: this.data.date,
      mealSlotId: mealOption.mealSlotId,
      ingredientName,
      quantity,
      unit,
      remark
    };
    return this.runPurchaseMutation(async (purchase) => {
      await purchase.createTempItem(payload);
      this.setData({ tempIngredientName: '', tempQuantity: '', tempUnit: '', tempRemark: '' });
    }, '新增临时采购项失败');
  },

  async copyList() {
    const mealOption = this.data.mealTypes[this.data.mealIndex] || this.data.mealTypes[0];
    try {
      if (mealOption.mealSlotId) {
        const text = await createApiRuntime().purchase.getCopyText({ date: this.data.date, mealSlotId: mealOption.mealSlotId });
        wx.setClipboardData({ data: appendTemporaryItems(text, this.data.tempItems) });
        return;
      }
      const summaryText = this.data.items.length
        ? this.data.items.map((item) => `${item.ingredientName} ${item.quantityText} / ${item.statusText}`).join('\n')
        : '';
      wx.setClipboardData({ data: appendTemporaryItems(summaryText, this.data.tempItems) });
    } catch (error) {
      wx.showToast({ title: resolveApiErrorMessage(error, '复制采购清单失败'), icon: 'none' });
    }
  }
});
