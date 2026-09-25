<template>
  <div class="view-stack">
    <SectionCard title="分类与筛选" subtitle="先选分类，再维护菜品与制作流程">
      <div class="toolbar-grid">
        <label class="form-field"><span>菜品类型</span><select v-model="productTypeFilter" @change="loadData"><option value="">全部类型</option><option value="NORMAL">普通菜品</option><option value="NOURISHMENT">滋补食品</option></select></label>
        <label class="form-field">
          <span>分类</span>
          <select v-model="categoryFilter">
            <option value="">全部分类</option>
            <option v-for="item in categories" :key="item.categoryId || item.id" :value="String(item.categoryId || item.id)">
              {{ item.name }}
            </option>
          </select>
        </label>

        <label class="form-field">
          <span>搜索</span>
          <input v-model.trim="keyword" type="text" placeholder="搜索菜名或描述" />
        </label>

        <div class="toolbar-actions">
          <button class="ghost-button" type="button" @click="openTemplateMarket">从模板导入</button>
          <button class="ghost-button" type="button" @click="openCreateCategory">新增分类</button>
          <button class="primary-button" type="button" @click="createNewDish">新增菜品</button>
        </div>
      </div>
    </SectionCard>

    <div class="content-grid order-layout">
      <SectionCard title="菜品列表" subtitle="真实接口菜品数据与上架状态">
        <div v-if="loading" class="table-empty">加载中...</div>

        <AppEmpty
          v-else-if="!filteredDishes.length"
          title="暂无菜品"
          description="可以先新增一个菜品，后续再配置家庭菜单。"
        />

        <div v-else class="table-card">
          <table class="table">
            <thead>
              <tr>
                <th>菜品</th>
                <th>分类</th>
                <th>价格</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredDishes" :key="item.dishId">
                <td>
                  <strong>{{ item.name }}</strong>
                  <ProductTypeBadge :value="item.productType" :template-type="item.templateType" />
                  <div class="table-sub">{{ item.description || '暂无描述' }}</div>
                </td>
                <td>{{ resolveCategoryName(item.categoryId) }}</td>
                <td>¥{{ item.basePrice ?? item.price }}</td>
                <td><StatusPill :status="String(item.status || '').toUpperCase()" /></td>
                <td>
                  <div class="inline-actions">
                    <button class="text-button" type="button" @click="editDish(item.dishId)">编辑</button>
                    <button v-if="isImportedDish(item)" class="text-button" type="button"
                      :disabled="syncingDishId === item.dishId" @click="syncDishToTemplate(item)">
                      {{ syncingDishId === item.dishId ? '提交中...' : '同步模板' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </SectionCard>

      <SectionCard :title="editingDishId ? '编辑菜品' : '新建菜品'" subtitle="维护基础信息、原材料和制作步骤">
        <p v-if="editorLoading">正在加载菜品详情…</p>
        <form v-if="!editorLoading" class="stack-form" @submit.prevent="submitDish">
          <NourishmentFields :form="form" :disabled="saving" />
          <div class="form-grid two-columns">
            <label class="form-field">
              <span>菜名</span>
              <input v-model.trim="form.name" type="text" placeholder="例如：土豆烧牛腩" />
            </label>

            <label class="form-field">
              <span>分类</span>
              <select v-model="form.categoryId">
                <option value="">请选择分类</option>
                <option v-for="item in categories" :key="item.categoryId || item.id" :value="Number(item.categoryId || item.id)">
                  {{ item.name }}
                </option>
              </select>
            </label>

            <label class="form-field">
              <span>价格</span>
              <input v-model.number="form.basePrice" type="number" min="0" step="0.01" />
            </label>

            <label class="form-field">
              <span>状态</span>
              <select v-model="form.status">
                <option value="active">上架</option>
                <option value="inactive">下架</option>
              </select>
            </label>
          </div>

          <label class="form-field">
            <span>描述</span>
            <textarea v-model.trim="form.description" placeholder="例如：软糯下饭，适合午餐晚餐" />
          </label>

          <div class="form-grid two-columns">
            <label class="form-field">
              <span>图片地址</span>
              <input v-model.trim="form.imageUrl" type="text" placeholder="/uploads/images/xxx.png" />
            </label>

            <label class="form-field">
              <span>上传图片</span>
              <input type="file" accept="image/*" @change="handleUpload" />
            </label>
          </div>

          <div class="stack-editor">
            <div class="editor-head">
              <strong>原材料</strong>
              <button class="ghost-button" type="button" @click="addIngredientRow">新增原材料</button>
            </div>

            <div class="editor-rows">
              <div v-for="(item, index) in form.ingredients" :key="`ingredient-${index}`" class="editor-row four">
                <select v-model="item.ingredientName" @change="syncIngredientMeta(item)">
                  <option value="">请选择食材</option>
                  <option v-for="ingredient in ingredientOptions" :key="ingredient.ingredientId" :value="ingredient.name">
                    {{ ingredient.name }}
                  </option>
                </select>

                <input v-model.number="item.quantity" type="number" min="0" step="0.01" placeholder="数量" />

                <input v-model.trim="item.unit" type="text" placeholder="单位" />

                <button class="text-button danger-text" type="button" @click="removeIngredientRow(index)">删除</button>
              </div>
            </div>
          </div>

          <div class="stack-editor">
            <div class="editor-head">
              <strong>制作步骤</strong>
              <button class="ghost-button" type="button" @click="addStepRow">新增步骤</button>
            </div>

            <div class="editor-rows">
              <div v-for="(item, index) in form.cookingSteps" :key="`step-${index}`" class="editor-row step">
                <input v-model.trim="item.title" type="text" placeholder="步骤标题" />
                <textarea v-model.trim="item.content" placeholder="步骤内容" /><StepImages v-model="item.imageUrls" editable :disabled="saving || stepUploading" @busy="stepUploading = $event" />
                <button class="text-button danger-text" type="button" @click="removeStepRow(index)">删除</button>
              </div>
            </div>
          </div>

          <div class="detail-actions">
            <button class="ghost-button" type="button" @click="resetForm">重置</button>
            <button v-if="editingDishId && editingSourceTemplateId" class="ghost-button" type="button"
              :disabled="syncingDishId === editingDishId" @click="syncDishToTemplate({ dishId: editingDishId, name: form.name, sourceTemplateId: editingSourceTemplateId })">
              {{ syncingDishId === editingDishId ? '提交中...' : '同步模板' }}
            </button>
            <button class="primary-button" type="submit" :disabled="saving || stepUploading">{{ saving ? '保存中...' : '保存菜品' }}</button>
          </div>
        </form>
      </SectionCard>
    </div>
  </div>
</template>

<script setup>
import StepImages from '../../components/StepImages.vue';
import { validateDishStepImages } from '../../utils/step-images';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import StatusPill from '../../components/StatusPill.vue';
import ProductTypeBadge from '../../components/ProductTypeBadge.vue';
import NourishmentFields from '../../components/NourishmentFields.vue';
import { foodInformation, validateFoodInformation } from '../../utils/dish-template-changes';
import { promptAction } from '../../utils/dialog';
import {
  createDishCategory,
  createMerchantDish,
  getMerchantDishDetail,
  listDishCategories,
  listMerchantDishes,
  submitImportedDishTemplateChange,
  updateMerchantDish
} from '../../api/dishes';
import { uploadDishImage } from '../../api/files';
import { listMerchantIngredients } from '../../api/ingredients';
import { notify } from '../../utils/feedback';

const loading = ref(false);
const saving = ref(false);
const stepUploading = ref(false);
const editorLoading = ref(false);
let editorGeneration = 0;
let loadGeneration = 0;
const router = useRouter();
const dishes = ref([]);
const categories = ref([]);
const ingredientOptions = ref([]);
const keyword = ref('');
const categoryFilter = ref('');
const productTypeFilter = ref('');
const editingDishId = ref('');
const editingSourceTemplateId = ref(null);
const syncingDishId = ref(null);
const form = reactive(createDishForm());

function createDishForm() {
  return {
    ...foodInformation(),
    name: '',
    categoryId: '',
    description: '',
    imageUrl: '',
    basePrice: 0,
    status: 'active',
    ingredients: [{ ingredientName: '', quantity: 0, unit: 'g', calcType: 'FIXED' }],
    cookingSteps: [{ title: '', content: '' }]
  };
}

const filteredDishes = computed(() => {
  return dishes.value.filter((item) => {
    const matchesCategory = !categoryFilter.value || String(item.categoryId) === categoryFilter.value;
    const text = `${item.name || ''} ${item.description || ''}`.toLowerCase();
    const matchesKeyword = !keyword.value || text.includes(keyword.value.toLowerCase());
    return matchesCategory && matchesKeyword;
  });
});

function assignForm(payload) {
  Object.assign(form, createDishForm(), {
    ...payload,
    ingredients: payload.ingredients?.length
      ? payload.ingredients.map((item) => ({ ...item }))
      : [{ ingredientName: '', quantity: 0, unit: 'g', calcType: 'FIXED' }],
    cookingSteps: payload.cookingSteps?.length
      ? payload.cookingSteps.map((item) => ({ ...item }))
      : [{ title: '', content: '' }]
  });
}

function resolveCategoryName(categoryId) {
  const current = categories.value.find((item) => String(item.categoryId || item.id) === String(categoryId));
  return current?.name || `分类 ${categoryId}`;
}

function syncIngredientMeta(item) {
  const current = ingredientOptions.value.find((ingredient) => ingredient.name === item.ingredientName);
  if (current && !item.unit) {
    item.unit = current.unit || '';
  }
}

async function loadData() {
  const generation = ++loadGeneration;
  loading.value = true;
  try {
    const [dishRows, categoryRows, ingredientRows] = await Promise.all([
      listMerchantDishes({ productType: productTypeFilter.value }),
      listDishCategories(),
      listMerchantIngredients()
    ]);
    if (generation !== loadGeneration) return;
    dishes.value = dishRows;
    categories.value = categoryRows;
    ingredientOptions.value = ingredientRows;
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}

function resetForm() {
  if (stepUploading.value) return;
  editorGeneration++;
  editorLoading.value = false;
  editingDishId.value = '';
  editingSourceTemplateId.value = null;
  assignForm(createDishForm());
}

function createNewDish() {
  resetForm();
}

function openTemplateMarket() {
  router.push('/dish-templates');
}

async function editDish(dishId) {
  if (saving.value || stepUploading.value) return;
  const generation = ++editorGeneration;
  editorLoading.value = true;
  editingDishId.value = dishId;
  try {
  const detail = await getMerchantDishDetail(dishId);
  if (generation !== editorGeneration) return;
  editingSourceTemplateId.value = detail.sourceTemplateId || null;
  assignForm({
    ...foodInformation(detail),
    name: detail.name,
    categoryId: detail.categoryId,
    description: detail.description,
    imageUrl: detail.imageUrl,
    basePrice: detail.basePrice ?? detail.price ?? 0,
    status: String(detail.status || 'active').toLowerCase(),
    ingredients: detail.ingredients || [],
    cookingSteps: detail.cookingSteps || []
  });
  } catch (error) {
    if (generation === editorGeneration) { resetForm(); notify(error.message || '菜品加载失败', 'error'); }
  } finally { if (generation === editorGeneration) editorLoading.value = false; }
}

function isImportedDish(dish) {
  return Boolean(dish?.templateImported || dish?.sourceTemplateId);
}

async function syncDishToTemplate(dish) {
  if (!dish?.dishId || !isImportedDish(dish) || syncingDishId.value) return;
  const submitNote = await promptAction({
    title: `同步“${dish.name || '当前菜品'}”到模板`,
    label: '申请说明（选填）',
    placeholder: '例如：采用商户实测后的食材用量',
    message: '提交后进入平台审核，不会立即修改系统菜库，制作步骤不会同步。',
    required: false,
    confirmText: '提交审核'
  });
  if (submitNote === null) return;
  syncingDishId.value = dish.dishId;
  try {
    const result = await submitImportedDishTemplateChange(dish.dishId, {
      submitNote: String(submitNote || '').trim() || null
    });
    notify(`模板修改申请 #${result.requestId} 已提交`, 'success');
    await router.push({ name: 'dish-template-changes', query: { requestId: result.requestId } });
  } finally {
    syncingDishId.value = null;
  }
}

async function submitDish() {
  if (saving.value || stepUploading.value || editorLoading.value) return;
  const validation = validateFoodInformation(form) || validateDishStepImages(form.cookingSteps);
  if (validation) { notify(validation, 'error'); return; }
  const payload = {
    ...foodInformation(form),
    name: form.name,
    categoryId: Number(form.categoryId),
    description: form.description,
    imageUrl: form.imageUrl,
    basePrice: Number(form.basePrice || 0),
    ingredients: form.ingredients.filter((item) => item.ingredientName),
    cookingSteps: form.cookingSteps
      .map((item, index) => ({ ...item, stepNo: index + 1, title: item.title, content: item.content, imageUrls: [...(item.imageUrls || [])] }))
      .filter((item) => item.title || item.content),
    status: form.status
  };

  saving.value = true;
  try {
    if (editingDishId.value) {
      await updateMerchantDish(editingDishId.value, payload);
    } else {
      await createMerchantDish(payload);
    }

    await loadData();
    resetForm();
  } finally { saving.value = false; }
}

async function handleUpload(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const result = await uploadDishImage(file);
  form.imageUrl = result.url || result.imageUrl || '';
}

function addIngredientRow() {
  form.ingredients.push({ ingredientName: '', quantity: 0, unit: 'g', calcType: 'FIXED' });
}

function removeIngredientRow(index) {
  form.ingredients.splice(index, 1);
  if (!form.ingredients.length) {
    addIngredientRow();
  }
}

function addStepRow() {
  if (saving.value || stepUploading.value) return;
  form.cookingSteps.push({ title: '', content: '' });
}

function removeStepRow(index) {
  if (saving.value || stepUploading.value) return;
  form.cookingSteps.splice(index, 1);
  if (!form.cookingSteps.length) {
    addStepRow();
  }
}

async function openCreateCategory() {
  const name = await promptAction({ title: '新增菜品分类', label: '分类名称', placeholder: '例如：家常热菜' });
  if (!name) return;
  await createDishCategory({ name });
  categories.value = await listDishCategories();
}

onMounted(() => {
  loadData();
  resetForm();
});
</script>
