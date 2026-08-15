import { request, normalizeArray } from './http';

export async function applyFamily(payload = {}) {
  return request('/api/family/apply', {
    method: 'POST',
    body: JSON.stringify({
      familyName: payload.familyName || ''
      ,merchantId: payload.merchantId
    }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function generateInviteCode() {
  return request('/api/family/invite', {
    method: 'POST'
  });
}

export async function joinFamilyByCode(payload = {}) {
  return request('/api/family/join', {
    method: 'POST',
    body: JSON.stringify({
      code: payload.code || ''
    }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function createFamilyMember(payload = {}) {
  return request('/api/family/members', {
    method: 'POST',
    body: JSON.stringify({
      name: payload.name || '',
      mobile: payload.mobile || ''
    }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export const directInviteFamilyMember = (userIdentifier) => request('/api/family/invitations/direct', {
  method: 'POST', body: JSON.stringify({ userIdentifier }), headers: { 'Content-Type': 'application/json' }
});
export const listMyFamilyInvitations = async () => normalizeArray(await request('/api/family/invitations/me', { method: 'GET' }));
export const acceptFamilyInvitation = (id) => request(`/api/family/invitations/${id}/accept`, { method: 'POST' });
export const rejectFamilyInvitation = (id, reason = '') => request(`/api/family/invitations/${id}/reject`, {
  method: 'POST', body: JSON.stringify({ reason }), headers: { 'Content-Type': 'application/json' }
});
export const listFamilyJoinApplications = async () => normalizeArray(await request('/api/family/join-applications', { method: 'GET' }));
export const approveFamilyJoinApplication = (id) => request(`/api/family/join-applications/${id}/approve`, { method: 'POST' });
export const rejectFamilyJoinApplication = (id, reason = '') => request(`/api/family/join-applications/${id}/reject`, {
  method: 'POST', body: JSON.stringify({ reason }), headers: { 'Content-Type': 'application/json' }
});
export const transferFamilyOwner = (targetUserId) => request('/api/family/owner', {
  method: 'PUT', body: JSON.stringify({ targetUserId }), headers: { 'Content-Type': 'application/json' }
});
export const dissolveCurrentFamily = () => request('/api/family/current', { method: 'DELETE' });
