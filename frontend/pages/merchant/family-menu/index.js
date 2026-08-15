const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiFamilyMenuScene } = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');

Page({
  data: {
    phase: 'loading', pageTitle: '正在读取家庭菜单', pageDescription: '请稍候',
    familyId: '', familyOptions: [], familyIndex: 0, sourceFamilyOptions: [], sourceFamilyIndex: 0,
    menuRows: [], saving: false, rowBusyMap: {}, context: null, currentFamilyName: '未选择家庭'
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

  saveMenuDraft(draft = this.menuDraft) {
    return createApiRuntime().merchant.saveFamilyMenu(this.data.familyId, {
      items: (draft || []).map((item) => ({ ...item }))
    });
  },

  async commitMenuDraft(nextDraft) {
    const priorDraft = this.menuDraft || [];
    this.menuDraft = nextDraft;
    try {
      await this.saveMenuDraft(nextDraft);
    } catch (error) {
      this.menuDraft = priorDraft;
      throw error;
    }
    await this.load({ silent: true });
  },

  async runSaving(task, fallback, rowBusyId = null) {
    if (rowBusyId !== null && this.data.rowBusyMap[rowBusyId]) return;
    if (rowBusyId === null && this.data.saving) return;
    if (rowBusyId === null) this.setData({ saving: true });
    else this.setData({ rowBusyMap: { ...this.data.rowBusyMap, [rowBusyId]: true } });
    try { await task(); }
    catch (error) { wx.showToast({ title: resolveApiErrorMessage(error, fallback), icon: 'none' }); }
    finally {
      if (rowBusyId === null) this.setData({ saving: false });
      else {
        const rowBusyMap = { ...this.data.rowBusyMap };
        delete rowBusyMap[rowBusyId];
        this.setData({ rowBusyMap });
      }
    }
  },

  bindFamily(event) {
    if (this.data.saving || Object.keys(this.data.rowBusyMap).length) return;
    const option = this.data.familyOptions[Number(event.detail.value || 0)];
    if (!option) return;
    this.setData({ familyId: option.value }, () => this.load());
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

  toggleDish(event) {
    const dishId = Number(event.currentTarget.dataset.id || 0);
    const nextDraft = (this.menuDraft || []).map((item) => ({ ...item }));
    const target = nextDraft.find((item) => Number(item.dishId) === dishId);
    if (!target) return;
    return this.runSaving(async () => {
      target.enabled = !target.enabled;
      await this.commitMenuDraft(nextDraft);
    }, '更新菜单失败', dishId);
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
    }, '更新价格失败', dishId);
  }
});
