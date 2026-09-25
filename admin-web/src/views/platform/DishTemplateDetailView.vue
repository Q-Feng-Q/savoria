<template>
  <div class="detail-page">
    <div class="detail-topbar"><button class="ghost-button" type="button" @click="router.push('/platform-dish-templates')">返回列表</button><button class="primary-button" type="button" :disabled="saving || stepUploading || !form" @click="save">{{ saving ? '保存中...' : '保存模板' }}</button></div>
    <div v-if="loading" class="table-empty">正在加载模板详情...</div>
    <template v-else-if="form && detail">
      <section class="summary-band"><div><span class="page-eyebrow">{{ detail.templateCode }} · v{{ detail.version }}</span><h2>{{ detail.name }}</h2><p>{{ detail.sourceType }} / {{ detail.sourceCategory || '-' }} · {{ detail.templateType }}</p></div><div class="status-group"><span>{{ detail.dataStatus }}</span><span :class="detail.procurementReady ? 'ready' : 'warning'">{{ detail.procurementReady ? '采购就绪' : '采购待完善' }}</span><span>{{ detail.imageRightsStatus }}</span></div></section>

      <form class="editor-layout" @submit.prevent="save">
        <section class="editor-panel"><NourishmentFields :form="form" :disabled="saving" /></section>
        <section class="editor-panel"><header><h3>基本信息</h3><span>派生状态由服务端保存后重新计算</span></header><div class="form-grid">
          <label class="form-field"><span>模板名称</span><input v-model.trim="form.name" maxlength="100" required /></label>
          <label class="form-field"><span>菜谱分类</span><select v-model.number="form.categoryId" required :disabled="categoriesLoading"><option v-for="category in categoryOptions" :key="category.categoryId" :value="category.categoryId">{{ category.name }}</option></select><small>可选择菜谱库中已有的分类</small><button v-if="categoriesError" type="button" class="text-button" @click="loadCategories">分类加载失败，重试</button></label>
          <label class="form-field form-wide"><span>菜谱简介</span><textarea v-model.trim="form.description" maxlength="255" placeholder="可留空" /></label>
          <label class="form-field"><span>参考价格</span><input v-model="form.referencePrice" type="number" min="0" step="0.01" placeholder="可留空" /></label>
          <label class="form-field"><span>口味标签</span><input v-model.trim="form.tasteTagsText" placeholder="家常，咸香" /></label>
          <fieldset class="meal-field form-wide"><legend>推荐餐次与状态</legend><label v-for="meal in meals" :key="meal.value"><input v-model="form.mealTags" type="checkbox" :value="meal.value" />{{ meal.label }}</label><label><input v-model="form.enabled" type="checkbox" />启用模板</label></fieldset>
        </div></section>

        <section class="editor-panel"><header><h3>食材与采购状态</h3><button class="text-button" type="button" @click="addIngredient">+ 添加食材</button></header><div class="ingredient-list"><div v-for="(item, index) in form.ingredients" :key="item.itemId" class="ingredient-row"><input v-model.trim="item.ingredientName" placeholder="食材名称" /><input v-model.trim="item.ingredientCategory" placeholder="分类" /><select v-model="item.quantityStatus"><option v-for="(label, value) in quantityStatuses" :key="value" :value="value">{{ label }}</option></select><template v-if="item.quantityStatus === 'VERIFIED'"><input v-model="item.quantity" type="number" min="0.01" step="0.01" placeholder="用量" /><input v-model.trim="item.unit" placeholder="单位" /><select v-model="item.calcType"><option value="FIXED">固定</option><option value="PER_PERSON">按人数</option></select></template><input v-else-if="item.quantityStatus === 'SOURCE_BATCH'" v-model.trim="item.sourceQuantityText" class="row-span" placeholder="原配方批量说明" /><span v-else class="row-span row-hint">{{ quantityStatuses[item.quantityStatus] }}</span><button class="text-button danger-text" type="button" @click="removeIngredient(index)">删除</button></div></div></section>

        <section class="editor-panel"><header><h3>制作步骤</h3><button class="text-button" type="button" @click="addStep">+ 添加步骤</button></header><div class="step-list"><div v-for="(item, index) in form.cookingSteps" :key="item.itemId" class="step-row"><b>{{ index + 1 }}</b><input v-model.trim="item.title" maxlength="100" placeholder="标题" /><textarea v-model.trim="item.content" placeholder="制作过程" /><StepImages v-model="item.imageUrls" editable :disabled="saving || stepUploading || assetBusy" @busy="stepUploading = $event" /><input v-model="item.durationSeconds" type="number" min="0" placeholder="秒" /><input v-model.trim="item.temperatureText" placeholder="温度" /><input v-model.trim="item.heatLevel" placeholder="火候" /><button class="text-button danger-text" type="button" @click="removeStep(index)">删除</button></div><p v-if="!form.cookingSteps.length" class="row-hint">该菜谱暂未提供制作步骤。</p></div></section>
      </form>

      <section class="editor-panel"><header><h3>来源记录</h3><span>只读，可追溯到固定来源版本</span></header><div class="source-list"><div v-for="record in detail.sourceRecords" :key="record.id || record.sourceKey"><strong>{{ record.sourceKey }}</strong><span>{{ record.sourceRevision || '-' }}</span><p>{{ record.contentSummary || record.sourceUrl || '无摘要' }}</p></div><p v-if="!detail.sourceRecords?.length" class="row-hint">暂无来源记录</p></div></section>

      <section class="editor-panel"><header><h3>内部图片审核</h3><span>图片只能通过受控资源 ID 访问</span></header><div class="asset-list"><article v-for="asset in detail.internalAssetReviews" :key="asset.assetId" class="asset-row"><div><strong>资源 #{{ asset.assetId }}</strong><span>{{ asset.assetStatus }} · {{ asset.mimeType }} · {{ formatSize(asset.fileSize) }}</span><p v-if="asset.rejectionReason">驳回原因：{{ asset.rejectionReason }}</p></div><img v-if="assetPreviewUrls[asset.assetId]" :src="assetPreviewUrls[asset.assetId]" alt="内部审核图片预览" /><div v-if="asset.assetStatus === 'INTERNAL_REVIEW'" class="asset-actions"><button class="ghost-button" type="button" :disabled="assetBusy || stepUploading || saving" @click="preview(asset)">预览</button><button class="ghost-button danger-text" type="button" :disabled="assetBusy || stepUploading || saving" @click="reject(asset)">驳回</button></div><div v-if="asset.assetStatus === 'INTERNAL_REVIEW'" class="promotion-form"><input v-model.trim="promotionForms[asset.assetId].author" placeholder="作者或来源平台" /><input v-model.trim="promotionForms[asset.assetId].sourceUrl" placeholder="来源页面 URL" /><input v-model.trim="promotionForms[asset.assetId].license" placeholder="授权说明" /><button class="primary-button" type="button" :disabled="assetBusy || stepUploading || saving" @click="promote(asset)">确认授权并发布</button></div></article><p v-if="!detail.internalAssetReviews?.length" class="row-hint">没有待审核图片资源。</p></div></section>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import StepImages from '../../components/StepImages.vue'; import NourishmentFields from '../../components/NourishmentFields.vue';
