function trimTrailingSlash(value) {
  return String(value || '').replace(/\/+$/, '');
}

function normalizeContainerPath(url, assetBaseUrl = '') {
  const value = String(url || '').trim();
  const baseUrl = trimTrailingSlash(assetBaseUrl);
  const withoutOrigin = baseUrl && value.startsWith(baseUrl) ? value.slice(baseUrl.length) : value;
  if (!withoutOrigin) return '/';
  if (/^https?:\/\//i.test(withoutOrigin)) {
    const match = withoutOrigin.match(/^https?:\/\/[^/]+(\/.*)?$/i);
    return (match && match[1]) || '/';
  }
  return withoutOrigin.startsWith('/') ? withoutOrigin : `/${withoutOrigin}`;
}

function resolveCloudHostingConfig(app) {
  const source = (app && app.globalData && app.globalData.cloudHosting) || {};
  const env = String(source.env || '').trim();
  const service = String(source.service || '').trim();
  const enabled = source.enabled === true && Boolean(env && service);
  return {
    enabled,
    env,
    service,
    assetBaseUrl: trimTrailingSlash(source.assetBaseUrl)
  };
}

function resolveCallContainer(callContainer) {
  if (typeof callContainer === 'function') return callContainer;
  return (options) => {
    if (typeof wx === 'undefined' || !wx.cloud || typeof wx.cloud.callContainer !== 'function') {
      return Promise.reject(new Error('当前微信基础库不支持云托管，请升级后重试'));
    }
    return wx.cloud.callContainer(options);
  };
}

function createCloudRequestAdapter({ env, service, assetBaseUrl = '', callContainer } = {}) {
  const invoke = resolveCallContainer(callContainer);
  return (options = {}) => invoke({
    config: { env },
    path: normalizeContainerPath(options.url, assetBaseUrl),
    method: options.method || 'GET',
    header: {
      ...(options.header || {}),
      'X-WX-SERVICE': service
    },
    data: options.data,
    dataType: options.dataType,
    responseType: options.responseType,
    timeout: options.timeout
  });
}

function resolveTransfer(method, transfer) {
  if (typeof transfer === 'function') return transfer;
  return (options) => new Promise((resolve, reject) => {
    if (typeof wx === 'undefined' || typeof wx[method] !== 'function') {
      reject(new Error(`当前微信基础库不支持 ${method}`));
      return;
    }
    wx[method]({ ...options, success: resolve, fail: reject });
  });
}

function createCloudTransferAdapter(method, { service, transfer } = {}) {
  const invoke = resolveTransfer(method, transfer);
  return (options = {}) => invoke({
    ...options,
    header: {
      ...(options.header || {}),
      'X-WX-SERVICE': service
    }
  });
}

module.exports = {
  createCloudRequestAdapter,
  createCloudTransferAdapter,
  normalizeContainerPath,
  resolveCloudHostingConfig
};
