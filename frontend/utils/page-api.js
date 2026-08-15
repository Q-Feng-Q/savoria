const { sessionStore } = require('./session');

function isMerchantSession(session) {
  if (!session) return false;
  if (Array.isArray(session.permissionCodes)) {
    const granted = session.permissionCodes.includes('MERCHANT_ADMIN') && Boolean(session.merchantId);
    return session.activeMode ? session.activeMode === 'merchant' && granted : granted;
  }
  if (session.activeMode) {
    return session.activeMode === 'merchant' && Boolean(session.merchantId);
  }
  return Boolean(session.merchantId) && (session.roleTemplate === 'merchant_admin'
    || (session.backendRoles || []).includes('merchant_admin')
    || (session.merchantAdminScopes || []).includes('merchant'));
}

function requireSession(options = {}) {
  const session = sessionStore.getSession();
  if (!session) {
    wx.reLaunch({ url: '/pages/auth/entry/index' });
    return null;
  }

  if (options.merchantOnly && !isMerchantSession(session)) {
    wx.showToast({ title: '当前身份无权限', icon: 'none' });
    wx.redirectTo({ url: '/pages/account/account-management/index' });
    return null;
  }

  return session;
}

function redirectBySession(session) {
  if (!session) {
    wx.reLaunch({ url: '/pages/auth/entry/index' });
    return;
  }

  if (isMerchantSession(session)) {
    wx.redirectTo({ url: '/pages/merchant/index' });
    return;
  }

  if (!session.familyId) {
    wx.switchTab({ url: '/pages/account/profile/index' });
    return;
  }

  wx.switchTab({ url: '/pages/family/home/index' });
}

function resolveApiErrorMessage(error, fallbackMessage = '请求失败，请稍后再试') {
  if (!error) return fallbackMessage;
  if (error.code === 40101) {
    sessionStore.clearSession();
    wx.reLaunch({ url: '/pages/auth/entry/index' });
    return '登录已失效，请重新进入';
  }
  return error.message || fallbackMessage;
}

function showApiError(error, fallbackMessage) {
  wx.showToast({
    title: resolveApiErrorMessage(error, fallbackMessage),
    icon: 'none'
  });
}

function resolveLoginErrorMessage(error) {
  return (error && error.message) || '账号或密码错误';
}

function showLoginError(error) {
  wx.showToast({ title: resolveLoginErrorMessage(error), icon: 'none' });
}

module.exports = {
  isMerchantSession,
  requireSession,
  redirectBySession,
  resolveLoginErrorMessage,
  resolveApiErrorMessage,
  showLoginError,
  showApiError
};
