import { normalizeArray, request } from './http';

export const listPendingDishReviews = async () => normalizeArray(await request('/api/admin/dish-reviews', { method: 'GET' }));
export const approveDishReview = (id, reason = '') => request(`/api/admin/dish-reviews/${id}/approve`, {
  method: 'POST', body: JSON.stringify({ reason }), headers: { 'Content-Type': 'application/json' }
});
export const rejectDishReview = (id, reason) => request(`/api/admin/dish-reviews/${id}/reject`, {
  method: 'POST', body: JSON.stringify({ reason }), headers: { 'Content-Type': 'application/json' }
});
