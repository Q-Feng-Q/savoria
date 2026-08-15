const { unwrapData } = require('./_shared');

function createUserService({ request }) {
  const call = async (path, method, data) => unwrapData(await request(path, {
    method,
    ...(data === undefined ? {} : { data })
  }));
  return {
    getContext: () => call('/api/users/me/context', 'GET'),
    getProfile: () => call('/api/users/me', 'GET'),
    updateProfile: (data) => call('/api/users/me', 'PUT', data),
    changeUsername: (data) => call('/api/users/me/username', 'PUT', data),
    changePassword: (data) => call('/api/users/me/password', 'PUT', data),
    sendEmailCode: (data) => call('/api/users/me/email/code', 'POST', data),
    bindEmail: (data) => call('/api/users/me/email', 'PUT', data),
    bindWechat: (data) => call('/api/users/me/wechat', 'POST', data),
    unbindWechat: () => call('/api/users/me/wechat', 'DELETE'),
    requestCancellation: (data) => call('/api/users/me/cancellation', 'POST', data),
    cancelCancellation: () => call('/api/users/me/cancellation', 'DELETE')
  };
}

module.exports = { createUserService };
