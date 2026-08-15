const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: {
    id: '',
    form: {
      contactName: '',
      phone: '',
      address: '',
      isDefault: false
    }
  },

  onLoad(query) {
    this.setData({ id: (query && query.id) || '' });
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;

    try {
      const addresses = await createApiRuntime().family.getAddresses();
      const current = (addresses || []).find((item) => Number(item.addressId) === Number(this.data.id));
      this.setData({
        form: current ? {
          contactName: current.contactName || '',
          phone: current.contactPhone || '',
          address: current.addressText || '',
          isDefault: Boolean(current.defaultAddress)
        } : {
          contactName: '',
          phone: '',
          address: '',
          isDefault: !(addresses || []).length
        }
      });
    } catch (error) {
      showApiError(error, '地址信息加载失败');
    }
  },

  bindField(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: event.detail.value });
  },

  toggleDefault() {
    this.setData({ 'form.isDefault': !this.data.form.isDefault });
  },

  async save() {
    const form = this.data.form;
    if (!String(form.contactName || '').trim() || !String(form.phone || '').trim() || !String(form.address || '').trim()) {
      wx.showToast({ title: '请把地址信息填写完整', icon: 'none' });
      return;
    }

    const payload = {
      contactName: String(form.contactName || '').trim(),
      contactPhone: String(form.phone || '').trim(),
      addressText: String(form.address || '').trim(),
      defaultAddress: Boolean(form.isDefault)
    };

    try {
      const runtime = createApiRuntime();
      if (this.data.id) {
        await runtime.family.updateAddress(this.data.id, payload);
      } else {
        await runtime.family.createAddress(payload);
      }
      wx.navigateBack();
    } catch (error) {
      showApiError(error, '保存地址失败');
    }
  }
});
