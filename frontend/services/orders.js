const { unwrapData } = require('./_shared');

function createOrdersService({ request }) {
  return {
    async submitOrder(payload) {
      return unwrapData(await request('/api/family/orders', {
        method: 'POST',
        data: payload
      }));
    },
    async listOrders() {
      return unwrapData(await request('/api/family/orders', { method: 'GET' }));
    },
    async getOrderDetail(orderId) {
      return unwrapData(await request(`/api/family/orders/${orderId}`, { method: 'GET' }));
    },
    async updateOrder(orderId) {
      return unwrapData(await request(`/api/family/orders/${orderId}`, { method: 'PUT' }));
    },
    async cancelOrder(orderId, payload) {
      return unwrapData(await request(`/api/family/orders/${orderId}/cancel`, {
        method: 'POST',
        data: payload
      }));
    }
  };
}

module.exports = {
  createOrdersService
};
