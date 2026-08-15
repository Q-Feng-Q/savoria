(function (root, factory) {
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = factory();
  } else {
    root.KitchenAdminApi = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  function trimTrailingSlash(value) {
    return String(value || '').replace(/\/+$/, '');
  }

  function normalizePathname(value) {
    const pathname = String(value || '');
    if (!pathname) {
      return '';
    }

    return pathname.startsWith('/') ? pathname : '/' + pathname;
  }

  function joinApiUrl(baseUrl, pathname) {
    const normalizedBaseUrl = trimTrailingSlash(baseUrl || '');
    let normalizedPathname = normalizePathname(pathname);

    if (normalizedBaseUrl.endsWith('/api') && normalizedPathname.startsWith('/api/')) {
      normalizedPathname = normalizedPathname.slice(4);
    }

    return normalizedBaseUrl + normalizedPathname;
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

  function putHeader(header, name, value) {
    if (value !== undefined && value !== null && value !== '') {
      header[name] = value;
    }
  }

  function buildAdminAuthHeaders(session) {
    if (!session) return {};
    const actor = session.actor || {};
    const header = {};

    putHeader(header, 'Authorization', session.accessToken ? 'Bearer ' + session.accessToken : '');
    putHeader(header, 'X-User-Id', firstPresent(session.userId, actor.userId));
    putHeader(header, 'X-Merchant-Id', firstPresent(session.merchantId, actor.merchantId));
    putHeader(header, 'X-Family-Id', firstPresent(session.familyId, actor.familyId));
    putHeader(header, 'X-Member-Id', firstPresent(session.memberId, actor.memberId));
    putHeader(header, 'X-Role-Template', session.roleTemplate || 'merchant_admin');
    putHeader(header, 'X-Backend-Roles', joinHeaderList(session.backendRoles));
    putHeader(header, 'X-Merchant-Admin-Scopes', joinHeaderList(session.merchantAdminScopes));

    return header;
  }

  function normalizeError(status, payload) {
    const error = new Error((payload && payload.message) || 'request failed with status ' + status);
    error.code = payload && payload.code ? payload.code : status;
    error.status = status;
    error.data = payload && Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : null;
    return error;
  }

  function createAdminApiClient(options) {
    const config = options || {};
    const baseUrl = trimTrailingSlash(config.baseUrl || '');
    const getSession = config.getSession || function () { return null; };
    const onAuthError = config.onAuthError || function () {};
    const fetchAdapter = config.fetch || (typeof fetch !== 'undefined' ? fetch.bind(globalThis) : null);

    return async function request(pathname, requestOptions) {
      if (!fetchAdapter) {
        throw new Error('fetch is not available');
      }

      const options = requestOptions || {};
      const headers = Object.assign(
        {},
        options.headers || {},
        buildAdminAuthHeaders(getSession())
      );

      const response = await fetchAdapter(joinApiUrl(baseUrl, pathname), Object.assign({}, options, {
        headers: headers
      }));
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

  return {
    buildAdminAuthHeaders: buildAdminAuthHeaders,
    createAdminApiClient: createAdminApiClient,
    joinApiUrl: joinApiUrl
  };
});
