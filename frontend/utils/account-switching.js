function isPlatformAdministrator(context) {
  if (context && Array.isArray(context.permissionCodes)) {
    return context.permissionCodes.includes('PLATFORM_ADMIN');
  }
  return (context && context.platformRoles || [])
    .some((role) => String(role).toLowerCase() === 'platform_admin');
}

function isUnauthorized(error) {
  return Boolean(error && (error.code === 40101 || error.statusCode === 401 || error.status === 401));
}

function modesFromContext(context) {
  if (context && Array.isArray(context.availableModes)) {
    return Array.from(new Set(context.availableModes
      .map((mode) => String(mode).toLowerCase())
      .filter((mode) => mode === 'family' || mode === 'merchant')));
  }
  const modes = [];
  const hasMerchant = Boolean(context && context.merchantId && context.merchantRole);
  if ((context && context.familyId) || !hasMerchant) modes.push('family');
  if (hasMerchant) modes.push('merchant');
  return modes;
}

function isPlatformOnlyContext(context, modes) {
  if (!isPlatformAdministrator(context)) return false;
  if (context && Array.isArray(context.availableModes)) return modes.length === 0;
  return !context.familyId && !(context.merchantId && context.merchantRole);
}

function destinationForSession(session) {
  if (session.activeMode === 'merchant') return '/pages/merchant/index';
  return session.familyId ? '/pages/family/home/index' : '/pages/account/profile/index';
}

function switchError(message, properties = {}) {
  return Object.assign(new Error(message), properties);
}

function sessionFromContext(target, context, activeMode, modes) {
  const merchantMode = activeMode === 'merchant';
  const hasMerchantPermission = Array.isArray(context.permissionCodes)
    ? context.permissionCodes.includes('MERCHANT_ADMIN')
    : Boolean(context.merchantId && context.merchantRole);
  return {
    ...target,
    ...context,
    memberId: context.familyId ? context.userId : null,
    roleTemplate: merchantMode
      ? 'merchant_admin'
      : String(context.familyRole || 'user').toLowerCase(),
    backendRoles: Array.from(new Set([
      ...(target.backendRoles || []),
      ...(context.platformRoles || []),
      ...(context.merchantRole ? [String(context.merchantRole).toLowerCase()] : [])
    ])),
    merchantAdminScopes: context.merchantId && hasMerchantPermission ? ['merchant'] : [],
    activeMode,
    availableModes: modes,
    requiresLogin: false,
    loginMode: 'api'
  };
}

async function refreshAccountIdentity(selection, dependencies) {
  const { sessionStore } = dependencies;
  const previous = sessionStore.getSession();
  const target = sessionStore.getAccount(selection.userId);
  if (!target || target.requiresLogin || !target.accessToken) {
    throw switchError('该账号需要重新登录', { requiresLogin: true, username: target && target.username });
  }
  sessionStore.setSession(target, { save: false, touch: false });
  try {
    const context = await dependencies.loadContext();
    const modes = modesFromContext(context);
    const platformOnly = isPlatformOnlyContext(context, modes);
    if (platformOnly) throw switchError('平台管理员请前往 Web 管理后台登录', { platformAdmin: true });
    const activeMode = modes.includes(target.activeMode) ? target.activeMode : modes[0];
    return sessionStore.setSession(sessionFromContext(target, context, activeMode, modes));
  } catch (error) {
    if (previous) sessionStore.setSession(previous, { save: false, touch: false });
    else sessionStore.clearSession();
    throw error;
  }
}

async function switchAccountIdentity(selection, dependencies) {
  const sessionStore = dependencies.sessionStore;
  const previous = sessionStore.getSession();
  const target = sessionStore.getAccount(selection.userId);
  if (!target || target.requiresLogin || !target.accessToken) {
    throw switchError('该账号需要重新登录', { requiresLogin: true, username: target && target.username });
  }
  sessionStore.setSession({ ...target, activeMode: selection.mode }, { save: false, touch: false });
  try {
    const context = await dependencies.loadContext();
    const modes = modesFromContext(context);
    const platformOnly = isPlatformOnlyContext(context, modes);
    if (platformOnly) {
      throw switchError('平台管理员请前往 Web 管理后台登录', { platformAdmin: true });
    }
    if (!modes.includes(selection.mode)) {
      throw switchError('该身份已被停用或撤销', { identityUnavailable: true });
    }

    const session = sessionStore.setSession(sessionFromContext(target, context, selection.mode, modes));
    return { session, destination: destinationForSession(session) };
  } catch (error) {
    if (error && error.platformAdmin) {
      sessionStore.removeAccount(selection.userId);
      if (previous && String(previous.userId) !== String(selection.userId)) {
        sessionStore.setSession(previous, { save: false, touch: false });
      } else {
        sessionStore.clearSession();
      }
    } else if (isUnauthorized(error)) {
      sessionStore.markRequiresLogin(selection.userId, true);
      if (previous && String(previous.userId) !== String(selection.userId)) {
        sessionStore.setSession(previous, { save: false, touch: false });
      }
      error.requiresLogin = true;
      error.username = target.username;
    } else if (previous) {
      sessionStore.setSession(previous, { save: false, touch: false });
    } else {
      sessionStore.clearSession();
    }
    throw error;
  }
}

module.exports = {
  destinationForSession,
  modesFromContext,
  refreshAccountIdentity,
  switchAccountIdentity
};
