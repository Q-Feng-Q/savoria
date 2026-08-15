<template>
  <Transition name="toast">
    <div v-if="visible" class="app-toast" :class="`is-${tone}`" role="status" aria-live="polite">
      <span class="app-toast__dot" aria-hidden="true" />
      <span>{{ message }}</span>
    </div>
  </Transition>
</template>

<script setup>
defineProps({
  message: { type: String, default: '' },
  tone: { type: String, default: 'info' },
  visible: { type: Boolean, default: false }
});
</script>
