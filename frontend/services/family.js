const { createQueryString, unwrapData } = require('./_shared');

function createFamilyService({ request }) {
  return {
    async getOnboarding() {
      return unwrapData(await request('/api/family/onboarding', { method: 'GET' }));
    },
    async applyFamily(payload) {
      return unwrapData(await request('/api/family/apply', { method: 'POST', data: payload }));
    },
    async joinFamily(payload) {
      return unwrapData(await request('/api/family/join', { method: 'POST', data: payload }));
    },
    async exitFamily() {
      return unwrapData(await request('/api/family/exit', { method: 'POST' }));
    },
    async generateInvitation() {
      return unwrapData(await request('/api/family/invite', { method: 'POST' }));
    },
    async getHome() {
      return unwrapData(await request('/api/family/home', { method: 'GET' }));
    },
    async getMealSlots() {
      return unwrapData(await request('/api/family/meal-slots', { method: 'GET' }));
    },
    async getMenuItems(params = {}) {
      return unwrapData(await request(`/api/family/menu${createQueryString(params)}`, { method: 'GET' }));
    },
    async getDishDetail(dishId) {
      return unwrapData(await request(`/api/family/menu/${dishId}`, { method: 'GET' }));
    },
    async getAddresses() {
      return unwrapData(await request('/api/family/addresses', { method: 'GET' }));
    },
    async createAddress(payload) {
      return unwrapData(await request('/api/family/addresses', { method: 'POST', data: payload }));
    },
    async updateAddress(addressId, payload) {
      return unwrapData(await request(`/api/family/addresses/${addressId}`, { method: 'PUT', data: payload }));
    },
    async deleteAddress(addressId) {
      return unwrapData(await request(`/api/family/addresses/${addressId}`, { method: 'DELETE' }));
    },
    async setDefaultAddress(addressId) {
      return unwrapData(await request(`/api/family/addresses/${addressId}/default`, { method: 'POST' }));
    },
    async getWalletLedgers() {
      return unwrapData(await request('/api/family/me/wallet/ledgers', { method: 'GET' }));
    },
    async directInvite(payload) {
      return unwrapData(await request('/api/family/invitations/direct', { method: 'POST', data: payload }));
    },
    async getMyInvitations() {
      return unwrapData(await request('/api/family/invitations/me', { method: 'GET' }));
    },
    async acceptInvitation(id) {
      return unwrapData(await request(`/api/family/invitations/${id}/accept`, { method: 'POST' }));
    },
    async rejectInvitation(id, payload) {
      return unwrapData(await request(`/api/family/invitations/${id}/reject`, { method: 'POST', data: payload }));
    },
    async getJoinApplications() {
      return unwrapData(await request('/api/family/join-applications', { method: 'GET' }));
    },
    async approveJoinApplication(id) {
      return unwrapData(await request(`/api/family/join-applications/${id}/approve`, { method: 'POST' }));
    },
    async rejectJoinApplication(id, payload) {
      return unwrapData(await request(`/api/family/join-applications/${id}/reject`, { method: 'POST', data: payload }));
    },
    async transferOwner(payload) {
      return unwrapData(await request('/api/family/owner', { method: 'PUT', data: payload }));
    },
    async dissolveFamily() {
      return unwrapData(await request('/api/family/current', { method: 'DELETE' }));
    }
  };
}

module.exports = {
  createFamilyService
};
