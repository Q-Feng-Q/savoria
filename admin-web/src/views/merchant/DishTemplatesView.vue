<template>
  <div class="template-workspace">
    <header class="template-toolbar">
      <div class="template-filters">
        <label class="form-field compact-field">
          <span>分类</span>
          <select v-model="query.categoryId" @change="loadTemplates(1)">
            <option value="">全部分类</option>
            <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
              {{ category.name }}
            </option>
          </select>
        </label>
        <label class="form-field compact-field search-field">
          <span>搜索菜名</span>
          <input v-model.trim="query.keyword" type="search" placeholder="例如：番茄、排骨" @keyup.enter="loadTemplates(1)" />
        </label>
        <label class="form-field compact-field">
          <span>导入状态</span>
          <select v-model="query.imported" @change="loadTemplates(1)">
            <option value="">全部</option><option value="false">未导入</option><option value="true">已导入</option>
          </select>
        </label>
        <button class="primary-button search-button" type="button" @click="loadTemplates(1)">查询</button>
      </div>
      <div class="selection-tools">
        <span>共 {{ page.total }} 道，已选 {{ selectedIds.length }} 道</span>
        <button class="text-button" type="button" @click="router.push('/dish-template-changes')">修改申请</button>
        <button class="text-button" type="button" @click="selectCurrentPage">全选当前页</button>
        <button class="text-button" type="button" @click="selectedIds = []">清空</button>
      </div>
    </header>

    <div v-if="loading" class="template-empty">加载模板菜品...</div>
    <AppEmpty v-else-if="!page.items.length" title="没有匹配的模板菜" description="调整分类、关键词或导入状态后再试。" />
    <div v-else class="template-table-wrap">
      <table class="table template-table">
        <thead><tr><th class="select-column"></th><th>菜品</th><th>分类</th><th>口味</th><th>食材</th><th>参考价</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in page.items" :key="item.templateId" :class="{ imported: item.imported }">
            <td><input type="checkbox" :checked="selectedIds.includes(item.templateId)" :disabled="item.imported" @change="toggle(item)" /></td>
            <td><div class="dish-cell"><img :src="resolveAssetUrl(item.imageUrl)" :alt="item.name" /><div><strong>{{ item.name }}</strong><span>{{ item.description }}</span></div></div></td>
            <td>{{ item.categoryName }}</td>
            <td>{{ (item.tasteTags || []).join('、') || '家常' }}</td>
            <td>{{ item.ingredientCount }} 种</td>
            <td>¥{{ item.referencePrice }}</td>
            <td><span :class="item.imported ? 'status-imported' : 'status-ready'">{{ item.imported ? '已导入' : '可导入' }}</span></td>
            <td><button class="text-button" type="button" @click="openChangeEditor(item.templateId)">申请修改</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <footer class="template-footer">
      <div class="pagination">
        <button class="ghost-button" type="button" :disabled="page.page <= 1" @click="loadTemplates(page.page - 1)">上一页</button>
        <span>第 {{ page.page }} 页</span>
        <button class="ghost-button" type="button" :disabled="page.page * page.pageSize >= page.total" @click="loadTemplates(page.page + 1)">下一页</button>
      </div>
      <div class="footer-actions">
        <button class="ghost-button" type="button" @click="router.push('/dishes')">返回菜品管理</button>
        <button class="primary-button" type="button" :disabled="!selectedIds.length || importing" @click="submitImport">
          {{ importing ? '正在导入...' : `导入所选 ${selectedIds.length} 道` }}
        </button>
      </div>
    </footer>

    <div v-if="editorOpen" class="template-editor-layer" @click.self="closeChangeEditor">
      <section class="template-editor-modal" role="dialog" aria-modal="true">
        <header><div><span class="page-eyebrow">TEMPLATE CHANGE</span><h3>申请修改模板</h3><p>平台审核通过前不会修改系统模板，商户已导入菜品也不会跟随变化。</p></div><button class="modal-close" type="button" aria-label="关闭" @click="closeChangeEditor">×</button></header>
        <form v-if="editorForm" class="template-editor-body" @submit.prevent="submitChange">
          <section class="editor-section"><h4>基本信息</h4><div class="editor-form-grid">
            <label class="form-field"><span>菜品名称</span><input v-model.trim="editorForm.name" maxlength="100" required /></label>
            <label class="form-field"><span>模板分类</span><select v-model="editorForm.categoryId" required><option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">{{ category.name }}</option></select></label>
            <label class="form-field editor-wide"><span>菜品简介</span><textarea v-model.trim="editorForm.description" maxlength="255" required /></label>
            <label class="form-field"><span>参考价格</span><input v-model="editorForm.referencePrice" type="number" min="0" step="0.01" required /></label>
            <label class="form-field"><span>排序值</span><input v-model="editorForm.sortOrder" type="number" /></label>
            <label class="form-field editor-wide"><span>口味标签</span><input v-model.trim="editorForm.tasteTagsText" placeholder="家常，咸香" /></label>
            <fieldset class="meal-field editor-wide"><legend>推荐餐次</legend><label v-for="meal in meals" :key="meal.value"><input v-model="editorForm.mealTags" type="checkbox" :value="meal.value" />{{ meal.label }}</label><label><input v-model="editorForm.enabled" type="checkbox" />审核通过后启用</label></fieldset>
          </div></section>
          <section class="editor-section"><div class="editor-section-head"><h4>图片与版权</h4><label class="ghost-button upload-label"><input type="file" accept="image/*" @change="uploadTemplateImage" />{{ uploading ? '上传中...' : '上传图片' }}</label></div><div class="editor-image-row"><img :src="resolveAssetUrl(editorForm.imageUrl)" alt="模板菜预览" /><div class="editor-form-grid">
            <label class="form-field editor-wide"><span>图片本地路径</span><input v-model.trim="editorForm.imageUrl" required /></label>
            <label class="form-field editor-wide"><span>图片来源页面</span><input v-model.trim="editorForm.imageSourceUrl" required /></label>
            <label class="form-field"><span>图片作者或来源平台</span><input v-model.trim="editorForm.imageAuthor" required /></label>
            <label class="form-field"><span>图片授权说明</span><input v-model.trim="editorForm.imageLicense" required /></label>
          </div></div></section>
          <section class="editor-section"><div class="editor-section-head"><h4>食材清单</h4><button class="text-button" type="button" @click="addTemplateIngredient">+ 添加食材</button></div><div class="template-ingredient-list">
            <div v-for="(ingredient, index) in editorForm.ingredients" :key="index" class="template-ingredient-row"><input v-model.trim="ingredient.ingredientName" placeholder="食材名称" required /><input v-model.trim="ingredient.ingredientCategory" placeholder="分类" required /><input v-model="ingredient.quantity" type="number" min="0" step="0.01" placeholder="用量" required /><input v-model.trim="ingredient.unit" placeholder="单位" required /><select v-model="ingredient.calcType"><option value="FIXED">固定消耗</option><option value="PER_PERSON">按人数</option><option value="NO_PURCHASE">不进采购</option></select><button class="text-button danger-text" type="button" @click="removeTemplateIngredient(index)">删除</button></div>
          </div></section>
          <section class="editor-section"><label class="form-field"><span>提交说明</span><textarea v-model.trim="submitNote" maxlength="500" placeholder="说明修改原因和主要变化" /></label></section>
          <footer><button class="ghost-button" type="button" @click="closeChangeEditor">取消</button><button class="primary-button" type="submit" :disabled="editorSaving || uploading">{{ editorSaving ? '提交中...' : '提交审核' }}</button></footer>
        </form>
        <div v-else class="table-empty">正在加载模板详情...</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppEmpty from '../../components/AppEmpty.vue';
