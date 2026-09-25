import { reactive } from 'vue';
import { getApiBaseUrl, requestWithoutSession } from '../api/http';
import { normalizeBranding } from '../utils/branding';

export const branding = reactive(normalizeBranding());
export function setBranding(value) { Object.assign(branding, normalizeBranding(value)); }
let loading;
export function loadPublicBranding() {
  if (loading) return loading;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 5000);
  loading = requestWithoutSession('/api/public/system-settings', {
    signal: controller.signal, credentials: 'omit', cache: 'no-store'
  }).then(setBranding).catch(() => {}).finally(() => { clearTimeout(timeout); loading = null; });
  return loading;
}
export { getApiBaseUrl };
