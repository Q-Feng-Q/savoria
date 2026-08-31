const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError, resolveApiErrorMessage } = require('../../../utils/page-api');
const { decorateChangeRequest } = require('../../../utils/dish-template-change');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

const FIELD_META = [
  ['name', '菜品名称'], ['categoryId', '分类 ID'], ['description', '菜品简介'],
  ['referencePrice', '参考价格'], ['tasteTags', '口味标签'], ['mealTags', '推荐餐次'],
  ['imageUrl', '图片路径'], ['imageSourceUrl', '来源页面'], ['imageAuthor', '图片作者'],
  ['imageLicense', '授权说明'], ['sortOrder', '排序值'], ['enabled', '启用状态']
];

function displayValue(value) {
  if (Array.isArray(value)) return value.join('、') || '无';
  if (typeof value === 'boolean') return value ? '启用' : '停用';
  return value === undefined || value === null || value === '' ? '未填写' : String(value);
}

function buildComparison(base = {}, target = {}) {
  return FIELD_META.map(([key, label]) => ({
    key, label, baseText: displayValue(base[key]), targetText: displayValue(target[key]),
    changed: JSON.stringify(base[key]) !== JSON.stringify(target[key])
  }));
}

function decorateIngredients(rows) {
  return (rows || []).map((item, index) => ({ ...item, key: `${index}-${item.ingredientName}`, text: `${item.quantity} ${item.unit} · ${item.ingredientCategory} · ${item.calcType}` }));
}

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'loading', errorMessage: '', detail: null, comparison: [], baseIngredients: [], targetIngredients: [], withdrawing: false },
  onLoad(options) { this.requestId = Number(options.id); },
  onShow() { this.load(); },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()); },
  async load() {
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    this.setData({ phase: 'loading', errorMessage: '', detail: null, comparison: [], baseIngredients: [], targetIngredients: [] });
    try {
      const result = await createApiRuntime().merchant.getDishTemplateChangeDetail(this.requestId);
      const detail = decorateChangeRequest(result);
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ detail, comparison: buildComparison(detail.baseSnapshot, detail.targetSnapshot), baseIngredients: decorateIngredients(detail.baseSnapshot.ingredients), targetIngredients: decorateIngredients(detail.targetSnapshot.ingredients), phase: 'ready' });
    } catch (error) {
      if (this.identityLoad.isCurrent(loadToken)) this.setData({ phase: 'error', errorMessage: resolveApiErrorMessage(error, '申请详情加载失败') });
    }
  },
  retryLoad() { return this.load(); },
  async withdraw() {
    if (!this.data.detail || !this.data.detail.canWithdraw || this.data.withdrawing) return;
    const result = await wx.showModal({ title: '撤回修改申请', content: '撤回后管理员将无法继续审核，本次修改也不会生效。', confirmText: '确认撤回' });
    if (!result.confirm) return;
    this.setData({ withdrawing: true });
    try { await createApiRuntime().merchant.withdrawDishTemplateChange(this.requestId); wx.showToast({ title: '申请已撤回', icon: 'success' }); await this.load(); }
    catch (error) { showApiError(error, '撤回申请失败'); }
    finally { this.setData({ withdrawing: false }); }
  }
});

module.exports = { buildComparison };
