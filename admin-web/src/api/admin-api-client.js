function trimTrailingSlash(value) {
  return String(value || '').replace(/\/+$/, '');
}

function normalizePathname(value) {
  const pathname = String(value || '');
  if (!pathname) {
    return '';
  }

  return pathname.startsWith('/') ? pathname : `/${pathname}`;
}

export function joinApiUrl(baseUrl, pathname) {
  const normalizedBaseUrl = trimTrailingSlash(baseUrl || '');
  let normalizedPathname = normalizePathname(pathname);

  if (normalizedBaseUrl.endsWith('/api') && normalizedPathname.startsWith('/api/')) {
    normalizedPathname = normalizedPathname.slice(4);
  }

  return `${normalizedBaseUrl}${normalizedPathname}`;
}

function firstPresent() {
  for (let index = 0; index < arguments.length; index += 1) {
    const value = arguments[index];
    if (value !== undefined && value !== null && value !== '') {
      return value;
    }
  }

  return undefined;
}

function joinHeaderList(value) {
  if (Array.isArray(value)) {
    return value.join(',');
  }

  return value || '';
}

function putHeader(headers, name, value) {
  if (value !== undefined && value !== null && value !== '') {
    headers[name] = value;
  }
}

export function buildAdminAuthHeaders(session) {
  if (!session) {
    return {};
  }

  const actor = session.actor || {};
  const headers = {};

  putHeader(headers, 'Authorization', session.accessToken ? `Bearer ${session.accessToken}` : '');
  putHeader(headers, 'X-User-Id', firstPresent(session.userId, actor.userId));
  putHeader(headers, 'X-Merchant-Id', firstPresent(session.merchantId, actor.merchantId));
  putHeader(headers, 'X-Family-Id', firstPresent(session.familyId, actor.familyId));
  putHeader(headers, 'X-Member-Id', firstPresent(session.memberId, actor.memberId));
  putHeader(headers, 'X-Role-Template', session.roleTemplate || 'merchant_admin');
  putHeader(headers, 'X-Backend-Roles', joinHeaderList(session.backendRoles));
  putHeader(headers, 'X-Merchant-Admin-Scopes', joinHeaderList(session.merchantAdminScopes));

  return headers;
}

function normalizeError(status, payload) {
  const error = new Error((payload && payload.message) || `request failed with status ${status}`);
  error.code = payload && payload.code ? payload.code : status;
  error.status = status;
  error.data = payload && Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : null;
  return error;
}

export function createAdminApiClient(options = {}) {
  const baseUrl = trimTrailingSlash(options.baseUrl || '');
  const getSession = options.getSession || (() => null);
  const onAuthError = options.onAuthError || (() => {});
  const fetchAdapter = options.fetch || (typeof fetch !== 'undefined' ? fetch.bind(globalThis) : null);

  return async function request(pathname, requestOptions = {}) {
    if (!fetchAdapter) {
      throw new Error('fetch is not available');
    }

    const headers = Object.assign(
      {},
      requestOptions.headers || {},
      buildAdminAuthHeaders(getSession())
    );

    const response = await fetchAdapter(joinApiUrl(baseUrl, pathname), {
      ...requestOptions,
      headers
    });
    const payload = await response.json();

    if (!response.ok || !payload || payload.code !== 0) {
      const error = normalizeError(response.status, payload);
      if (error.status === 401 || error.code === 40101) {
        onAuthError(error);
      }
      throw error;
    }

    return payload.data;
  };
}
