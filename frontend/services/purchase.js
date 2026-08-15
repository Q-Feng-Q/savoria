const { createQueryString, unwrapData } = require('./_shared');

function createPurchaseService({ request }) {
  return {
    async getSummary(params = {}) {
      return unwrapData(await request(`/api/merchant/purchases/summary${createQueryString(params)}`, { method: 'GET' }));
    },
    async getTempItems(params = {}) {
      return unwrapData(await request(`/api/merchant/purchases/temp-items${createQueryString(params)}`, { method: 'GET' }));
    },
    async getByFamily(params = {}) {
      return unwrapData(await request(`/api/merchant/purchases/by-family${createQueryString(params)}`, { method: 'GET' }));
    },
    async toggleChecked(itemId, payload) {
      return unwrapData(await request(`/api/merchant/purchases/items/${itemId}/checked`, { method: 'POST', data: payload }));
    },
    async createTempItem(payload) {
      return unwrapData(await request('/api/merchant/purchases/temp-items', { method: 'POST', data: payload }));
    },
    async deleteTempItem(itemId) {
      return unwrapData(await request(`/api/merchant/purchases/temp-items/${itemId}`, { method: 'DELETE' }));
    },
    async getCopyText(params = {}) {
      return unwrapData(await request(`/api/merchant/purchases/copy-text${createQueryString(params)}`, { method: 'GET' }));
    }
  };
}

module.exports = {
  createPurchaseService
};
