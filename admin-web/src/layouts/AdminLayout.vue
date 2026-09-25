<template>
  <div class="admin-shell">
    <button v-if="drawerOpen" class="sidebar-backdrop" type="button" aria-label="关闭菜单" @click="drawerOpen = false" />
    <aside class="admin-sidebar" :class="{ 'is-open': drawerOpen }">
      <div class="brand-lockup">
        <BrandLogo variant="small" />
        <span class="brand-kicker">厨房管理手账</span>
        <strong class="brand-name">{{ branding.siteName }}</strong>
        <p>{{ authStore.isPlatformAdmin ? '平台工作台' : '照看每一餐的日常' }}</p>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.key"
          :to="item.tabs[0].path"
          class="sidebar-link"
          :class="{ 'is-active': activeWorkspace?.key === item.key }"
        >
          <img class="nav-story-icon" :src="storyIcons[item.icon]" alt="" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-foot">
        <span class="sidebar-caption brand-name">{{ branding.siteName }}</span>
        <strong>认真做好每一餐</strong>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <button class="mobile-menu-button" type="button" aria-label="打开菜单" @click="drawerOpen = true">☰</button>
        <div>
          <span class="page-eyebrow">今日经营 · {{ todayLabel }}</span>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="topbar-actions">
          <div class="merchant-chip">
            <span>{{ authStore.merchantName }}</span>
            <strong>{{ authStore.session?.nickname || authStore.session?.username || '已登录' }}</strong>
          </div>
          <button class="ghost-button" type="button" @click="handleLogout">退出登录</button>
        </div>
      </header>

      <main class="admin-content">
        <nav v-if="activeWorkspace && activeWorkspace.tabs.length > 1" class="workspace-tabs" aria-label="工作区页面">
          <RouterLink v-for="tab in activeWorkspace.tabs" :key="tab.path" :to="tab.path" :class="{ 'is-current': route.path === tab.path }" :aria-current="route.path === tab.path ? 'page' : undefined">{{ tab.label }}</RouterLink>
        </nav>
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { visibleWorkspaces, workspaceForPath } from '../workspaces';
import { useAuthStore } from '../stores/auth';
import BrandLogo from '../components/BrandLogo.vue';
import { branding } from '../stores/branding';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const navItems = computed(() => visibleWorkspaces(authStore.isPlatformAdmin, Boolean(authStore.session?.merchantId || authStore.session?.actor?.merchantId) || !authStore.isPlatformAdmin));
const activeWorkspace = computed(() => workspaceForPath(route.path, navItems.value));
const iconFiles = import.meta.glob('../../../frontend/assets/ui/story/*-green.png', { eager: true, query: '?url', import: 'default' });
const storyIcons = Object.fromEntries(Object.entries(iconFiles).map(([path, url]) => [path.split('/').pop().replace('-green.png', ''), url]));
const drawerOpen = ref(false);
const navIcons = { home: '⌂', receipt: '单', dish: '味', leaf: '叶', users: '家', building: '全', settings: '设', clipboard: '审', layout: '菜', basket: '采', bell: '铃' };
const todayLabel = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' }).format(new Date());

const currentTitle = computed(() => activeWorkspace.value?.label || route.meta.title || `${branding.siteName}商户后台`);
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
<style scoped>.brand-name { overflow-wrap: anywhere; max-width: 100%; }</style>
