<template>
  <div class="admin-shell">
    <button v-if="drawerOpen" class="sidebar-backdrop" type="button" aria-label="关闭菜单" @click="drawerOpen = false" />
    <aside class="admin-sidebar" :class="{ 'is-open': drawerOpen }">
      <div class="brand-lockup">
        <span class="brand-kicker">SHI GUANG ZHI WEI</span>
        <strong>食光知味</strong>
        <p>商户高频经营工作台</p>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.key"
          :to="item.route"
          class="sidebar-link"
          active-class="is-active"
        >
          <span class="nav-icon" aria-hidden="true">{{ navIcons[item.icon] || '·' }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-foot">
        <span class="sidebar-caption">当前接口</span>
        <strong>{{ authStore.apiBaseUrl }}</strong>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <button class="mobile-menu-button" type="button" aria-label="打开菜单" @click="drawerOpen = true">☰</button>
        <div>
          <span class="page-eyebrow">今日经营 · {{ todayLabel }}</span>
          <h1>{{ currentTitle }}</h1>
          <p>{{ currentSubtitle }}</p>
        </div>
        <div class="topbar-actions">
          <div class="merchant-chip">
            <span>{{ authStore.merchantName }}</span>
            <strong>商户 #{{ authStore.session?.merchantId || '-' }}</strong>
          </div>
          <button class="ghost-button" type="button" @click="handleLogout">退出登录</button>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { ADMIN_NAV_ITEMS } from '../config';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const platformOnly = new Set(['platform-families', 'platform-merchants', 'users', 'dish-reviews', 'system-settings']);
const navItems = computed(() => ADMIN_NAV_ITEMS.filter((item) => !platformOnly.has(item.key) || authStore.isPlatformAdmin));
const drawerOpen = ref(false);
const navIcons = { home: '⌂', receipt: '单', dish: '味', leaf: '叶', users: '家', building: '全', settings: '设', clipboard: '审', layout: '菜', basket: '采', bell: '铃' };
const todayLabel = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' }).format(new Date());

const currentTitle = computed(() => route.meta.title || '食光知味商户后台');
const currentSubtitle = computed(() => route.meta.subtitle || '商户高频经营工作台');

onMounted(() => {
  authStore.hydrate();
});

watch(() => route.fullPath, () => {
  drawerOpen.value = false;
});

function handleLogout() {
  authStore.logout();
  router.replace('/login');
}
</script>
