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
        <button class="text-button" type="button" @click="selectCurrentPage">全选当前页</button>
        <button class="text-button" type="button" @click="selectedIds = []">清空</button>
      </div>
    </header>

    <div v-if="loading" class="template-empty">加载模板菜品...</div>
    <AppEmpty v-else-if="!page.items.length" title="没有匹配的模板菜" description="调整分类、关键词或导入状态后再试。" />
    <div v-else class="template-table-wrap">
      <table class="table template-table">
        <thead><tr><th class="select-column"></th><th>菜品</th><th>分类</th><th>口味</th><th>食材</th><th>参考价</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="item in page.items" :key="item.templateId" :class="{ imported: item.imported }">
            <td><input type="checkbox" :checked="selectedIds.includes(item.templateId)" :disabled="item.imported" @change="toggle(item)" /></td>
            <td><div class="dish-cell"><img :src="item.imageUrl" :alt="item.name" /><div><strong>{{ item.name }}</strong><span>{{ item.description }}</span></div></div></td>
            <td>{{ item.categoryName }}</td>
            <td>{{ (item.tasteTags || []).join('、') || '家常' }}</td>
            <td>{{ item.ingredientCount }} 种</td>
            <td>¥{{ item.referencePrice }}</td>
            <td><span :class="item.imported ? 'status-imported' : 'status-ready'">{{ item.imported ? '已导入' : '可导入' }}</span></td>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppEmpty from '../../components/AppEmpty.vue';
import { importDishTemplates, listDishTemplateCategories, listDishTemplates } from '../../api/dishes';
import { notify } from '../../utils/feedback';

const router = useRouter();
const categories = ref([]);
const selectedIds = ref([]);
const loading = ref(false);
const importing = ref(false);
const query = reactive({ categoryId: '', keyword: '', imported: '' });
const page = reactive({ items: [], total: 0, page: 1, pageSize: 20 });

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
</style>
