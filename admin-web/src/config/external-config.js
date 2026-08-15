const BACKEND_CONFIG_URL = '/backend.config.json';

function normalizeBaseUrl(value) {
  let normalized = String(value || '').replace(/\/+$/, '');
  if (
    normalized
    && /^(?:localhost|(?:\d{1,3}\.){3}\d{1,3}|[a-z0-9-]+(?:\.[a-z0-9-]+)+)(?::\d+)?(?:\/.*)?$/i.test(normalized)
  ) {
    normalized = `http://${normalized}`;
  }

  return normalized;
}

function normalizeExternalConfig(value) {
  const source = value && typeof value === 'object' ? value : {};
  return {
    apiBaseUrl: normalizeBaseUrl(source.apiBaseUrl),
    devProxyTarget: normalizeBaseUrl(source.devProxyTarget),
    preferProxyInDev: source.preferProxyInDev !== false
  };
}

export async function loadExternalConfig(fetchAdapter = window.fetch.bind(window)) {
  try {
    const response = await fetchAdapter(BACKEND_CONFIG_URL, {
      cache: 'no-store'
    });

    if (!response.ok) {
      throw new Error(`failed to load backend config: ${response.status}`);
    }

    const payload = await response.json();
    const config = normalizeExternalConfig(payload);
    globalThis.KitchenAdminExternalConfig = config;
    return config;
  } catch (error) {
    globalThis.KitchenAdminExternalConfig = {};
    console.warn('[admin-web] failed to load backend.config.json, fallback to default /api mode', error);
    return {};
  }
}
