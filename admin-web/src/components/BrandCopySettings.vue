<template>
  <SectionCard title="品牌文案" subtitle="用于小程序的欢迎语与氛围寄语；清空后恢复系统默认文案">
    <fieldset class="copy-settings" :disabled="disabled">
      <div class="copy-settings__grid">
        <label v-for="field in fields" :key="field.key" class="form-field copy-field">
          <span>{{ field.label }}</span>
          <small>{{ field.purpose }}</small>
          <textarea
            :value="modelValue[field.key]"
            :maxlength="BRAND_COPY_LIMITS[field.key]"
            rows="3"
            :placeholder="`默认：${BRAND_COPY_DEFAULTS[field.key]}`"
            @input="change(field.key, $event.target.value)"
          />
          <small class="copy-field__count">{{ lengthOf(field.key) }} / {{ BRAND_COPY_LIMITS[field.key] }}</small>
        </label>
      </div>
    </fieldset>
  </SectionCard>
</template>

<script setup>
import SectionCard from './SectionCard.vue';
import { BRAND_COPY_DEFAULTS, BRAND_COPY_LIMITS } from '../utils/branding';

const props = defineProps({ modelValue: { type: Object, required: true }, disabled: Boolean });
const emit = defineEmits(['update:modelValue']);
const fields = [
  { key: 'brandTagline', label: '通用品牌标语', purpose: '登录、注册等品牌入口' },
  { key: 'homeHeroTagline', label: '首页顶部标语', purpose: '家庭首页首屏主视觉' },
  { key: 'homeFooterMessage', label: '首页底部寄语', purpose: '首页、订单与个人中心页尾' },
  { key: 'cartHeroTagline', label: '餐篮顶部标语', purpose: '共享餐篮页首屏' },
  { key: 'deliveryMessage', label: '配送提示', purpose: '选择配送时的温暖提示' },
  { key: 'pickupMessage', label: '自取提示', purpose: '选择到店自取时的温暖提示' },
  { key: 'cartFooterMessage', label: '餐篮底部寄语', purpose: '餐篮提交区上方的品牌寄语' },
  { key: 'profileWelcomeMessage', label: '个人中心欢迎语', purpose: '已加入家庭用户的欢迎文案' }
];

function change(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value });
}
function lengthOf(key) {
  return typeof props.modelValue[key] === 'string' ? props.modelValue[key].length : 0;
}
</script>

<style scoped>
.copy-settings { border: 0; padding: 0; margin: 0; min-width: 0; }
.copy-settings__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.copy-field { align-items: stretch; }
.copy-field > small { color: #7c705f; line-height: 1.5; }
.copy-field textarea { min-height: 92px; resize: vertical; line-height: 1.65; }
.copy-field__count { text-align: right; }
@media (max-width: 760px) { .copy-settings__grid { grid-template-columns: 1fr; } }
</style>
