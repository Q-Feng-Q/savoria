function resolveLoginMethod(accountType) {
  return accountType === 'merchant' ? 'adminLogin' : 'userLogin';
}

function isMerchantSession(session) {
  if (!session) return false;
  if (Array.isArray(session.permissionCodes)) {
    return session.permissionCodes.includes('MERCHANT_ADMIN') && Boolean(session.merchantId);
  }
  return session.roleTemplate === 'merchant_admin'
    || (session.backendRoles || []).includes('merchant_admin')
    || (session.merchantAdminScopes || []).includes('merchant');
}

function evaluateLoginSession(session, accountType) {
  if (!session) return { allowed: false, destination: '', message: '登录响应缺少账号信息，请联系管理员' };
  const backendRoles = session.backendRoles || [];
  const isPlatformAdmin = Array.isArray(session.permissionCodes)
    ? session.permissionCodes.includes('PLATFORM_ADMIN')
    : session.roleTemplate === 'platform_admin'
      || backendRoles.includes('platform_admin') || backendRoles.includes('PLATFORM_ADMIN');
  if (isPlatformAdmin && !session.merchantId) return { allowed: false, destination: '', message: '平台管理员请前往 Web 管理后台登录' };
  const resolvedType = accountType || (isMerchantSession(session) ? 'merchant' : 'family');
  if (resolvedType === 'merchant') {
    if (isMerchantSession(session) && session.merchantId) return { allowed: true, destination: 'merchant', message: '' };
    return { allowed: false, destination: '', message: '商户账号尚未绑定商户，请联系平台管理员' };
  }
  if (session.familyId && session.memberId) return { allowed: true, destination: 'family', message: '' };
  if (session.memberId) return { allowed: true, destination: 'personal', message: '' };
  return { allowed: false, destination: '', message: '当前账号不是可用的普通用户账号' };
}

module.exports = { evaluateLoginSession, resolveLoginMethod };