import { getAdminDishTemplate, previewDishTemplateAsset, promoteDishTemplateImage, rejectDishTemplateImage, updateAdminDishTemplate } from '../../api/admin-dish-templates';
import { buildTemplateSnapshot, createTemplateChangeForm, validateTemplateSnapshot } from '../../utils/dish-template-changes';
import { promptAction } from '../../utils/dialog';
import { notify } from '../../utils/feedback';
import { listAdminDishTemplates } from '../../api/admin-dish-templates';

const route = useRoute(); const router = useRouter();
const stepUploading = ref(false); const loading = ref(true); const saving = ref(false); const assetBusy = ref(false);
const detail = ref(null); const form = ref(null);
const categories = ref([]); const categoriesLoading = ref(false); const categoriesError = ref(false);
const categoryOptions = computed(() => {
  const items = new Map(categories.value.map(item => [Number(item.categoryId), item]));
  if (detail.value?.categoryId && !items.has(Number(detail.value.categoryId))) items.set(Number(detail.value.categoryId), { categoryId: detail.value.categoryId, name: detail.value.categoryName || '当前分类' });
  return [...items.values()];
});
async function loadCategories() {
  categoriesLoading.value = true; categoriesError.value = false;
  try {
    const items = new Map();
    for (let page = 1; ; page++) {
      const result = await listAdminDishTemplates({ page, pageSize: 100 });
      const rows = result.items || [];
      rows.forEach(item => { if (item.categoryId && item.categoryName) items.set(Number(item.categoryId), { categoryId: item.categoryId, name: item.categoryName }); });
      if (!rows.length || page * (result.pageSize || 100) >= (result.total || rows.length)) break;
    }
    categories.value = [...items.values()];
  } catch { categoriesError.value = true; }
  finally { categoriesLoading.value = false; }
}
const assetPreviewUrls = reactive({}); const promotionForms = reactive({});
const meals = [{ value: 'BREAKFAST', label: '早餐' }, { value: 'LUNCH', label: '午餐' }, { value: 'DINNER', label: '晚餐' }];
const quantityStatuses = { VERIFIED: '已核定', SOURCE_BATCH: '原配方批量', MISSING: '用量待补', NOT_APPLICABLE: '无需采购' };

