<template>
  <div class="view-stack">
    <SectionCard title="订单筛选" subtitle="按状态和关键词快速定位订单">
      <div class="toolbar-grid">
        <label class="form-field">
          <span>状态</span>
          <select v-model="statusFilter">
            <option value="">全部状态</option>
            <option value="PENDING">待确认</option>
            <option value="CONFIRMED">已确认</option>
            <option value="PREPARING">备菜中</option>
            <option value="READY">待取餐/待配送</option>
            <option value="DONE">已完成</option>
            <option value="CANCELLED">已取消</option>
            <option value="REJECTED">已拒单</option>
          </select>
        </label>
        <label class="form-field">
          <span>搜索</span>
          <input v-model.trim="keyword" type="text" placeholder="搜索家庭、订单号或菜名" />
        </label>
        <div class="toolbar-actions">
          <button class="ghost-button" type="button" @click="loadOrders">刷新</button>
        </div>
      </div>
    </SectionCard>

    <div class="content-grid order-layout">
      <SectionCard title="订单列表" subtitle="当前商户下的全部家庭订单">
        <div v-if="loading" class="table-empty">加载中...</div>
        <AppEmpty
          v-else-if="!filteredOrders.length"
          title="暂无匹配订单"
          description="试着清空筛选条件，或者等待家庭端提交新订单。"
        />
        <div v-else class="table-card">
          <table class="table">
            <thead>
              <tr>
                <th>日期</th>
                <th>家庭</th>
                <th>餐次</th>
                <th>总额</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredOrders" :key="item.orderId">
                <td>{{ item.serviceDate }}</td>
                <td>{{ item.familyName || `家庭 #${item.familyId}` }}</td>
                <td>{{ item.mealSlotName || item.mealLabel || item.mealSlotId }}</td>
                <td>¥{{ item.totalAmount }}</td>
                <td><StatusPill :status="item.status" /></td>
                <td>
                  <button class="text-button" type="button" @click="selectOrder(item.orderId)">查看</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </SectionCard>

      <SectionCard title="订单详情" subtitle="查看菜品、配送和成员归属">
        <template #actions>
          <button v-if="selectedOrderId" class="ghost-button" type="button" @click="selectOrder(selectedOrderId)">刷新详情</button>
        </template>

        <AppEmpty
          v-if="!selectedOrder"
          title="请选择订单"
          description="从左侧列表点开任意订单，就能在这里处理确认、拒单、配送费和状态推进。"
        />

        <div v-else class="detail-stack">
          <div class="detail-grid two">
            <div class="detail-line"><span>订单号</span><strong>#{{ selectedOrder.orderId }}</strong></div>
            <div class="detail-line"><span>状态</span><StatusPill :status="selectedOrder.status" /></div>
            <div class="detail-line"><span>家庭</span><strong>{{ selectedOrder.familyName || `家庭 #${selectedOrder.familyId}` }}</strong></div>
            <div class="detail-line"><span>提交人</span><strong>{{ selectedOrder.submitterMemberName || selectedOrder.submitterMemberId || '-' }}</strong></div>
            <div class="detail-line"><span>日期</span><strong>{{ selectedOrder.serviceDate }}</strong></div>
            <div class="detail-line"><span>餐次</span><strong>{{ selectedOrder.mealSlotName || selectedOrder.mealSlotId }}</strong></div>
            <div class="detail-line"><span>配送方式</span><strong>{{ selectedOrder.deliveryMode }}</strong></div>
            <div class="detail-line"><span>配送费</span><strong>¥{{ selectedOrder.deliveryFee }}</strong></div>
          </div>

          <div class="info-block">
            <strong>整单备注</strong>
            <p>{{ selectedOrder.remark || '无备注' }}</p>
          </div>

          <div class="info-block">
            <strong>菜品明细</strong>
            <div class="item-stack">
              <article v-for="item in selectedOrder.items || []" :key="`${selectedOrder.orderId}-${item.dishId}-${item.ownerMemberId}`" class="list-row-card static">
                <div>
                  <strong>{{ item.dishName }}</strong>
                  <p>{{ item.ownerMemberName || `成员 #${item.ownerMemberId}` }} · {{ item.itemRemark || '无单品备注' }}</p>
                </div>
                <span class="quantity-text">x{{ item.quantity }}</span>
              </article>
            </div>
          </div>

          <div class="detail-actions">
            <button class="primary-button" type="button" @click="handleConfirm" :disabled="selectedOrder.status !== 'PENDING'">确认订单</button>
            <button class="ghost-button" type="button" @click="handleAdvance">推进状态</button>
            <button class="ghost-button" type="button" @click="handleDeliveryFee">调整配送费</button>
            <button class="ghost-button danger-inline" type="button" @click="handleReject">拒单</button>
            <button class="ghost-button danger-inline" type="button" @click="handleCancel">取消</button>
          </div>
        </div>
      </SectionCard>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import StatusPill from '../../components/StatusPill.vue';
