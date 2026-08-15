(function (root, factory) {
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = factory();
  } else {
    root.KitchenAdminRuntimeConfig = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  const DEFAULT_API_PROXY_BASE_URL = '/api';
  const DEFAULT_EXTERNAL_CONFIG_PATH = 'public/backend.config.json';

  function trimTrailingSlash(value) {
    return String(value || '').replace(/\/+$/, '');
  }

  function looksLikeHostWithoutProtocol(value) {
    return /^(?:localhost|(?:\d{1,3}\.){3}\d{1,3}|[a-z0-9-]+(?:\.[a-z0-9-]+)+)(?::\d+)?(?:\/.*)?$/i
      .test(String(value || ''));
  }

  function normalizeBaseUrl(value) {
    let normalized = trimTrailingSlash(value);
    if (normalized && looksLikeHostWithoutProtocol(normalized)) {
      normalized = `http://${normalized}`;
    }
    return normalized || '';
  }

  function normalizeExternalConfig(value) {
    const source = value && typeof value === 'object' ? value : {};
    return {
      apiBaseUrl: normalizeBaseUrl(source.apiBaseUrl),
      devProxyTarget: normalizeBaseUrl(source.devProxyTarget),
      preferProxyInDev: source.preferProxyInDev !== false
    };
  }

  function isAbsoluteUrl(value) {
    return /^https?:\/\//i.test(String(value || ''));
  }

  function isLocalBrowserHost(hostname) {
    return hostname === '127.0.0.1' || hostname === 'localhost';
  }

  function getExternalConfig() {
    const globalConfig = typeof globalThis !== 'undefined'
      ? globalThis.KitchenAdminExternalConfig
      : undefined;
    return normalizeExternalConfig(globalConfig);
  }

  function readExternalConfigFile(filePath) {
    if (typeof require !== 'function') {
      return {};
    }

    try {
      const fs = require('node:fs');
      const path = require('node:path');
      const resolvedPath = path.resolve(String(filePath || DEFAULT_EXTERNAL_CONFIG_PATH));

      if (!fs.existsSync(resolvedPath)) {
        return {};
      }

      const raw = fs.readFileSync(resolvedPath, 'utf8');
      return normalizeExternalConfig(JSON.parse(raw));
    } catch (error) {
      return {};
    }
  }

  function resolveAdminApiBaseUrl(options = {}) {
    const protocol = options.protocol || '';
    const hostname = options.hostname || '';
    const externalConfig = normalizeExternalConfig(options.externalConfig || getExternalConfig());
    const explicit = normalizeBaseUrl(
      options.storedBaseUrl || options.envBaseUrl || externalConfig.apiBaseUrl
    );

    if (
      protocol !== 'file:'
      && isLocalBrowserHost(hostname)
      && externalConfig.preferProxyInDev
      && externalConfig.devProxyTarget
      && isAbsoluteUrl(explicit)
    ) {
      return DEFAULT_API_PROXY_BASE_URL;
    }

    if (explicit) {
      return explicit;
    }

    if (protocol === 'file:') {
      return externalConfig.devProxyTarget || externalConfig.apiBaseUrl || '';
    }

    return DEFAULT_API_PROXY_BASE_URL;
  }

  function resolveAdminProxyTarget(value, options = {}) {
    const externalConfig = normalizeExternalConfig(options.externalConfig || getExternalConfig());
    return normalizeBaseUrl(value || externalConfig.devProxyTarget);
  }

  return {
    DEFAULT_API_PROXY_BASE_URL: DEFAULT_API_PROXY_BASE_URL,
    DEFAULT_EXTERNAL_CONFIG_PATH: DEFAULT_EXTERNAL_CONFIG_PATH,
    trimTrailingSlash: trimTrailingSlash,
    normalizeBaseUrl: normalizeBaseUrl,
    normalizeExternalConfig: normalizeExternalConfig,
    getExternalConfig: getExternalConfig,
    readExternalConfigFile: readExternalConfigFile,
    resolveAdminApiBaseUrl: resolveAdminApiBaseUrl,
    resolveAdminProxyTarget: resolveAdminProxyTarget
  };
});
