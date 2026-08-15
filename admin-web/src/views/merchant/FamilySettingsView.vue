<template>
  <div class="view-stack">
    <SectionCard title="申请创建家庭" subtitle="提交家庭创建申请，需平台管理员审批">
      <form class="stack-form" @submit.prevent="handleApply">
        <label class="form-field">
          <span>家庭名称</span>
          <input v-model.trim="applyForm.familyName" type="text" placeholder="例如：张三的家庭" />
        </label>
        <div class="detail-actions">
          <button class="primary-button" type="submit" :disabled="applyLoading">
            {{ applyLoading ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </form>
      <p v-if="applyMessage" class="form-hint" :class="{ 'form-error': applyError }">{{ applyMessage }}</p>
    </SectionCard>

    <SectionCard title="邀请成员加入" subtitle="生成邀请码分享给家人，有效期 7 天">
      <div class="detail-actions">
        <button class="primary-button" type="button" :disabled="inviteLoading" @click="handleInvite">
          {{ inviteLoading ? '生成中...' : '生成邀请码' }}
        </button>
      </div>
      <div v-if="inviteCode" class="detail-line" style="margin-top: 16px;">
        <span>邀请码</span>
        <strong style="font-size: 20px; letter-spacing: 2px;">{{ inviteCode }}</strong>
      </div>
      <p v-if="inviteMessage" class="form-hint" :class="{ 'form-error': inviteError }">{{ inviteMessage }}</p>
    </SectionCard>

    <SectionCard title="通过邀请码加入家庭" subtitle="输入家庭成员分享的邀请码即可加入">
      <form class="stack-form" @submit.prevent="handleJoin">
        <label class="form-field">
          <span>邀请码</span>
          <input v-model.trim="joinForm.code" type="text" placeholder="请输入8位邀请码" />
        </label>
        <div class="detail-actions">
          <button class="primary-button" type="submit" :disabled="joinLoading">
            {{ joinLoading ? '加入中...' : '加入家庭' }}
          </button>
        </div>
      </form>
      <p v-if="joinMessage" class="form-hint" :class="{ 'form-error': joinError }">{{ joinMessage }}</p>
    </SectionCard>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import SectionCard from '../../components/SectionCard.vue';
import { applyFamily, generateInviteCode, joinFamilyByCode } from '../../api/family-applications';
import { useAuthStore } from '../../stores/auth';

const authStore = useAuthStore();

const applyForm = reactive({ familyName: '' });
const applyLoading = ref(false);
const applyMessage = ref('');
const applyError = ref(false);

const inviteCode = ref('');
const inviteLoading = ref(false);
const inviteMessage = ref('');
const inviteError = ref(false);

const joinForm = reactive({ code: '' });
const joinLoading = ref(false);
const joinMessage = ref('');
const joinError = ref(false);

async function handleApply() {
  applyMessage.value = '';
  applyError.value = false;

  if (!applyForm.familyName) {
    applyMessage.value = '请输入家庭名称';
    applyError.value = true;
    return;
  }

  applyLoading.value = true;
  try {
    await applyFamily({ familyName: applyForm.familyName, merchantId: authStore.session?.merchantId });
    applyMessage.value = '申请已提交，等待管理员审批';
    applyError.value = false;
    applyForm.familyName = '';
  } catch (error) {
    applyMessage.value = error?.message || '申请失败';
    applyError.value = true;
  } finally {
    applyLoading.value = false;
  }
}

async function handleInvite() {
  inviteMessage.value = '';
  inviteError.value = false;
  inviteLoading.value = true;

  try {
    const code = await generateInviteCode();
    inviteCode.value = typeof code === 'string' ? code : code?.code || String(code);
    inviteMessage.value = '邀请码已生成，有效期7天';
    inviteError.value = false;
  } catch (error) {
    inviteMessage.value = error?.message || '生成失败';
    inviteError.value = true;
  } finally {
    inviteLoading.value = false;
  }
}

async function handleJoin() {
  joinMessage.value = '';
  joinError.value = false;

  if (!joinForm.code) {
    joinMessage.value = '请输入邀请码';
    joinError.value = true;
    return;
  }

  joinLoading.value = true;
  try {
    await joinFamilyByCode({ code: joinForm.code });
    joinMessage.value = '成功加入家庭';
    joinError.value = false;
    joinForm.code = '';
  } catch (error) {
    joinMessage.value = error?.message || '加入失败';
    joinError.value = true;
  } finally {
    joinLoading.value = false;
  }
}
</script>
