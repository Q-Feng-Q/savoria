const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');
const { recordWithinGrant } = require('../../../../utils/notebook-permissions');

function display(field, value) {
  if (field.type === 'IMAGE') return `${Array.isArray(value) ? value.length : 0} 张私有图片`;
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (Array.isArray(value)) return value.join('、');
  return value == null || value === '' ? '未填写' : String(value);
}

Page({
  identityLoad: createIdentityLoadGuard(),
  data: { phase: 'loading', id: null, record: null, fields: [], owner: false,
    canEdit: false, images: [], imageError: '', errorMessage: '' },
  onLoad(options) { this.setData({ id: Number(options && options.id) }); },
  onShow() { return this.load(); },
  async load() {
    const session = requireSession();
    if (!session || !this.data.id) return;
    const ticket = this.identityLoad.begin(session);
    this.setData({ phase: 'loading' });
    try {
      const notebook = createApiRuntime().notebook;
      const record = await notebook.getRecord(this.data.id);
      if (!this.identityLoad.isCurrent(ticket)) return;
      const owner = Number(session.userId) === record.ownerUserId;
      const [images, grants] = await Promise.all([
        notebook.listRecordImages(this.data.id).catch(() => []),
        owner ? Promise.resolve([]) : notebook.listShared().catch(() => [])
      ]);
      if (!this.identityLoad.isCurrent(ticket)) return;
      const canEdit = owner || (grants || []).some((grant) => grant.eventId === record.eventId
        && recordWithinGrant(grant, record));
      this.setData({ phase: 'ready', record, owner, canEdit, images,
        imageError: (record.fields || []).some((field) => field.type === 'IMAGE'
          && ((record.values || {})[field.key] || []).length)
          && !images.length ? '图片暂不可预览，请稍后重试' : '',
        fields: (record.fields || []).map((field) => ({ ...field,
          display: display(field, (record.values || {})[field.key]),
          imageItems: field.type === 'IMAGE' ? images.filter((image) =>
            ((record.values || {})[field.key] || []).includes(image.valueKey)) : [] })) });
    } catch (error) {
      if (!this.identityLoad.isCurrent(ticket)) return;
      this.setData({ phase: 'error', errorMessage: error.message || '记录加载失败' });
      showApiError(error, '记录加载失败');
    }
  },
  async openImage(event) {
    const imageId = Number(event.currentTarget.dataset.id);
    if (!imageId) return;
    try {
      const file = await createApiRuntime().notebook.previewImage(imageId);
      wx.previewImage({ current: file, urls: [file] });
    } catch (error) { showApiError(error, '图片读取失败或权限已失效'); }
  },
  edit() { wx.navigateTo({ url: `/pages/notebook/detail/record-edit/index?id=${this.data.id}` }); },
  async deleteRecord() {
    if (!this.data.owner) return;
    const answer = await wx.showModal({ title: '删除记录', content: '记录及其私有图片将删除，无法恢复。',
      confirmText: '删除', confirmColor: '#b84d3a' });
    if (!answer.confirm) return;
    try { await createApiRuntime().notebook.deleteRecord(this.data.id); wx.navigateBack(); }
    catch (error) { showApiError(error, '删除记录失败'); }
  }
});
