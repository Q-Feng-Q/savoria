<template>
  <div class="login-shell">
    <section class="login-panel">
      <div class="login-copy">
        <span class="login-kicker">SHI GUANG ZHI WEI</span>
        <h1>食光知味商户后台</h1>
        <p>登录页只保留账号密码输入。接口地址统一从配置文件读取，登录后进入订单、菜品、家庭和采购管理。</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <label class="form-field">
          <span>账号</span>
          <input v-model.trim="form.username" type="text" placeholder="请输入商户后台账号" />
        </label>

        <label class="form-field">
          <span>密码</span>
          <input v-model="form.password" type="password" placeholder="请输入密码" />
        </label>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <button class="primary-button login-button" type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? '登录中...' : '进入后台' }}
        </button>
      </form>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getAdminLandingRoute } from '../../config';
import { useAuthStore } from '../../stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const errorMessage = ref('');
const form = reactive({
  username: '',
  password: ''
});

async function handleLogin() {
  errorMessage.value = '';

  if (!form.username || !form.password) {
    errorMessage.value = '请输入账号和密码';
    return;
  }

  try {
    const session = await authStore.login(form);
    router.replace(getAdminLandingRoute(session));
  } catch (error) {
    errorMessage.value = error?.message || '登录失败，请检查配置文件、账号或密码';
  }
}
</script>
