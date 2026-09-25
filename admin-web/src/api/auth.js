
import { clearStoredSession, requestWithoutSession, setStoredSession } from './http';
import { isPlatformAdminIdentity } from '../config';

export function isMerchantAdminSession(session) {
  if (!session) return false;
  const actor = session.actor || {};
  const roles = [
    session.roleTemplate, actor.roleTemplate, session.role, actor.role,
    ...(session.backendRoles || []), ...(actor.backendRoles || []),
    ...(session.roles || []), ...(actor.roles || [])
  ].map(value => String(value || '').toLowerCase());
  const scopes = [...(session.merchantAdminScopes || []), ...(actor.merchantAdminScopes || [])]
    .map(value => String(value).toLowerCase());
  return roles.includes('merchant_admin') || scopes.includes('merchant') || scopes.includes('merchant_admin');
}

export function isPlatformAdminSession(session) {
  return isPlatformAdminIdentity(session);
}

export async function loginWithUserAccount(payload = {}) {
  const session = await requestWithoutSession('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({
      username: payload.username || '',
      password: payload.password || ''
    })
  });

  return setStoredSession({
    ...session,
    loginMode: 'api'
  });
}

export async function loginWithAdminAccount(payload = {}) {
  const session = await requestWithoutSession('/api/auth/admin/login', {
    method: 'POST',
    body: JSON.stringify({
      username: payload.username || '',
      password: payload.password || ''
    })
  });

  if (!isMerchantAdminSession(session) && !isPlatformAdminSession(session)) {
    const error = new Error('当前账号没有商户后台权限');
    error.code = 40301;
    error.data = session;
    throw error;
  }

  return setStoredSession({
    ...session,
    loginMode: 'api'
  });
}

export function logoutAdmin() {
  clearStoredSession();
}
