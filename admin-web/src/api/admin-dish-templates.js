import { buildAdminAuthHeaders, joinApiUrl } from './admin-api-client';
import { getApiBaseUrl, getStoredSession, request } from './http';

function querySuffix(query = {}) {
  const search = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, value);
  });
  return search.toString() ? `?${search}` : '';
}

export function listAdminDishTemplates(query = {}) {
  return request(`/api/admin/dish-templates${querySuffix(query)}`, { method: 'GET' });
}

export function getAdminDishTemplate(templateId) {
  return request(`/api/admin/dish-templates/${templateId}`, { method: 'GET' });
}

export function listAdminDishTemplateSourceRecords(templateId) {
  return request(`/api/admin/dish-templates/${templateId}/source-records`, { method: 'GET' });
}

export function updateAdminDishTemplate(templateId, payload) {
  return request(`/api/admin/dish-templates/${templateId}`, {
    method: 'PUT', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
  });
}

export async function previewDishTemplateAsset(assetId) {
  const response = await window.fetch(joinApiUrl(getApiBaseUrl(), `/api/admin/dish-template-assets/${assetId}/preview`), {
    method: 'GET', headers: buildAdminAuthHeaders(getStoredSession())
  });
  if (!response.ok) throw new Error('图片预览加载失败');
  return URL.createObjectURL(await response.blob());
}

export function promoteDishTemplateImage(templateId, payload) {
  return request(`/api/admin/dish-templates/${templateId}/image-promotion`, {
    method: 'POST', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
  });
}

export function rejectDishTemplateImage(templateId, assetId, reason) {
  return request(`/api/admin/dish-templates/${templateId}/image-assets/${assetId}/reject`, {
    method: 'POST', body: JSON.stringify({ reason }), headers: { 'Content-Type': 'application/json' }
  });
}