function prepare(value) { detail.value = value; form.value = createTemplateChangeForm(value); for (const asset of value.internalAssetReviews || []) promotionForms[asset.assetId] ||= { author: '', sourceUrl: '', license: '' }; }
async function load() { if (stepUploading.value) return; loading.value = true; try { prepare(await getAdminDishTemplate(route.params.templateId)); } finally { loading.value = false; } }
function addIngredient() { form.value.ingredients.push({ itemId: `ingredient-new-${Date.now()}`, ingredientName: '', ingredientCategory: '', quantityStatus: 'MISSING', quantity: '', unit: '', calcType: '', sourceQuantityText: '', sortOrder: form.value.ingredients.length + 1 }); }
function removeIngredient(index) { form.value.ingredients.splice(index, 1); }
function addStep() { if (saving.value || stepUploading.value) return; form.value.cookingSteps.push({ itemId: `step-new-${Date.now()}`, title: '', content: '', durationSeconds: '', temperatureText: '', heatLevel: '', componentTemplateId: '' }); }
function removeStep(index) { if (saving.value || stepUploading.value) return; form.value.cookingSteps.splice(index, 1); }
async function save() { if (saving.value || stepUploading.value) return; const snapshot = buildTemplateSnapshot(form.value); const validation = validateTemplateSnapshot(snapshot); if (validation) { notify(validation, 'error'); return; } const { sortOrder, ...editable } = snapshot; saving.value = true; try { await updateAdminDishTemplate(detail.value.templateId, { ...editable, schemaVersion: 2, expectedVersion: detail.value.version }); notify('模板已保存，完整状态已重新计算', 'success'); await load(); } finally { saving.value = false; } }
async function preview(asset) { assetBusy.value = true; try { if (assetPreviewUrls[asset.assetId]) URL.revokeObjectURL(assetPreviewUrls[asset.assetId]); assetPreviewUrls[asset.assetId] = await previewDishTemplateAsset(asset.assetId); } finally { assetBusy.value = false; } }
async function promote(asset) { if (stepUploading.value || saving.value) return; const values = promotionForms[asset.assetId]; if (!values.author || !values.sourceUrl || !values.license) { notify('请补全作者、来源页面和授权说明', 'error'); return; } assetBusy.value = true; try { await promoteDishTemplateImage(detail.value.templateId, { internalAssetId: asset.assetId, expectedVersion: detail.value.version, ...values }); notify('图片已发布', 'success'); await load(); } finally { assetBusy.value = false; } }
async function reject(asset) { if (stepUploading.value || saving.value) return; const reason = await promptAction({ title: '驳回内部图片', label: '驳回原因', required: true, danger: true, confirmText: '确认驳回' }); if (!reason || saving.value || stepUploading.value || assetBusy.value) return; assetBusy.value = true; try { await rejectDishTemplateImage(detail.value.templateId, asset.assetId, reason); notify('图片已驳回', 'success'); await load(); } finally { assetBusy.value = false; } }
const formatSize = (value) => value == null ? '-' : value < 1024 ? `${value} B` : `${(value / 1024).toFixed(1)} KB`;
onMounted(() => { load(); loadCategories(); });
onBeforeUnmount(() => Object.values(assetPreviewUrls).forEach((url) => URL.revokeObjectURL(url)));
</script>

