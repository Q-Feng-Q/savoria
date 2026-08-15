import { request } from './http';

export const getSystemSettings = () => request('/api/admin/system-settings', { method: 'GET' });
export const updateSystemSettings = (payload) => request('/api/admin/system-settings', {
  method: 'PUT', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
});
export const sendTestEmail = (recipient) => request('/api/admin/system-settings/test-email', {
  method: 'POST', body: JSON.stringify({ recipient }), headers: { 'Content-Type': 'application/json' }
});
