<template>
  <div class="view-stack">
    <SectionCard title="菜品审核记录" subtitle="查看新增与修改菜品的审核结果，待审核记录可主动撤回">
      <template #actions>
        <button class="ghost-button" type="button" :disabled="loading" @click="loadReviews">刷新</button>
      </template>

      <div class="toolbar-grid">
        <label class="form-field">
          <span>审核状态</span>
          <select v-model="statusFilter">
            <option value="">全部状态</option>
            <option value="PENDING">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">未通过</option>
            <option value="WITHDRAWN">已撤回</option>
          </select>
        </label>
      </div>

      <div v-if="loading" class="table-empty">正在加载审核记录...</div>
      <AppEmpty v-else-if="!filteredReviews.length" title="暂无审核记录" description="当前筛选条件下没有菜品审核记录。" />
      <div v-else class="user-table-scroll">
        <table class="user-data-table review-table">
          <thead><tr><th>提交内容</th><th>目标菜品</th><th>状态</th><th>提交时间</th><th>审核说明</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in filteredReviews" :key="item.id">
              <td><strong>{{ typeLabel(item.submissionType) }}</strong><small>审核单 #{{ item.id }}</small></td>
              <td>{{ item.targetDishId ? `#${item.targetDishId}` : '新菜品' }}</td>
              <td><StatusPill :status="item.status" /></td>
              <td>{{ formatTime(item.submittedAt) }}</td>
              <td>{{ item.reviewReason || '等待审核' }}</td>
              <td><button v-if="item.status === 'PENDING'" class="text-button danger-text" type="button" :disabled="busyId === item.id" @click="handleWithdraw(item)">{{ busyId === item.id ? '撤回中...' : '撤回' }}</button><span v-else class="time-text">已结束</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import StatusPill from '../../components/StatusPill.vue';
import { confirmAction } from '../../utils/dialog';
import { listMerchantDishReviews, withdrawDishReview } from '../../api/merchant-dish-reviews';

const loading = ref(false);
const busyId = ref(null);
const statusFilter = ref('');
const reviews = ref([]);
const filteredReviews = computed(() => reviews.value.filter((item) => !statusFilter.value || item.status === statusFilter.value));

function typeLabel(type) {
  return ({ CREATE: '新增菜品', UPDATE: '修改菜品', STATUS: '状态调整' })[type] || type || '菜品变更';
}
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-'; }
async function loadReviews() {
  loading.value = true;
  try { reviews.value = await listMerchantDishReviews(); } finally { loading.value = false; }
}
async function handleWithdraw(item) {
  const accepted = await confirmAction({ title: '撤回菜品审核', message: '撤回后本次菜品变更不会继续审核。', confirmText: '确认撤回' });
  if (!accepted) return;
  busyId.value = item.id;
  try { await withdrawDishReview(item.id); await loadReviews(); } finally { busyId.value = null; }
}
onMounted(loadReviews);
</script>

<style scoped>
.review-table{min-width:920px}.review-table td:nth-child(5){max-width:280px;white-space:normal}.toolbar-grid{margin-bottom:16px}
</style>
