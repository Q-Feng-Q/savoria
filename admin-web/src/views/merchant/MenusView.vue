<template>
  <div class="view-stack">
    <SectionCard title="家庭菜单配置" subtitle="按家庭启停菜品并保存家庭专属价格">
      <div class="toolbar-grid">
        <label class="form-field">
          <span>目标家庭</span>
          <select v-model="selectedFamilyId" @change="loadMenu">
            <option v-for="item in families" :key="item.familyId" :value="item.familyId">{{ item.familyName }}</option>
          </select>
        </label>
        <label class="form-field">
          <span>从其他家庭复制</span>
          <select v-model="copySourceFamilyId">
            <option value="">请选择来源家庭</option>
            <option v-for="item in copySourceOptions" :key="item.familyId" :value="item.familyId">{{ item.familyName }}</option>
          </select>
        </label>
        <div class="toolbar-actions">
          <button class="ghost-button" type="button" @click="copyMenu">复制菜单</button>
          <button class="primary-button" type="button" @click="saveMenu">保存菜单</button>
        </div>
      </div>

      <div class="table-card">
        <table class="table">
          <thead>
            <tr>
              <th>启用</th>
              <th>菜品</th>
              <th>排序</th>
              <th>家庭价</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in menuRows" :key="item.dishId">
              <td><input v-model="item.enabled" type="checkbox" /></td>
              <td>
                <strong>{{ item.dishName }}</strong>
                <div class="table-sub">{{ item.categoryName || item.categoryId || '未分类' }}</div>
              </td>
              <td><input v-model.number="item.sortOrder" class="table-input" type="number" min="0" step="1" /></td>
              <td><input v-model.number="item.familyFinalPrice" class="table-input" type="number" min="0" step="0.01" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import SectionCard from '../../components/SectionCard.vue';
import {
  copyFamilyMenu,
  getFamilyMenu,
  listMerchantFamilies,
  saveFamilyMenu
} from '../../api/families';

const families = ref([]);
const selectedFamilyId = ref('');
const copySourceFamilyId = ref('');
const menuRows = ref([]);

const copySourceOptions = computed(() => families.value.filter((item) => item.familyId !== selectedFamilyId.value));

async function loadFamilies() {
  families.value = await listMerchantFamilies();
  if (!selectedFamilyId.value && families.value[0]) {
    selectedFamilyId.value = families.value[0].familyId;
    await loadMenu();
  }
}

async function loadMenu() {
  if (!selectedFamilyId.value) return;
  menuRows.value = await getFamilyMenu(selectedFamilyId.value);
}

async function saveMenu() {
  if (!selectedFamilyId.value) return;
  await saveFamilyMenu(selectedFamilyId.value, menuRows.value.map((item) => ({
    dishId: item.dishId,
    enabled: item.enabled,
    sortOrder: Number(item.sortOrder || 0),
    familyFinalPrice: Number(item.familyFinalPrice || 0)
  })));
  await loadMenu();
}

async function copyMenu() {
  if (!selectedFamilyId.value || !copySourceFamilyId.value) return;
  await copyFamilyMenu(selectedFamilyId.value, copySourceFamilyId.value);
  await loadMenu();
}

onMounted(loadFamilies);
</script>
