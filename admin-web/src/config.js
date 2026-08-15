import '../ui-config.js';

const config = globalThis.KitchenAdminUiConfig || {};

export const ADMIN_SESSION_KEY = config.ADMIN_SESSION_KEY || 'family_kitchen_admin_session';
export const ADMIN_NAV_ITEMS = config.ADMIN_NAV_ITEMS || [];
export const ADMIN_STATUS_LABELS = config.ADMIN_STATUS_LABELS || {};
export const getDefaultAdminRoute = config.getDefaultAdminRoute || (() => '/dashboard');
export const getAdminLandingRoute = config.getAdminLandingRoute || getDefaultAdminRoute;
export const isPlatformAdminIdentity = config.isPlatformAdminIdentity || (() => false);
export const findAdminNavItem = config.findAdminNavItem || (() => null);
