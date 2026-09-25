<template><small v-if="value === 'NOURISHMENT' && templateType !== 'COMPONENT'" class="product-type-badge">滋补食品</small></template>
<script setup>
defineProps({ value: { type: String, default: 'NORMAL' }, templateType: String });
</script>
<style scoped>
.product-type-badge{display:inline-block;padding:3px 7px;margin:4px 6px;border:1px solid var(--line);border-radius:5px;font-size:11px;color:var(--brand-deep);white-space:nowrap}
</style>
