const { createQueryString, unwrapData } = require('./_shared');

function createNotificationsService({ request }) {
  return {
    async getNotifications(params = {}) {
      return unwrapData(await request(`/api/notifications${createQueryString(params)}`, {
        method: 'GET'
      }));
    },
    async markRead(notificationId) {
      return unwrapData(await request(`/api/notifications/${notificationId}/read`, {
        method: 'POST'
      }));
    },
    async markAllRead(payload) {
      return unwrapData(await request('/api/notifications/read-all', {
        method: 'POST',
        data: payload
      }));
    }
  };
}

module.exports = {
  createNotificationsService
};
