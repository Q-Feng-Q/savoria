const { unwrapData } = require('./_shared');

function createMerchantService({ request }) {
  return {
    async getDishes() {
      return unwrapData(await request('/api/merchant/dishes', { method: 'GET' }));
    },
    async getDishDetail(dishId) {
      return unwrapData(await request(`/api/merchant/dishes/${dishId}`, { method: 'GET' }));
    },
    async createDish(payload) {
      return unwrapData(await request('/api/merchant/dishes', { method: 'POST', data: payload }));
    },
    async updateDish(dishId, payload) {
      return unwrapData(await request(`/api/merchant/dishes/${dishId}`, { method: 'PUT', data: payload }));
    },
    async updateDishStatus(dishId, payload) {
      return unwrapData(await request(`/api/merchant/dishes/${dishId}/status`, { method: 'PUT', data: payload }));
    },
    async updateCookingSteps(dishId, payload) {
      return unwrapData(await request(`/api/merchant/dishes/${dishId}/cooking-steps`, { method: 'PUT', data: payload }));
    },
    async getDishCategories() {
      return unwrapData(await request('/api/merchant/dish-categories', { method: 'GET' }));
    },
    async getDishTemplateCategories() {
      return unwrapData(await request('/api/merchant/dish-template-categories', { method: 'GET' }));
    },
    async getDishTemplates(query = {}) {
      const search = Object.keys(query)
        .filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== '')
        .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`)
        .join('&');
      return unwrapData(await request(`/api/merchant/dish-templates${search ? `?${search}` : ''}`, { method: 'GET' }));
    },
    async getDishTemplateDetail(templateId) {
      return unwrapData(await request(`/api/merchant/dish-templates/${templateId}`, { method: 'GET' }));
    },
    async importDishTemplates(templateIds) {
      return unwrapData(await request('/api/merchant/dish-templates/import', {
        method: 'POST', data: { templateIds }
      }));
    },
    async importAllDishTemplates() {
      return unwrapData(await request('/api/merchant/dish-templates/import-all', { method: 'POST' }));
    },
    async getDishReviews() {
      return unwrapData(await request('/api/merchant/dish-reviews', { method: 'GET' }));
    },
    async getDishReviewDetail(reviewId) {
      return unwrapData(await request(`/api/merchant/dish-reviews/${reviewId}`, { method: 'GET' }));
    },
    async withdrawDishReview(reviewId) {
      return unwrapData(await request(`/api/merchant/dish-reviews/${reviewId}/withdraw`, { method: 'POST' }));
    },
    async createDishCategory(payload) {
      return unwrapData(await request('/api/merchant/dish-categories', { method: 'POST', data: payload }));
    },
    async updateDishCategory(categoryId, payload) {
      return unwrapData(await request(`/api/merchant/dish-categories/${categoryId}`, { method: 'PUT', data: payload }));
    },
    async deleteDishCategory(categoryId) {
      return unwrapData(await request(`/api/merchant/dish-categories/${categoryId}`, { method: 'DELETE' }));
    },
    async getOrders() {
      return unwrapData(await request('/api/merchant/orders', { method: 'GET' }));
    },
    async getOrderDetail(orderId) {
      return unwrapData(await request(`/api/merchant/orders/${orderId}`, { method: 'GET' }));
    },
    async getFamilies() {
      return unwrapData(await request('/api/merchant/families', { method: 'GET' }));
    },
    async getFamilyDetail(familyId) {
      return unwrapData(await request(`/api/merchant/families/${familyId}`, { method: 'GET' }));
    },
    async updateFamilyProfile(familyId, payload) {
      return unwrapData(await request(`/api/merchant/families/${familyId}/profile`, { method: 'PUT', data: payload }));
    },
    async updateFamilyDeliveryPolicy(familyId, payload) {
      return unwrapData(await request(`/api/merchant/families/${familyId}/delivery-policy`, { method: 'PUT', data: payload }));
    },
    async getIngredients() {
      return unwrapData(await request('/api/merchant/ingredients', { method: 'GET' }));
    },
    async createIngredient(payload) {
      return unwrapData(await request('/api/merchant/ingredients', { method: 'POST', data: payload }));
    },
    async updateIngredient(ingredientId, payload) {
      return unwrapData(await request(`/api/merchant/ingredients/${ingredientId}`, { method: 'PUT', data: payload }));
    },
    async deleteIngredient(ingredientId) {
      return unwrapData(await request(`/api/merchant/ingredients/${ingredientId}`, { method: 'DELETE' }));
    },
    async confirmOrder(orderId) {
      return unwrapData(await request(`/api/merchant/orders/${orderId}/confirm`, { method: 'POST' }));
    },
    async rejectOrder(orderId) {
      return unwrapData(await request(`/api/merchant/orders/${orderId}/reject`, { method: 'POST' }));
    },
    async cancelOrder(orderId, payload) {
      return unwrapData(await request(`/api/merchant/orders/${orderId}/cancel`, { method: 'POST', data: payload }));
    },
    async updateDeliveryFee(orderId, payload) {
      return unwrapData(await request(`/api/merchant/orders/${orderId}/delivery-fee`, { method: 'POST', data: payload }));
    },
    async advanceOrderStatus(orderId, payload) {
      return unwrapData(await request(`/api/merchant/orders/${orderId}/status`, { method: 'POST', data: payload }));
    },
    async getFamilyMenu(familyId) {
      return unwrapData(await request(`/api/merchant/families/${familyId}/menu`, { method: 'GET' }));
    },
    async saveFamilyMenu(familyId, payload) {
      return unwrapData(await request(`/api/merchant/families/${familyId}/menu`, { method: 'PUT', data: payload }));
    },
    async copyFamilyMenu(familyId, payload) {
      return unwrapData(await request(`/api/merchant/families/${familyId}/menu/copy`, { method: 'POST', data: payload }));
    },
    async getMemberWalletLedgers(memberId) {
      return unwrapData(await request(`/api/merchant/members/${memberId}/wallet/ledgers`, { method: 'GET' }));
    },
    async adjustMemberBalance(memberId, payload) {
      return unwrapData(await request(`/api/merchant/members/${memberId}/wallet/adjust`, { method: 'POST', data: payload }));
    }
  };
}

module.exports = {
  createMerchantService
};
