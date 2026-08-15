import { getApiBaseUrl, getStoredSession } from './http';
import { joinApiUrl } from './admin-api-client';

export async function uploadDishImage(file) {
  const session = getStoredSession();
  const formData = new FormData();
  formData.append('file', file);

  const response = await window.fetch(joinApiUrl(getApiBaseUrl(), '/api/files/images'), {
    method: 'POST',
    headers: {
      Authorization: session?.accessToken ? `Bearer ${session.accessToken}` : '',
      'X-User-Id': session?.userId ?? '',
      'X-Merchant-Id': session?.merchantId ?? '',
      'X-Family-Id': session?.familyId ?? '',
      'X-Member-Id': session?.memberId ?? '',
      'X-Role-Template': session?.roleTemplate ?? 'merchant_admin',
      'X-Backend-Roles': Array.isArray(session?.backendRoles) ? session.backendRoles.join(',') : '',
      'X-Merchant-Admin-Scopes': Array.isArray(session?.merchantAdminScopes) ? session.merchantAdminScopes.join(',') : ''
    },
    body: formData
  });
  const payload = await response.json();

  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || '图片上传失败');
  }

  return payload.data;
}
