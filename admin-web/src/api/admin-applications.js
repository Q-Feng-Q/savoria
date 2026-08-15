import { request, normalizeArray } from './http';

export async function listFamilyApplications(status) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return normalizeArray(await request(`/api/admin/family-applications${query}`, { method: 'GET' }));
}

export async function approveFamilyApplication(applicationId, payload = {}) {
  return request(`/api/admin/family-applications/${applicationId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ remark: payload.remark || '' }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function rejectFamilyApplication(applicationId, payload = {}) {
  return request(`/api/admin/family-applications/${applicationId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ remark: payload.remark || '' }),
    headers: { 'Content-Type': 'application/json' }
  });
}

export async function adminCreateMember(payload = {}) {
  return request('/api/admin/members', {
    method: 'POST',
    body: JSON.stringify({
      familyId: payload.familyId,
      name: payload.name || '',
      mobile: payload.mobile || '',
      roleTemplate: payload.roleTemplate || 'member'
    }),
    headers: { 'Content-Type': 'application/json' }
  });
}
