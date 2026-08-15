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

export async function getPurchaseSummary(params = {}) {
  return normalizeArray(await request(`/api/merchant/purchases/summary${buildQuery(params)}`, { method: 'GET' }));
}

export async function getPurchaseByFamily(params = {}) {
  return normalizeArray(await request(`/api/merchant/purchases/by-family${buildQuery(params)}`, { method: 'GET' }));
}

export async function togglePurchaseChecked(itemId, checked) {
  return request(`/api/merchant/purchases/items/${itemId}/checked`, {
    method: 'POST',
    body: JSON.stringify({ checked }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function createTempPurchaseItem(payload) {
  return request('/api/merchant/purchases/temp-items', {
    method: 'POST',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function deleteTempPurchaseItem(itemId) {
  return request(`/api/merchant/purchases/temp-items/${itemId}`, {
    method: 'DELETE'
  });
}

export async function getPurchaseCopyText(params = {}) {
  return request(`/api/merchant/purchases/copy-text${buildQuery(params)}`, { method: 'GET' });
}
