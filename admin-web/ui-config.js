(function (root, factory) {
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = factory();
  } else {
    root.KitchenAdminUiConfig = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  const ADMIN_SESSION_KEY = 'family_kitchen_admin_session';

  const ADMIN_NAV_ITEMS = [
    { key: 'dashboard', label: '工作台', route: '/dashboard', icon: 'home' },
    { key: 'orders', label: '订单管理', route: '/orders', icon: 'receipt' },
    { key: 'dishes', label: '菜品管理', route: '/dishes', icon: 'dish' },
    { key: 'merchant-dish-reviews', label: '审核记录', route: '/merchant-dish-reviews', icon: 'clipboard' },
    { key: 'ingredients', label: '食材管理', route: '/ingredients', icon: 'leaf' },
    { key: 'families', label: '家庭管理', route: '/families', icon: 'users' },
    { key: 'platform-families', label: '平台家庭中心', route: '/platform-families', icon: 'building' },
    { key: 'platform-merchants', label: '商户管理', route: '/platform-merchants', icon: 'building' },
    { key: 'users', label: '用户管理', route: '/users', icon: 'users' },
    { key: 'dish-reviews', label: '菜品审核', route: '/dish-reviews', icon: 'clipboard' },
    { key: 'system-settings', label: '系统配置', route: '/system-settings', icon: 'settings' },
    { key: 'family-settings', label: '家庭设置', route: '/family-settings', icon: 'settings' },
    { key: 'family-applications', label: '申请审批', route: '/family-applications', icon: 'clipboard' },
    { key: 'menus', label: '家庭菜单', route: '/menus', icon: 'layout' },
    { key: 'purchases', label: '采购清单', route: '/purchases', icon: 'basket' },
    { key: 'notifications', label: '通知中心', route: '/notifications', icon: 'bell' }
  ];

  const ADMIN_STATUS_LABELS = {
    PENDING: '待确认',
    CONFIRMED: '已确认',
    PREPARING: '备菜中',
    READY: '待取餐/待配送',
    DONE: '已完成',
    CANCELLED: '已取消',
    REJECTED: '已拒单'
    ,APPROVED: '已通过'
    ,WITHDRAWN: '已撤回'
  };

  function getDefaultAdminRoute() {
    return ADMIN_NAV_ITEMS[0].route;
  }

  function identityRoles(session) {
    const actor = session && session.actor ? session.actor : {};
    return [].concat(
      (session && session.backendRoles) || [], actor.backendRoles || [],
      (session && session.roles) || [], actor.roles || []
    ).map(function (role) { return String(role).toLowerCase(); });
  }

  function isPlatformAdminIdentity(session) {
    const actor = session && session.actor ? session.actor : {};
    const template = String((session && session.roleTemplate) || actor.roleTemplate || '').toLowerCase();
    return template === 'platform_admin' || identityRoles(session).includes('platform_admin');
  }

  function getAdminLandingRoute(session) {
    return isPlatformAdminIdentity(session) ? '/platform-families' : getDefaultAdminRoute();
  }

  function findAdminNavItem(key) {
    return ADMIN_NAV_ITEMS.find(function (item) {
      return item.key === key;
    }) || null;
  }

  return {
    ADMIN_SESSION_KEY: ADMIN_SESSION_KEY,
    ADMIN_NAV_ITEMS: ADMIN_NAV_ITEMS,
    ADMIN_STATUS_LABELS: ADMIN_STATUS_LABELS,
    getDefaultAdminRoute: getDefaultAdminRoute,
    getAdminLandingRoute: getAdminLandingRoute,
    isPlatformAdminIdentity: isPlatformAdminIdentity,
    findAdminNavItem: findAdminNavItem
  };
});
