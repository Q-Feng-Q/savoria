<template>
  <div class="view-stack">
    <SectionCard title="食材管理" subtitle="支持真实接口增删改查与引用校验">
      <div class="content-grid order-layout">
        <div class="table-card">
          <table class="table">
            <thead>
              <tr>
                <th>食材名</th>
                <th>分类</th>
                <th>单位</th>
                <th>引用菜品</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in ingredients" :key="item.ingredientId">
                <td>{{ item.name }}</td>
                <td>{{ item.category }}</td>
                <td>{{ item.unit }}</td>
                <td>{{ item.referencedDishNames?.join('、') || `${item.referencedDishCount || 0} 道菜` }}</td>
                <td>
                  <button class="text-button" type="button" @click="editIngredient(item)">编辑</button>
                  <button class="text-button danger-text" type="button" :disabled="!item.removable" @click="removeIngredient(item)">
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <form class="stack-form section-card" @submit.prevent="submitIngredient">
          <header class="section-card__header">
            <div>
              <h3>{{ editingIngredientId ? '编辑食材' : '新建食材' }}</h3>
              <p>删除按钮会跟随后端的 `removable` 字段自动禁用。</p>
            </div>
          </header>
          <div class="section-card__body">
            <label class="form-field">
              <span>食材名</span>
              <input v-model.trim="form.name" type="text" />
            </label>
            <label class="form-field">
              <span>分类</span>
              <input v-model.trim="form.category" type="text" placeholder="例如：蔬菜 / 调料 / 肉类" />
            </label>
            <label class="form-field">
              <span>单位</span>
              <input v-model.trim="form.unit" type="text" placeholder="g / 个 / 袋" />
            </label>
            <div class="detail-actions">
              <button class="ghost-button" type="button" @click="resetForm">重置</button>
              <button class="primary-button" type="submit">保存</button>
            </div>
          </div>
        </form>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import SectionCard from '../../components/SectionCard.vue';
import { confirmAction } from '../../utils/dialog';
import {
  createMerchantIngredient,
  deleteMerchantIngredient,
  listMerchantIngredients,
  updateMerchantIngredient
} from '../../api/ingredients';

const ingredients = ref([]);
const editingIngredientId = ref('');
const form = reactive({
  name: '',
  category: '',
  unit: ''
});

async function loadIngredients() {
  ingredients.value = await listMerchantIngredients();
}

function resetForm() {
  editingIngredientId.value = '';
  form.name = '';
  form.category = '';
  form.unit = '';
}

function editIngredient(item) {
  editingIngredientId.value = item.ingredientId;
  form.name = item.name;
  form.category = item.category;
  form.unit = item.unit;
}

async function submitIngredient() {
  const payload = {
    name: form.name,
    category: form.category,
    unit: form.unit
  };

  if (editingIngredientId.value) {
    await updateMerchantIngredient(editingIngredientId.value, payload);
  } else {
    await createMerchantIngredient(payload);
  }

  await loadIngredients();
  resetForm();
}

async function removeIngredient(item) {
  if (!item.removable) return;
  if (!await confirmAction({ title: `删除食材“${item.name}”`, message: '删除后无法恢复。', danger: true, confirmText: '确认删除' })) return;
  await deleteMerchantIngredient(item.ingredientId);
  await loadIngredients();
}

onMounted(() => {
  loadIngredients();
  resetForm();
});
</script>
