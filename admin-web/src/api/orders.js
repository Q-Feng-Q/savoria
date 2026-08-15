import { normalizeArray, request } from './http';

export async function listMerchantOrders() {
  return normalizeArray(await request('/api/merchant/orders', { method: 'GET' }));
}

export async function getMerchantOrderDetail(orderId) {
  return request(`/api/merchant/orders/${orderId}`, { method: 'GET' });
}

export async function confirmMerchantOrder(orderId) {
  return request(`/api/merchant/orders/${orderId}/confirm`, { method: 'POST' });
}

export async function rejectMerchantOrder(orderId, reason) {
  return request(`/api/merchant/orders/${orderId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function cancelMerchantOrder(orderId, reason) {
  return request(`/api/merchant/orders/${orderId}/cancel`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function updateMerchantOrderDeliveryFee(orderId, deliveryFee) {
  return request(`/api/merchant/orders/${orderId}/delivery-fee`, {
    method: 'POST',
    body: JSON.stringify({ deliveryFee }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function advanceMerchantOrderStatus(orderId, status) {
  return request(`/api/merchant/orders/${orderId}/status`, {
    method: 'POST',
    body: JSON.stringify({ status }),
    headers: { 'Content-Type': 'application/json' }
  });
}
