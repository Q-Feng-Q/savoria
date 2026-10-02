const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMerchantIngredientsScene } = require('../../../utils/merchant-scenes');
const { requireSession, showApiError, resolveApiErrorMessage } = require('../../../utils/page-api');
const { createDirtyForm } = require('../../../utils/dirty-form');

Page({
  data: {
    phase: 'loading',
    errorMessage: '',
    editId: '',
    title: '新增食材',
    saving: false,
    referenceText: '',
    form: { name: '', unit: '个', category: '蔬菜' }
  },

  onLoad(options = {}) {
    this.dirtyForm = createDirtyForm(wx);
    const title = options.id ? '编辑食材' : '新增食材';
    this.setData({ editId: options.id || '', title });
    if (typeof wx.setNavigationBarTitle === 'function') wx.setNavigationBarTitle({ title });
  },

  onShow() {
    if (!this._loaded) return this.load();
  },

  onUnload() {
    if (this.dirtyForm) this.dirtyForm.dispose();
  },

  async load() {
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    this.setData({ phase: 'loading', errorMessage: '' });
    if (!this.data.editId) {
      this._loaded = true;
      this.setData({ phase: 'ready' });
      return;
    }
    try {
      const runtime = createApiRuntime();
      const ingredients = await runtime.merchant.getIngredients();
      if (generation !== this._loadGeneration) return;
      const rows = buildApiMerchantIngredientsScene({ session, ingredients }).ingredients;
      const ingredient = rows.find((item) => String(item.id) === String(this.data.editId));
      if (!ingredient) throw new Error('食材不存在或已被删除');
      this._loaded = true;
      this.setData({
        form: { name: ingredient.name, unit: ingredient.unit, category: ingredient.category },
        referenceText: ingredient.usedByDishCount ? `${ingredient.usedByDishCount} 道菜正在引用：${ingredient.usedByDishText}` : '',
        phase: 'ready'
      });
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ phase: 'error', errorMessage: resolveApiErrorMessage(error, '食材信息加载失败') });
    }
  },

  retryLoad() { return this.load(); },

  bindField(event) {
    if (this.data.saving || this.data.phase !== 'ready') return;
    const field = event.currentTarget.dataset.field;
    if (!['name', 'unit', 'category'].includes(field)) return;
    this.setData({ [`form.${field}`]: event.detail.value });
    if (this.dirtyForm) this.dirtyForm.markDirty();
  },

  async saveIngredient() {
    if (this.data.saving || this.data.phase !== 'ready') return;
    const editId = this.data.editId;
    const form = { ...this.data.form };
    const payload = {
      name: String(form.name || '').trim(),
      unit: String(form.unit || '').trim(),
      category: String(form.category || '').trim()
    };
    if (!payload.name || !payload.unit || !payload.category) {
      wx.showToast({ title: '请填写食材名称、单位和分类', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    try {
      const runtime = createApiRuntime();
      if (editId) await runtime.merchant.updateIngredient(editId, payload);
      else await runtime.merchant.createIngredient(payload);
      if (this.dirtyForm) this.dirtyForm.markClean();
      wx.showToast({ title: editId ? '已保存修改' : '已新增食材', icon: 'success' });
      wx.navigateBack({ delta: 1 });
    } catch (error) {
      showApiError(error, '保存食材失败');
    } finally {
      this.setData({ saving: false });
    }
  }
});
