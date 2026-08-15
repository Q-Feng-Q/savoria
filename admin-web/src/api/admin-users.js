import { normalizeArray, request } from './http';

export async function listAdminUsers(keyword = '') {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
  return normalizeArray(await request(`/api/admin/users${query}`, { method: 'GET' }));
}
export const createAdminUser = (payload) => request('/api/admin/users', { method: 'POST', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' } });
export const updateAdminUserStatus = (userId, status) => request(`/api/admin/users/${userId}/status`, { method: 'PUT', body: JSON.stringify({ status }), headers: { 'Content-Type': 'application/json' } });
export const updateAdminUserPlatformRole = (userId, platformAdmin) => request(`/api/admin/users/${userId}/platform-role`, { method: 'PUT', body: JSON.stringify({ platformAdmin }), headers: { 'Content-Type': 'application/json' } });
export const deleteAdminUser = (userId) => request(`/api/admin/users/${userId}`, { method: 'DELETE' });
