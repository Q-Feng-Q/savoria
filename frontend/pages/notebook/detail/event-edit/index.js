const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const template = require('../../../../utils/notebook-template');
const presets = require('../../../../utils/notebook-presets');
const { createDirtyForm } = require('../../../../utils/dirty-form');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');

const presetCatalog = presets.listPresets();
const presetCategories = ['全部', ...new Set(presetCatalog.map((item) => item.category))];

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'ready', editing: false, id: null, name: '', category: '', description: '',
    fields: [{ type: 'TEXT', typeIndex: 0, label: '', required: false, options: [], optionsText: '', unit: null }],
    typeLabels: template.FIELD_TYPES.map(template.fieldTypeLabel), errorMessage: '', busy: false,
    showPresetPicker: false, presetCount: presetCatalog.length, presetCategories, presetCategory: '全部',
    visiblePresets: presetCatalog, selectedPreset: null, selectedPresetFields: [] },
  onLoad(options) {
    this.form = createDirtyForm(wx);
    const session = requireSession();
    if (!session) return;
    this.boundUserId = session.userId;
    const id = Number(options && options.id);
    if (id > 0) { this.setData({ id, editing: true }); this.load(); }
  },
  onShow() {
    const session = requireSession();
    if (session && this.boundUserId !== undefined && session.userId !== this.boundUserId) {
      this.form.markClean();
      wx.showToast({ title: '账号已切换，请重新打开记事', icon: 'none' });
      wx.navigateBack();
    }
  },
  onUnload() { if (this.form) this.form.dispose(); },
  async load() {
    const session = requireSession();
    if (!session || session.userId !== this.boundUserId) return;
    const ticket = this.identityLoad.begin(session);
    this.setData({ phase: 'loading' });
    try {
      const notebook = createApiRuntime().notebook;
      const [event, versions] = await Promise.all([
        notebook.getEvent(this.data.id), notebook.listTemplates(this.data.id)
      ]);
      if (!this.identityLoad.isCurrent(ticket)) return;
      const latest = (versions || []).find((version) => version.version === event.currentTemplateVersion);
      this.originalFields = (latest && latest.fields) || [];
      this.setData({ phase: 'ready', name: event.name, category: event.category || '',
        description: event.description || '', fields: this.originalFields.map((field) => ({ ...field,
          typeIndex: template.FIELD_TYPES.indexOf(field.type), optionsText: (field.options || []).join('，') })) });
      this.form.markClean();
    } catch (error) { if (this.identityLoad.isCurrent(ticket)) {
      this.setData({ phase: 'error', errorMessage: error.message || '事件加载失败' });
      showApiError(error, '事件加载失败'); } }
  },
  onText(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ [key]: event.detail.value }); this.form.markDirty();
  },
  openPresetPicker() {
    if (this.data.editing) return;
    this.setData({ showPresetPicker: true });
  },
  closePresetPicker() { this.setData({ showPresetPicker: false }); },
  choosePresetCategory(event) {
    const category = event.currentTarget.dataset.category;
    if (!presetCategories.includes(category)) return;
    this.setData({ presetCategory: category, visiblePresets: category === '全部'
      ? presetCatalog : presetCatalog.filter((item) => item.category === category),
    selectedPreset: null, selectedPresetFields: [] });
  },
  selectPreset(event) {
    const preset = presets.getPreset(event.currentTarget.dataset.id);
    if (!preset) return;
    this.setData({ selectedPreset: preset, selectedPresetFields: preset.fields.map((field) => ({
      label: field.label, typeLabel: template.fieldTypeLabel(field.type), unit: field.unit,
      required: field.required, optionsText: (field.options || []).join('、') })) });
  },
  hasDraftContent() {
    const { name, category, description, fields } = this.data;
    const blankField = fields.length === 1 && fields[0].type === 'TEXT' && !fields[0].label &&
      !fields[0].required && !(fields[0].options || []).length && !fields[0].unit;
    return Boolean(String(name).trim() || String(category).trim() || String(description).trim() ||
      !blankField || this.form && this.form.isDirty());
  },
  async importPreset() {
    if (this.data.editing || !this.data.selectedPreset) return;
    const draft = presets.copyPresetToEvent(this.data.selectedPreset.id);
    if (!draft) return;
    if (this.hasDraftContent()) {
      const decision = await wx.showModal({ title: '替换当前内容？',
        content: '导入模板会替换尚未保存的事件名称、分类、说明和字段。',
        confirmText: '导入模板' });
      if (!decision.confirm) return;
    }
    this.setData({ name: draft.name, category: draft.category, description: draft.description,
      fields: draft.fields.map((field) => ({ ...field,
        typeIndex: template.FIELD_TYPES.indexOf(field.type), optionsText: field.options.join('，') })),
      showPresetPicker: false });
    this.form.markDirty();
  },
  fieldInput(event) {
    const index = Number(event.currentTarget.dataset.index);
    const key = event.currentTarget.dataset.key;
    const fields = this.data.fields.map((field) => ({ ...field }));
    fields[index][key] = event.detail.value;
    if (key === 'optionsText') fields[index].options = event.detail.value
      .split(/[,，\n]/).map((part) => part.trim()).filter(Boolean);
    this.setData({ fields }); this.form.markDirty();
  },
  fieldType(event) {
    const index = Number(event.currentTarget.dataset.index);
    const fields = this.data.fields.map((field) => ({ ...field }));
    fields[index].type = template.FIELD_TYPES[Number(event.detail.value)];
    fields[index].typeIndex = Number(event.detail.value);
    if (!['SINGLE_SELECT', 'MULTI_SELECT'].includes(fields[index].type)) {
      fields[index].options = [];
      fields[index].optionsText = '';
    }
    if (fields[index].type !== 'NUMBER') fields[index].unit = null;
    this.setData({ fields }); this.form.markDirty();
  },
  fieldRequired(event) {
    const index = Number(event.currentTarget.dataset.index);
    const fields = this.data.fields.map((field) => ({ ...field }));
    fields[index].required = Boolean(event.detail.value);
    this.setData({ fields }); this.form.markDirty();
  },
  addField() {
    this.setData({ fields: [...this.data.fields,
      { type: 'TEXT', typeIndex: 0, label: '', required: false, options: [], optionsText: '', unit: null }] });
    this.form.markDirty();
  },
  removeField(event) {
    const index = Number(event.currentTarget.dataset.index);
    this.setData({ fields: this.data.fields.filter((_, position) => position !== index) });
    this.form.markDirty();
  },
  moveField(event) {
    const index = Number(event.currentTarget.dataset.index);
    const next = index + Number(event.currentTarget.dataset.direction);
    if (index < 0 || next < 0 || next >= this.data.fields.length) return;
    const fields = this.data.fields.slice();
    [fields[index], fields[next]] = [fields[next], fields[index]];
    this.setData({ fields }); this.form.markDirty();
  },
  async save() {
    const session = requireSession();
    if (this.data.busy || !session || session.userId !== this.boundUserId) return;
    const ticket = this.identityLoad.begin(session);
    if (!String(this.data.name).trim()) return wx.showToast({ title: '请填写事件名称', icon: 'none' });
    const result = template.validateFields(this.data.fields);
    if (!result.valid) return wx.showToast({ title: result.message, icon: 'none' });
    const fields = template.prepareFields(this.data.fields, this.originalFields || []);
    this.setData({ busy: true });
    try {
      const notebook = createApiRuntime().notebook;
      const meta = { name: this.data.name.trim(), category: this.data.category.trim(),
        description: this.data.description.trim() };
      if (this.data.editing) {
        const templateChanged = JSON.stringify(fields) !== JSON.stringify(
          template.prepareFields(this.originalFields || [], this.originalFields || []));
        if (templateChanged) {
          const decision = await wx.showModal({ title: '发布新模板版本',
            content: '旧记录仍使用原模板。新字段只影响之后创建或显式升级的记录。', confirmText: '发布' });
          if (!decision.confirm) return;
        }
        if (!this.identityLoad.isCurrent(ticket)) return;
        await notebook.updateEvent(this.data.id, meta);
        if (!this.identityLoad.isCurrent(ticket)) return;
        if (templateChanged) await notebook.publishTemplate(this.data.id, fields);
      } else if (this.identityLoad.isCurrent(ticket)) await notebook.createEvent({ ...meta, fields });
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.form.markClean(); wx.navigateBack();
    } catch (error) { showApiError(error, '保存事件失败'); }
    finally { this.setData({ busy: false }); }
  }
});
