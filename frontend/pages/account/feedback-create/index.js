const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { identityKey } = require('../../../utils/identity-load');
const { validateDraft, createSubmission } = require('../../../utils/feedback');
const { removeTempFile } = require('../../../services/feedback');
const callWx = (name, options) => new Promise((resolve, reject) => wx[name]({ ...options, success: resolve, fail: reject }));

Page({
  currentSession() { const store = this.runtime.sessionStore; return store.getSession(); },
  data: { type: 'BUG', content: '', images: [], imagesReady: true, submittedId: null, saving: false, choosing: false, uncertain: false },
  onLoad() { this.runtime = createApiRuntime(); this.submission = createSubmission(); this.alive = true; },
  onShow() {
    const session = requireSession();
    if (!session) { this.clearDraft(); return; }
    const key = identityKey(session);
    if (this.owner && this.owner !== key) this.clearDraft();
    this.owner = key;
  },
  isCurrent() { return this.alive && this.owner === identityKey(this.currentSession()); },
  clearDraft() {
    this.data.images.forEach((image) => removeTempFile(image.path));
    this.submission.clear();
    this.setData({ type: 'BUG', content: '', images: [], imagesReady: true, submittedId: null, saving: false, choosing: false, uncertain: false });
  },
  onUnload() { this.alive = false; this.clearDraft(); },
  locked() { return this.data.saving || this.data.uncertain || this.data.submittedId; },
  input(event) { if (!this.locked()) this.setData({ content: event.detail.value }); },
  selectType(event) { if (!this.locked()) this.setData({ type: event.currentTarget.dataset.type }); },
  async chooseImages() {
    if (this.locked() || this.data.choosing || this.data.images.length >= 6 || !this.isCurrent()) return;
    this.setData({ choosing: true });
    const owner = this.owner;
    const temporaryPaths = new Set();
    try {
      const result = await callWx('chooseMedia', { count: 6 - this.data.images.length, mediaType: ['image'], sizeType: ['compressed'], sourceType: ['album', 'camera'] });
      result.tempFiles.forEach((file) => temporaryPaths.add(file.tempFilePath));
      for (const file of result.tempFiles) {
        if (!this.isCurrent() || owner !== this.owner) return;
        let path = file.tempFilePath;
        const info = await callWx('getImageInfo', { src: path });
        if (!['jpeg', 'jpg', 'png'].includes(String(info.type).toLowerCase())) throw new Error('仅支持 JPEG 或 PNG 图片');
        if (info.width * info.height > 20000000) throw new Error('图片像素过大，请缩小后重试');
        if (file.size > 4 * 1024 * 1024) {
          const compressed = await callWx('compressImage', { src: path, quality: 70 });
          path = compressed.tempFilePath;
          temporaryPaths.add(path);
        }
        const stats = await new Promise((resolve, reject) => wx.getFileSystemManager().getFileInfo({ filePath: path, success: resolve, fail: reject }));
        if (stats.size > 4 * 1024 * 1024) { removeTempFile(path); throw new Error('单张图片不能超过 4MB'); }
        if (!this.isCurrent() || owner !== this.owner) { removeTempFile(path); return; }
        const image = { key: `${Date.now()}-${Math.random()}`, path, state: 'uploading', imageId: null };
        this.setData({ images: [...this.data.images, image], imagesReady: false });
        await this.upload(image.key);
      }
    } catch (error) {
      if (this.isCurrent() && owner === this.owner && !String(error.errMsg || '').includes('cancel')) showApiError(error, '选择图片失败');
    } finally {
      const current = this.isCurrent() && owner === this.owner;
      const retained = new Set(current ? this.data.images.map((image) => image.path) : []);
      temporaryPaths.forEach((path) => { if (!retained.has(path)) removeTempFile(path); });
      if (current) this.setData({ choosing: false });
    }
  },
  async upload(key) {
    const image = this.data.images.find((item) => item.key === key);
    if (!image || !this.isCurrent()) return;
    const owner = this.owner;
    this.updateImage(key, { state: 'uploading', error: '' });
    try {
      const result = await this.runtime.feedback.uploadImage(image.path);
      if (this.isCurrent() && owner === this.owner) this.updateImage(key, { state: 'ready', imageId: result.imageId });
    } catch (error) {
      if (this.isCurrent() && owner === this.owner) {
        this.updateImage(key, { state: 'failed', error: error.message || '图片上传失败，请重试' });
        showApiError(error, '图片上传失败，请重试');
      }
    }
  },
  updateImage(key, patch) {
    const images = this.data.images.map((image) => image.key === key ? { ...image, ...patch } : image);
    this.setData({ images, imagesReady: images.every((image) => image.state === 'ready') });
  },
  retryImage(event) {
    if (this.locked()) return;
    const key = event.currentTarget.dataset.key;
    const image = this.data.images.find((item) => item.key === key);
    if (image && image.state === 'failed') this.upload(key);
  },
  removeImage(event) {
    if (this.locked()) return;
    const key = event.currentTarget.dataset.key;
    const image = this.data.images.find((item) => item.key === key);
    if (image) removeTempFile(image.path);
    const images = this.data.images.filter((item) => item.key !== key);
    this.setData({ images, imagesReady: images.every((item) => item.state === 'ready') });
  },
  preview(event) { if (this.isCurrent()) wx.previewImage({ current: event.currentTarget.dataset.path, urls: this.data.images.map((image) => image.path) }); },
  async submit() {
    if (this.data.saving || this.data.choosing || !this.isCurrent()) return;
    if (this.data.submittedId) { wx.redirectTo({ url: `/pages/account/feedback-detail/index?id=${this.data.submittedId}` }); return; }
    const message = validateDraft(this.data);
    if (message) { wx.showToast({ title: message, icon: 'none' }); return; }
    const payload = this.submission.prepare(this.data);
    const owner = this.owner;
    this.setData({ saving: true });
    try {
      const result = await this.runtime.feedback.submit(payload);
      if (!this.isCurrent() || owner !== this.owner) return;
      this.submission.clear();
      this.setData({ uncertain: false, submittedId: result.feedbackId });
      wx.redirectTo({ url: `/pages/account/feedback-detail/index?id=${result.feedbackId}` });
    } catch (error) {
      if (!this.isCurrent() || owner !== this.owner) return;
      const definitive = [400, 413, 422, 429].includes(error.statusCode);
      if (definitive) this.submission.clear();
      this.setData({ uncertain: !definitive });
      showApiError(error, '提交未确认，请重试');
    } finally { if (this.isCurrent() && owner === this.owner) this.setData({ saving: false }); }
  }
});
