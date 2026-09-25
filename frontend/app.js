const { sessionStore } = require('./utils/session');

const CLOUD_ENV = 'prod-d5g0vleyp9ed264bf';
const CLOUD_SERVICE = 'springboot-6dl0';
const CLOUD_ASSET_BASE_URL = `https://${CLOUD_ENV}.service.tcloudbase.com`;

App({
  onLaunch() {
    if (wx.cloud && typeof wx.cloud.init === 'function') {
      wx.cloud.init({
        env: CLOUD_ENV,
        traceUser: true
      });
    }
  },

  globalData: {
    apiBaseUrl: CLOUD_ASSET_BASE_URL,
    cloudHosting: {
      enabled: true,
      env: CLOUD_ENV,
      service: CLOUD_SERVICE,
      assetBaseUrl: CLOUD_ASSET_BASE_URL
    },
    sessionStore
  }
});
