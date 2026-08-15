
import { clearStoredSession, requestWithoutSession, setStoredSession } from './http';
import { isPlatformAdminIdentity } from '../config';

export function isMerchantAdminSession(session) {
  if (!session) return false;

  const actor = session.actor || {};

  const hasMerchantRole =
    session.roleTemplate === 'merchant_admin'
    || actor.roleTemplate === 'merchant_admin'
    || session.role === 'merchant_admin'
    || actor.role === 'merchant_admin'
    || (session.backendRoles || []).includes('merchant_admin')
    || (actor.backendRoles || []).includes('merchant_admin')
    || (session.roles || []).includes('merchant_admin')
    || (actor.roles || []).includes('merchant_admin')
    || (session.merchantAdminScopes || []).includes('merchant')
    || (actor.merchantAdminScopes || []).includes('merchant');

  if (hasMerchantRole) {
    return true;
  }

  if (session.merchantId || actor.merchantId) {
    return true;
  }

  return false;
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
