const { createQueryString, unwrapData } = require('./_shared');

function createCartService({ request }) {
  return {
    async getCart(params) {
      return unwrapData(await request(`/api/family/cart${createQueryString(params)}`, {
        method: 'GET',
        data: params
      }));
    },
    async addItem(payload) {
      return unwrapData(await request('/api/family/cart/items', {
        method: 'POST',
        data: payload
      }));
    },
    async updateItem(itemId, payload) {
      return unwrapData(await request(`/api/family/cart/items/${itemId}`, {
        method: 'PUT',
        data: payload
      }));
    },
    async deleteItem(itemId) {
      return unwrapData(await request(`/api/family/cart/items/${itemId}`, {
        method: 'DELETE'
      }));
    },
    async updateRemark(payload) {
      return unwrapData(await request('/api/family/cart/remark', {
        method: 'PUT',
        data: payload
      }));
    }
  };
}

module.exports = {
  createCartService
};
