<template>
  <div class="view-stack">
    <SectionCard title="通知中心" subtitle="统一查看商户端订单、采购和钱包提醒">
      <div class="toolbar-grid">
        <label class="form-field">
          <span>筛选</span>
          <select v-model="readStatus">
            <option value="all">全部</option>
            <option value="unread">未读</option>
            <option value="read">已读</option>
          </select>
        </label>
        <div class="toolbar-actions">
          <button class="ghost-button" type="button" @click="loadNotifications">刷新</button>
          <button class="primary-button" type="button" @click="markAllRead">全部已读</button>
        </div>
      </div>

      <div v-if="loading" class="table-empty">加载中...</div>
      <AppEmpty
        v-else-if="!notifications.length"
        title="暂无通知"
        description="当前筛选条件下没有通知，可以稍后再看。"
      />
      <div v-else class="notification-list">
        <article v-for="item in notifications" :key="item.notificationId" class="notification-row">
          <div>
            <div class="title-row">
              <strong>{{ item.title }}</strong>
              <span v-if="!item.read" class="dot-badge">未读</span>
            </div>
            <p>{{ item.content }}</p>
          </div>
          <div class="inline-actions">
            <span class="time-text">{{ item.createdAt }}</span>
            <button v-if="!item.read" class="text-button" type="button" @click="markRead(item.notificationId)">标记已读</button>
          </div>
        </article>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead
} from '../../api/notifications';

const loading = ref(false);
const readStatus = ref('all');
const notifications = ref([]);

async function loadNotifications() {
  loading.value = true;
  try {
    notifications.value = await listNotifications({
      receiverScope: 'merchant',
      readStatus: readStatus.value,
      pageSize: 50
    });
  } finally {
    loading.value = false;
  }
}

async function markRead(notificationId) {
  await markNotificationRead(notificationId);
  await loadNotifications();
}

async function markAllRead() {
  await markAllNotificationsRead('merchant');
  await loadNotifications();
}

watch(readStatus, loadNotifications);
onMounted(loadNotifications);
</script>
