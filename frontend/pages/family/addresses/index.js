const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiAddressBookScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

Page({
  identityLoad: createIdentityLoadGuard(),
  data: {
    addresses: [],
    context: null
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    this.setData({ addresses: [], context: null });

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const addresses = await runtime.family.getAddresses();
      const scene = buildApiAddressBookScene({
        homeData: bundle.homeData,
        addresses
          });
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData(scene);
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
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
    try {
      await createApiRuntime().family.setDefaultAddress(event.currentTarget.dataset.id);
      await this.load();
    } catch (error) {
      showApiError(error, '设置默认地址失败');
    }
  }
});
