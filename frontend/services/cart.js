const { unwrapData } = require('./_shared');

function createCartService({ request }) {
  return {
    async getCart() {
      return unwrapData(await request('/api/family/cart', { method: 'GET' }));
    },
    async setItemQuantity(payload) {
      return unwrapData(await request('/api/family/cart/items', { method: 'PUT', data: payload }));
    },
    async addItem(payload) {
      return this.setItemQuantity(payload);
    },
    async updateItem(_itemId, payload) {
      return this.setItemQuantity(payload);
    },
    async deleteItem(_itemId, payload) {
      return this.setItemQuantity({ ...payload, quantity: 0 });
    },
    async updateExpectedMealTime(payload) {
      return unwrapData(await request('/api/family/cart/expected-meal-time', {
        method: 'PUT', data: payload
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
