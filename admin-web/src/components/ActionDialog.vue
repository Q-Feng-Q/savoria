<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="action-dialog-layer" @click.self="cancel">
        <section class="action-dialog" role="dialog" aria-modal="true" :aria-labelledby="titleId">
          <span class="page-eyebrow">{{ danger ? '请谨慎确认' : '食光知味工作台' }}</span>
          <h3 :id="titleId">{{ title }}</h3>
          <p v-if="message">{{ message }}</p>
          <label v-if="mode === 'prompt'" class="form-field action-dialog__field">
            <span>{{ label }}</span>
            <select v-if="options.length" v-model="value" autofocus>
              <option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
            <input v-else v-model="value" :type="inputType" :placeholder="placeholder" autofocus @keyup.enter="submit" />
          </label>
          <p v-if="error" class="form-error">{{ error }}</p>
          <footer class="action-dialog__actions">
            <button class="ghost-button" type="button" @click="cancel">取消</button>
            <button class="primary-button" :class="{ 'is-danger': danger }" type="button" @click="submit">{{ confirmText }}</button>
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { ACTION_DIALOG_EVENT } from '../utils/dialog';

const visible = ref(false);
const mode = ref('confirm');
const title = ref('确认操作');
const message = ref('');
const label = ref('请输入');
const value = ref('');
const inputType = ref('text');
const placeholder = ref('');
const options = ref([]);
const required = ref(false);
const danger = ref(false);
const confirmText = ref('确认');
const error = ref('');
const titleId = 'action-dialog-title';
let resolver = null;

function open(event) {
  const config = event.detail || {};
  mode.value = config.mode || 'confirm';
  title.value = config.title || '确认操作';
  message.value = config.message || '';
  label.value = config.label || '请输入';
  value.value = config.defaultValue ?? (config.options?.[0]?.value ?? '');
  inputType.value = config.inputType || 'text';
  placeholder.value = config.placeholder || '';
  options.value = config.options || [];
  required.value = Boolean(config.required);
  danger.value = Boolean(config.danger);
  confirmText.value = config.confirmText || (danger.value ? '确认操作' : '确认');
  error.value = '';
  resolver = config.resolve;
  visible.value = true;
}

function finish(result) {
  visible.value = false;
  const resolve = resolver;
  resolver = null;
  resolve?.(result);
}

function cancel() { finish(null); }
function submit() {
  if (mode.value === 'prompt' && required.value && String(value.value).trim() === '') {
    error.value = '请填写后再继续';
    return;
  }
  finish(mode.value === 'prompt' ? value.value : true);
}

onMounted(() => window.addEventListener(ACTION_DIALOG_EVENT, open));
onBeforeUnmount(() => window.removeEventListener(ACTION_DIALOG_EVENT, open));
</script>
