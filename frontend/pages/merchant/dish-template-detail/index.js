const { createApiRuntime } = require('../../../utils/api-runtime');
const { toImageUrl } = require('../../../utils/image-url');
const { requireSession, showApiError } = require('../../../utils/page-api');

Page({
  data: { detail: null, loading: true },
  onLoad(options) { this.templateId = Number(options.id); this.load(); },
  async load() {
    if (!requireSession({ merchantOnly: true })) return;
    try {
      const runtime = createApiRuntime();
      const detail = await runtime.merchant.getDishTemplateDetail(this.templateId);
      this.setData({ detail: { ...detail, imageUrl: toImageUrl(runtime.baseUrl, detail.imageUrl),
        tasteText: (detail.tasteTags || []).join(' · '), mealText: (detail.mealTags || []).map(value => ({ BREAKFAST:'早餐',LUNCH:'午餐',DINNER:'晚餐' }[value] || value)).join(' · ')
      }, loading: false });
    } catch (error) { this.setData({ loading: false }); showApiError(error, '模板详情加载失败'); }
  },
  async importTemplate() {
    if (!this.data.detail || this.data.detail.imported) return;
    try {
      await createApiRuntime().merchant.importDishTemplates([this.templateId]);
      wx.showToast({ title: '已导入', icon: 'success' }); await this.load();
    } catch (error) { showApiError(error, '模板导入失败'); }
  }
});
