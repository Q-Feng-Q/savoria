<template>
  <div class="view-stack">
    <SectionCard title="平台菜谱模板" subtitle="维护全部来源菜谱、配料组件、采购完整度与图片审核状态">
      <template #actions><button class="ghost-button" type="button" :disabled="loading" @click="load(1)">刷新</button></template>
      <div class="filter-grid">
        <label class="form-field"><span>关键词</span><input v-model.trim="query.keyword" type="search" placeholder="菜名或模板编码" @keyup.enter="load(1)" /></label>
        <label class="form-field"><span>来源</span><select v-model="query.sourceType"><option value="">全部</option><option value="COOK_LIKE_HOC">CookLikeHOC</option><option value="LOCAL_EXTENSION">本地扩展</option></select></label>
        <label class="form-field"><span>模板类型</span><select v-model="query.templateType"><option value="">全部</option><option value="DISH">成品菜</option><option value="COMPONENT">配料组件</option></select></label>
        <label class="form-field"><span>数据状态</span><select v-model="query.dataStatus"><option value="">全部</option><option value="READY">完整</option><option value="NEEDS_PURCHASE_DATA">缺采购数据</option><option value="NEEDS_PRICE">缺价格</option><option value="NEEDS_BOTH">采购与价格均缺</option></select></label>
        <label class="form-field"><span>来源分类</span><input v-model.trim="query.sourceCategory" placeholder="例如：炒菜" /></label>
        <label class="form-field"><span>图片</span><select v-model="query.missingImage"><option value="">全部</option><option value="true">缺少公开图片</option><option value="false">已有公开图片</option></select></label>
        <label class="form-field"><span>步骤</span><select v-model="query.missingSteps"><option value="">全部</option><option value="true">缺少步骤</option><option value="false">已有步骤</option></select></label>
        <button class="primary-button" type="button" @click="load(1)">查询</button>
      </div>

      <div v-if="loading" class="table-empty">正在加载模板...</div>
      <AppEmpty v-else-if="!page.items.length" title="没有匹配的模板" description="调整筛选条件后再试。" />
      <div v-else class="table-scroll"><table class="data-table"><thead><tr><th>模板</th><th>来源</th><th>类型</th><th>价格</th><th>采购</th><th>图片</th><th>步骤</th><th>状态</th><th>操作</th></tr></thead><tbody>
        <tr v-for="item in page.items" :key="item.templateId"><td><strong>{{ item.name }}</strong><small>{{ item.templateCode }} · v{{ item.version }}</small></td><td>{{ sourceLabel(item.sourceType) }}<small>{{ item.sourceCategory || '-' }}</small></td><td>{{ item.templateType === 'COMPONENT' ? '配料组件' : '成品菜' }}</td><td>{{ item.referencePrice == null ? '价格待完善' : `¥${item.referencePrice}` }}</td><td><span :class="item.procurementReady ? 'state-ready' : 'state-warning'">{{ item.procurementReady ? '可采购' : '待完善' }}</span></td><td>{{ imageRightsLabel(item.imageRightsStatus) }}</td><td>{{ item.missingSteps ? '步骤待补' : '已录入' }}</td><td>{{ dataStatusLabel(item.dataStatus) }}</td><td><button class="text-button" type="button" @click="router.push(`/platform-dish-templates/${item.templateId}`)">维护</button></td></tr>
      </tbody></table></div>
      <div class="pager"><button class="ghost-button" type="button" :disabled="page.page <= 1" @click="load(page.page - 1)">上一页</button><span>第 {{ page.page }} 页 · 共 {{ page.total }} 条</span><button class="ghost-button" type="button" :disabled="page.page * page.pageSize >= page.total" @click="load(page.page + 1)">下一页</button></div>
    </SectionCard>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import { listAdminDishTemplates } from '../../api/admin-dish-templates';

const router = useRouter();
const loading = ref(false);
const query = reactive({ keyword: '', sourceType: '', templateType: '', dataStatus: '', sourceCategory: '', missingImage: '', missingSteps: '' });
const page = reactive({ items: [], total: 0, page: 1, pageSize: 20 });
const sourceLabel = (value) => value === 'COOK_LIKE_HOC' ? 'CookLikeHOC' : value === 'LOCAL_EXTENSION' ? '本地扩展' : value || '-';
const imageRightsLabel = (value) => ({ DECLARED: '已发布', UNDECLARED: '待审核', NONE: '无图片' }[value] || value || '-');
const dataStatusLabel = (value) => ({ READY: '完整', NEEDS_PURCHASE_DATA: '缺采购数据', NEEDS_PRICE: '缺价格', NEEDS_BOTH: '采购与价格均缺' }[value] || value || '-');
async function load(targetPage = 1) { loading.value = true; try { const result = await listAdminDishTemplates({ ...query, page: targetPage, pageSize: page.pageSize }); Object.assign(page, { items: result.items || [], total: result.total || 0, page: result.page || targetPage, pageSize: result.pageSize || 20 }); } finally { loading.value = false; } }
onMounted(() => load(1));
</script>

<style scoped>
.filter-grid{display:grid;grid-template-columns:1.4fr repeat(6,minmax(120px,1fr)) auto;gap:10px;align-items:end;margin-bottom:18px}.table-scroll{overflow:auto}.data-table{width:100%;min-width:1120px;border-collapse:collapse}.data-table th,.data-table td{padding:12px;border-bottom:1px solid var(--line);text-align:left;font-size:13px}.data-table th{background:#f6f2e9;color:var(--text-soft)}.data-table small{display:block;margin-top:4px;color:var(--text-soft)}.state-ready{color:#287a4b;font-weight:700}.state-warning{color:#a06214;font-weight:700}.pager{display:flex;align-items:center;justify-content:flex-end;gap:12px;margin-top:18px;color:var(--text-soft);font-size:13px}@media(max-width:1200px){.filter-grid{grid-template-columns:repeat(4,1fr)}}@media(max-width:720px){.filter-grid{grid-template-columns:1fr 1fr}}
</style>
