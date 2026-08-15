const { createApiRuntime } = require('../../utils/api-runtime');
const { buildApiMerchantScene } = require('../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../utils/page-api');
const { todayText } = require('../../utils/date');

const loadingRegionStates = () => ({
  families: { phase: 'loading', message: '' },
  orders: { phase: 'loading', message: '' },
  purchase: { phase: 'loading', message: '' },
  menu: { phase: 'loading', message: '' }
});

const resultValue = (result) => (result && result.status === 'fulfilled' ? result.value : []);
const resultState = (result, fallbackMessage) => {
  if (result.status === 'fulfilled') return { phase: 'ready', message: '' };
  return { phase: 'error', message: resolveApiErrorMessage(result.reason, fallbackMessage) };
};

Page({
  data: {
    phase: 'loading',
    pageTitle: '',
    pageDescription: '',
    context: null,
    todayLabel: todayText(),
    todayCommand: { activeOrderCount: 0 },
    familyCount: 0,
    todayPurchaseItemCount: 0,
    regionStates: loadingRegionStates()
  },

  onShow() {
    this.load();
  },

  isCurrentGeneration(generation, runtime, payload) {
    return generation === this._loadGeneration
      && runtime === this._runtime
      && (!payload || payload === this._overviewPayload);
  },

  async load() {
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    const session = requireSession({ merchantOnly: true });
    if (!session) return;

    const runtime = createApiRuntime();
    this._session = session;
    this._runtime = runtime;
    this._overviewPayload = null;
    this.setData({
      phase: 'loading',
      pageTitle: '',
      pageDescription: '',
      regionStates: loadingRegionStates()
    });

    const [familiesResult, ordersResult, purchaseResult] = await Promise.allSettled([
      runtime.merchant.getFamilies(),
      runtime.merchant.getOrders(),
      runtime.purchase.getSummary({ date: todayText(), includePending: true })
    ]);
    if (!this.isCurrentGeneration(generation, runtime)) return;
    const familiesState = resultState(familiesResult, '家庭概况加载失败');
    const ordersState = resultState(ordersResult, '订单概况加载失败');
    const purchaseState = resultState(purchaseResult, '采购概况加载失败');

    if ([familiesResult, ordersResult, purchaseResult].every((result) => result.status === 'rejected')) {
      this.setData({
        phase: 'error',
        pageTitle: '工作台暂时无法加载',
        pageDescription: '请检查网络后重试。',
        regionStates: {
          families: familiesState,
          orders: ordersState,
          purchase: purchaseState,
          menu: { phase: 'error', message: '家庭概况不可用，菜单暂不可加载' }
        }
      });
      return;
    }

    const families = resultValue(familiesResult);
    const selectedFamilyId = (families[0] && families[0].familyId) || null;
    let menuResult = null;
    let menuState = { phase: 'error', message: '家庭概况不可用，菜单暂不可加载' };
    if (familiesResult.status === 'fulfilled') {
      [menuResult] = await Promise.allSettled([
        selectedFamilyId ? runtime.merchant.getFamilyMenu(selectedFamilyId) : Promise.resolve([])
      ]);
      if (!this.isCurrentGeneration(generation, runtime)) return;
      menuState = resultState(menuResult, '菜单概况加载失败');
    }

    const payload = {
      families,
      orders: resultValue(ordersResult),
      purchaseSummary: resultValue(purchaseResult),
      familyMenuItems: resultValue(menuResult),
      selectedFamilyId
    };
    const regionStates = {
      families: familiesState,
      orders: ordersState,
      purchase: purchaseState,
      menu: menuState
    };
    if (!this.isCurrentGeneration(generation, runtime)) return;
    this._overviewPayload = payload;
    this.applyOverview(generation, runtime, session, payload, regionStates);
  },

  applyOverview(generation, runtime, session, payload, regionStates = this.data.regionStates) {
    if (!payload || !this.isCurrentGeneration(generation, runtime, payload)) return;
    const scene = buildApiMerchantScene({ session, ...payload });
    const allRegionsLoaded = ['families', 'orders', 'purchase', 'menu']
      .every((region) => regionStates[region].phase === 'ready');
    const allOverviewCollectionsEmpty = [
      payload.families,
      payload.orders,
      payload.purchaseSummary,
      payload.familyMenuItems
    ].every((items) => items.length === 0);
    const hasUsableOverview = Boolean(scene && scene.context && scene.context.merchant)
      && !(allRegionsLoaded && allOverviewCollectionsEmpty);
    if (!this.isCurrentGeneration(generation, runtime, payload)) return;
    this.setData({
      ...scene,
      familyCount: payload.families.length,
      regionStates,
      phase: hasUsableOverview ? 'ready' : 'empty',
      pageTitle: hasUsableOverview ? '' : '暂无工作台信息',
      pageDescription: hasUsableOverview ? '' : '当前账号还没有可用的商户概览。'
    });
  },

  retryLoad() {
    return this.load();
  },

  async retryFamilies() {
    const generation = this._loadGeneration;
    const runtime = this._runtime;
    const payload = this._overviewPayload;
    const session = this._session;
    if (!runtime || !payload) return this.load();
    this.setRegionState('families', 'loading', '', generation, runtime, payload);
    this.setRegionState('menu', 'loading', '', generation, runtime, payload);
    try {
      const families = await runtime.merchant.getFamilies();
      if (!this.isCurrentGeneration(generation, runtime, payload)) return;
      payload.families = families;
      payload.selectedFamilyId = (families[0] && families[0].familyId) || null;
      this.setRegionState('families', 'ready', '', generation, runtime, payload);
      try {
        const familyMenuItems = payload.selectedFamilyId
          ? await runtime.merchant.getFamilyMenu(payload.selectedFamilyId)
          : [];
        if (!this.isCurrentGeneration(generation, runtime, payload)) return;
        payload.familyMenuItems = familyMenuItems;
        this.setRegionState('menu', 'ready', '', generation, runtime, payload);
      } catch (error) {
        if (!this.isCurrentGeneration(generation, runtime, payload)) return;
        const message = resolveApiErrorMessage(error, '菜单概况加载失败');
        this.setRegionState('menu', 'error', message, generation, runtime, payload);
      }
      this.applyOverview(generation, runtime, session, payload, this.data.regionStates);
    } catch (error) {
      if (!this.isCurrentGeneration(generation, runtime, payload)) return;
      const message = resolveApiErrorMessage(error, '家庭概况加载失败');
      this.setRegionState('families', 'error', message, generation, runtime, payload);
      this.setRegionState('menu', 'error', '家庭概况不可用，菜单暂不可加载', generation, runtime, payload);
    }
  },

  async retryOrders() {
    return this.retryRegion('orders', '订单概况加载失败', (runtime) => runtime.merchant.getOrders(), 'orders');
  },

  async retryPurchase() {
    return this.retryRegion(
      'purchase',
      '采购概况加载失败',
      (runtime) => runtime.purchase.getSummary({ date: todayText(), includePending: true }),
      'purchaseSummary'
    );
  },

  async retryMenu() {
    const generation = this._loadGeneration;
    const runtime = this._runtime;
    const payload = this._overviewPayload;
    if (!runtime || !payload) return this.load();
    if (this.data.regionStates.families.phase !== 'ready' || !payload.selectedFamilyId) {
      this.setRegionState(
        'menu',
        'error',
        '家庭概况不可用，菜单暂不可加载',
        generation,
        runtime,
        payload
      );
      return;
    }
    return this.retryRegion(
      'menu',
      '菜单概况加载失败',
      (runtime, payload) => (payload.selectedFamilyId
        ? runtime.merchant.getFamilyMenu(payload.selectedFamilyId)
        : Promise.resolve([])),
      'familyMenuItems'
    );
  },

  async retryRegion(region, fallbackMessage, request, payloadKey) {
    const generation = this._loadGeneration;
    const runtime = this._runtime;
    const payload = this._overviewPayload;
    const session = this._session;
    if (!runtime || !payload) return this.load();
    this.setRegionState(region, 'loading', '', generation, runtime, payload);
    try {
      const value = await request(runtime, payload);
      if (!this.isCurrentGeneration(generation, runtime, payload)) return;
      payload[payloadKey] = value;
      this.setRegionState(region, 'ready', '', generation, runtime, payload);
      this.applyOverview(generation, runtime, session, payload, this.data.regionStates);
    } catch (error) {
      if (!this.isCurrentGeneration(generation, runtime, payload)) return;
      const message = resolveApiErrorMessage(error, fallbackMessage);
      this.setRegionState(region, 'error', message, generation, runtime, payload);
    }
  },

  setRegionState(region, phase, message, generation, runtime, payload) {
    if (!this.isCurrentGeneration(generation, runtime, payload)) return;
    this.setData({
      regionStates: {
        ...this.data.regionStates,
        [region]: { phase, message }
      }
    });
  },

  openPurchase() {
    wx.navigateTo({ url: `/pages/merchant/purchase/index?date=${todayText()}` });
  },

  openNotifications() {
    wx.navigateTo({ url: '/pages/account/notifications/index?actor=merchant' });
  },

  openAccountSwitcher() {
    wx.navigateTo({ url: '/pages/account/account-management/index' });
  },

  openMerchantOrders() {
    wx.redirectTo({ url: '/pages/merchant/merchant-orders/index' });
  }
});
