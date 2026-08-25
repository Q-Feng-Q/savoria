const { createApiRuntime } = require('../../../utils/api-runtime');
const { createDirtyForm } = require('../../../utils/dirty-form');
const { toImageUrl } = require('../../../utils/image-url');
const { requireSession, showApiError, resolveApiErrorMessage } = require('../../../utils/page-api');
const { buildTemplateSnapshot, validateTemplateSnapshot } = require('../../../utils/dish-template-change');

const MEALS = [
  { value: 'BREAKFAST', label: '早餐' },
  { value: 'LUNCH', label: '午餐' },
  { value: 'DINNER', label: '晚餐' }
];
const CALC_TYPES = [
  { value: 'FIXED', label: '固定消耗' },
  { value: 'PER_PERSON', label: '按人数' },
  { value: 'NO_PURCHASE', label: '不进采购' }
];

function clone(value) { return JSON.parse(JSON.stringify(value)); }

function decorateForm(detail) {
  const snapshot = buildTemplateSnapshot(detail);
  return {
    ...snapshot,
    tasteTagsText: snapshot.tasteTags.join('，'),
    ingredients: snapshot.ingredients.map((item) => ({
      ...item,
      calcIndex: item.calcType === 'PER_PERSON' ? 1 : (item.calcType === 'NO_PURCHASE' ? 2 : 0),
      calcLabel: item.calcType === 'PER_PERSON' ? '按人数' : (item.calcType === 'NO_PURCHASE' ? '不进采购' : '固定消耗')
    }))
  };
}

