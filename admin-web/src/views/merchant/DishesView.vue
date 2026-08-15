<template>
  <div class="view-stack">
    <SectionCard title="分类与筛选" subtitle="先选分类，再维护菜品与制作流程">
      <div class="toolbar-grid">
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
                  <div class="table-sub">{{ item.description || '暂无描述' }}</div>
                </td>
                <td>{{ resolveCategoryName(item.categoryId) }}</td>
                <td>¥{{ item.basePrice ?? item.price }}</td>
                <td><StatusPill :status="String(item.status || '').toUpperCase()" /></td>
                <td><button class="text-button" type="button" @click="editDish(item.dishId)">编辑</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </SectionCard>

      <SectionCard :title="editingDishId ? '编辑菜品' : '新建菜品'" subtitle="维护基础信息、原材料和制作步骤">
        <form class="stack-form" @submit.prevent="submitDish">
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
                <textarea v-model.trim="item.content" placeholder="步骤内容" />
                <button class="text-button danger-text" type="button" @click="removeStepRow(index)">删除</button>
              </div>
            </div>
          </div>

          <div class="detail-actions">
            <button class="ghost-button" type="button" @click="resetForm">重置</button>
            <button class="primary-button" type="submit">保存菜品</button>
          </div>
        </form>
      </SectionCard>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import StatusPill from '../../components/StatusPill.vue';
import { promptAction } from '../../utils/dialog';
import {
  createDishCategory,
  createMerchantDish,
  getMerchantDishDetail,
  listDishCategories,
  listMerchantDishes,
  updateMerchantDish
} from '../../api/dishes';
import { uploadDishImage } from '../../api/files';
import { listMerchantIngredients } from '../../api/ingredients';

const loading = ref(false);
const router = useRouter();
const dishes = ref([]);
const categories = ref([]);
const ingredientOptions = ref([]);
const keyword = ref('');
const categoryFilter = ref('');
const editingDishId = ref('');
const form = reactive(createDishForm());

function createDishForm() {
  return {
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
  loading.value = true;
  try {
    const [dishRows, categoryRows, ingredientRows] = await Promise.all([
      listMerchantDishes(),
      listDishCategories(),
      listMerchantIngredients()
    ]);
    dishes.value = dishRows;
    categories.value = categoryRows;
    ingredientOptions.value = ingredientRows;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  editingDishId.value = '';
  assignForm(createDishForm());
}

function createNewDish() {
  resetForm();
}

function openTemplateMarket() {
  router.push('/dish-templates');
}

async function editDish(dishId) {
  editingDishId.value = dishId;
  const detail = await getMerchantDishDetail(dishId);
  assignForm({
    name: detail.name,
    categoryId: detail.categoryId,
    description: detail.description,
    imageUrl: detail.imageUrl,
    basePrice: detail.basePrice ?? detail.price ?? 0,
    status: String(detail.status || 'active').toLowerCase(),
    ingredients: detail.ingredients || [],
    cookingSteps: detail.cookingSteps || []
  });
}

async function submitDish() {
  const payload = {
    name: form.name,
    categoryId: Number(form.categoryId),
    description: form.description,
    imageUrl: form.imageUrl,
    basePrice: Number(form.basePrice || 0),
    ingredients: form.ingredients.filter((item) => item.ingredientName),
    cookingSteps: form.cookingSteps
      .map((item, index) => ({ stepNo: index + 1, title: item.title, content: item.content }))
      .filter((item) => item.title || item.content),
    status: form.status
  };

  if (editingDishId.value) {
    await updateMerchantDish(editingDishId.value, payload);
  } else {
    await createMerchantDish(payload);
  }

  await loadData();
  resetForm();
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
  form.cookingSteps.push({ title: '', content: '' });
}

function removeStepRow(index) {
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
