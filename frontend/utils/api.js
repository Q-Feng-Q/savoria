function defaultRequestAdapter(options) {
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      success: resolve,
      fail: reject
    });
  });
}

function trimTrailingSlash(value) {
  return String(value || '').replace(/\/+$/, '');
}

function normalizeError(statusCode, payload) {
  const error = new Error((payload && payload.message) || `request failed with status ${statusCode}`);
  error.code = payload && payload.code ? payload.code : statusCode;
  error.statusCode = statusCode;
  error.data = payload && Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : null;
  return error;
}

function firstPresent(...values) {
  return values.find((value) => value !== undefined && value !== null && value !== '');
}

function joinHeaderList(value) {
  if (Array.isArray(value)) {
    return value.join(',');
  }
  return value || '';
}

function putHeader(header, name, value) {
  if (value !== undefined && value !== null && value !== '') {
    header[name] = value;
  }
}

function buildAuthHeaders(session) {
  if (!session) return {};
  const actor = session.actor || {};
  const header = {};

  putHeader(header, 'Authorization', session.accessToken ? `Bearer ${session.accessToken}` : '');
  putHeader(header, 'X-User-Id', firstPresent(session.userId, actor.userId));
  putHeader(header, 'X-Merchant-Id', firstPresent(session.merchantId, actor.merchantId));
  putHeader(header, 'X-Family-Id', firstPresent(session.familyId, actor.familyId));
  putHeader(header, 'X-Member-Id', firstPresent(session.memberId, actor.memberId));
  putHeader(header, 'X-Role-Template', session.roleTemplate || 'member');
  putHeader(header, 'X-Backend-Roles', joinHeaderList(session.backendRoles));
  putHeader(header, 'X-Merchant-Admin-Scopes', joinHeaderList(session.merchantAdminScopes));

  return header;
}

function createApiClient(options = {}) {
  const requestAdapter = options.request || defaultRequestAdapter;
  const getToken = options.getToken || (() => '');
  const getSession = options.getSession || (() => null);
  const baseUrl = trimTrailingSlash(options.baseUrl || '');

  return async function request(pathname, requestOptions = {}) {
    const session = getSession();
    const token = getToken();
    const header = {
      ...(requestOptions.header || {}),
      ...buildAuthHeaders(session)
    };

    if (token && !header.Authorization) {
      header.Authorization = `Bearer ${token}`;
    }

    const backendPath = pathname === '/api' ? '/' : (pathname.startsWith('/api/') ? pathname.slice(4) : pathname);
    const response = await requestAdapter({
      ...requestOptions,
      url: `${baseUrl}${backendPath}`,
      header
    });

    const payload = response.data || {};
    if (response.statusCode >= 400 || payload.code !== 0) {
      throw normalizeError(response.statusCode, payload);
    }

    return payload;
  };
}

module.exports = {
  buildAuthHeaders,
  createApiClient
};
