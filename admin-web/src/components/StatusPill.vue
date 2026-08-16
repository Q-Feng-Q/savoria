<template>
  <span class="status-pill" :class="toneClass">{{ displayLabel }}</span>
</template>

<script setup>
import { computed } from 'vue';
import { ADMIN_STATUS_LABELS } from '../config';

const props = defineProps({
  status: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: ''
  }
});

const displayLabel = computed(() => props.label || ADMIN_STATUS_LABELS[props.status] || props.status || '未知');
const toneClass = computed(() => {
  const status = String(props.status || '').toUpperCase();
  if (status === 'PENDING') return 'tone-amber';
  if (status === 'CONFIRMED' || status === 'DONE' || status === 'APPROVED') return 'tone-green';
  if (status === 'PREPARING' || status === 'READY') return 'tone-blue';
  if (status === 'CANCELLED' || status === 'REJECTED') return 'tone-red';
  return 'tone-slate';
});
</script>
