const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError, resolveApiErrorMessage } = require('../../../utils/page-api');
const { createDirtyForm } = require('../../../utils/dirty-form');

const calculationTypes = [
  { value: 'FIXED', label: '固定消耗' },
  { value: 'PER_PERSON', label: '按份估算' },
  { value: 'NO_PURCHASE', label: '不进采购' }
];

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function validateDish(dish) {
  if (!String(dish.name || '').trim()) return '请填写菜品名称';
  if (dish.categoryId === null || dish.categoryId === undefined || dish.categoryId === '') return '请选择菜品分类';
  if (dish.basePrice === null || dish.basePrice === undefined || String(dish.basePrice).trim() === '') return '请填写菜品价格';
  const price = Number(dish.basePrice);
  if (!Number.isFinite(price) || price < 0) return '菜品价格必须是大于等于 0 的数字';
  for (const item of dish.ingredients || []) {
    const quantity = Number(item.quantity);
    const calcType = item.calcType || item.calculationType;
    if (!String(item.name || item.ingredientName || '').trim() || !String(item.unit || '').trim()
      || !Number.isFinite(quantity) || quantity < 0 || !['FIXED', 'PER_PERSON', 'NO_PURCHASE'].includes(calcType)) return '请检查原材料名称、单位、用量和计算方式';
  }
  if ((dish.cookingSteps || []).some((item) => !String(item.content || '').trim())) return '请填写制作步骤内容';
  return '';
}

function isPendingReviewResult(result) { return Boolean(result && (result.outcome === 'PENDING_REVIEW' || result.status === 'PENDING_REVIEW')); }
function saveSuccessMessage(result) { return isPendingReviewResult(result) ? '已提交审核' : '菜品已保存'; }

function buildDishPayload(dish) {
  return {
    name: String(dish.name || '').trim(), categoryId: dish.categoryId,
    description: String(dish.description || '').trim(), imageUrl: dish.imageUrl || '',
    basePrice: Number(dish.basePrice),
    ingredients: (dish.ingredients || []).map((item) => ({ ingredientName: String(item.name || item.ingredientName || '').trim(), quantity: Number(item.quantity), unit: String(item.unit || '').trim(), calcType: item.calcType || item.calculationType || 'FIXED' })),
    cookingSteps: (dish.cookingSteps || []).map((item, index) => ({ stepNo: index + 1, title: String(item.title || '').trim(), content: String(item.content || '').trim() })),
    status: dish.status || 'active'
  };
}

