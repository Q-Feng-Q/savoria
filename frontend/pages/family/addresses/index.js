const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiAddressBookScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
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

    const runtime = createApiRuntime();
    try {
      const bundle = await loadFamilyBundle(runtime);
      const addresses = await runtime.family.getAddresses();
      const scene = buildApiAddressBookScene({
        homeData: bundle.homeData,
        addresses
      });
      this.setData(scene);
    } catch (error) {
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
