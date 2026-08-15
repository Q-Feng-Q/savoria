import { normalizeArray, request } from './http';

export async function listMerchantFamilies() {
  return normalizeArray(await request('/api/merchant/families', { method: 'GET' }));
}

export async function getMerchantFamilyDetail(familyId) {
  return request(`/api/merchant/families/${familyId}`, { method: 'GET' });
}

export async function updateMerchantFamilyProfile(familyId, payload) {
  return request(`/api/merchant/families/${familyId}/profile`, {
    method: 'PUT',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function updateMerchantFamilyDeliveryPolicy(familyId, payload) {
  return request(`/api/merchant/families/${familyId}/delivery-policy`, {
    method: 'PUT',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function getMemberWalletLedgers(memberId) {
  return normalizeArray(await request(`/api/merchant/members/${memberId}/wallet/ledgers`, { method: 'GET' }));
}

export async function adjustMemberWallet(memberId, payload) {
  return request(`/api/merchant/members/${memberId}/wallet/adjust`, {
    method: 'POST',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function getFamilyMenu(familyId) {
  return normalizeArray(await request(`/api/merchant/families/${familyId}/menu`, { method: 'GET' }));
}

export async function saveFamilyMenu(familyId, items) {
  return request(`/api/merchant/families/${familyId}/menu`, {
    method: 'PUT',
    body: JSON.stringify({ items }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function copyFamilyMenu(familyId, sourceFamilyId) {
  return request(`/api/merchant/families/${familyId}/menu/copy`, {
    method: 'POST',
    body: JSON.stringify({ sourceFamilyId }),
    headers: { 'Content-Type': 'application/json' }
  });
}
