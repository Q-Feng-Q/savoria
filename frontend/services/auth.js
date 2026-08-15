const { unwrapData } = require('./_shared');

function createAuthService({ request }) {
  return {
    async register(payload) {
      return unwrapData(await request('/api/auth/register', { method: 'POST', data: payload }));
    },
    async wechatLogin(payload) {
      return unwrapData(await request('/api/auth/wechat/login', {
        method: 'POST',
        data: payload
      }));
    },

    async portalLogin(payload) {
      return unwrapData(await request('/api/auth/login', {
        method: 'POST',
        data: payload
      }));
    },

    async userLogin(payload) {
      return unwrapData(await request('/api/auth/login', {
        method: 'POST',
        data: payload
      }));
    },

    async adminLogin(payload) {
      return unwrapData(await request('/api/auth/admin/login', {
        method: 'POST',
        data: payload
      }));
    },
    async sendPasswordResetCode(payload) {
      return unwrapData(await request('/api/auth/password/reset-code', { method: 'POST', data: payload }));
    },
    async resetPassword(payload) {
      return unwrapData(await request('/api/auth/password/reset', { method: 'POST', data: payload }));
    },
    async logout() {
      return unwrapData(await request('/api/auth/logout', { method: 'POST' }));
    },
    async logoutAll() {
      return unwrapData(await request('/api/auth/logout-all', { method: 'POST' }));
    }
  };
}

module.exports = {
  createAuthService
};
