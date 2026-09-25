<template>
  <div v-if="form.templateType !== 'COMPONENT'" class="food-information">
    <label class="form-field"><span>菜品类型</span><select v-model="form.productType" :disabled="disabled"><option value="NORMAL">普通菜品</option><option value="NOURISHMENT">滋补食品</option></select></label>
    <template v-if="form.productType === 'NOURISHMENT'">
      <p class="food-hint">以下内容均可留空，仅用于食品信息说明，不作疾病治疗或疗效承诺。</p>
      <label v-for="[key, label] in NOURISHMENT_FIELDS" :key="key" class="form-field"><span>{{ label }}（选填）</span><textarea v-model="form[key]" maxlength="1000" :disabled="disabled" placeholder="可留空，最多 1000 字" /></label>
    </template>
  </div>
</template>
<script setup>
import { NOURISHMENT_FIELDS } from '../utils/dish-template-changes';
defineProps({ form: { type: Object, required: true }, disabled: Boolean });
</script>
<style scoped>
.food-information{display:grid;gap:14px;min-width:0}.food-hint{margin:0;color:var(--text-soft);font-size:12px}.food-information textarea{min-height:88px;resize:vertical}
</style>
