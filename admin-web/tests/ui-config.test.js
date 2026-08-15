const test = require('node:test');
const assert = require('node:assert/strict');

const {
  ADMIN_SESSION_KEY,
  ADMIN_NAV_ITEMS,
  ADMIN_STATUS_LABELS,
  getDefaultAdminRoute,
  findAdminNavItem
} = require('../ui-config');
const { getAdminLandingRoute, isPlatformAdminIdentity } = require('../ui-config');

test('admin ui config exposes a stable session key and default route', () => {
  assert.equal(ADMIN_SESSION_KEY, 'family_kitchen_admin_session');
  assert.equal(getDefaultAdminRoute(), '/dashboard');
});

test('platform administrators land on a platform workspace instead of merchant dashboard', () => {
  assert.equal(getAdminLandingRoute({ backendRoles: ['platform_admin'] }), '/platform-families');
  assert.equal(getAdminLandingRoute({ backendRoles: ['PLATFORM_ADMIN'] }), '/platform-families');
  assert.equal(getAdminLandingRoute({ roleTemplate: 'merchant_admin', merchantId: 2 }), '/dashboard');
});

test('platform administrator identity accepts backend role casing', () => {
  assert.equal(isPlatformAdminIdentity({ backendRoles: ['PLATFORM_ADMIN'] }), true);
  assert.equal(isPlatformAdminIdentity({ actor: { backendRoles: ['platform_admin'] } }), true);
  assert.equal(isPlatformAdminIdentity({ backendRoles: ['MERCHANT_ADMIN'] }), false);
});

test('admin ui config keeps all required merchant modules in navigation', () => {
  const keys = ADMIN_NAV_ITEMS.map((item) => item.key);

  assert.deepEqual(keys, [
    'dashboard',
    'orders',
    'dishes',
    'merchant-dish-reviews',
    'ingredients',
    'families',
    'platform-families',
    'platform-merchants',
    'users',
    'dish-reviews',
    'system-settings',
    'family-settings',
    'family-applications',
    'menus',
    'purchases',
    'notifications'
  ]);
  assert.equal(findAdminNavItem('orders').label, '订单管理');
});

test('admin ui config normalizes status labels for merchant pages', () => {
  assert.equal(ADMIN_STATUS_LABELS.PENDING, '待确认');
  assert.equal(ADMIN_STATUS_LABELS.PREPARING, '备菜中');
  assert.equal(ADMIN_STATUS_LABELS.CANCELLED, '已取消');
});
