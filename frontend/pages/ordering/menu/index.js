const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMenuScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { createRequestId } = require('../../../utils/action-request');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');
const { PRODUCT_TYPES } = require('../../../utils/nourishment');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { searchKeyword: '', activeCategoryKey: 'all', activeCategoryLabel: '全部菜品', categoryOptions: [], menuCards: [], visibleMenuCards: [], menuSections: [], cartItemCount: 0,
    currentDate: '', currentMemberName: '', resultSummaryText: '', emptyStateText: '', context: null,
    productType: '', productTypeIndex: 0, productTypes: [{ value: '', label: '全部类型' }, ...PRODUCT_TYPES],
    dishScrollIntoView: '', categoryScrollIntoView: 'menu-category-all', dishScrollWithAnimation: false,
    mutationBusy: false, phase: 'loading', errorMessage: '' },
  onShow() { this.load(); },
  async load() {
    const session = requireSession({ familyOnly: true });
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    const runtime = createApiRuntime();
    this.sceneSource = null;
    this.setData({ phase: 'loading', errorMessage: '', context: null, menuCards: [], visibleMenuCards: [], menuSections: [] });
    try {
      const bundle = await loadFamilyBundle(runtime);
      const [menuItems, cart] = await Promise.all([runtime.family.getMenuItems(), runtime.cart.getCart()]);
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.sceneSource = { homeData: bundle.homeData, menuItems, cart, runtime };
      this.refreshView({ searchKeyword: this.data.searchKeyword || '' });
      this.setData({ phase: 'ready' }, () => this.scheduleSectionMeasurement());
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '点菜页加载失败' });
      showApiError(error, '点菜页加载失败');
    }
  },
  refreshView(options = {}) {
    if (!this.sceneSource) return;
    const searchKeyword = Object.prototype.hasOwnProperty.call(options, 'searchKeyword')
      ? options.searchKeyword : this.data.searchKeyword;
    const activeCategoryKey = Object.prototype.hasOwnProperty.call(options, 'activeCategoryKey')
      ? options.activeCategoryKey : this.data.activeCategoryKey;
    const resetScroll = Boolean(options.resetScroll);
    const productType = Object.prototype.hasOwnProperty.call(options, 'productType') ? options.productType : this.data.productType;
    const nextState = { ...buildApiMenuScene({ ...this.sceneSource, searchKeyword, activeCategoryKey,
      productType, imageBaseUrl: this.sceneSource.runtime.baseUrl }), searchKeyword, productType };
    if (resetScroll) {
      this.sectionOffsets = [];
      this.currentDishScrollTop = 0;
      this.pendingScrollCategoryKey = 'all';
      Object.assign(nextState, {
        dishScrollIntoView: 'menu-list-top',
        categoryScrollIntoView: 'menu-category-all',
        dishScrollWithAnimation: false
      });
    }
    this.setData(nextState, () => this.scheduleSectionMeasurement());
  },
  handleSearchInput(event) { this.refreshView({ searchKeyword: event.detail.value || '', activeCategoryKey: 'all', resetScroll: true }); },
  changeProductType(event) {
    const index = Number(event.detail.value);
    const option = this.data.productTypes[index];
    if (!option) return;
    this.setData({ productTypeIndex: index });
    this.refreshView({ productType: option.value, activeCategoryKey: 'all', resetScroll: true });
    const first = this.data.menuSections[0];
    if (first) {
      this.pendingScrollCategoryKey = first.key;
      this.setActiveCategory(first.key);
      this.setData({ dishScrollIntoView: first.anchorId, dishScrollWithAnimation: false });
    }
  },
  clearSearch() { this.refreshView({ searchKeyword: '', activeCategoryKey: 'all', resetScroll: true }); },
  selectCategory(event) {
    const key = String(event.currentTarget.dataset.key || 'all');
    const target = key === 'all' ? 'menu-list-top'
      : ((this.data.menuSections || []).find((section) => section.key === key) || {}).anchorId;
    if (!target) return;
    this.pendingScrollCategoryKey = key;
    this.setActiveCategory(key);
    this.setData({ dishScrollIntoView: target, dishScrollWithAnimation: true });
  },
  setActiveCategory(key) {
    const options = this.data.categoryOptions || [];
    const selected = options.find((item) => item.key === key) || options[0];
    if (!selected) return;
    this.setData({
      activeCategoryKey: selected.key,
      activeCategoryLabel: selected.key === 'all' ? '全部菜品' : selected.label,
      categoryScrollIntoView: 'menu-category-' + selected.key,
      categoryOptions: options.map((item) => ({
        ...item,
        activeClass: item.key === selected.key ? 'active' : ''
      }))
    });
  },
  handleDishScroll(event) {
    const scrollTop = Number((event.detail || {}).scrollTop || 0);
    this.currentDishScrollTop = scrollTop;
    if (!(this.sectionOffsets || []).length && this.pendingScrollCategoryKey && this.pendingScrollCategoryKey !== 'all') return;
    let activeKey = 'all';
    for (const section of this.sectionOffsets || []) {
      if (scrollTop + 24 < section.top) break;
      activeKey = section.key;
    }
    if (activeKey !== this.data.activeCategoryKey) this.setActiveCategory(activeKey);
    if (this.pendingScrollCategoryKey === activeKey) {
      this.pendingScrollCategoryKey = null;
      this.setData({ dishScrollIntoView: '', dishScrollWithAnimation: false });
    }
  },
  scheduleSectionMeasurement() {
    const measure = () => this.measureMenuSections();
    if (wx.nextTick) wx.nextTick(measure);
    else setTimeout(measure, 0);
  },
  measureMenuSections() {
    if (!wx.createSelectorQuery) return;
    const query = wx.createSelectorQuery().in(this);
    query.select('.dish-list-scroll').boundingClientRect();
    query.selectAll('.menu-category-section').boundingClientRect();
    query.exec((results) => {
      const container = results && results[0];
      const sections = results && results[1];
      if (!container || !Array.isArray(sections)) {
        this.sectionOffsets = [];
        return;
      }
      const scrollTop = Number(this.currentDishScrollTop || 0);
      this.sectionOffsets = sections.map((section) => ({
        key: String((section.dataset || {}).key || ''),
        top: Number(section.top) - Number(container.top) + scrollTop
      })).filter((section) => section.key).sort((left, right) => left.top - right.top);
    });
  },
  noop() {},
  openDishFromRow(event) { wx.navigateTo({ url: `/pages/ordering/dish-detail/index?id=${event.detail.dish.id}` }); },
  async mutateDish(dish, quantity) {
    if (this.data.mutationBusy || !this.sceneSource) return;
    this.setData({ mutationBusy: true });
    const cart = this.sceneSource.cart;
    try {
      this.sceneSource.cart = await this.sceneSource.runtime.cart.setItemQuantity({
        cartId: cart.cartId, cartVersion: cart.version, requestId: createRequestId(`dish-${dish.id}`),
        dishId: Number(dish.id), quantity: Math.max(0, Number(quantity || 0)), itemRemark: ''
      });
      this.refreshView();
    } catch (error) {
      if (error && (error.code === 40931 || error.code === 40932)) {
        await this.load();
        wx.showToast({ title: error.code === 40932 ? '原餐篮已提交，已切换到新餐篮' : '餐篮已被家人更新，请重新操作', icon: 'none' });
      } else showApiError(error, '更新餐篮失败');
    } finally { this.setData({ mutationBusy: false }); }
  },
  changeDishQuantity(event) { const dish = event.detail.dish; return this.mutateDish(dish, event.detail.value); },
  addDish(event) {
    const dish = (this.data.menuCards || []).find((item) => Number(item.id) === Number(event.currentTarget.dataset.id));
    if (dish) return this.mutateDish(dish, Number(dish.myQuantity || 0) + 1);
  },
  removeDish(event) {
    const dish = (this.data.menuCards || []).find((item) => Number(item.cartLineId) === Number(event.currentTarget.dataset.lineId));
    if (dish) return this.mutateDish(dish, Number(dish.myQuantity || 0) - 1);
  },
  openDishDetail(event) { wx.navigateTo({ url: `/pages/ordering/dish-detail/index?id=${event.currentTarget.dataset.id}` }); },
});
