<template>
  <div class="view-stack"><SectionCard title="待审核菜品" subtitle="审核结果将直接影响商户菜品是否进入正式菜单">
    <div class="detail-actions"><button class="ghost-button" type="button" @click="load">刷新</button></div>
    <div v-if="loading" class="table-empty">加载中...</div>
    <AppEmpty v-else-if="!items.length" title="暂无待审核菜品" description="当前没有需要平台处理的菜品提交。" />
    <div v-else class="notification-list">
      <article v-for="item in items" :key="item.id" class="notification-row">
        <div class="review-copy"><strong>提交 #{{ item.id }}</strong><p>商户 {{ item.merchantId }} · {{ item.submissionType }} · {{ item.submittedAt }}</p>
          <dl v-if="item.snapshot" class="review-food-information">
            <dt>菜品类型</dt><dd>{{ productTypeLabel(item.snapshot.productType) }}</dd>
            <template v-for="[key, label] in NOURISHMENT_FIELDS" :key="key"><dt>{{ label }}</dt><dd>{{ item.snapshot[key] == null ? '历史快照未记录' : item.snapshot[key] || '未填写' }}</dd></template>
          </dl>
          <p v-else>快照无法解析，请核对提交内容后审核。</p>
          <section v-for="step in item.snapshot?.cookingSteps || []" :key="step.stepNo"><h4>{{step.stepNo}}. {{step.title || '制作步骤'}}</h4><p>{{step.content}}</p><StepImages :model-value="step.imageUrls || []" /></section>
        </div>
        <div class="inline-actions"><button class="text-button" @click="approve(item.id)">通过</button><button class="ghost-button" @click="reject(item.id)">拒绝</button></div>
      </article>
    </div>
  </SectionCard></div>
</template>
<script setup>
import StepImages from '../../components/StepImages.vue';
import { onMounted, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import { approveDishReview, listPendingDishReviews, rejectDishReview } from '../../api/dish-reviews';
import { promptAction } from '../../utils/dialog';
import { NOURISHMENT_FIELDS } from '../../utils/dish-template-changes';
const items = ref([]); const loading = ref(false);
function parseSnapshot(snapshotJson) {
  try {
    const snapshot = JSON.parse(snapshotJson);
    return snapshot && typeof snapshot === 'object' && !Array.isArray(snapshot) ? snapshot : null;
  } catch { return null; }
}
function productTypeLabel(value) { return value == null ? '历史快照未记录' : ({ NORMAL: '普通菜品', NOURISHMENT: '滋补食品' }[value] || value || '未填写'); }
async function load(){ loading.value=true; try{items.value=(await listPendingDishReviews()).map(item => ({ ...item, snapshot: parseSnapshot(item.snapshotJson) }));}finally{loading.value=false;} }
async function approve(id){ await approveDishReview(id); await load(); }
async function reject(id){ const reason=await promptAction({title:'拒绝菜品审核',label:'拒绝原因',danger:true}); if(!reason)return; await rejectDishReview(id,reason); await load(); }
onMounted(load);
</script>
<style scoped>
.review-copy{min-width:0;flex:1}.review-food-information{display:grid;grid-template-columns:90px minmax(0,1fr);gap:8px;font-size:13px}.review-food-information dt{color:var(--text-soft)}.review-food-information dd{margin:0;white-space:pre-wrap;overflow-wrap:anywhere}
</style>
