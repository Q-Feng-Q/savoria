import { normalizeArray, request } from './http';

export async function listMerchantDishReviews() {
  return normalizeArray(await request('/api/merchant/dish-reviews', { method: 'GET' }));
}

export function getMerchantDishReview(reviewId) {
  return request(`/api/merchant/dish-reviews/${reviewId}`, { method: 'GET' });
}

export function withdrawDishReview(reviewId) {
  return request(`/api/merchant/dish-reviews/${reviewId}/withdraw`, { method: 'POST' });
}
