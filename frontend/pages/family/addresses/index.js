const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiAddressBookScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: {
    addresses: [],
    context: null,
    phase: 'loading',
    errorMessage: '',
    busyAddressMap: {}
  },

  onShow() {
    this.load();
  },

  retryLoad() { return this.load(); },

  async load({ silent = false } = {}) {
    const session = requireSession({ familyOnly: true });
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    if (!silent) this.setData({ phase: 'loading', errorMessage: '', addresses: [], context: null });

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const addresses = await runtime.family.getAddresses();
      const scene = buildApiAddressBookScene({
        homeData: bundle.homeData,
        addresses
          });
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ ...scene, phase: 'ready', errorMessage: '' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      if (!silent) this.setData({ phase: 'error', errorMessage: (error && error.message) || '地址加载失败' });
      showApiError(error, '地址加载失败');
    }
  },

  addAddress() {
    wx.navigateTo({ url: '/pages/family/address-edit/index' });
  },

  editAddress(event) {
    wx.navigateTo({ url: `/pages/family/address-edit/index?id=${event.currentTarget.dataset.id}` });
  },

  async setDefault(event) {
    const id = event.currentTarget.dataset.id;
    if (!id || this.data.busyAddressMap[id]) return;
    this.setData({ [`busyAddressMap.${id}`]: true });
    try {
      await createApiRuntime().family.setDefaultAddress(id);
      await this.load({ silent: true });
    } catch (error) {
      showApiError(error, '设置默认地址失败');
    } finally { this.setData({ [`busyAddressMap.${id}`]: false }); }
  }
});
