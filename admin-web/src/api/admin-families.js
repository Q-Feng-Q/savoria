import { normalizeArray, request } from './http';

export async function listAdminFamilyOptions() {
  return normalizeArray(await request('/api/admin/families/options', { method: 'GET' }));
}

export async function getAdminFamilyDetail(familyId) {
  return request(`/api/admin/families/${familyId}`, { method: 'GET' });
}

export async function updateAdminFamily(familyId, payload) {
  return request(`/api/admin/families/${familyId}`, {
    method: 'PUT', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
  });
}

export async function disableAdminFamily(familyId) {
  return request(`/api/admin/families/${familyId}`, { method: 'DELETE' });
}

export async function updateAdminFamilyMember(familyId, memberId, payload) {
  return request(`/api/admin/families/${familyId}/members/${memberId}`, {
    method: 'PUT', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
  });
}

export async function disableAdminFamilyMember(familyId, memberId) {
  return request(`/api/admin/families/${familyId}/members/${memberId}`, { method: 'DELETE' });
}

export async function createAdminFamilyAddress(familyId, payload) {
  return request(`/api/admin/families/${familyId}/addresses`, {
    method: 'POST', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
  });
}

export async function updateAdminFamilyAddress(familyId, addressId, payload) {
  return request(`/api/admin/families/${familyId}/addresses/${addressId}`, {
    method: 'PUT', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
  });
}

export async function deleteAdminFamilyAddress(familyId, addressId) {
  return request(`/api/admin/families/${familyId}/addresses/${addressId}`, { method: 'DELETE' });
}

export async function setAdminFamilyDefaultAddress(familyId, addressId) {
  return request(`/api/admin/families/${familyId}/addresses/${addressId}/default`, { method: 'POST' });
}

export async function getAdminFamilyMenu(familyId) {
  return normalizeArray(await request(`/api/admin/families/${familyId}/menu`, { method: 'GET' }));
}

export async function saveAdminFamilyMenu(familyId, items) {
  return request(`/api/admin/families/${familyId}/menu`, {
    method: 'PUT', body: JSON.stringify({ items }), headers: { 'Content-Type': 'application/json' }
  });
}

export async function adjustAdminFamilyMemberBalance(familyId, memberId, payload) {
  return request(`/api/admin/families/${familyId}/members/${memberId}/wallet/adjust`, {
    method: 'POST', body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' }
  });
}
