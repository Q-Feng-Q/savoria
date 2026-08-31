const pageOperations = require('./delivery-page-operations.json')
const serviceContracts = require('./delivery-service-contracts.json')

const page = (domain, options = {}) => ({
  domain,
  async: false,
  list: false,
  form: false,
  refresh: false,
  tenantScope: 'account',
  operations: [],
  ...options
})

const withOperations = (pages) => Object.freeze(Object.fromEntries(
  Object.entries(pages).map(([path, metadata]) => [path, Object.freeze({
    ...metadata,
    operations: Object.freeze(pageOperations[path] || [])
  })])
))

module.exports = Object.freeze({
  version: 1,
  baseline: Object.freeze({ frontendTests: 348, backendTests: 285, backendSkipped: 12 }),
  serviceModules: Object.freeze([
    '_shared.js', 'auth.js', 'cart.js', 'family.js', 'files.js', 'merchant.js',
    'notifications.js', 'orders.js', 'purchase.js', 'system.js', 'user.js'
  ]),
  backendRouteBoundary: Object.freeze({
    includePrefixes: Object.freeze([
      '/api/auth', '/api/users/me', '/api/family', '/api/merchant',
      '/api/notifications', '/api/files', '/api/public/system-settings'
    ]),
    exclusions: Object.freeze([
      Object.freeze({ route: '/api/admin/**', reason: 'admin-web is outside this delivery audit' }),
      Object.freeze({
        route: '/api/merchant/members/{memberId}/wallet/**',
        reason: 'retired personal-wallet compatibility endpoints are fenced and must not return to the mini program'
      })
    ])
  }),
  operations: Object.freeze(Object.fromEntries(serviceContracts.map((contract) => [
    contract.key,
    Object.freeze(contract)
  ]))),
  pages: withOperations({
    'pages/account/account-management/index': page('account', { async: true, list: true, refresh: true }),
    'pages/account/account-security/index': page('account', { async: true, form: true }),
    'pages/account/notifications/index': page('account', { async: true, list: true, refresh: true }),
    'pages/account/profile/index': page('account', { async: true, refresh: true }),
    'pages/account/profile-edit/index': page('account', { async: true, form: true }),
    'pages/auth/entry/index': page('auth', { async: true, form: true, tenantScope: 'public' }),
    'pages/auth/password-recovery/index': page('auth', { async: true, form: true, tenantScope: 'public' }),
    'pages/auth/register/index': page('auth', { async: true, form: true, tenantScope: 'public' }),
    'pages/family/address-edit/index': page('family', { async: true, form: true, tenantScope: 'family' }),
    'pages/family/addresses/index': page('family', { async: true, list: true, refresh: true, tenantScope: 'family' }),
    'pages/family/family-management/index': page('family', { async: true, list: true, form: true, refresh: true, tenantScope: 'family' }),
    'pages/family/family-start/index': page('family', { async: true, form: true }),
    'pages/family/home/index': page('family', { async: true, refresh: true, tenantScope: 'family' }),
    'pages/family/wallet/index': page('family', { async: true, form: true, refresh: true, tenantScope: 'family' }),
    'pages/family/wallet-ledger/index': page('family', { async: true, list: true, refresh: true, tenantScope: 'family' }),
    'pages/merchant/index': page('merchant', { async: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/dish-edit/index': page('merchant', { async: true, form: true, tenantScope: 'merchant' }),
    'pages/merchant/dish-reviews/index': page('merchant', { async: true, list: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/dish-template-change-detail/index': page('merchant', { async: true, form: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/dish-template-change-edit/index': page('merchant', { async: true, form: true, tenantScope: 'merchant' }),
    'pages/merchant/dish-template-changes/index': page('merchant', { async: true, list: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/dish-template-detail/index': page('merchant', { async: true, tenantScope: 'merchant' }),
    'pages/merchant/dish-templates/index': page('merchant', { async: true, list: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/family-menu/index': page('merchant', { async: true, list: true, form: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/ingredient-edit/index': page('merchant', { async: true, form: true, tenantScope: 'merchant' }),
    'pages/merchant/merchant-dishes/index': page('merchant', { async: true, list: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/merchant-families/index': page('merchant', { async: true, list: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/merchant-family-detail/index': page('merchant', { async: true, list: true, form: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/merchant-order-detail/index': page('merchant', { async: true, form: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/merchant-orders/index': page('merchant', { async: true, list: true, refresh: true, tenantScope: 'merchant' }),
    'pages/merchant/merchant-profile-edit/index': page('merchant', { async: true, form: true, tenantScope: 'merchant' }),
    'pages/merchant/purchase/index': page('merchant', { async: true, list: true, form: true, refresh: true, tenantScope: 'merchant' }),
    'pages/ordering/cart/index': page('ordering', { async: true, list: true, form: true, refresh: true, tenantScope: 'family' }),
    'pages/ordering/dish-detail/index': page('ordering', { async: true, refresh: true, tenantScope: 'family' }),
    'pages/ordering/menu/index': page('ordering', { async: true, list: true, refresh: true, tenantScope: 'family' }),
    'pages/ordering/order-detail/index': page('ordering', { async: true, refresh: true, tenantScope: 'family' }),
    'pages/ordering/orders/index': page('ordering', { async: true, list: true, refresh: true, tenantScope: 'family' })
  })
})
