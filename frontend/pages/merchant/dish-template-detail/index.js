const { createApiRuntime } = require('../../../utils/api-runtime');
const { toImageUrl } = require('../../../utils/image-url');
const { decorateTemplateDetail } = require('../../../utils/dish-template-selection');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: { detail: null, loading: true, phase: 'loading', errorMessage: '', importing: false },
  onLoad(options) { this.templateId = Number(options && options.id); this.load(); },
  retryLoad() { return this.load(); },
  async load() {
    if (!requireSession({ merchantOnly: true })) return;
    if (!Number.isFinite(this.templateId) || this.templateId <= 0) {
      this.setData({ loading: false, phase: 'error', errorMessage: '缺少模板菜品编号', detail: null });
      return;
    }
    this.setData({ loading: true, phase: 'loading', errorMessage: '', detail: null });
    try {
      const runtime = createApiRuntime();
      const detail = await runtime.merchant.getDishTemplateDetail(this.templateId);
      this.setData({
        detail: decorateTemplateDetail(detail, (value) => toImageUrl(runtime.baseUrl, value)),
        loading: false,
        phase: 'ready',
        errorMessage: ''
      });
    } catch (error) {
      this.setData({ loading: false, phase: 'error', errorMessage: (error && error.message) || '模板详情加载失败' });
      showApiError(error, '模板详情加载失败');
    }
  },
  async importTemplate() {
    if (this.data.importing || !this.data.detail || this.data.detail.imported) return;
    this.setData({ importing: true });
    try {
      await createApiRuntime().merchant.importDishTemplates([this.templateId]);
      wx.showToast({ title: '已导入', icon: 'success' }); await this.load();
    } catch (error) { showApiError(error, '模板导入失败'); }
    finally { this.setData({ importing: false }); }
  },
  requestChange() {
    wx.navigateTo({ url: `/pages/merchant/dish-template-change-edit/index?id=${this.templateId}` });
  }
});
