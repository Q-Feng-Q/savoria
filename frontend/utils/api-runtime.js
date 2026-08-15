const { createApiClient } = require('./api');
const { sessionStore: defaultSessionStore } = require('./session');
const { createAuthService } = require('../services/auth');
const { createFamilyService } = require('../services/family');
const { createCartService } = require('../services/cart');
const { createOrdersService } = require('../services/orders');
const { createNotificationsService } = require('../services/notifications');
const { createMerchantService } = require('../services/merchant');
const { createPurchaseService } = require('../services/purchase');
const { createFilesService } = require('../services/files');
const { createUserService } = require('../services/user');
const { createSystemService } = require('../services/system');

function resolveApp(app) {
  if (app) return app;
  if (typeof getApp === 'function') {
    try {
      return getApp();
    } catch (error) {
      return null;
    }
  }
  return null;
}

function resolveBaseUrl(app) {
  return (app && app.globalData && app.globalData.apiBaseUrl) || '';
}

function resolveSessionStore(app) {
  return (app && app.globalData && app.globalData.sessionStore) || defaultSessionStore;
}

function isApiSession(session) {
  return Boolean(session && session.loginMode === 'api');
}

function resolveNotificationScope(session) {
  return session && session.roleTemplate === 'merchant_admin' ? 'merchant' : 'account';
}

function createApiRuntime(options = {}) {
  const app = resolveApp(options.app);
  const baseUrl = options.baseUrl || resolveBaseUrl(app);
  const sessionStore = options.sessionStore || resolveSessionStore(app);
  const request = createApiClient({
    baseUrl,
    request: options.request,
    getSession: () => sessionStore.getSession(),
    getToken: () => sessionStore.getToken()
  });

  return {
    app,
    baseUrl,
    request,
    sessionStore,
    auth: createAuthService({ request }),
    user: createUserService({ request }),
    system: createSystemService({ request }),
    family: createFamilyService({ request }),
    cart: createCartService({ request }),
    orders: createOrdersService({ request }),
    notifications: createNotificationsService({ request }),
    merchant: createMerchantService({ request }),
    purchase: createPurchaseService({ request }),
    files: createFilesService({
      request,
      baseUrl,
      getSession: () => sessionStore.getSession(),
      upload: options.upload
    })
  };
}

module.exports = {
  createApiRuntime,
  isApiSession,
  resolveNotificationScope,
  resolveBaseUrl
};
