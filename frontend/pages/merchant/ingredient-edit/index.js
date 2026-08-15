const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMerchantIngredientsScene } = require('../../../utils/merchant-scenes');
const { requireSession, showApiError, resolveApiErrorMessage } = require('../../../utils/page-api');
const { createDirtyForm } = require('../../../utils/dirty-form');

Page({
  data: {
    phase: 'loading',
    errorMessage: '',
    saving: false,
    busyIngredientId: '',
    summaryCards: [],
    ingredients: [],
    editId: '',
    context: null,
    form: {
      name: '',
      unit: '个',
      category: '蔬菜',
      remark: ''
    }
  },

  onLoad() {
    this.dirtyForm = createDirtyForm(wx);
  },

  onUnload() {
    if (this.dirtyForm) this.dirtyForm.dispose();
  },

  onShow() {
    this.load();
  },

  async load({ silent = false } = {}) {
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    if (!silent) this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const ingredients = await createApiRuntime().merchant.getIngredients();
      if (generation !== this._loadGeneration) return;
      const scene = buildApiMerchantIngredientsScene({ session, ingredients });
      this.setData({
        ...scene,
        ingredients: scene.ingredients || [],
        phase: (scene.ingredients || []).length ? 'ready' : 'empty'
      });
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ phase: 'error', errorMessage: resolveApiErrorMessage(error, '食材列表加载失败') });
    }
  },

  retryLoad() { return this.load(); },

  bindField(event) {
    if (this.data.saving || this.data.busyIngredientId) return;
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: event.detail.value });
    if (this.dirtyForm) this.dirtyForm.markDirty();
  },

  fillForm(event) {
    if (this.data.saving || this.data.busyIngredientId) return;
    const ingredient = this.data.ingredients.find((item) => Number(item.id) === Number(event.currentTarget.dataset.id));
    if (!ingredient) return;
    this.setData({
      editId: ingredient.id,
      form: {
        name: ingredient.name,
        unit: ingredient.unit,
        category: ingredient.category,
        remark: ingredient.remark || ''
      }
    });
  },

  resetForm() {
    if (this.data.saving || this.data.busyIngredientId) return;
    this.applyResetForm();
  },

  applyResetForm() {
    this.setData({
      editId: '',
      form: {
        name: '',
        unit: '个',
        category: '蔬菜',
        remark: ''
      }
    });
    if (this.dirtyForm) this.dirtyForm.markClean();
  },

  async saveIngredient() {
    if (this.data.saving || this.data.busyIngredientId) return;
    const editId = this.data.editId;
    const form = { ...this.data.form };
    const payload = { name: String(form.name || '').trim(), unit: String(form.unit || '').trim(), category: String(form.category || '').trim() };
    if (!payload.name || !payload.unit || !payload.category) {
      wx.showToast({ title: '请填写食材名称、单位和分类', icon: 'none' });
      return;
    }
    const requestFingerprint = JSON.stringify({ editId, form: this.data.form });

    try {
      this.setData({ saving: true });
      const runtime = createApiRuntime();
      if (editId) {
        await runtime.merchant.updateIngredient(editId, payload);
      } else {
        await runtime.merchant.createIngredient(payload);
      }
      wx.showToast({ title: editId ? '已保存修改' : '已新增食材', icon: 'success' });
      if (JSON.stringify({ editId: this.data.editId, form: this.data.form }) === requestFingerprint) {
        this.setData({ editId: '', form: { name: '', unit: '个', category: '蔬菜', remark: '' } });
        if (this.dirtyForm) this.dirtyForm.markClean();
        await this.load({ silent: true });
      }
    } catch (error) {
      showApiError(error, '保存食材失败');
    } finally {
      this.setData({ saving: false });
    }
  },

  async removeIngredient(event) {
    if (this.data.saving) return;
    const id = event.currentTarget.dataset.id;
    const ingredient = this.data.ingredients.find((item) => String(item.id) === String(id));
    if (!ingredient || !ingredient.removable) {
      wx.showToast({ title: '该食材已被菜品引用，暂不能删除', icon: 'none' });
      return;
    }
    if (this.data.busyIngredientId) return;
    this.setData({ busyIngredientId: id });
    try {
      await createApiRuntime().merchant.deleteIngredient(id);
      wx.showToast({ title: '已删除食材', icon: 'success' });
      if (String(this.data.editId) === String(id)) this.applyResetForm();
      await this.load({ silent: true });
    } catch (error) {
      showApiError(error, '删除食材失败');
    } finally {
      this.setData({ busyIngredientId: '' });
    }
  }
});
