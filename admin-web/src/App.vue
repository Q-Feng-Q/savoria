<template>
  <RouterView />
  <AppToast :message="toast.message" :tone="toast.tone" :visible="toast.visible" />
  <ActionDialog />
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive } from 'vue';
import { RouterView, useRouter } from 'vue-router';
import AppToast from './components/AppToast.vue';
import ActionDialog from './components/ActionDialog.vue';
import { FEEDBACK_EVENT } from './utils/feedback';
import { SESSION_INVALID_EVENT } from './api/session-events';

const router = useRouter();
const toast = reactive({ message: '', tone: 'info', visible: false });
let toastTimer = 0;

function showToast(event) {
  window.clearTimeout(toastTimer);
  toast.message = event.detail?.message || '操作已完成';
  toast.tone = event.detail?.tone || 'info';
  toast.visible = true;
  toastTimer = window.setTimeout(() => { toast.visible = false; }, event.detail?.duration || 2800);
}

function handleSessionInvalid() {
  showToast({ detail: { message: '登录状态已失效，请重新登录', tone: 'warning', duration: 3600 } });
  router.replace({ path: '/login', query: { reason: 'expired' } });
}

onMounted(() => {
  window.addEventListener(FEEDBACK_EVENT, showToast);
  window.addEventListener(SESSION_INVALID_EVENT, handleSessionInvalid);
});

onBeforeUnmount(() => {
  window.clearTimeout(toastTimer);
  window.removeEventListener(FEEDBACK_EVENT, showToast);
  window.removeEventListener(SESSION_INVALID_EVENT, handleSessionInvalid);
});
</script>
