import { buildAdminAuthHeaders, joinApiUrl } from './admin-api-client';
import { clearStoredSession, getApiBaseUrl, getStoredSession, request } from './http';
import { emitSessionInvalid } from './session-events';

export function listAdminFeedback(query = {}) {
  const search = new URLSearchParams();
  for (const key of ['page', 'pageSize', 'type', 'status']) {
    if (query[key] !== undefined && query[key] !== '') search.set(key, query[key]);
  }
  return request(`/admin/feedback?${search}`, { method: 'GET' });
}
export function getAdminFeedback(id) {
  return request(`/admin/feedback/${encodeURIComponent(id)}`, { method: 'GET' });
}
export function updateAdminFeedback(id, payload) {
  return request(`/admin/feedback/${encodeURIComponent(id)}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
}
export async function getFeedbackImage(imageId) {
  const session = getStoredSession();
  const identity = JSON.stringify(session);
  const baseUrl = getApiBaseUrl();
  const response = await window.fetch(joinApiUrl(baseUrl, `/feedback/images/${encodeURIComponent(imageId)}`), {
    method: 'GET', headers: buildAdminAuthHeaders(session), cache: 'no-store'
  });
  if (!response.ok) {
    const error = Object.assign(new Error('图片加载失败，请重试'), { status: response.status });
    if (response.status === 401 && JSON.stringify(getStoredSession()) === identity && getApiBaseUrl() === baseUrl) {
      clearStoredSession(); emitSessionInvalid(error);
    }
    throw error;
  }
  return response.blob();
}
