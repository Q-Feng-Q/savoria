import { request } from './http';

function querySuffix(query = {}) {
  const search = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, value);
  });
  return search.toString() ? `?${search}` : '';
}

export function submitDishTemplateChange(templateId, payload) {
  return request(`/api/merchant/dish-templates/${templateId}/change-requests`, {
    method: 'POST', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
  });
}

export function listMerchantDishTemplateChanges(query = {}) {
  return request(`/api/merchant/dish-template-change-requests${querySuffix(query)}`, { method: 'GET' });
}

export function getMerchantDishTemplateChange(requestId) {
  return request(`/api/merchant/dish-template-change-requests/${requestId}`, { method: 'GET' });
}

export function withdrawDishTemplateChange(requestId) {
  return request(`/api/merchant/dish-template-change-requests/${requestId}/withdraw`, { method: 'POST' });
}

export function listAdminDishTemplateChanges(query = {}) {
  return request(`/api/admin/dish-template-change-requests${querySuffix(query)}`, { method: 'GET' });
}

export function getAdminDishTemplateChange(requestId) {
  return request(`/api/admin/dish-template-change-requests/${requestId}`, { method: 'GET' });
}

export function approveDishTemplateChange(requestId, reason = '') {
  return request(`/api/admin/dish-template-change-requests/${requestId}/approve`, {
    method: 'POST', body: JSON.stringify({ reason }), headers: { 'Content-Type': 'application/json' }
  });
}

export function rejectDishTemplateChange(requestId, reason) {
  return request(`/api/admin/dish-template-change-requests/${requestId}/reject`, {
    method: 'POST', body: JSON.stringify({ reason }), headers: { 'Content-Type': 'application/json' }
  });
}
