const { createApiRuntime } = require('../../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../../utils/identity-load');
const { dateKey, monthBounds, timeZone } = require('../../../../utils/notebook-calendar');
const { prepareExport, writeExportFile } = require('../../../../utils/notebook-export');

const modal = (options) => new Promise((resolve) => wx.showModal({ ...options,
  success: (result) => resolve(Boolean(result.confirm)), fail: () => resolve(false) }));
const clipboard = (data) => new Promise((resolve, reject) => wx.setClipboardData({ data,
  success: resolve, fail: reject }));

Page({
  identityLoad: createIdentityLoadGuard(),
  payload: null,
  data: { phase: 'ready', eventId: null, from: '', to: '', timeZone: '',
    maxQueryMonths: 36, previewCount: null, busy: false, errorMessage: '' },
  onLoad(options) {
    const today = dateKey(new Date());
    const month = monthBounds(today.slice(0, 7));
    this.setData({ eventId: Number(options.eventId) || null, from: options.from || month.from,
      to: options.to || today, timeZone: timeZone() });
  },
  onShow() { this.loadConfig(); },
  onHide() { this.payload = null; this.identityLoad.begin(null); },
  onUnload() { this.payload = null; this.identityLoad.begin(null); },
  async loadConfig() {
    const session = requireSession();
    if (!session) return;
    const ticket = this.identityLoad.begin(session);
    try {
      const config = await createApiRuntime().notebook.getConfig();
      if (this.identityLoad.isCurrent(ticket)) {
        this.setData({ maxQueryMonths: config.maxQueryMonths || 36 });
        await this.preview(ticket);
      }
    } catch (error) { if (this.identityLoad.isCurrent(ticket)) showApiError(error, '导出设置加载失败'); }
  },
  changeDate(event) {
    this.setData({ [event.currentTarget.dataset.field]: event.detail.value, previewCount: null },
      () => this.preview());
  },
  range() {
    const { from, to, timeZone: zone, maxQueryMonths } = this.data;
    const touched = (Number(to.slice(0, 4)) - Number(from.slice(0, 4))) * 12
      + Number(to.slice(5, 7)) - Number(from.slice(5, 7)) + 1;
    if (from > to || touched < 1 || touched > maxQueryMonths) {
      throw new Error(`请选择不超过 ${maxQueryMonths} 个月的日期范围`);
    }
    return { from, to, timeZone: zone };
  },
  async preview(ticket = this.identityLoad.begin(requireSession())) {
    if (!this.data.eventId) return;
    try {
      const query = this.range();
      const notebook = createApiRuntime().notebook;
      let count = 0;
      for (let page = 0; ; page += 1) {
        const result = await notebook.listRecords(this.data.eventId, { ...query, page, size: 100 });
        if (!this.identityLoad.isCurrent(ticket)) return;
        count += (result.items || []).length;
        if (!result.hasMore) break;
      }
      this.setData({ previewCount: count, errorMessage: '' });
    } catch (error) {
      if (this.identityLoad.isCurrent(ticket)) this.setData({ previewCount: null,
        errorMessage: error.message || '预览失败' });
    }
  },
  async perform(mode) {
    if (this.data.busy || !this.data.eventId) return;
    let range;
    try { range = this.range(); }
    catch (error) { return wx.showToast({ title: error.message, icon: 'none' }); }
    const confirmed = await modal({ title: mode === 'COPY' ? '复制记事 JSON？' : '导出记事文件？',
      content: '导出后数据会离开私人空间。已经复制或分享的内容无法远程收回。' });
    if (!confirmed) return;
    const ticket = this.identityLoad.begin(requireSession());
    this.setData({ busy: true, errorMessage: '' });
    try {
      const notebook = createApiRuntime().notebook;
      this.payload = await notebook.exportEvent(this.data.eventId, range, mode);
      if (!this.identityLoad.isCurrent(ticket)) return;
      const prepared = prepareExport(this.payload);
      if (mode === 'COPY' && prepared.tooLargeForClipboard) {
        this.payload = null;
        const fallback = await modal({ title: '复制内容过大', content: '改为创建 JSON 文件？' });
        if (fallback && this.identityLoad.isCurrent(ticket)) await this.exportFile(notebook, range, ticket);
      } else if (mode === 'COPY') {
        await clipboard(prepared.text);
        wx.showToast({ title: '已复制', icon: 'success' });
      } else await this.saveAndShare(this.payload, ticket);
    } catch (error) {
      if (this.identityLoad.isCurrent(ticket)) {
        this.setData({ errorMessage: error.message || '导出失败' });
        showApiError(error, '导出失败');
      }
    } finally { this.payload = null; this.setData({ busy: false }); }
  },
  async exportFile(notebook, range, ticket) {
    this.payload = await notebook.exportEvent(this.data.eventId, range, 'FILE');
    if (this.identityLoad.isCurrent(ticket)) await this.saveAndShare(this.payload, ticket);
  },
  async saveAndShare(payload, ticket) {
    const filePath = await writeExportFile(payload, {
      fileSystem: wx.getFileSystemManager(), userDataPath: wx.env.USER_DATA_PATH });
    if (!this.identityLoad.isCurrent(ticket)) return;
    if (wx.shareFileMessage) wx.shareFileMessage({ filePath, fileName: '记事导出.json',
      fail: () => wx.showToast({ title: '文件已保存，可稍后分享', icon: 'none' }) });
    else wx.showToast({ title: 'JSON 文件已保存', icon: 'success' });
  },
  copy() { return this.perform('COPY'); },
  download() { return this.perform('FILE'); }
});
