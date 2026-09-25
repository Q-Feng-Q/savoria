<template>
  <div class="settings-workspace">
    <header class="settings-header"><div><span class="page-eyebrow">PLATFORM SETTINGS</span><h2>平台配置</h2><p>统一管理账号绑定、邮件服务与平台运行策略。</p></div><button class="primary-button" :disabled="saving || brandSaving || loading" @click="save">{{ saving ? '保存中...' : '保存全部配置' }}</button></header>
    <form class="settings-layout" @submit.prevent="save">
      <BrandSettings :model-value="form" :disabled="saving || loading" @update:model-value="Object.assign(form, $event)" @saving="brandSaving = $event" />
      <SectionCard title="账号绑定" subtitle="关闭后不会删除用户已有绑定">
        <div class="setting-switches">
          <label class="setting-switch"><span><strong>手机号绑定</strong><small>允许用户新增或换绑手机号</small></span><input v-model="form.mobileBindingEnabled" type="checkbox" /></label>
          <label class="setting-switch"><span><strong>邮箱绑定</strong><small>使用平台邮件服务发送验证码</small></span><input v-model="form.emailBindingEnabled" type="checkbox" /></label>
          <label class="setting-switch"><span><strong>微信绑定</strong><small>允许通过小程序授权绑定微信</small></span><input v-model="form.wechatBindingEnabled" type="checkbox" /></label>
        </div>
      </SectionCard>
      <SectionCard title="邮件服务器" subtitle="SMTP 授权码已加密保存，页面不会回显明文">
        <div class="settings-grid">
          <label class="form-field"><span>SMTP 主机</span><input v-model.trim="form.smtpHost" :required="form.emailBindingEnabled" placeholder="smtp.example.com" /></label>
          <label class="form-field"><span>SMTP 端口</span><input v-model.number="form.smtpPort" type="number" min="1" max="65535" :required="form.emailBindingEnabled" /></label>
          <label class="form-field"><span>登录账号</span><input v-model.trim="form.smtpUsername" :required="form.emailBindingEnabled" /></label>
          <label class="form-field"><span>授权码</span><input v-model="form.smtpPassword" type="password" :placeholder="form.smtpPasswordConfigured ? '已配置，留空表示不修改' : '请输入邮箱授权码'" /></label>
          <label class="form-field"><span>发件人</span><input v-model.trim="form.smtpFrom" type="email" :required="form.emailBindingEnabled" /></label>
          <label class="setting-inline"><input v-model="form.smtpTlsEnabled" type="checkbox" /> 启用 STARTTLS</label>
          <label class="form-field settings-grid__wide"><span>测试收件邮箱</span><span class="settings-test-row"><input v-model.trim="testRecipient" type="email" placeholder="保存配置后发送测试邮件" /><button class="secondary-button" type="button" :disabled="testing || !testRecipient" @click="testMail">{{ testing ? '发送中...' : '发送测试邮件' }}</button></span></label>
        </div>
      </SectionCard>
      <SectionCard title="站点与运行" subtitle="维护模式和菜品审核对全平台生效">
        <div class="settings-grid"><label class="form-field settings-grid__wide"><span>维护提示</span><input v-model.trim="form.maintenanceMessage" required /></label></div>
        <div class="setting-switches setting-switches--compact"><label class="setting-switch"><span><strong>菜品审核</strong></span><input v-model="form.dishReviewEnabled" type="checkbox" /></label><label class="setting-switch"><span><strong>维护模式</strong></span><input v-model="form.maintenanceEnabled" type="checkbox" /></label></div>
      </SectionCard>
      <p v-if="message" class="form-hint settings-message">{{ message }}</p>
    </form>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue';
import SectionCard from '../../components/SectionCard.vue';
import BrandSettings from '../../components/BrandSettings.vue';
import { BRAND_DEFAULTS, brandPayload, normalizeBranding } from '../../utils/branding';
import { setBranding } from '../../stores/branding';
import { getSystemSettings, updateSystemSettings, sendTestEmail } from '../../api/system-settings';
const form=reactive({...BRAND_DEFAULTS,dishReviewEnabled:true,maintenanceEnabled:false,maintenanceMessage:'系统维护中，请稍后再试',mobileBindingEnabled:true,emailBindingEnabled:true,wechatBindingEnabled:true,smtpHost:'',smtpPort:587,smtpUsername:'',smtpPassword:'',smtpPasswordConfigured:false,smtpTlsEnabled:true,smtpFrom:''});
const saving=ref(false),testing=ref(false),testRecipient=ref(''),message=ref('');
const brandSaving=ref(false),loading=ref(true);
async function load(){try{const result=await getSystemSettings();Object.assign(form,result,normalizeBranding(result));form.smtpPassword='';setBranding(result);loading.value=false;}catch(error){message.value=error?.message||'加载配置失败，请刷新后重试';}}
async function save(){if(saving.value||brandSaving.value||loading.value)return;saving.value=true;message.value='';try{const result=await updateSystemSettings({...form,...brandPayload(form),smtpPassword:form.smtpPassword||''});Object.assign(form,result,normalizeBranding(result));setBranding(result);form.smtpPassword='';message.value='配置已保存';}catch(error){message.value=error?.message||'保存失败，输入已保留';}finally{saving.value=false;}}
async function testMail(){testing.value=true;message.value='';try{await sendTestEmail(testRecipient.value);message.value='测试邮件已发送，请检查收件箱';}finally{testing.value=false;}}
onMounted(load);
</script>
<style scoped>.settings-test-row{display:flex;gap:12px;align-items:center}.settings-test-row input{flex:1}.settings-test-row .secondary-button{white-space:nowrap}</style>
