const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createDirtyForm } = require('../../../../utils/dirty-form');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');
const { recordWithinGrant } = require('../../../../utils/notebook-permissions');
const { validateValues, normalizeValues, editorFor } = require('../../../../utils/notebook-template');
const calendar = require('../../../../utils/notebook-calendar');

const localDate = (value) => calendar.dateKey(new Date(value));
const localTime = (value) => { const date = new Date(value); return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`; };
const instant = (day, time) => new Date(`${day}T${time}:00`).toISOString();

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'loading', eventId: null, recordId: null, title: '', note: '',
    occurredDate: '', startTime: '12:00', endDate: '', endTime: '12:00',
    fields: [], values: {}, imageRefs: {}, templateVersion: 1, currentVersion: 1,
    upgradeMode: false, busy: false, errorMessage: '' },
  onLoad(options) {
    this.form = createDirtyForm(wx);
    const session = requireSession();
    if (!session) return;
    this.boundUserId = session.userId;
    this.setData({ eventId: Number(options && options.eventId) || null,
      recordId: Number(options && options.id) || null,
      occurredDate: (options && options.date) || calendar.dateKey(new Date()),
      endDate: (options && options.date) || calendar.dateKey(new Date()) });
    this.load();
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
    this.setData({ phase: 'loading', errorMessage: '' });
    try {
      const notebook = createApiRuntime().notebook;
      const record = this.data.recordId ? await notebook.getRecord(this.data.recordId) : null;
      if (!this.identityLoad.isCurrent(ticket)) return;
      const eventId = record ? record.eventId : this.data.eventId;
      if (record && Number(session.userId) !== record.ownerUserId) {
        const grants = await notebook.listShared();
        if (!this.identityLoad.isCurrent(ticket)) return;
        if (!(grants || []).some((grant) => grant.eventId === eventId
            && recordWithinGrant(grant, record))) throw new Error('当前只有查看权限');
      }
      const form = await notebook.getRecordTemplate(eventId).catch((error) => {
        if (!record) throw error;
        return null;
      });
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.currentFields = form ? form.fields : record.fields;
      const fields = record ? record.fields : this.currentFields;
      const from = record && record.occurredFrom;
      const to = record && record.occurredTo;
      const values = record ? record.values || {} : {};
      this.lockVersion = record && record.lockVersion;
      this.originalMetadata = record ? { title: record.title, note: record.note || '',
        occurredFrom: instant(localDate(record.occurredFrom), localTime(record.occurredFrom)),
        occurredTo: instant(localDate(record.occurredTo), localTime(record.occurredTo)) } : null;
      const imageList = record ? await notebook.listRecordImages(record.id).catch(() => []) : [];
      if (!this.identityLoad.isCurrent(ticket)) return;
      const imageRefs = Object.fromEntries(imageList.map((image) => [image.valueKey, image.imageId]));
      this.setData({ phase: 'ready', eventId, fields: this.decorate(fields, values),
        currentVersion: form ? form.templateVersion : record.templateVersion,
        templateVersion: record ? record.templateVersion : form.templateVersion,
        title: record ? record.title : '', note: record && record.note || '',
        values, imageRefs,
        occurredDate: from ? localDate(from) : this.data.occurredDate,
        startTime: from ? localTime(from) : this.data.startTime,
        endDate: to ? localDate(to) : this.data.endDate,
        endTime: to ? localTime(to) : this.data.endTime,
        upgradeMode: false });
      this.form.markClean();
    } catch (error) { if (this.identityLoad.isCurrent(ticket)) {
      this.setData({ phase: 'error', errorMessage: error.message || '记录加载失败' });
      showApiError(error, '记录加载失败'); } }
  },
  decorate(fields, values = {}) {
    return (fields || []).map((field) => ({ ...field, editor: editorFor(field.type),
      displayValue: values[field.key] == null ? '' : values[field.key],
      datetimeDate: values[field.key] ? localDate(values[field.key]) : '',
      datetimeTime: values[field.key] ? localTime(values[field.key]) : '',
      optionsView: (field.options || []).map((label) => ({ label,
        checked: (values[field.key] || []).includes(label) })),
      imageKeys: values[field.key] || [],
      choiceIndex: Math.max(0, (field.options || []).indexOf(values[field.key])),
      ratingIndex: Math.max(0, Number(values[field.key] || 1) - 1) }));
  },
  onText(event) { this.setData({ [event.currentTarget.dataset.key]: event.detail.value }); this.form.markDirty(); },
  onDate(event) { this.setData({ [event.currentTarget.dataset.key]: event.detail.value }); this.form.markDirty(); },
  onFieldDatetime(event) {
    const { key, part } = event.currentTarget.dataset;
    const prior = this.data.values[key];
    const date = part === 'date' ? event.detail.value
      : prior ? localDate(prior) : calendar.dateKey(new Date());
    const time = part === 'time' ? event.detail.value : prior ? localTime(prior) : '12:00';
    const values = { ...this.data.values, [key]: instant(date, time) };
    this.setData({ values, fields: this.decorate(this.data.fields, values) });
    this.form.markDirty();
  },
  onField(event) {
    const key = event.currentTarget.dataset.key;
    const field = this.data.fields.find((item) => item.key === key);
    let value = event.detail.value;
    if (field.type === 'SINGLE_SELECT') value = field.options[Number(value)];
    if (field.type === 'RATING') value = Number(value) + 1;
    const values = { ...this.data.values, [key]: value };
    this.setData({ values, fields: this.decorate(this.data.fields, values) }); this.form.markDirty();
  },
  async addImage(event) {
    const key = event.currentTarget.dataset.key;
    const session = requireSession();
    if (!session || session.userId !== this.boundUserId) return;
    const ticket = this.identityLoad.begin(session);
    try {
      const selected = await wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'] });
      const filePath = selected.tempFiles && selected.tempFiles[0] && selected.tempFiles[0].tempFilePath;
      if (!filePath) return;
      const uploaded = await createApiRuntime().notebook.uploadImage({ eventId: this.data.eventId,
        recordId: this.data.recordId, filePath });
      if (!this.identityLoad.isCurrent(ticket)) return;
      const current = this.data.values[key] || [];
      const values = { ...this.data.values, [key]: [...current, uploaded.valueKey] };
      this.setData({ values, fields: this.decorate(this.data.fields, values),
        imageRefs: { ...this.data.imageRefs, [uploaded.valueKey]: uploaded.imageId } });
      this.form.markDirty();
    } catch (error) { showApiError(error, '图片上传失败'); }
  },
  removeImage(event) {
    const key = event.currentTarget.dataset.key;
    const valueKey = event.currentTarget.dataset.value;
    const values = { ...this.data.values,
      [key]: (this.data.values[key] || []).filter((value) => value !== valueKey) };
    this.setData({ values, fields: this.decorate(this.data.fields, values) });
    this.form.markDirty();
  },
  async previewImage(event) {
    const valueKey = event.currentTarget.dataset.value;
    const imageId = this.data.imageRefs[valueKey];
    if (!imageId) return wx.showToast({ title: '图片暂不可预览', icon: 'none' });
    try {
      const file = await createApiRuntime().notebook.previewImage(imageId);
      wx.previewImage({ current: file, urls: [file] });
    } catch (error) { showApiError(error, '图片读取失败或权限已失效'); }
  },
  beginUpgrade() {
    if (!this.data.recordId || this.data.currentVersion === this.data.templateVersion) return;
    this.beforeUpgrade = { fields: this.data.fields, values: this.data.values };
    const values = {};
    this.currentFields.forEach((field) => { if (this.data.values[field.key] !== undefined) values[field.key] = this.data.values[field.key]; });
    this.setData({ fields: this.decorate(this.currentFields, values), values, upgradeMode: true });
    this.form.markDirty();
  },
  cancelUpgrade() {
    if (!this.beforeUpgrade) return;
    this.setData({ fields: this.beforeUpgrade.fields, values: this.beforeUpgrade.values,
      upgradeMode: false });
    this.beforeUpgrade = null;
  },
  async save() {
    const session = requireSession();
    if (this.data.busy || !session || session.userId !== this.boundUserId) return;
    const ticket = this.identityLoad.begin(session);
    if (!String(this.data.title).trim()) return wx.showToast({ title: '请填写标题', icon: 'none' });
    const checked = validateValues(this.data.fields, this.data.values);
    if (!checked.valid) return wx.showToast({ title: checked.message, icon: 'none' });
    let occurredFrom, occurredTo;
    try { occurredFrom = instant(this.data.occurredDate, this.data.startTime);
      occurredTo = instant(this.data.endDate, this.data.endTime); }
    catch (_) { return wx.showToast({ title: '请检查发生时间', icon: 'none' }); }
    if (occurredFrom > occurredTo) return wx.showToast({ title: '结束时间不能早于开始时间', icon: 'none' });
    if (this.data.upgradeMode && this.originalMetadata &&
        (this.data.title.trim() !== this.originalMetadata.title ||
          this.data.note !== this.originalMetadata.note ||
          occurredFrom !== this.originalMetadata.occurredFrom ||
          occurredTo !== this.originalMetadata.occurredTo)) {
      return wx.showToast({ title: '请先单独保存标题、时间或备注，再升级模板', icon: 'none' });
    }
    const values = normalizeValues(this.data.fields, this.data.values);
    this.setData({ busy: true });
    try {
      const notebook = createApiRuntime().notebook;
      if (this.data.upgradeMode) {
        const decision = await wx.showModal({ title: '升级此记录的模板？',
          content: '升级前内容会保留为修订快照；其他记录不受影响。', confirmText: '升级' });
        if (!decision.confirm) return;
        if (!this.identityLoad.isCurrent(ticket)) return;
        await notebook.upgradeRecord(this.data.recordId, values, this.lockVersion);
      } else if (this.data.recordId) {
        if (!this.identityLoad.isCurrent(ticket)) return;
        await notebook.updateRecord(this.data.recordId, { expectedVersion: this.lockVersion,
          occurredFrom, occurredTo, title: this.data.title.trim(), note: this.data.note, values });
      } else if (this.identityLoad.isCurrent(ticket)) await notebook.createRecord(this.data.eventId, {
        occurredFrom, occurredTo, title: this.data.title.trim(), note: this.data.note, values });
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.form.markClean(); wx.navigateBack();
    } catch (error) { showApiError(error, '保存记录失败，请检查是否有版本冲突'); }
    finally { this.setData({ busy: false }); }
  }
});
