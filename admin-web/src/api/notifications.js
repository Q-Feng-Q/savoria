import { normalizeArray, request } from './http';

function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, value);
    }
  });
  const value = query.toString();
  return value ? `?${value}` : '';
}

export async function listNotifications(params = {}) {
  const data = await request(`/api/notifications${buildQuery(params)}`, { method: 'GET' });
  return normalizeArray(data);
}

export async function markNotificationRead(notificationId) {
  return request(`/api/notifications/${notificationId}/read`, { method: 'POST' });
}

export async function markAllNotificationsRead(receiverScope = 'merchant') {
  return request('/api/notifications/read-all', {
    method: 'POST',
    body: JSON.stringify({ receiverScope }),
    headers: { 'Content-Type': 'application/json' }
  });
}
