const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiFamilyMenuScene } = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');

function dishKey(value) { return String(value); }

function deriveSelection(draft, selectedDishMap) {
  const ids = new Set((draft || []).map((item) => dishKey(item.dishId)));
  const selected = Object.keys(selectedDishMap || {}).filter((id) => ids.has(id) && selectedDishMap[id]);
  return {
    selectedDishMap: Object.fromEntries(selected.map((id) => [id, true])),
    selectedCount: selected.length,
    allSelected: ids.size > 0 && selected.length === ids.size
  };
}

Page({
  data: {
    phase: 'loading', pageTitle: '正在读取家庭菜单', pageDescription: '请稍候',
    familyId: '', familyOptions: [], familyIndex: 0, sourceFamilyOptions: [], sourceFamilyIndex: 0,
    menuRows: [], selectedDishMap: {}, selectedCount: 0, allSelected: false,
    mutationBusy: false, context: null, currentFamilyName: '未选择家庭'
  },

  onLoad(query) { this.setData({ familyId: (query && query.familyId) || '' }); },
  onShow() { this.load(); },

  async load({ silent = false } = {}) {
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    if (!silent) this.setData({ phase: 'loading', pageTitle: '正在读取家庭菜单', pageDescription: '请稍候' });
    try {
      const runtime = createApiRuntime();
      const families = await runtime.merchant.getFamilies();
      const requestedFamily = (families || []).find((item) => Number(item.familyId) === Number(this.data.familyId));
      const currentFamilyId = requestedFamily
        ? requestedFamily.familyId
        : ((families[0] && families[0].familyId) || '');
      const menuItems = currentFamilyId ? await runtime.merchant.getFamilyMenu(currentFamilyId) : [];
      if (generation !== this._loadGeneration) return;
      this.menuDraft = (menuItems || []).map((item) => ({
        dishId: item.dishId,
        enabled: Boolean(item.enabled),
        sortOrder: Number(item.sortOrder || 0),
        familyFinalPrice: Number(item.familyFinalPrice ?? item.basePrice ?? 0)
      }));
      const scene = buildApiFamilyMenuScene({ session, currentFamilyId, families, menuItems, imageBaseUrl: runtime.baseUrl });
      const current = (scene.familyOptions || [])[scene.familyIndex];
      this.setData({
        ...scene,
        familyId: currentFamilyId,
        selectedDishMap: {},
        selectedCount: 0,
        allSelected: false,
        currentFamilyName: current ? current.label : '未选择家庭',
        phase: scene.menuRows.length ? 'ready' : 'empty',
        pageTitle: scene.menuRows.length ? '' : '暂无可配置菜品',
        pageDescription: scene.menuRows.length ? '' : (currentFamilyId ? '可复制其他家庭菜单，或先在菜品管理中上架菜品。' : '当前商户还没有服务家庭。')
      });
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ phase: 'error', pageTitle: '家庭菜单加载失败', pageDescription: resolveApiErrorMessage(error, '家庭菜单加载失败') });
    }
  },

  retryLoad() { return this.load(); },

  refreshSelection(selectedDishMap = this.data.selectedDishMap) {
    const selection = deriveSelection(this.menuDraft, selectedDishMap);
    this.setData(selection);
    return selection;
  },

  toggleSelection(event) {
    if (this.data.mutationBusy) return;
    const id = dishKey(event.currentTarget.dataset.id);
    if (!(this.menuDraft || []).some((item) => dishKey(item.dishId) === id)) return;
    const selectedDishMap = { ...this.data.selectedDishMap };
    if (selectedDishMap[id]) delete selectedDishMap[id];
    else selectedDishMap[id] = true;
    this.refreshSelection(selectedDishMap);
  },

  toggleSelectAll() {
    if (this.data.mutationBusy) return;
    if (this.data.allSelected) {
      this.refreshSelection({});
      return;
    }
    this.refreshSelection(Object.fromEntries((this.menuDraft || []).map((item) => [dishKey(item.dishId), true])));
  },

  saveMenuDraft(draft = this.menuDraft) {
    return createApiRuntime().merchant.saveFamilyMenu(this.data.familyId, {
      items: (draft || []).map((item) => ({ ...item }))
    });
  },

  async commitMenuDraft(nextDraft) {
    await this.saveMenuDraft(nextDraft);
    await this.load({ silent: true });
  },

  async runSaving(task, fallback) {
    if (this.data.mutationBusy) return;
    this.setData({ mutationBusy: true });
    try { await task(); }
    catch (error) { wx.showToast({ title: resolveApiErrorMessage(error, fallback), icon: 'none' }); }
    finally { this.setData({ mutationBusy: false }); }
  },

  async bindFamily(event) {
    if (this.data.mutationBusy) return;
    const option = this.data.familyOptions[Number(event.detail.value || 0)];
    if (!option) return;
    this.setData({ familyId: option.value, selectedDishMap: {}, selectedCount: 0, allSelected: false });
    return this.load();
  },

  bindSourceFamily(event) { this.setData({ sourceFamilyIndex: Number(event.detail.value || 0) }); },

  copyFromFamily() {
    const option = this.data.sourceFamilyOptions[Number(this.data.sourceFamilyIndex || 0)];
    if (!option || !this.data.familyId) return;
    return this.runSaving(async () => {
      await createApiRuntime().merchant.copyFamilyMenu(this.data.familyId, {
        sourceFamilyId: option.value
      });
      wx.showToast({ title: '已复制菜单', icon: 'success' });
      await this.load({ silent: true });
    }, '复制菜单失败');
  },

  enableSelected() {
    if (this.data.mutationBusy || !this.data.selectedCount) return;
    const selected = this.data.selectedDishMap;
    const nextDraft = (this.menuDraft || []).map((item) => ({
      ...item,
      enabled: selected[dishKey(item.dishId)] ? true : item.enabled
    }));
    return this.runSaving(async () => {
      await this.saveMenuDraft(nextDraft);
      await this.load({ silent: true });
      wx.showToast({ title: '所选菜品已启用', icon: 'success' });
    }, '批量启用失败');
  },

  enableAll() {
    if (this.data.mutationBusy) return;
    const draft = this.menuDraft || [];
    if (!draft.length || draft.every((item) => item.enabled)) {
      wx.showToast({ title: '全部菜品已启用', icon: 'none' });
      return;
    }
    const nextDraft = draft.map((item) => ({ ...item, enabled: true }));
    return this.runSaving(async () => {
      await this.saveMenuDraft(nextDraft);
      await this.load({ silent: true });
      wx.showToast({ title: '全部菜品已启用', icon: 'success' });
    }, '一键启用失败');
  },

  toggleDish(event) {
    const dishId = Number(event.currentTarget.dataset.id || 0);
    const nextDraft = (this.menuDraft || []).map((item) => ({ ...item }));
    const target = nextDraft.find((item) => Number(item.dishId) === dishId);
    if (!target) return;
    return this.runSaving(async () => {
      target.enabled = !target.enabled;
      await this.commitMenuDraft(nextDraft);
    }, '更新菜单失败');
  },

  changePrice(event) {
    const dishId = Number(event.currentTarget.dataset.id || 0);
    const delta = Number(event.currentTarget.dataset.delta || 0);
    const nextDraft = (this.menuDraft || []).map((item) => ({ ...item }));
    const target = nextDraft.find((item) => Number(item.dishId) === dishId);
    if (!target) return;
    return this.runSaving(async () => {
      target.familyFinalPrice = Math.max(0, Number(target.familyFinalPrice || 0) + delta);
      await this.commitMenuDraft(nextDraft);
    }, '更新价格失败');
  }
});

module.exports = { dishKey, deriveSelection };