Page({
  data: {
    phase: 'loading',
    errorMessage: '',
    saving: false,
    uploading: false,
    id: '',
    dish: null,
    ingredients: [],
    dishCategories: [],
    categoryIds: [],
    categoryIndex: 0,
    ingredientIndex: 0,
    calculationTypes,
    calculationIndex: 0,
    newIngredientQuantity: '',
    stepTitle: '',
    stepContent: '',
    editTitle: '新增菜品',
    selectedCategoryLabel: '',
    selectedIngredientName: '',
    selectedCalculationLabel: calculationTypes[0].label,
    statusLabel: '',
    hasCookingSteps: false
    ,reviewEnabled: false
  },

  onLoad(query) {
    this.dirtyForm = createDirtyForm(wx);
    this.setData({ id: query.id || '' });
  },

  onUnload() {
    if (this.dirtyForm) this.dirtyForm.dispose();
  },

  markDirty() {
    if (this.dirtyForm) this.dirtyForm.markDirty();
    this._formDirty = true;
  },

  onShow() {
    if (this._hasLoaded && this._formDirty) return this.refreshIngredients();
    return this.load();
  },

  async refreshIngredients() {
    const generation = this._loadGeneration || 0;
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    try {
      const previous = this.data.ingredients[this.data.ingredientIndex];
      const ingredients = await createApiRuntime().merchant.getIngredients();
      if (generation !== this._loadGeneration) return;
      const preservedIndex = previous ? ingredients.findIndex((item) => Number(item.ingredientId) === Number(previous.ingredientId)) : -1;
      const ingredientIndex = preservedIndex >= 0 ? preservedIndex : 0;
      this.setData({ ingredients, ingredientIndex, selectedIngredientName: ingredients[ingredientIndex] ? ingredients[ingredientIndex].name : '' });
    } catch (error) { showApiError(error, '食材列表加载失败'); }
  },

  async load() {
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const runtime = createApiRuntime();
      const [dishCategories, ingredients, detail, settings] = await Promise.all([
        runtime.merchant.getDishCategories(),
        runtime.merchant.getIngredients(),
        this.data.id ? runtime.merchant.getDishDetail(this.data.id) : Promise.resolve(null),
        runtime.system && runtime.system.getPublicSettings ? runtime.system.getPublicSettings().catch(() => ({})) : Promise.resolve({})
      ]);
      if (generation !== this._loadGeneration) return;
      if (this.data.id && !detail) throw new Error('菜品不存在或未找到');

      const categoryIds = dishCategories.map((item) => item.categoryId);
      const selectedDish = detail ? {
        dishId: detail.dishId,
        name: detail.name || '',
        categoryId: detail.categoryId || (categoryIds[0] || null),
        description: detail.description || '',
        badge: '',
        imageUrl: detail.imageUrl || '',
        basePrice: detail.basePrice ?? detail.price ?? '',
        status: detail.status || 'active',
        ingredients: (detail.ingredients || []).map((item, index) => ({
          id: `${detail.dishId}-${index}`,
          ingredientId: item.ingredientId || null,
          name: item.ingredientName,
          quantity: item.quantity,
          unit: item.unit,
          calcType: item.calcType || 'FIXED',
          calculationType: item.calcType || 'FIXED'
        })),
        cookingSteps: (detail.cookingSteps || []).slice().sort((left, right) => Number(left.stepNo || 0) - Number(right.stepNo || 0))
      } : {
        name: '',
        categoryId: categoryIds[0] || null,
        description: '',
        badge: '',
        imageUrl: '',
        basePrice: '',
        status: 'active',
        ingredients: [],
        cookingSteps: []
      };
      const categoryIndex = Math.max(0, categoryIds.findIndex((item) => Number(item) === Number(selectedDish.categoryId)));

      this.setData({
        dish: selectedDish,
        ingredients,
        dishCategories: dishCategories.map((item) => item.name),
        categoryIds,
        categoryIndex,
        ingredientIndex: 0,
        calculationIndex: 0,
        selectedCategoryLabel: dishCategories[categoryIndex] ? dishCategories[categoryIndex].name : '',
        selectedIngredientName: ingredients[0] ? ingredients[0].name : '',
        selectedCalculationLabel: this.data.calculationTypes[0].label,
        reviewEnabled: Boolean(settings && settings.dishReviewEnabled),
        editTitle: detail ? '编辑菜品' : '新增菜品',
        statusLabel: String(selectedDish.status || '').toUpperCase() === 'ACTIVE' || selectedDish.status === 'active' ? '当前上架' : '当前下架',
        hasCookingSteps: Boolean((selectedDish.cookingSteps || []).length),
        phase: 'ready'
      });
      this._hasLoaded = true;
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ phase: 'error', errorMessage: resolveApiErrorMessage(error, '菜品编辑器加载失败') });
    }
  },

  retryLoad() { return this.load(); },

  isMutationBusy() { return this.data.saving || this.data.uploading; },

  bindField(event) {
    if (this.isMutationBusy()) return;
    const field = event.currentTarget.dataset.field;
    this.setData({ [`dish.${field}`]: event.detail.value });
    this.markDirty();
  },

  async chooseDishImage() {
    if (this.isMutationBusy()) return;
    const applyImage = async (file) => {
      if (!file || !file.tempFilePath) return;
      if (Number(file.size || 0) > 4 * 1024 * 1024) {
        wx.showToast({ title: '图片不能超过 4MB', icon: 'none' });
        return;
      }
      try {
        this.setData({ uploading: true });
        const uploaded = await createApiRuntime().files.uploadImage(file.tempFilePath);
        this.setData({ 'dish.imageUrl': uploaded.imageUrl || uploaded.url || '' });
        this.markDirty();
      } catch (error) {
        showApiError(error, '图片上传失败');
      } finally {
        this.setData({ uploading: false });
      }
    };

    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          await applyImage(res.tempFiles && res.tempFiles[0]);
        }
      });
      return;
    }

    wx.chooseImage({
      count: 1,
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const path = res.tempFilePaths && res.tempFilePaths[0];
        if (!path) return;
        await applyImage({ tempFilePath: path, size: 0 });
      }
    });
  },

  bindCategory(event) {
    if (this.isMutationBusy()) return;
    const categoryIndex = Number(event.detail.value || 0);
    this.setData({
      categoryIndex,
      selectedCategoryLabel: this.data.dishCategories[categoryIndex] || '',
      'dish.categoryId': this.data.categoryIds[categoryIndex] || null
    });
    this.markDirty();
  },

  bindIngredient(event) {
    if (this.isMutationBusy()) return;
    const ingredientIndex = Number(event.detail.value || 0);
    this.setData({
      ingredientIndex,
      selectedIngredientName: (this.data.ingredients[ingredientIndex] || {}).name || ''
    });
  },

  bindCalculation(event) {
    if (this.isMutationBusy()) return;
    const calculationIndex = Number(event.detail.value || 0);
    this.setData({
      calculationIndex,
      selectedCalculationLabel: this.data.calculationTypes[calculationIndex].label
    });
  },

  bindQuantity(event) {
    if (this.isMutationBusy()) return;
    this.setData({ newIngredientQuantity: event.detail.value });
  },

  bindStepTitle(event) {
    if (this.isMutationBusy()) return;
    this.setData({ stepTitle: event.detail.value });
  },

  bindStepContent(event) {
    if (this.isMutationBusy()) return;
    this.setData({ stepContent: event.detail.value });
  },

  addIngredient() {
    if (this.isMutationBusy()) return;
    const ingredient = this.data.ingredients[this.data.ingredientIndex];
    const calcType = this.data.calculationTypes[this.data.calculationIndex].value;
    const quantityText = String(this.data.newIngredientQuantity ?? '').trim();
    const quantity = Number(quantityText);
    if (!ingredient || !quantityText || !Number.isFinite(quantity) || quantity < 0) {
      wx.showToast({ title: '请填写原材料用量', icon: 'none' });
      return;
    }
    const next = clone(this.data.dish.ingredients || []);
    next.push({
      ingredientId: ingredient.ingredientId,
      name: ingredient.name,
      quantity,
      unit: ingredient.unit,
      calcType,
      calculationType: calcType
    });
    this.setData({ 'dish.ingredients': next, newIngredientQuantity: '' });
    this.markDirty();
  },

  removeIngredient(event) {
    if (this.isMutationBusy()) return;
    const next = clone(this.data.dish.ingredients || []);
    next.splice(Number(event.currentTarget.dataset.index || 0), 1);
    this.setData({ 'dish.ingredients': next });
    this.markDirty();
  },

  addCookingStep() {
    if (this.isMutationBusy()) return;
    const title = String(this.data.stepTitle || '').trim();
    const content = String(this.data.stepContent || '').trim();
    if (!content) {
      wx.showToast({ title: '请填写步骤内容', icon: 'none' });
      return;
    }
    const next = clone(this.data.dish.cookingSteps || []);
    next.push({
      stepNo: next.length + 1,
      title,
      content
    });
    this.markDirty();
    this.setData({
      'dish.cookingSteps': next,
      stepTitle: '',
      stepContent: '',
      hasCookingSteps: Boolean(next.length)
    });
  },

  removeCookingStep(event) {
    if (this.isMutationBusy()) return;
    const next = clone(this.data.dish.cookingSteps || []);
    next.splice(Number(event.currentTarget.dataset.index || 0), 1);
    next.forEach((item, index) => {
      item.stepNo = index + 1;
    });
    this.markDirty();
    this.setData({
      'dish.cookingSteps': next,
      hasCookingSteps: Boolean(next.length)
    });
  },

  toggleStatus() {
    if (this.isMutationBusy()) return;
    const status = String(this.data.dish.status || '').toUpperCase() === 'ACTIVE' || this.data.dish.status === 'active'
      ? 'inactive'
      : 'active';
    this.setData({
      'dish.status': status,
      statusLabel: status === 'active' ? '当前上架' : '当前下架'
    });
    this.markDirty();
  },

  openIngredientLibrary() {
    if (this.isMutationBusy()) return;
    wx.navigateTo({ url: '/pages/merchant/ingredient-edit/index' });
  },

  async saveDish() {
    if (this.data.saving || this.data.uploading || !this.data.dish) return;
    const dish = clone(this.data.dish);
    const validationMessage = validateDish(dish);
    if (validationMessage) { wx.showToast({ title: validationMessage, icon: 'none' }); return; }
    const payload = buildDishPayload(dish);
    const saveFingerprint = JSON.stringify(dish);

    try {
      this.setData({ saving: true });
      const runtime = createApiRuntime();
      let result;
      if (this.data.id) {
        result = await runtime.merchant.updateDish(this.data.id, payload);
      } else {
        result = await runtime.merchant.createDish(payload);
      }
      wx.showToast({ title: saveSuccessMessage(result), icon: 'success' });
      if (JSON.stringify(this.data.dish) === saveFingerprint) {
        if (this.dirtyForm) this.dirtyForm.markClean();
        this._formDirty = false;
        setTimeout(() => wx.navigateBack(), 250);
      }
    } catch (error) {
      showApiError(error, '保存菜品失败');
    } finally {
      this.setData({ saving: false });
    }
  }
});

module.exports = { validateDish, buildDishPayload, saveSuccessMessage, isPendingReviewResult };
