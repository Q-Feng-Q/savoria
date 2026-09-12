const { createApiRuntime } = require('../../../utils/api-runtime');
const { createDirtyForm } = require('../../../utils/dirty-form');
const { requireSession, showApiError, resolveApiErrorMessage } = require('../../../utils/page-api');
const { buildTemplateSnapshot, validateTemplateSnapshot } = require('../../../utils/dish-template-change');

const MEALS = [
  { value: 'BREAKFAST', label: '早餐' },
  { value: 'LUNCH', label: '午餐' },
  { value: 'DINNER', label: '晚餐' }
];
const CALC_TYPES = [
  { value: 'FIXED', label: '固定消耗' },
  { value: 'PER_PERSON', label: '按人数' }
];
const QUANTITY_STATUSES = [
  { value: 'VERIFIED', label: '用量已核实' },
  { value: 'SOURCE_BATCH', label: '原配方批量' },
  { value: 'MISSING', label: '用量待完善' },
  { value: 'NOT_APPLICABLE', label: '无需采购' }
];

function clone(value) { return JSON.parse(JSON.stringify(value)); }

function decorateForm(detail) {
  const snapshot = buildTemplateSnapshot(detail);
  return {
    ...snapshot,
    tasteTagsText: snapshot.tasteTags.join('，'),
    ingredients: snapshot.ingredients.map((item) => ({
      ...item,
      quantityStatusIndex: Math.max(0, QUANTITY_STATUSES.findIndex((status) => status.value === item.quantityStatus)),
      quantityStatusLabel: (QUANTITY_STATUSES.find((status) => status.value === item.quantityStatus) || QUANTITY_STATUSES[0]).label,
      calcIndex: item.calcType === 'PER_PERSON' ? 1 : 0,
      calcLabel: item.calcType === 'PER_PERSON' ? '按人数' : '固定消耗'
    })),
    cookingSteps: snapshot.cookingSteps
  };
}

Page({
  data: {
    phase: 'loading', errorMessage: '', saving: false,
    form: null, submitNote: '', categories: [], categoryNames: [], categoryIndex: 0,
    meals: MEALS, calcTypes: CALC_TYPES, quantityStatuses: QUANTITY_STATUSES
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
        form: decorateForm(detail),
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
    if (this.data.saving) return;
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
  bindIngredientStatus(event) {
    const index = Number(event.currentTarget.dataset.index);
    const statusIndex = Number(event.detail.value || 0);
    const status = QUANTITY_STATUSES[statusIndex];
    const updates = {
      [`form.ingredients[${index}].quantityStatus`]: status.value,
      [`form.ingredients[${index}].quantityStatusIndex`]: statusIndex,
      [`form.ingredients[${index}].quantityStatusLabel`]: status.label
    };
    if (status.value !== 'VERIFIED') {
      updates[`form.ingredients[${index}].quantity`] = null;
      updates[`form.ingredients[${index}].unit`] = null;
      updates[`form.ingredients[${index}].calcType`] = null;
    } else {
      updates[`form.ingredients[${index}].quantity`] = '';
      updates[`form.ingredients[${index}].unit`] = 'g';
      updates[`form.ingredients[${index}].calcType`] = 'FIXED';
      updates[`form.ingredients[${index}].calcIndex`] = 0;
      updates[`form.ingredients[${index}].calcLabel`] = '固定消耗';
    }
    this.setData(updates);
    this.markDirty();
  },
  addIngredient() {
    const ingredients = clone(this.data.form.ingredients || []);
    ingredients.push({
      itemId: `client-ingredient:${Date.now()}:${ingredients.length + 1}`,
      ingredientName: '', ingredientCategory: '', quantityStatus: 'VERIFIED',
      quantityStatusIndex: 0, quantityStatusLabel: '用量已核实', quantity: '', unit: 'g',
      calcType: 'FIXED', calcIndex: 0, calcLabel: '固定消耗', sourceText: null,
      sourceQuantityText: null, componentTemplateId: null, componentMultiplier: null,
      sortOrder: ingredients.length + 1
    });
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
  bindStepField(event) {
    const index = Number(event.currentTarget.dataset.index);
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.cookingSteps[${index}].${field}`]: event.detail.value });
    this.markDirty();
  },
  addCookingStep() {
    const cookingSteps = clone(this.data.form.cookingSteps || []);
    cookingSteps.push({
      itemId: `client-step:${Date.now()}:${cookingSteps.length + 1}`,
      stepNo: cookingSteps.length + 1,
      title: null,
      content: '',
      durationSeconds: null,
      temperatureText: null,
      heatLevel: null,
      componentTemplateId: null
    });
    this.setData({ 'form.cookingSteps': cookingSteps });
    this.markDirty();
  },
  removeCookingStep(event) {
    const cookingSteps = clone(this.data.form.cookingSteps || []);
    cookingSteps.splice(Number(event.currentTarget.dataset.index), 1);
    cookingSteps.forEach((item, index) => { item.stepNo = index + 1; });
    this.setData({ 'form.cookingSteps': cookingSteps });
    this.markDirty();
  },
  async submit() {
    if (this.data.saving || !this.data.form) return;
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
