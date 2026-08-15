import { defineStore } from 'pinia';
import { loginWithAdminAccount, loginWithUserAccount, logoutAdmin, isPlatformAdminSession } from '../api/auth';
import {
  getApiBaseUrl,
  getStoredSession,
  hasStoredSession
} from '../api/http';

export const useAuthStore = defineStore('admin-auth', {
  state: () => ({
    session: getStoredSession(),
    apiBaseUrl: getApiBaseUrl(),
    loading: false
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.session),
    merchantName: (state) => {
      if (!state.session) return '食光知味';
      if (isPlatformAdminSession(state.session)) return '平台管理后台';
      return state.session.roleTemplate === 'merchant_admin' ? '商户后台' : '后台';
    },
    isPlatformAdmin: (state) => isPlatformAdminSession(state.session)
  },
  actions: {
    hydrate() {
      this.session = getStoredSession();
      this.apiBaseUrl = getApiBaseUrl();
    },
    async login(payload = {}) {
      this.loading = true;
      try {
        this.session = await loginWithAdminAccount(payload);
        this.apiBaseUrl = getApiBaseUrl();
        return this.session;
      } finally {
        this.loading = false;
      }
    },
    async loginUser(payload = {}) {
      this.loading = true;
      try {
        this.session = await loginWithUserAccount(payload);
        this.apiBaseUrl = getApiBaseUrl();
        return this.session;
      } finally {
        this.loading = false;
      }
    },
    logout() {
      logoutAdmin();
      this.session = null;
    },
    hasSession() {
      return hasStoredSession();
    }
  }
});
