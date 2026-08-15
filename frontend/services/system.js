const { unwrapData } = require('./_shared');
function createSystemService({ request }) {
  return { getPublicSettings: async () => unwrapData(await request('/api/public/system-settings', { method: 'GET' })) };
}
module.exports = { createSystemService };
