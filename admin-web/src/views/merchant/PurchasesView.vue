<template>
  <div class="view-stack">
    <SectionCard title="采购汇总" subtitle="支持预估、家庭明细、勾选和临时补录">
      <div class="toolbar-grid">
        <label class="form-field">
          <span>日期</span>
          <input v-model="date" type="date" />
        </label>

        <label class="form-field">
          <span>家庭明细</span>
          <select v-model="selectedFamilyId" @change="loadFamilyPurchaseItems">
            <option value="">请选择家庭</option>
            <option v-for="item in families" :key="item.familyId" :value="item.familyId">{{ item.familyName }}</option>
          </select>
        </label>

        <label class="form-field">
          <span>采购文本餐次</span>
          <select v-model="selectedCopyMealSlotId">
            <option v-for="item in mealSlotFilterOptions" :key="`copy-${item.value || 'all'}`" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>

        <div class="toolbar-actions">
          <button class="ghost-button" type="button" @click="copyText">复制采购文本</button>
          <button class="primary-button" type="button" @click="loadData">刷新采购</button>
        </div>
      </div>

      <div class="content-grid order-layout">
        <div class="table-card">
          <table class="table">
            <thead>
              <tr>
                <th>食材</th>
                <th>数量</th>
                <th>来源状态</th>
                <th>来源数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in summaryRows" :key="`${item.ingredientName}-${item.unit}`">
                <td>{{ item.ingredientName }}</td>
                <td>{{ item.quantity }} {{ item.unit }}</td>
                <td>{{ item.sourceStatus }}</td>
                <td>{{ item.sources?.length || 0 }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <SectionCard title="家庭采购明细" subtitle="勾选已买、删除临时项、补录新项">
          <AppEmpty
            v-if="!selectedFamilyId"
            title="先选择家庭"
            description="右侧家庭明细依赖 familyId，选中后才能看到可勾选的采购行。"
          />

          <div v-else class="detail-stack">
            <div class="item-stack">
              <article
                v-for="item in familyRows"
                :key="item.itemId || `${item.ingredientName}-${item.remark}`"
                class="list-row-card static"
              >
                <div>
                  <strong>{{ item.ingredientName }}</strong>
                  <p>{{ item.quantity }} {{ item.unit }} · {{ item.sourceStatus || 'CONFIRMED' }} · {{ item.remark || '无备注' }}</p>
                </div>
                <div class="inline-actions">
                  <label v-if="item.itemId" class="checkbox-line">
                    <input :checked="Boolean(item.checked)" type="checkbox" @change="toggleChecked(item, $event)" />
                    <span>已买</span>
                  </label>
                  <button v-if="item.tempItem || item.manual" class="text-button danger-text" type="button" @click="deleteTempItem(item)">
                    删除
                  </button>
                </div>
              </article>
            </div>

            <div class="section-divider"></div>

            <div class="form-grid two-columns">
              <label class="form-field">
                <span>食材名</span>
                <input v-model.trim="tempForm.ingredientName" type="text" />
              </label>

              <label class="form-field">
                <span>数量</span>
                <input v-model.number="tempForm.quantity" type="number" min="0" step="0.01" />
              </label>

              <label class="form-field">
                <span>单位</span>
                <input v-model.trim="tempForm.unit" type="text" />
              </label>

              <label class="form-field">
                <span>餐次</span>
                <select v-model.number="tempForm.mealSlotId">
                  <option v-for="item in mealSlotOptions" :key="`temp-${item.value}`" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>
            </div>

            <label class="form-field">
              <span>备注</span>
              <input v-model.trim="tempForm.remark" type="text" />
            </label>

            <div class="detail-actions">
              <button class="primary-button" type="button" @click="createTempItem">补录临时采购</button>
            </div>
          </div>
        </SectionCard>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import { listMerchantFamilies } from '../../api/families';
import {
  createTempPurchaseItem,
  deleteTempPurchaseItem,
  getPurchaseByFamily,
  getPurchaseCopyText,
  getPurchaseSummary,
  togglePurchaseChecked
} from '../../api/purchases';
import { MEAL_SLOT_FILTER_OPTIONS, MEAL_SLOT_OPTIONS } from '../../constants/meal-slots';

const date = ref(new Date().toISOString().slice(0, 10));
const families = ref([]);
const selectedFamilyId = ref('');
const selectedCopyMealSlotId = ref('');
const summaryRows = ref([]);
const familyRows = ref([]);
const mealSlotOptions = MEAL_SLOT_OPTIONS;
const mealSlotFilterOptions = MEAL_SLOT_FILTER_OPTIONS;
const tempForm = reactive({
  ingredientName: '',
  quantity: 1,
  unit: '个',
  mealSlotId: 20,
  remark: '临时补录'
});

async function loadFamilies() {
  families.value = await listMerchantFamilies();
}

async function loadSummary() {
  summaryRows.value = await getPurchaseSummary({
    date: date.value,
    includePending: true
  });
}

async function loadFamilyPurchaseItems() {
  if (!selectedFamilyId.value) {
    familyRows.value = [];
    return;
  }

  familyRows.value = await getPurchaseByFamily({
    familyId: selectedFamilyId.value,
    date: date.value,
    includePending: true
  });
}

async function loadData() {
  await Promise.all([loadFamilies(), loadSummary()]);
  if (selectedFamilyId.value) {
    await loadFamilyPurchaseItems();
  }
}

async function toggleChecked(item, event) {
  await togglePurchaseChecked(item.itemId, event.target.checked);
  await loadFamilyPurchaseItems();
}

async function createTempItem() {
  await createTempPurchaseItem({
    date: date.value,
    mealSlotId: Number(tempForm.mealSlotId || 20),
    ingredientName: tempForm.ingredientName,
    quantity: Number(tempForm.quantity || 0),
    unit: tempForm.unit,
    remark: tempForm.remark
  });
  await loadData();
  await loadFamilyPurchaseItems();
}

async function deleteTempItem(item) {
  if (!item.itemId) return;
  await deleteTempPurchaseItem(item.itemId);
  await loadFamilyPurchaseItems();
  await loadSummary();
}

async function copyText() {
  const params = {
    date: date.value
  };

  if (selectedCopyMealSlotId.value) {
    params.mealSlotId = Number(selectedCopyMealSlotId.value);
  }

  const result = await getPurchaseCopyText(params);
  const text = typeof result === 'string' ? result : (result.text || '');
  await navigator.clipboard.writeText(text);
}

onMounted(loadData);
</script>
