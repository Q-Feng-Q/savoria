<template>
  <article class="stat-card">
    <span class="stat-label">{{ label }}</span>
    <strong class="stat-value">{{ value }}</strong>
    <span class="stat-note">{{ note }}</span>
  </article>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  note: { type: String, default: '' }
});
</script>
