<template>
  <span class="brand-logo" :style="{ width: `${logo.size}px` }" :aria-label="logo.name" role="img">
    <img v-if="!textOnly" :src="src" alt="" @error="onError" />
    <span v-else class="brand-logo__text">{{ logo.name }}</span>
  </span>
</template>
<script setup>
import { computed, ref, watch } from 'vue';
import { branding, getApiBaseUrl } from '../stores/branding';
import { resolveLogo } from '../utils/branding';
const props = defineProps({ variant: { type: String, default: 'standard' }, config: { type: Object, default: null } });
const logo = computed(() => resolveLogo(props.config || branding, props.variant, getApiBaseUrl()));
const src = ref('');
const textOnly = ref(false);
watch(() => [logo.value.src, logo.value.fallback], () => { src.value = logo.value.src; textOnly.value = false; }, { immediate: true });
function onError() {
  if (src.value !== logo.value.fallback) src.value = logo.value.fallback;
  else textOnly.value = true;
}
</script>
<style scoped>
.brand-logo { display: inline-flex; flex: 0 1 auto; max-width: 100%; aspect-ratio: 1; vertical-align: middle; align-items: center; justify-content: center; overflow: hidden; }
.brand-logo img { display: block; width: 100%; height: 100%; object-fit: contain; }
.brand-logo__text { font-size: 12px; line-height: 1.2; overflow-wrap: anywhere; text-align: center; }
</style>
