const BRAND_NAME = '食光栀味';
const VARIANTS = {
  small: { url: 'siteLogoSmallUrl', size: 'siteLogoSmallSize', defaultSize: 32, min: 16, max: 64, asset: 64 },
  standard: { url: 'siteLogoUrl', size: 'siteLogoSize', defaultSize: 56, min: 24, max: 120, asset: 128 },
  large: { url: 'siteLogoLargeUrl', size: 'siteLogoLargeSize', defaultSize: 96, min: 48, max: 160, asset: 256 }
};

function isSafeBrandUrl(value) {
  if (typeof value !== 'string' || value.length > 500 || /[\u0000-\u0020\u007f\\]/.test(value)) return false;
  if (!value) return true;
  if (/^\/(?!\/)/.test(value)) return true;
  return /^https:\/\/[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?(?::\d{1,5})?(?:[/?#][^\s\\]*)?$/i.test(value);
}

function normalizeBranding(raw = {}) {
  raw = raw && typeof raw === 'object' ? raw : {};
  const name = typeof raw.siteName === 'string' ? raw.siteName.trim().slice(0, 100) : '';
  const result = { siteName: name || BRAND_NAME };
  for (const variant of Object.values(VARIANTS)) {
    result[variant.url] = isSafeBrandUrl(raw[variant.url]) ? raw[variant.url] : '';
    const value = raw[variant.size];
    result[variant.size] = Number.isInteger(value)
      ? Math.max(variant.min, Math.min(variant.max, value)) : variant.defaultSize;
  }
  result.siteFaviconUrl = isSafeBrandUrl(raw.siteFaviconUrl) ? raw.siteFaviconUrl : '';
  return result;
}

function resolveBrandUrl(value, baseUrl = '') {
  if (!value || !isSafeBrandUrl(value)) return '';
  if (/^https:\/\//i.test(value)) return value;
  return String(baseUrl).replace(/\/+$/, '').replace(/\/api$/, '') + value;
}

function resolveLogo(brand, size = 'standard', baseUrl = '') {
  const variant = VARIANTS[size] || VARIANTS.standard;
  const value = normalizeBranding(brand);
  const fallback = `/assets/brand/logo/logo-${variant.asset}.png`;
  return {
    src: resolveBrandUrl(value[variant.url] || value.siteLogoUrl, baseUrl) || fallback,
    fallback, sizeRpx: value[variant.size] * 2, name: value.siteName
  };
}

// Each API origin has its own cache. A late response from another backend is ignored.
function createBrandStore({ load, baseUrl = () => '', now = Date.now, storage = {} } = {}) {
  let origin;
  let value = normalizeBranding();
  let pending = null;
  let lastAttempt = -Infinity;
  const listeners = new Set();
  const getOrigin = typeof baseUrl === 'function' ? baseUrl : () => baseUrl;
  const notify = () => listeners.forEach(listener => listener({ ...value }));
  function selectOrigin() {
    const next = getOrigin() || '';
    if (next !== origin) {
      origin = next;
      pending = null;
      lastAttempt = -Infinity;
      let cached;
      try { cached = storage.get && storage.get(`brand-v1:${origin}`); } catch (_) { /* Storage is optional. */ }
      value = normalizeBranding(cached);
    }
    return origin;
  }
  function get() { selectOrigin(); return { ...value }; }
  function refresh() {
    const requestOrigin = selectOrigin();
    if (pending) return pending;
    if (now() - lastAttempt < 30000) return Promise.resolve(get());
    lastAttempt = now();
    const request = Promise.resolve().then(() => load(requestOrigin)).then(raw => {
      if (requestOrigin !== getOrigin()) return get();
      if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return get();
      value = normalizeBranding(raw);
      try { if (storage.set) storage.set(`brand-v1:${origin}`, value); } catch (_) { /* Quota must not block the page. */ }
      notify();
      return get();
    }).catch(() => get()).finally(() => { if (pending === request) pending = null; });
    pending = request;
    return request;
  }
  function subscribe(listener) {
    listeners.add(listener);
    listener(get());
    return () => listeners.delete(listener);
  }
  return { get, refresh, subscribe, baseUrl: getOrigin };
}

function getApiBaseUrl() {
  try { return getApp().globalData.apiBaseUrl || ''; } catch (_) { return ''; }
}

function loadPublicBranding(baseUrl) {
  return new Promise((resolve, reject) => {
    if (typeof wx === 'undefined' || !wx.request) { reject(new Error('request unavailable')); return; }
    // Follow the configured backend prefix; public settings need no session.
    const base = String(baseUrl).replace(/\/+$/, '');
    wx.request({ url: `${base}/public/system-settings`, method: 'GET', timeout: 5000,
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300 && response.data && response.data.code === 0) resolve(response.data.data);
        else reject(new Error('brand settings unavailable'));
      }, fail: reject });
  });
}

const brandStore = createBrandStore({ load: loadPublicBranding, baseUrl: getApiBaseUrl, storage: {
  get: key => typeof wx !== 'undefined' && wx.getStorageSync ? wx.getStorageSync(key) : null,
  set: (key, value) => { if (typeof wx !== 'undefined' && wx.setStorageSync) wx.setStorageSync(key, value); }
} });

function withBranding(page, { store = brandStore, navigationTitle = false } = {}) {
  function unsubscribe(instance) {
    if (instance._unsubscribeBrand) instance._unsubscribeBrand();
    instance._unsubscribeBrand = null;
  }
  return {
    ...page,
    data: { ...page.data, brandName: store.get().siteName },
    onShow(...args) {
      unsubscribe(this);
      this._unsubscribeBrand = store.subscribe(brand => {
        this.setData({ brandName: brand.siteName });
        if (navigationTitle && typeof wx !== 'undefined' && wx.setNavigationBarTitle) {
          wx.setNavigationBarTitle({ title: brand.siteName });
        }
      });
      store.refresh();
      if (page.onShow) return page.onShow.apply(this, args);
    },
    onHide(...args) {
      unsubscribe(this);
      if (page.onHide) return page.onHide.apply(this, args);
    },
    onUnload(...args) {
      unsubscribe(this);
      if (page.onUnload) return page.onUnload.apply(this, args);
    }
  };
}

module.exports = { BRAND_NAME, VARIANTS, isSafeBrandUrl, normalizeBranding, resolveBrandUrl, resolveLogo, createBrandStore, brandStore, loadPublicBranding, withBranding };
