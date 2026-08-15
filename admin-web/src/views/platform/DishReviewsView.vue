<template>
  <div class="view-stack"><SectionCard title="待审核菜品" subtitle="审核结果将直接影响商户菜品是否进入正式菜单">
    <div class="detail-actions"><button class="ghost-button" type="button" @click="load">刷新</button></div>
    <div v-if="loading" class="table-empty">加载中...</div>
    <AppEmpty v-else-if="!items.length" title="暂无待审核菜品" description="当前没有需要平台处理的菜品提交。" />
    <div v-else class="notification-list">
      <article v-for="item in items" :key="item.id" class="notification-row">
        <div><strong>提交 #{{ item.id }}</strong><p>商户 {{ item.merchantId }} · {{ item.submissionType }} · {{ item.submittedAt }}</p></div>
        <div class="inline-actions"><button class="text-button" @click="approve(item.id)">通过</button><button class="ghost-button" @click="reject(item.id)">拒绝</button></div>
      </article>
    </div>
  </SectionCard></div>
</template>
<script setup>
import { onMounted, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import { approveDishReview, listPendingDishReviews, rejectDishReview } from '../../api/dish-reviews';
import { promptAction } from '../../utils/dialog';
const items = ref([]); const loading = ref(false);
async function load(){ loading.value=true; try{items.value=await listPendingDishReviews();}finally{loading.value=false;} }
async function approve(id){ await approveDishReview(id); await load(); }
async function reject(id){ const reason=await promptAction({title:'拒绝菜品审核',label:'拒绝原因',danger:true}); if(!reason)return; await rejectDishReview(id,reason); await load(); }
onMounted(load);
</script>