import { promptAction } from '../../utils/dialog';
import {
  advanceMerchantOrderStatus,
  cancelMerchantOrder,
  confirmMerchantOrder,
  getMerchantOrderDetail,
  listMerchantOrders,
  rejectMerchantOrder,
  updateMerchantOrderDeliveryFee
} from '../../api/orders';

const loading = ref(false);
const orders = ref([]);
const selectedOrderId = ref(null);
const selectedOrder = ref(null);
const keyword = ref('');
const statusFilter = ref('');

const filteredOrders = computed(() => {
  return orders.value.filter((item) => {
    const statusMatches = !statusFilter.value || item.status === statusFilter.value;
    const searchSource = [
      item.familyName,
      item.orderNo,
      item.mealSlotName,
      ...(item.items || []).map((dish) => dish.dishName)
    ].filter(Boolean).join(' ');
    const keywordMatches = !keyword.value || searchSource.toLowerCase().includes(keyword.value.toLowerCase());
    return statusMatches && keywordMatches;
  });
});

async function loadOrders() {
  loading.value = true;
  try {
    orders.value = await listMerchantOrders();
    if (!selectedOrderId.value && orders.value[0]) {
      await selectOrder(orders.value[0].orderId);
    }
  } finally {
    loading.value = false;
  }
}

async function selectOrder(orderId) {
  selectedOrderId.value = orderId;
  selectedOrder.value = await getMerchantOrderDetail(orderId);
}

async function handleConfirm() {
  if (!selectedOrderId.value) return;
  await confirmMerchantOrder(selectedOrderId.value);
  await loadOrders();
  await selectOrder(selectedOrderId.value);
}

async function handleReject() {
  if (!selectedOrderId.value) return;
  const reason = await promptAction({ title: '拒绝订单', label: '拒单原因', placeholder: '请向家庭说明原因', danger: true });
  if (!reason) return;
  await rejectMerchantOrder(selectedOrderId.value, reason);
  await loadOrders();
  await selectOrder(selectedOrderId.value);
}

async function handleCancel() {
  if (!selectedOrderId.value) return;
  const reason = await promptAction({ title: '取消订单', label: '取消原因', placeholder: '请填写取消原因', danger: true });
  if (!reason) return;
  await cancelMerchantOrder(selectedOrderId.value, reason);
  await loadOrders();
  await selectOrder(selectedOrderId.value);
}

async function handleDeliveryFee() {
  if (!selectedOrder.value) return;
  const nextValue = await promptAction({ title: '调整配送费', label: '新的配送费', inputType: 'number', defaultValue: String(selectedOrder.value.deliveryFee ?? 0) });
  if (nextValue === null) return;
  await updateMerchantOrderDeliveryFee(selectedOrderId.value, Number(nextValue || 0));
  await loadOrders();
  await selectOrder(selectedOrderId.value);
}

async function handleAdvance() {
  if (!selectedOrder.value) return;
  const nextStatus = await promptAction({ title: '推进订单状态', label: '下一状态', defaultValue: 'PREPARING', options: [
    { value: 'CONFIRMED', label: '已确认' },
    { value: 'PREPARING', label: '备菜中' },
    { value: 'READY', label: '待取餐/待配送' },
    { value: 'DONE', label: '已完成' }
  ] });
  if (!nextStatus) return;
  await advanceMerchantOrderStatus(selectedOrderId.value, nextStatus);
  await loadOrders();
  await selectOrder(selectedOrderId.value);
}

onMounted(loadOrders);
</script>
