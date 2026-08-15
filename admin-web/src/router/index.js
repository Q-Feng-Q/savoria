import { createRouter, createWebHistory } from 'vue-router';
import { getAdminLandingRoute, getDefaultAdminRoute } from '../config';
import { getStoredSession, hasStoredSession } from '../api/http';
import AdminLayout from '../layouts/AdminLayout.vue';
import LoginView from '../views/auth/LoginView.vue';
import DashboardView from '../views/merchant/DashboardView.vue';
import OrdersView from '../views/merchant/OrdersView.vue';
import DishesView from '../views/merchant/DishesView.vue';
import MerchantDishReviewsView from '../views/merchant/DishReviewsView.vue';
import DishTemplatesView from '../views/merchant/DishTemplatesView.vue';
import IngredientsView from '../views/merchant/IngredientsView.vue';
import FamiliesView from '../views/merchant/FamiliesView.vue';
import MenusView from '../views/merchant/MenusView.vue';
import PurchasesView from '../views/merchant/PurchasesView.vue';
import NotificationsView from '../views/merchant/NotificationsView.vue';
import FamilyApplicationsView from '../views/platform/FamilyApplicationsView.vue';
import FamilySettingsView from '../views/merchant/FamilySettingsView.vue';
import NotFoundView from '../views/shared/NotFoundView.vue';
import PlatformFamiliesView from '../views/platform/PlatformFamiliesView.vue';
import DishReviewsView from '../views/platform/DishReviewsView.vue';
import SystemSettingsView from '../views/platform/SystemSettingsView.vue';
import UsersView from '../views/platform/UsersView.vue';
import MerchantsView from '../views/platform/MerchantsView.vue';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      public: true,
      title: '后台登录',
      subtitle: '接入商户真实接口'
    }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: () => getAdminLandingRoute(getStoredSession()),
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: {
          title: '工作台',
          subtitle: '订单、采购和通知的实时概览'
        }
      },
      {
        path: 'orders',
        name: 'orders',
        component: OrdersView,
        meta: {
          title: '订单管理',
          subtitle: '处理确认、拒单、配送费和状态推进'
        }
      },
      {
        path: 'dishes',
        name: 'dishes',
        component: DishesView,
        meta: {
          title: '菜品管理',
          subtitle: '维护菜品、分类、制作流程和图片'
        }
      },
      {
        path: 'merchant-dish-reviews',
        name: 'merchant-dish-reviews',
        component: MerchantDishReviewsView,
        meta: {
          title: '审核记录',
          subtitle: '跟踪本商户菜品审核进度并撤回待审核提交'
        }
      },
      {
        path: 'dish-templates',
        name: 'dish-templates',
        component: DishTemplatesView,
        meta: {
          title: '模板菜市场',
          subtitle: '选择平台家常菜模板并导入商户菜品库'
        }
      },
      {
        path: 'ingredients',
        name: 'ingredients',
        component: IngredientsView,
        meta: {
          title: '食材管理',
          subtitle: '维护食材字典与引用状态'
        }
      },
      {
        path: 'families',
        name: 'families',
        component: FamiliesView,
        meta: {
          title: '家庭管理',
          subtitle: '查看家庭资料、配送策略和成员余额'
        }
      },
      {
        path: 'family-settings',
        name: 'family-settings',
        component: FamilySettingsView,
        meta: {
          title: '家庭设置',
          subtitle: '申请创建家庭、邀请码、加入家庭'
        }
      },
      {
        path: 'platform-families',
        name: 'platform-families',
        component: PlatformFamiliesView,
        meta: {
          title: '平台家庭中心',
          subtitle: '跨商户维护家庭、成员、地址与配送策略'
        }
      },
      {
        path: 'platform-merchants', name: 'platform-merchants', component: MerchantsView,
        meta: { title: '商户管理', subtitle: '维护私厨商户、负责人与私密邀请码', requiresPlatformAdmin: true }
      },
      {
        path: 'users', name: 'users', component: UsersView,
        meta: { title: '用户管理', subtitle: '维护平台注册账户、状态和平台角色', requiresPlatformAdmin: true }
      },
      {
        path: 'dish-reviews', name: 'dish-reviews', component: DishReviewsView,
        meta: { title: '菜品审核', subtitle: '审核商户提交的新菜与菜品变更', requiresPlatformAdmin: true }
      },
      {
        path: 'system-settings', name: 'system-settings', component: SystemSettingsView,
        meta: { title: '系统配置', subtitle: '维护站点信息、审核开关与维护模式', requiresPlatformAdmin: true }
      },
      {
        path: 'family-applications',
        name: 'family-applications',
        component: FamilyApplicationsView,
        meta: {
          title: '家庭申请审批',
          subtitle: '审批家庭创建申请、后台添加用户'
        }
      },
      {
        path: 'menus',
        name: 'menus',
        component: MenusView,
        meta: {
          title: '家庭菜单配置',
          subtitle: '按家庭启停菜品并覆盖家庭专属价格'
        }
      },
      {
        path: 'purchases',
        name: 'purchases',
        component: PurchasesView,
        meta: {
          title: '采购清单',
          subtitle: '查看汇总采购、家庭采购明细与临时项'
        }
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: NotificationsView,
        meta: {
          title: '通知中心',
          subtitle: '订单、采购和余额提醒统一处理'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: { public: true, title: '页面未找到' }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.name === 'login' && hasStoredSession()) {
      return getAdminLandingRoute(getStoredSession());
    }
    return true;
  }

  if (!hasStoredSession()) {
    return '/login';
  }
  if (to.meta.requiresPlatformAdmin) {
    const session = getStoredSession();
    const roles = session?.backendRoles || [];
    if (session?.roleTemplate !== 'platform_admin' && !roles.some((role) => String(role).toLowerCase() === 'platform_admin')) {
      return getDefaultAdminRoute();
    }
  }

  return true;
});

export default router;
