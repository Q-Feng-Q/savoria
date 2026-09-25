export const BRAND_DEFAULTS = Object.freeze({
  siteName: '食光栀味', siteLogoUrl: '', siteLogoSmallUrl: '', siteLogoLargeUrl: '', siteFaviconUrl: '',
  siteLogoSmallSize: 32, siteLogoSize: 56, siteLogoLargeSize: 96
});
export const BRAND_SIZES = Object.freeze({ siteLogoSmallSize: [16, 64], siteLogoSize: [24, 120], siteLogoLargeSize: [48, 160] });
export const BRAND_URLS = ['siteLogoSmallUrl', 'siteLogoUrl', 'siteLogoLargeUrl', 'siteFaviconUrl'];

export function isBrandUrl(value) {
  if (typeof value !== 'string' || value.length > 500 || /[\x00-\x20\x7f\\]/.test(value)) return false;
  if (!value) return true;
  if (value.startsWith('/')) return !value.startsWith('//');
  if (!/^https:\/\/[^/]/i.test(value)) return false;
  try { const url = new URL(value); return url.protocol === 'https:' && Boolean(url.hostname) && !url.username && !url.password; } catch { return false; }
}

export function normalizeBranding(value = {}) {
  const source = value || {};
  const result = { ...BRAND_DEFAULTS };
  const name = typeof source.siteName === 'string' ? source.siteName.trim() : '';
  result.siteName = name || BRAND_DEFAULTS.siteName;
  for (const key of BRAND_URLS) result[key] = isBrandUrl(source[key]) ? source[key] : '';
  for (const [key, [min, max]] of Object.entries(BRAND_SIZES)) {
    const size = source[key];
    result[key] = typeof size === 'number' && Number.isFinite(size) ? Math.min(max, Math.max(min, Math.round(size))) : BRAND_DEFAULTS[key];
  }
  return result;
}

export function brandPayload(value) {
  for (const key of BRAND_URLS) if (value[key] != null && !isBrandUrl(value[key])) throw new Error('图片地址须为 HTTPS 或单斜杠开头的站点路径，最多 500 字符');
  for (const [key, [min, max]] of Object.entries(BRAND_SIZES)) {
    if (value[key] != null && (!Number.isInteger(value[key]) || value[key] < min || value[key] > max)) throw new Error(`展示尺寸须为 ${min}～${max} 范围内的整数`);
  }
  if (value.siteName != null && (!String(value.siteName).trim() || String(value.siteName).length > 100)) throw new Error('站点名称须为 1～100 个字符');
  return normalizeBranding(value);
}

export function resetBrandImages(value) {
  return { ...value, ...Object.fromEntries([...BRAND_URLS, ...Object.keys(BRAND_SIZES)].map((key) => [key, BRAND_DEFAULTS[key]])) };
}

export function resolveBrandUrl(value, apiBase = '/api') {
  if (!isBrandUrl(value) || !value) return '';
  if (!value.startsWith('/')) return value;
  if (/^https?:\/\//i.test(apiBase)) return new URL(value, apiBase).href;
  const base = apiBase.replace(/\/+$/, '');
  return value.startsWith(`${base}/`) ? value : `${base}${value}`;
}

export function resolveLogo(value, variant = 'standard', apiBase = '/api') {
  const brand = normalizeBranding(value);
  const variants = { small: ['siteLogoSmallUrl', 'siteLogoSmallSize', 64], standard: ['siteLogoUrl', 'siteLogoSize', 128], large: ['siteLogoLargeUrl', 'siteLogoLargeSize', 256] };
  const [urlKey, sizeKey, pixels] = variants[variant] || variants.standard;
  const fallback = `/brand/logo-${pixels}.png`;
  return { src: resolveBrandUrl(brand[urlKey] || brand.siteLogoUrl, apiBase) || fallback, fallback, size: brand[sizeKey], name: brand.siteName };
}

export function applyBrandDocument(doc, value, title = '', apiBase = '/api') {
  const brand = normalizeBranding(value);
  doc.title = title ? `${title} · ${brand.siteName}` : brand.siteName;
  let link = doc.querySelector('link[rel="icon"]');
  if (!link) { link = doc.createElement('link'); link.rel = 'icon'; doc.head.appendChild(link); }
  const href = resolveBrandUrl(brand.siteFaviconUrl, apiBase) || '/favicon.ico';
  if (link.dataset?.brandSource === href) return;
  if (link.dataset) link.dataset.brandSource = href;
  link.onerror = () => { link.onerror = null; link.href = '/favicon.ico'; };
  link.href = href;
}