Page({
  data: {
    phase: 'loading', errorMessage: '', saving: false, uploading: false,
    form: null, displayImageUrl: '', submitNote: '', categories: [], categoryNames: [], categoryIndex: 0,
    meals: MEALS, calcTypes: CALC_TYPES
  },
  onLoad(options) {
    this.templateId = Number(options.id);
    this.dirtyForm = createDirtyForm(wx);
    this.load();
  },
  onUnload() { if (this.dirtyForm) this.dirtyForm.dispose(); },
  markDirty() { if (this.dirtyForm) this.dirtyForm.markDirty(); },
  async load() {
    if (!requireSession({ merchantOnly: true })) return;
    if (!this.templateId) { this.setData({ phase: 'error', errorMessage: '缺少模板菜品编号' }); return; }
    this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const runtime = createApiRuntime();
      const [detail, categories] = await Promise.all([
        runtime.merchant.getDishTemplateDetail(this.templateId),
        runtime.merchant.getDishTemplateCategories()
      ]);
      const categoryIndex = Math.max(0, categories.findIndex((item) => Number(item.categoryId) === Number(detail.categoryId)));
      this.setData({
        form: decorateForm(detail), displayImageUrl: toImageUrl(runtime.baseUrl, detail.imageUrl),
        categories, categoryNames: categories.map((item) => item.name),
        categoryIndex,
        meals: MEALS.map((item) => ({ ...item, selected: detail.mealTags && detail.mealTags.includes(item.value) })),
        phase: 'ready'
      });
      if (this.dirtyForm) this.dirtyForm.markClean();
    } catch (error) {
      this.setData({ phase: 'error', errorMessage: resolveApiErrorMessage(error, '模板信息加载失败') });
    }
  },
  retryLoad() { return this.load(); },
  bindField(event) {
    if (this.data.saving || this.data.uploading) return;
    this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail.value });
    this.markDirty();
  },
  bindSubmitNote(event) { this.setData({ submitNote: event.detail.value }); this.markDirty(); },
  bindCategory(event) {
    const categoryIndex = Number(event.detail.value || 0);
    const category = this.data.categories[categoryIndex];
    this.setData({ categoryIndex, 'form.categoryId': category ? category.categoryId : null });
    this.markDirty();
  },
  toggleMeal(event) {
    const value = event.currentTarget.dataset.value;
    const current = this.data.form.mealTags || [];
    const mealTags = current.includes(value) ? current.filter((item) => item !== value) : [...current, value];
    this.setData({ 'form.mealTags': mealTags, meals: MEALS.map((item) => ({ ...item, selected: mealTags.includes(item.value) })) });
    this.markDirty();
  },
  toggleEnabled() {
    this.setData({ 'form.enabled': !this.data.form.enabled });
    this.markDirty();
  },
  bindIngredientField(event) {
    const index = Number(event.currentTarget.dataset.index);
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.ingredients[${index}].${field}`]: event.detail.value });
    this.markDirty();
  },
  bindIngredientCalc(event) {
    const index = Number(event.currentTarget.dataset.index);
    const calcIndex = Number(event.detail.value || 0);
    this.setData({
      [`form.ingredients[${index}].calcType`]: CALC_TYPES[calcIndex].value,
      [`form.ingredients[${index}].calcIndex`]: calcIndex,
      [`form.ingredients[${index}].calcLabel`]: CALC_TYPES[calcIndex].label
    });
    this.markDirty();
  },
  addIngredient() {
    const ingredients = clone(this.data.form.ingredients || []);
    ingredients.push({ ingredientName: '', ingredientCategory: '', quantity: '', unit: '克', calcType: 'FIXED', calcIndex: 0, calcLabel: '固定消耗', sortOrder: ingredients.length + 1 });
    this.setData({ 'form.ingredients': ingredients });
    this.markDirty();
  },
  removeIngredient(event) {
    if ((this.data.form.ingredients || []).length <= 1) {
      wx.showToast({ title: '至少保留 1 项食材', icon: 'none' });
      return;
    }
    const ingredients = clone(this.data.form.ingredients);
    ingredients.splice(Number(event.currentTarget.dataset.index), 1);
    ingredients.forEach((item, index) => { item.sortOrder = index + 1; });
    this.setData({ 'form.ingredients': ingredients });
    this.markDirty();
  },
  async chooseImage() {
    if (this.data.uploading || this.data.saving) return;
    const applyImage = async (file) => {
      if (!file || !file.tempFilePath) return;
      if (Number(file.size || 0) > 4 * 1024 * 1024) { wx.showToast({ title: '图片不能超过 4MB', icon: 'none' }); return; }
      this.setData({ uploading: true });
      try {
        const uploaded = await createApiRuntime().files.uploadImage(file.tempFilePath);
        this.setData({
          'form.imageUrl': uploaded.url || '',
          displayImageUrl: uploaded.imageUrl || toImageUrl(createApiRuntime().baseUrl, uploaded.url)
        });
        this.markDirty();
      } catch (error) { showApiError(error, '图片上传失败'); }
      finally { this.setData({ uploading: false }); }
    };
    if (wx.chooseMedia) {
      wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'], success: (result) => applyImage(result.tempFiles && result.tempFiles[0]) });
      return;
    }
    wx.chooseImage({ count: 1, sourceType: ['album', 'camera'], success: (result) => applyImage({ tempFilePath: result.tempFilePaths && result.tempFilePaths[0], size: 0 }) });
  },
  async submit() {
    if (this.data.saving || this.data.uploading || !this.data.form) return;
    const source = { ...clone(this.data.form), tasteTags: this.data.form.tasteTagsText };
    const targetSnapshot = buildTemplateSnapshot(source);
    const validation = validateTemplateSnapshot(targetSnapshot);
    if (validation) { wx.showToast({ title: validation, icon: 'none' }); return; }
    this.setData({ saving: true });
    try {
      const result = await createApiRuntime().merchant.submitDishTemplateChange(this.templateId, {
        submitNote: String(this.data.submitNote || '').trim(), targetSnapshot
      });
      if (this.dirtyForm) this.dirtyForm.markClean();
      await wx.showModal({ title: '申请已提交', content: `申请 #${result.requestId} 已进入平台审核，审核前不会修改系统模板。`, showCancel: false });
      wx.redirectTo({ url: `/pages/merchant/dish-template-change-detail/index?id=${result.requestId}` });
    } catch (error) { showApiError(error, '模板修改申请提交失败'); }
    finally { this.setData({ saving: false }); }
  }
});

module.exports = { decorateForm };
