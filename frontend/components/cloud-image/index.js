const { createCloudImageResolver } = require('../../utils/cloud-image');

let sharedResolver;

function getResolver() {
  if (sharedResolver) return sharedResolver;
  const app = typeof getApp === 'function' ? getApp() : null;
  const config = (app && app.globalData && app.globalData.cloudHosting) || {};
  if (typeof wx === 'undefined' || !wx.cloud || !wx.getFileSystemManager || !wx.env) return null;
  sharedResolver = createCloudImageResolver({
    env: config.env,
    service: config.service,
    assetBaseUrl: config.assetBaseUrl,
    callContainer: (options) => wx.cloud.callContainer(options),
    fileSystem: wx.getFileSystemManager(),
    userDataPath: wx.env.USER_DATA_PATH
  });
  return sharedResolver;
}

Component({
  properties: {
    src: { type: String, value: '', observer: 'onSourceChange' },
    mode: { type: String, value: 'aspectFill' },
    lazyLoad: { type: Boolean, value: true },
    fallback: { type: String, value: '/assets/brand/dish-placeholder.png' }
  },
  data: { resolvedSrc: '' },
  lifetimes: {
    attached() {
      this.onSourceChange(this.properties.src);
    }
  },
  methods: {
    async onSourceChange(source) {
      const token = (this.resolveToken || 0) + 1;
      this.resolveToken = token;
      const value = String(source || '').trim();
      if (!value) {
        this.setData({ resolvedSrc: '' });
        return;
      }
      const resolver = getResolver();
      if (!resolver) {
        this.setData({ resolvedSrc: value });
        return;
      }
      try {
        const resolvedSrc = await resolver(value);
        if (this.resolveToken === token) this.setData({ resolvedSrc });
      } catch (error) {
        if (this.resolveToken === token) this.setData({ resolvedSrc: this.properties.fallback });
        this.triggerEvent('error', { source: value, message: error.message || '图片加载失败' });
      }
    },
    onNativeLoad(event) {
      this.triggerEvent('load', event.detail);
    },
    onNativeError(event) {
      if (this.data.resolvedSrc !== this.properties.fallback) {
        this.setData({ resolvedSrc: this.properties.fallback });
      }
      this.triggerEvent('error', event.detail);
    }
  }
});
