<template>
  <SectionCard title="品牌标识" subtitle="HTTPS 图片或后端站点路径；留空使用随应用部署的默认图">
    <fieldset class="brand-settings" :disabled="disabled || saving">
      <div class="settings-grid">
        <label class="form-field settings-grid__wide"><span>站点名称</span><input :value="modelValue.siteName" maxlength="100" required @input="change('siteName', $event.target.value)" /></label>
        <label v-for="field in urls" :key="field.key" class="form-field"><span>{{ field.label }}</span><input :value="modelValue[field.key]" maxlength="500" placeholder="https://… 或 /uploads/…" @input="change(field.key, $event.target.value)" /></label>
        <label v-for="field in sizes" :key="field.key" class="form-field"><span>{{ field.label }}（px）</span><input :value="modelValue[field.key]" type="number" :min="BRAND_SIZES[field.key][0]" :max="BRAND_SIZES[field.key][1]" step="1" required @input="change(field.key, $event.target.value === '' ? '' : Number($event.target.value))" /></label>
      </div>
      <div class="brand-previews" aria-label="品牌图片实时预览">
        <div v-for="item in previews" :key="item.variant"><BrandLogo :variant="item.variant" :config="modelValue" /><small>{{ item.label }}</small></div>
      </div>
      <p v-if="validationError" class="form-error" role="alert">{{ validationError }}</p>
      <p v-if="message" :class="failed ? 'form-error' : 'form-hint'" role="status">{{ message }}</p>
      <div class="brand-settings__actions">
        <button type="button" class="primary-button" :disabled="Boolean(validationError)" @click="save">{{ saving ? '保存中…' : '保存品牌标识' }}</button>
        <button type="button" class="secondary-button" @click="reset">恢复默认品牌图片与尺寸</button>
      </div>
    </fieldset>
  </SectionCard>
</template>
<script setup>
import { computed, ref } from 'vue';
import SectionCard from './SectionCard.vue';
import BrandLogo from './BrandLogo.vue';
import { updateBrandingSettings } from '../api/system-settings';
import { setBranding } from '../stores/branding';
import { BRAND_SIZES, brandPayload, normalizeBranding, resetBrandImages } from '../utils/branding';
const props = defineProps({ modelValue: { type: Object, required: true }, disabled: Boolean });
const emit = defineEmits(['update:modelValue', 'saving']);
const saving = ref(false), message = ref(''), failed = ref(false);
const urls = [{ key: 'siteLogoSmallUrl', label: '小号 Logo 地址' }, { key: 'siteLogoUrl', label: '标准 Logo 地址' }, { key: 'siteLogoLargeUrl', label: '大号 Logo 地址' }, { key: 'siteFaviconUrl', label: '浏览器 ICO 地址' }];
const sizes = [{ key: 'siteLogoSmallSize', label: '小号展示尺寸' }, { key: 'siteLogoSize', label: '标准展示尺寸' }, { key: 'siteLogoLargeSize', label: '大号展示尺寸' }];
const previews = [{ variant: 'small', label: '小号' }, { variant: 'standard', label: '标准' }, { variant: 'large', label: '大号' }];
const validationError = computed(() => { try { brandPayload(props.modelValue); return ''; } catch (error) { return error.message; } });
function change(key, value) { emit('update:modelValue', { ...props.modelValue, [key]: value }); message.value = ''; }
function reset() { emit('update:modelValue', resetBrandImages(props.modelValue)); message.value = '已恢复默认草稿，保存后生效'; failed.value = false; }
async function save() {
  if (saving.value || props.disabled) return;
  saving.value = true; emit('saving', true); message.value = ''; failed.value = false;
  try {
    const result = await updateBrandingSettings(brandPayload(props.modelValue));
    setBranding(result);
    emit('update:modelValue', { ...props.modelValue, ...normalizeBranding(result) });
    message.value = '品牌标识已保存';
  } catch (error) { failed.value = true; message.value = error?.message || '保存失败，输入已保留'; }
  finally { saving.value = false; emit('saving', false); }
}
</script>
<style scoped>
.brand-settings { border: 0; padding: 0; margin: 0; min-width: 0; }
.brand-previews { display: flex; flex-wrap: wrap; align-items: end; gap: 24px; margin: 24px 0; }
.brand-previews > div { display: flex; flex-direction: column; align-items: center; gap: 8px; max-width: 100%; }
.brand-previews small { color: #65705b; }
.brand-settings__actions { display: flex; flex-wrap: wrap; gap: 12px; }
.brand-settings__actions button { max-width: 100%; white-space: normal; }
</style>