<style scoped>
.detail-page{display:grid;gap:18px}.detail-topbar{display:flex;justify-content:space-between}.summary-band,.editor-panel{padding:20px 22px;border:1px solid var(--line);border-radius:8px;background:#fff}.summary-band{display:flex;align-items:flex-start;justify-content:space-between}.summary-band h2{margin:5px 0}.summary-band p{margin:0;color:var(--text-soft)}.status-group{display:flex;gap:8px;flex-wrap:wrap}.status-group span{padding:7px 9px;border-radius:6px;background:#f2eee7;font-size:12px}.status-group .ready{background:#e7f4e9;color:#287a4b}.status-group .warning{background:#fff0d6;color:#9b6511}.editor-layout{display:grid;gap:18px}.editor-panel>header{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}.editor-panel h3{margin:0}.editor-panel header span,.row-hint{color:var(--text-soft);font-size:12px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.form-wide{grid-column:1/-1}.meal-field{display:flex;gap:18px;padding:12px;border:1px solid var(--line);border-radius:8px}.ingredient-list,.step-list,.source-list,.asset-list{display:grid;gap:10px}.ingredient-row{display:grid;grid-template-columns:1.2fr 1fr 1fr .7fr .6fr .8fr auto;gap:8px}.ingredient-row input,.ingredient-row select,.step-row input,.step-row textarea,.promotion-form input{min-width:0;padding:8px 10px;border:1px solid var(--line-strong);border-radius:7px;background:#fff}.row-span{grid-column:4/7}.step-row{display:grid;grid-template-columns:32px 150px minmax(280px,1fr) 90px 110px 100px auto;gap:8px;align-items:start}.step-row textarea{min-height:70px;resize:vertical}.source-list>div{padding:12px;border-left:3px solid var(--brand);background:#f7f8f4}.source-list span{margin-left:12px;color:var(--text-soft);font-size:12px}.source-list p{margin:5px 0 0}.asset-row{display:grid;grid-template-columns:minmax(220px,1fr) 180px auto;gap:14px;align-items:start;padding:14px;border:1px solid var(--line);border-radius:8px}.asset-row span,.asset-row p{display:block;margin:5px 0 0;color:var(--text-soft);font-size:12px}.asset-row img{width:180px;height:130px;object-fit:contain;background:#f2eee7}.asset-actions{display:flex;gap:8px}.promotion-form{grid-column:1/-1;display:grid;grid-template-columns:1fr 1.5fr 1fr auto;gap:8px}@media(max-width:980px){.ingredient-row,.step-row{grid-template-columns:1fr 1fr}.row-span{grid-column:auto}.asset-row{grid-template-columns:1fr}.promotion-form{grid-template-columns:1fr 1fr}}@media(max-width:620px){.form-grid,.promotion-form{grid-template-columns:1fr}.form-wide{grid-column:auto}.summary-band{display:grid;gap:14px}}
</style>
