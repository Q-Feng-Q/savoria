import { normalizeArray, request } from './http';

export async function listMerchantDishes() {
  return normalizeArray(await request('/api/merchant/dishes', { method: 'GET' }));
}

export async function getMerchantDishDetail(dishId) {
  return request(`/api/merchant/dishes/${dishId}`, { method: 'GET' });
}

export async function listDishCategories() {
  return normalizeArray(await request('/api/merchant/dish-categories', { method: 'GET' }));
}

export async function listDishTemplateCategories() {
  return normalizeArray(await request('/api/merchant/dish-template-categories', { method: 'GET' }));
}

export async function listDishTemplates(query = {}) {
  const search = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, value);
  });
  const suffix = search.toString() ? `?${search}` : '';
  return request(`/api/merchant/dish-templates${suffix}`, { method: 'GET' });
}

export async function getDishTemplateDetail(templateId) {
  return request(`/api/merchant/dish-templates/${templateId}`, { method: 'GET' });
}

export async function importDishTemplates(templateIds) {
  return request('/api/merchant/dish-templates/import', {
    method: 'POST', body: JSON.stringify({ templateIds }), headers: { 'Content-Type': 'application/json' }
  });
}

export async function createDishCategory(payload) {
  return request('/api/merchant/dish-categories', {
    method: 'POST',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function updateDishCategory(categoryId, payload) {
  return request(`/api/merchant/dish-categories/${categoryId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function deleteDishCategory(categoryId) {
  return request(`/api/merchant/dish-categories/${categoryId}`, {
    method: 'DELETE'
  });
}

export async function createMerchantDish(payload) {
  return request('/api/merchant/dishes', {
    method: 'POST',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function updateMerchantDish(dishId, payload) {
  return request(`/api/merchant/dishes/${dishId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function updateMerchantDishCookingSteps(dishId, cookingSteps) {
  return request(`/api/merchant/dishes/${dishId}/cooking-steps`, {
    method: 'PUT',
    body: JSON.stringify({ cookingSteps }),
    headers: { 'Content-Type': 'application/json' }
  });
}
