const { createApiRuntime } = require('../../../utils/api-runtime');
const { requireSession } = require('../../../utils/page-api');
const { identityKey } = require('../../../utils/identity-load');
const { presentFeedback } = require('../../../utils/feedback');
const { removeTempFile } = require('../../../services/feedback');
Page({
  currentSession() { const store = this.runtime.sessionStore; return store.getSession(); },
  data: { detail: null, images: [], loading: false, error: '' },
  onLoad(options) { this.id = options.id; this.runtime = createApiRuntime(); this._loadGeneration = 0; this.alive = true; },
  onShow() { const session = requireSession(); this.owner = identityKey(session); this.clearImages(); if (session) this.load(); },
  isCurrent(generation) { return this.alive && this._loadGeneration === generation && this.owner === identityKey(this.currentSession()); },
  clearImages() { this._loadGeneration += 1; this.data.images.forEach((image) => removeTempFile(image.path)); this.setData({ images: [], detail: null }); },
  onHide() { this.clearImages(); },
  onUnload() { this.alive = false; this.clearImages(); },
  async load() {
    this.clearImages();
    const generation = this._loadGeneration;
    this.setData({ loading: true, error: '' });
    try {
      const detail = await this.runtime.feedback.detail(this.id);
      if (!this.isCurrent(generation)) return;
      this.setData({ detail: presentFeedback(detail), images: (detail.images || []).map((image) => ({ ...image, path: '', failed: false })) });
      await Promise.all(this.data.images.map((image) => this.download(image.imageId, generation)));
    } catch (error) { if (this.isCurrent(generation)) this.setData({ error: error.message || '反馈详情读取失败' }); }
    finally { if (this.isCurrent(generation)) this.setData({ loading: false }); }
  },
  async download(id, generation) {
    if (!this.isCurrent(generation)) return;
    const existing = this.data.images.find((image) => image.imageId === id);
    if (!existing || existing.downloading) return;
    this.setData({ images: this.data.images.map((image) => image.imageId === id ? { ...image, downloading: true, failed: false } : image) });
    try {
      const path = await this.runtime.feedback.downloadImage(id);
      if (!this.isCurrent(generation)) { removeTempFile(path); return; }
      if (existing.path && existing.path !== path) removeTempFile(existing.path);
      this.setData({ images: this.data.images.map((image) => image.imageId === id ? { ...image, path, failed: false, downloading: false } : image) });
    } catch (_) { if (this.isCurrent(generation)) this.setData({ images: this.data.images.map((image) => image.imageId === id ? { ...image, failed: true, downloading: false } : image) }); }
  },
  retryImage(event) { this.download(event.currentTarget.dataset.id, this._loadGeneration); },
  preview(event) { if (this.isCurrent(this._loadGeneration)) wx.previewImage({ current: event.currentTarget.dataset.path, urls: this.data.images.filter((image) => image.path).map((image) => image.path) }); }
});
