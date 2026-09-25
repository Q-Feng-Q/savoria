const { createApiRuntime } = require('../../utils/api-runtime');
const { sessionStore } = require('../../utils/session');
const { identityKey } = require('../../utils/identity-load');
const { toImageUrl } = require('../../utils/image-url');
const { uploadStepImages, MAX_STEP_IMAGES } = require('../../utils/step-images');

Component({
  properties: { images: { type: Array, value: [] }, editable: Boolean, disabled: Boolean },
  data: { previews: [], busy: false },
  observers: { images() { if (this._attached) this.syncImages(); } },
  lifetimes: {
    attached() { this._attached = true; this.syncImages(); },
    detached() { this._attached = false; }
  },
  methods: {
    syncImages() {
      const base = createApiRuntime().baseUrl;
      this.setData({ previews: (this.properties.images || []).map(url => toImageUrl(base, url)) });
    },
    preview(event) {
      const urls = this.data.previews;
      if (urls.length) wx.previewImage({ urls, current: urls[Number(event.currentTarget.dataset.index)] });
    },
    remove(event) {
      if (!this.properties.editable || this.properties.disabled || this.data.busy) return;
      const images = [...(this.properties.images || [])];
      images.splice(Number(event.currentTarget.dataset.index), 1);
      this.triggerEvent('change', { images });
    },
    async choose() {
      if (!this.properties.editable || this.properties.disabled || this.data.busy) return;
      const existing = [...(this.properties.images || [])];
      const count = MAX_STEP_IMAGES - existing.length;
      if (count <= 0) return;
      const identity = identityKey(sessionStore.getSession());
      const isCurrent = () => this._attached && identity === identityKey(sessionStore.getSession());
      this.setData({ busy: true });
      this.triggerEvent('busy', { busy: true });
      try {
        const files = await new Promise((resolve, reject) => {
          if (wx.chooseMedia) wx.chooseMedia({ count, mediaType: ['image'], sourceType: ['album', 'camera'], success: r => resolve(r.tempFiles || []), fail: reject });
          else wx.chooseImage({ count, sourceType: ['album', 'camera'], success: r => resolve((r.tempFiles || []).map(f => ({tempFilePath:f.path,size:f.size}))), fail: reject });
        });
        if (!isCurrent()) return;
        const result = await uploadStepImages({ existing, files, isCurrent, upload: path => createApiRuntime().files.uploadImage(path) });
        if (!result.stale && isCurrent()) {
          this.triggerEvent('change', { images: result.images });
          if (result.failed) wx.showToast({ title: `${result.failed} 张上传失败，请重试（单张≤4MB）`, icon: 'none' });
        }
      } catch (error) {
        if (isCurrent() && !/cancel/i.test(String(error.errMsg || error.message))) wx.showToast({ title: error.message || '选择图片失败', icon: 'none' });
      } finally {
        if (this._attached) { this.setData({ busy: false }); this.triggerEvent('busy', { busy: false }); }
      }
    }
  }
});
