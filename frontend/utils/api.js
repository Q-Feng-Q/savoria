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

function normalizeBackendPath(pathname) {
  const value = String(pathname || '');
  if (value === '/api') return '/';
  return value.startsWith('/api/') ? value.slice(4) : value;
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

function loginIdentity(session) {
  if (!session) return '';
  const refreshToken = String(session.refreshToken || '');
  return refreshToken ? refreshToken.split('.')[0] : String(session.accessToken || '');
}

function createApiClient(options = {}) {
  const requestAdapter = options.request || defaultRequestAdapter;
  const getToken = options.getToken || (() => '');
  const getSession = options.getSession || (() => null);
  const baseUrl = trimTrailingSlash(options.baseUrl || '');
  const refreshSession = options.refreshSession;

  return async function request(pathname, requestOptions = {}) {
    const backendPath = normalizeBackendPath(pathname);
    const origin = getSession();
    const sameIdentity = () => {
      if (!origin) return true;
      const current = getSession();
      return Boolean(current && String(current.userId) === String(origin.userId)
        && current.activeMode === origin.activeMode
        && loginIdentity(current) === loginIdentity(origin));
    };
    const accountSwitched = () => {
      const error = new Error('账号已切换，已取消旧请求');
      error.code = 'ACCOUNT_SWITCHED';
      return error;
    };
    async function send() {
      if (!sameIdentity()) throw accountSwitched();
      const session = getSession();
      const token = getToken();
      const header = { ...(requestOptions.header || {}), ...buildAuthHeaders(session) };
      if (token && !header.Authorization) header.Authorization = `Bearer ${token}`;
      const response = await requestAdapter({
        ...requestOptions, url: `${baseUrl}${backendPath}`, header
      });
      if (!sameIdentity()) throw accountSwitched();
      const payload = response.data || {};
      if (response.statusCode >= 400 || payload.code !== 0) {
        throw normalizeError(response.statusCode, payload);
      }
      return payload;
    }

    try { return await send(); }
    catch (error) {
      const unauthorized = error.code === 40101 || error.code === 401 || error.statusCode === 401;
      if (unauthorized && !sameIdentity()) throw accountSwitched();
      const renewalAllowed = !backendPath.startsWith('/auth/')
        || backendPath === '/auth/logout' || backendPath === '/auth/logout-all';
      if (!unauthorized || !refreshSession || !renewalAllowed) throw error;
      let renewed = false;
      try { renewed = await refreshSession(); } catch (refreshError) {
        const invalidRefresh = refreshError.code === 40101 || refreshError.code === 401
          || refreshError.statusCode === 401;
        if (invalidRefresh) throw error;
        throw refreshError;
      }
      if (!sameIdentity()) throw accountSwitched();
      if (!renewed) throw error;
      return send();
    }
  };
}

module.exports = {
  buildAuthHeaders,
  createApiClient,
  normalizeBackendPath
};
