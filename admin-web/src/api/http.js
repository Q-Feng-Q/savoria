import '../../runtime-config.js';
import { ADMIN_SESSION_KEY } from '../config';
import { createAdminApiClient, joinApiUrl } from './admin-api-client';
import { emitSessionInvalid } from './session-events';
import { notify } from '../utils/feedback';

const runtimeConfig = globalThis.KitchenAdminRuntimeConfig || {};
const BASE_URL_KEY = 'family_kitchen_admin_base_url';
const DEFAULT_API_BASE_URL = runtimeConfig.DEFAULT_API_PROXY_BASE_URL || '/api';

function trimTrailingSlash(value) {
  return String(value || '').replace(/\/+$/, '');
}

function getSafeStorage() {
  if (typeof localStorage !== 'undefined') {
    return localStorage;
  }

  return {
    getItem() {
      return null;
    },
    setItem() {},
    removeItem() {}
  };
}

export function getApiBaseUrl() {
  const storage = getSafeStorage();
  const stored = storage.getItem(BASE_URL_KEY) || '';
  const envValue = typeof import.meta !== 'undefined' && import.meta.env ? import.meta.env.VITE_API_BASE_URL : '';
  const hostname = typeof window !== 'undefined' && window.location ? window.location.hostname : '';
  const protocol = typeof window !== 'undefined' && window.location ? window.location.protocol : '';

  if (runtimeConfig.resolveAdminApiBaseUrl) {
    return runtimeConfig.resolveAdminApiBaseUrl({
      protocol,
      hostname,
      storedBaseUrl: stored,
      envBaseUrl: envValue
    });
  }

  return trimTrailingSlash(stored || envValue || DEFAULT_API_BASE_URL);
}

export function setApiBaseUrl(baseUrl) {
  const storage = getSafeStorage();
  const normalized = trimTrailingSlash(baseUrl || DEFAULT_API_BASE_URL);
  storage.setItem(BASE_URL_KEY, normalized);
  return normalized;
}

export function getStoredSession() {
  const storage = getSafeStorage();
  const raw = storage.getItem(ADMIN_SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch (error) {
    storage.removeItem(ADMIN_SESSION_KEY);
    return null;
  }
}

export function setStoredSession(session) {
  const storage = getSafeStorage();
  storage.setItem(ADMIN_SESSION_KEY, JSON.stringify(session));
  return session;
}

export function clearStoredSession() {
  const storage = getSafeStorage();
  storage.removeItem(ADMIN_SESSION_KEY);
}

export function hasStoredSession() {
  return Boolean(getStoredSession());
}

export function createRequestClient() {
  return createAdminApiClient({
    baseUrl: getApiBaseUrl(),
    getSession: getStoredSession,
    onAuthError(error) {
      clearStoredSession();
      emitSessionInvalid(error);
    },
    fetch: window.fetch.bind(window)
  });
}

export async function request(pathname, options) {
  const client = createRequestClient();
  try {
    return await client(pathname, options);
  } catch (error) {
    if (error?.status !== 401 && error?.status !== 403 && error?.code !== 40101 && error?.code !== 40301) {
      notify(error?.message || '请求失败，请稍后重试', 'error');
    }
    throw error;
  }
}

export async function requestWithoutSession(pathname, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };
  const response = await window.fetch(joinApiUrl(getApiBaseUrl(), pathname), {
    ...options,
    headers
  });

  let payload = null;
  try {
    payload = await response.json();
  } catch (error) {
    const requestError = new Error(`request failed with status ${response.status}`);
    requestError.code = response.status;
    requestError.data = null;
    throw requestError;
  }

  if (!response.ok || !payload || payload.code !== 0) {
    const error = new Error((payload && payload.message) || `request failed with status ${response.status}`);
    error.code = payload && payload.code ? payload.code : response.status;
    error.data = payload && Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : null;
    throw error;
  }

  return payload.data;
}

export function normalizeArray(value) {
  if (Array.isArray(value)) {
    return value;
  }
  if (value && Array.isArray(value.items)) {
    return value.items;
  }
  if (value && Array.isArray(value.records)) {
    return value.records;
  }
  if (value && Array.isArray(value.content)) {
    return value.content;
  }
  return [];
}