import { getDishTemplateDetail, importDishTemplates, listDishTemplateCategories, listDishTemplates } from '../../api/dishes';
import { submitDishTemplateChange } from '../../api/dish-template-changes';
import { uploadDishImage } from '../../api/files';
import { getApiBaseUrl } from '../../api/http';
import { buildTemplateSnapshot, createTemplateChangeForm, validateTemplateSnapshot } from '../../utils/dish-template-changes';
import { notify } from '../../utils/feedback';

const router = useRouter();
const categories = ref([]);
const selectedIds = ref([]);
const loading = ref(false);
const importing = ref(false);
const editorOpen = ref(false);
const editorForm = ref(null);
const editorTemplateId = ref(null);
const editorSaving = ref(false);
const uploading = ref(false);
const submitNote = ref('');
const meals = [{ value: 'BREAKFAST', label: '早餐' }, { value: 'LUNCH', label: '午餐' }, { value: 'DINNER', label: '晚餐' }];
const query = reactive({ categoryId: '', keyword: '', imported: '' });
const page = reactive({ items: [], total: 0, page: 1, pageSize: 20 });

function resolveAssetUrl(value) {
  const path = String(value || '');
  if (!path || /^https?:\/\//i.test(path)) return path;
  const base = getApiBaseUrl();
  if (base === '/api') return `/api${path.startsWith('/') ? path : `/${path}`}`;
  return `${String(base || '').replace(/\/api\/?$/, '').replace(/\/$/, '')}${path.startsWith('/') ? path : `/${path}`}`;
}

async function loadTemplates(targetPage = 1) {
  loading.value = true;
  try {
    const result = await listDishTemplates({ ...query, page: targetPage, pageSize: page.pageSize });
    Object.assign(page, { items: result.items || [], total: result.total || 0, page: result.page || targetPage, pageSize: result.pageSize || 20 });
  } finally { loading.value = false; }
}

function toggle(item) {
  if (item.imported) return;
  if (selectedIds.value.includes(item.templateId)) selectedIds.value = selectedIds.value.filter(id => id !== item.templateId);
  else if (selectedIds.value.length < 100) selectedIds.value = [...selectedIds.value, item.templateId];
}

function selectCurrentPage() {
  const available = page.items.filter(item => !item.imported).map(item => item.templateId);
  selectedIds.value = Array.from(new Set([...selectedIds.value, ...available])).slice(0, 100);
}

async function submitImport() {
  importing.value = true;
  try {
    const result = await importDishTemplates(selectedIds.value);
    notify(`导入完成：成功 ${result.importedCount || 0} 道，跳过 ${result.skippedCount || 0} 道`, 'success');
    selectedIds.value = [];
    await loadTemplates(page.page);
  } finally { importing.value = false; }
}

async function openChangeEditor(templateId) {
  editorOpen.value = true;
  editorTemplateId.value = templateId;
  submitNote.value = '';
  editorForm.value = null;
  editorForm.value = createTemplateChangeForm(await getDishTemplateDetail(templateId));
}

function closeChangeEditor() {
  if (editorSaving.value || uploading.value) return;
  editorOpen.value = false;
  editorForm.value = null;
}

function addTemplateIngredient() {
  editorForm.value.ingredients.push({ ingredientName: '', ingredientCategory: '', quantity: 0, unit: '克', calcType: 'FIXED', sortOrder: editorForm.value.ingredients.length + 1 });
}

function removeTemplateIngredient(index) {
  if (editorForm.value.ingredients.length <= 1) { notify('至少保留 1 项食材', 'error'); return; }
  editorForm.value.ingredients.splice(index, 1);
}

async function uploadTemplateImage(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  uploading.value = true;
  try { const result = await uploadDishImage(file); editorForm.value.imageUrl = result.url || result.imageUrl || ''; }
  finally { uploading.value = false; event.target.value = ''; }
}

async function submitChange() {
  const targetSnapshot = buildTemplateSnapshot(editorForm.value);
  const validation = validateTemplateSnapshot(targetSnapshot);
  if (validation) { notify(validation, 'error'); return; }
  editorSaving.value = true;
  try {
    const result = await submitDishTemplateChange(editorTemplateId.value, { submitNote: submitNote.value, targetSnapshot });
    notify(`修改申请 #${result.requestId} 已提交`, 'success');
    editorOpen.value = false;
    editorForm.value = null;
    router.push('/dish-template-changes');
  } finally { editorSaving.value = false; }
}

onMounted(async () => {
  categories.value = await listDishTemplateCategories();
  await loadTemplates(1);
});
</script>

<style scoped>
.template-workspace { display:grid; gap:18px; }
.template-toolbar,.template-footer { display:flex; justify-content:space-between; align-items:end; gap:20px; padding:18px 22px; background:#fff; border:1px solid #e5e7eb; border-radius:8px; }
.template-filters { display:grid; grid-template-columns:180px minmax(240px,1fr) 160px auto; align-items:end; gap:14px; flex:1; }
.compact-field { margin:0; }.search-button { min-height:42px; }.selection-tools,.pagination,.footer-actions { display:flex; align-items:center; gap:14px; white-space:nowrap; }
.template-table-wrap { overflow:auto; background:#fff; border:1px solid #e5e7eb; border-radius:8px; }.template-table { min-width:980px; }.select-column { width:42px; }
.dish-cell { display:flex; align-items:center; gap:14px; min-width:320px; }.dish-cell img { width:68px; height:68px; flex:none; object-fit:cover; border-radius:6px; background:#f3f4f6; }.dish-cell span { display:block; max-width:330px; margin-top:5px; color:#6b7280; font-size:12px; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; }
tr.imported { opacity:.62; }.status-imported { color:#6b7280; }.status-ready { color:#287a4b; font-weight:700; }.template-empty { padding:70px; text-align:center; color:#6b7280; }
@media (max-width: 900px) { .template-toolbar,.template-footer { align-items:stretch; flex-direction:column; }.template-filters { grid-template-columns:1fr 1fr; }.selection-tools { justify-content:space-between; }.footer-actions { margin-left:auto; } }
.template-editor-layer{position:fixed;inset:0;z-index:90;display:grid;place-items:center;padding:24px;background:rgba(26,34,29,.56);backdrop-filter:blur(4px)}.template-editor-modal{width:min(1080px,100%);max-height:calc(100vh - 48px);overflow:auto;border:1px solid var(--line);border-radius:18px;background:#fffdf8;box-shadow:0 28px 90px rgba(22,32,26,.28)}.template-editor-modal>header{position:sticky;top:0;z-index:2;display:flex;align-items:flex-start;justify-content:space-between;padding:22px 24px;border-bottom:1px solid var(--line);background:#fffaf0}.template-editor-modal h3{margin:5px 0;font-size:24px}.template-editor-modal header p{margin:0;color:var(--text-soft)}.template-editor-body{display:grid;gap:0}.editor-section{padding:20px 24px;border-bottom:1px solid var(--line)}.editor-section h4{margin:0 0 16px;font-size:17px}.editor-section-head{display:flex;align-items:center;justify-content:space-between}.editor-section-head h4{margin:0}.editor-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;flex:1}.editor-wide{grid-column:1/-1}.meal-field{display:flex;align-items:center;gap:18px;padding:12px 14px;border:1px solid var(--line);border-radius:10px}.meal-field legend{padding:0 6px;color:var(--text-soft);font-size:12px}.meal-field label{display:flex;align-items:center;gap:6px}.editor-image-row{display:flex;align-items:flex-start;gap:18px;margin-top:16px}.editor-image-row>img{width:180px;height:150px;flex:none;object-fit:cover;border-radius:8px;background:#f0ece4}.upload-label{display:inline-flex;align-items:center;cursor:pointer}.upload-label input{display:none}.template-ingredient-list{display:grid;gap:10px;margin-top:16px}.template-ingredient-row{display:grid;grid-template-columns:1.2fr 1fr .7fr .6fr 1fr auto;gap:8px}.template-ingredient-row input,.template-ingredient-row select{min-width:0;min-height:40px;padding:8px 10px;border:1px solid var(--line-strong);border-radius:8px;background:#fff}.template-editor-body>footer{position:sticky;bottom:0;display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;background:#fffaf0;border-top:1px solid var(--line)}@media(max-width:760px){.template-editor-layer{padding:10px}.template-editor-modal{max-height:calc(100vh - 20px)}.editor-form-grid{grid-template-columns:1fr}.editor-wide{grid-column:auto}.editor-image-row{flex-direction:column}.template-ingredient-row{grid-template-columns:1fr 1fr}.meal-field{align-items:flex-start;flex-wrap:wrap}}
</style>
