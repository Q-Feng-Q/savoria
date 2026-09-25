<template>
  <div v-if="value.productType === 'NOURISHMENT' && value.templateType !== 'COMPONENT' && sections.length" class="food-details">
    <section v-for="section in sections" :key="section.key"><h4>{{ section.label }}</h4><p>{{ section.text }}</p></section>
  </div>
</template>
<script setup>
import { computed } from 'vue';
import { NOURISHMENT_FIELDS } from '../utils/dish-template-changes';
const props = defineProps({ value: { type: Object, required: true } });
const sections = computed(() => NOURISHMENT_FIELDS.map(([key, label]) => ({ key, label, text: String(props.value[key] || '').trim() })).filter(section => section.text));
</script>
<style scoped>
.food-details{display:grid;gap:14px}.food-details h4{margin:0 0 6px}.food-details p{margin:0;white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.65;color:var(--text-soft)}
</style>
