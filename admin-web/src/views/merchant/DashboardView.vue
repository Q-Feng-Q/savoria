<template>
  <div class="view-stack">
    <div class="stats-grid">
      <StatCard label="待确认订单" :value="stats.pending" note="优先处理新提交订单" />
      <StatCard label="备菜中" :value="stats.preparing" note="需要关注取消与配送" />
      <StatCard label="采购项" :value="stats.purchases" note="含预估与已确认采购" />
      <StatCard label="未读通知" :value="stats.unread" note="订单、采购、余额提醒" />
    </div>

    <div class="content-grid two-up">
      <SectionCard title="订单动态" subtitle="按最新时间查看待处理订单">
        <template #actions>
          <button class="ghost-button" type="button" @click="loadData">刷新</button>
        </template>

        <div v-if="loading" class="table-empty">加载中...</div>
        <AppEmpty
          v-else-if="!orders.length"
          title="暂无订单"
          description="接口已接通，但当前没有可处理的商户订单。"
        />
        <div v-else class="list-stack">
          <button
            v-for="item in orders.slice(0, 6)"
            :key="item.orderId"
            class="list-row-card"
            type="button"
            @click="goOrders"
          >
            <div>
              <strong>{{ item.serviceDate }} / {{ item.familyName || `家庭 #${item.familyId}` }}</strong>
              <p>{{ item.items?.length || 0 }} 道菜 · ¥{{ item.totalAmount }}</p>
            </div>
            <StatusPill :status="item.status" />
          </button>
        </div>
      </SectionCard>

      <SectionCard title="采购预览" subtitle="聚合今日采购摘要">
        <template #actions>
          <button class="ghost-button" type="button" @click="goPurchases">采购清单</button>
        </template>

        <div v-if="loading" class="table-empty">加载中...</div>
        <AppEmpty
          v-else-if="!purchases.length"
          title="暂无采购项"
          description="当前日期下还没有生成采购汇总。"
        />
        <div v-else class="list-stack">
          <article v-for="item in purchases.slice(0, 6)" :key="`${item.ingredientName}-${item.unit}`" class="list-row-card static">
            <div>
              <strong>{{ item.ingredientName }}</strong>
              <p>{{ item.sourceStatus }} · {{ item.sources?.length || 0 }} 个来源</p>
            </div>
            <span class="quantity-text">{{ item.quantity }} {{ item.unit }}</span>
          </article>
        </div>
      </SectionCard>
    </div>

    <SectionCard title="通知中心" subtitle="商户端最近 8 条通知">
      <template #actions>
        <button class="ghost-button" type="button" @click="goNotifications">全部通知</button>
      </template>

      <div v-if="loading" class="table-empty">加载中...</div>
      <AppEmpty
        v-else-if="!notifications.length"
        title="暂无通知"
        description="通知中心已经准备好，等后端数据推送即可显示。"
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
          <span class="time-text">{{ item.createdAt }}</span>
        </article>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import StatCard from '../../components/StatCard.vue';
import StatusPill from '../../components/StatusPill.vue';
import { getDashboardSnapshot } from '../../api/dashboard';

const router = useRouter();
const loading = ref(false);
const orders = ref([]);
const purchases = ref([]);
const notifications = ref([]);

const stats = computed(() => ({
  pending: orders.value.filter((item) => item.status === 'PENDING').length,
  preparing: orders.value.filter((item) => item.status === 'PREPARING').length,
  purchases: purchases.value.length,
  unread: notifications.value.filter((item) => !item.read).length
}));

async function loadData() {
  loading.value = true;
  try {
    const snapshot = await getDashboardSnapshot(new Date().toISOString().slice(0, 10));
    orders.value = snapshot.orders;
    purchases.value = snapshot.purchases;
    notifications.value = snapshot.notifications;
  } finally {
    loading.value = false;
  }
}

function goOrders() {
  router.push('/orders');
}

function goPurchases() {
  router.push('/purchases');
}

function goNotifications() {
  router.push('/notifications');
}

onMounted(loadData);
</script>
