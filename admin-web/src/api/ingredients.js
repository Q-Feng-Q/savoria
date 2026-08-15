import { normalizeArray, request } from './http';

export async function listMerchantIngredients() {
  return normalizeArray(await request('/api/merchant/ingredients', { method: 'GET' }));
}

export async function createMerchantIngredient(payload) {
  return request('/api/merchant/ingredients', {
    method: 'POST',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function updateMerchantIngredient(ingredientId, payload) {
  return request(`/api/merchant/ingredients/${ingredientId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function deleteMerchantIngredient(ingredientId) {
  return request(`/api/merchant/ingredients/${ingredientId}`, {
    method: 'DELETE'
  });
}
