<template>
  <div class="view-stack">
    <SectionCard title="家庭创建申请" subtitle="审批用户提交的家庭创建请求">
      <template #actions>
        <select v-model="statusFilter" class="table-input" @change="loadApplications">
          <option value="">全部</option>
          <option value="pending">待审批</option>
          <option value="approved">已通过</option>
          <option value="rejected">已拒绝</option>
        </select>
      </template>

      <div v-if="applications.length" class="table-card">
        <table class="table">
          <thead>
            <tr>
              <th>申请ID</th>
              <th>申请人</th>
              <th>家庭 / 商户方案</th>
              <th>状态</th>
              <th>申请时间</th>
              <th>审核备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in applications" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.applicantMemberId }}</td>
              <td><strong>{{ item.familyName }}</strong><small class="table-sub">{{ merchantSummary(item) }}</small></td>
              <td>
                <span class="status-pill" :class="statusTone(item.status)">
                  {{ statusLabel(item.status) }}
                </span>
              </td>
              <td>{{ item.createdAt }}</td>
              <td>{{ item.reviewRemark || '-' }}</td>
              <td>
                <div v-if="item.status === 'pending'" class="inline-actions">
                  <button class="text-button" type="button" @click="handleApprove(item)">通过</button>
                  <button class="text-button danger-text" type="button" @click="handleReject(item)">拒绝</button>
                </div>
                <span v-else class="table-sub">已处理</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppEmpty
        v-else
        title="暂无申请记录"
        description="当前筛选条件下没有家庭创建申请。"
      />
    </SectionCard>

    <SectionCard title="后台添加用户" subtitle="平台管理员为指定家庭直接添加成员">
      <form class="form-grid two-columns" @submit.prevent="handleAddMember">
        <label class="form-field">
          <span>目标家庭</span>
          <select v-model.number="memberForm.familyId">
            <option :value="null">请选择家庭</option>
            <option v-for="family in familyOptions" :key="family.familyId" :value="family.familyId">
              {{ family.familyName }} · {{ family.merchantName }}
            </option>
          </select>
        </label>
        <label class="form-field">
          <span>成员姓名</span>
          <input v-model.trim="memberForm.name" type="text" placeholder="成员姓名" />
        </label>
        <label class="form-field">
          <span>手机号</span>
          <input v-model.trim="memberForm.mobile" type="text" placeholder="可选" />
        </label>
        <label class="form-field">
          <span>角色模板</span>
          <select v-model="memberForm.roleTemplate">
            <option value="member">普通成员</option>
            <option value="admin">家庭管理员</option>
          </select>
        </label>
        <div class="detail-actions" style="grid-column: 1 / -1;">
          <button class="primary-button" type="submit" :disabled="addingMember">
            {{ addingMember ? '添加中...' : '添加成员' }}
          </button>
        </div>
      </form>
      <p v-if="addMemberMessage" class="form-hint" :class="{ 'form-error': addMemberError }">
        {{ addMemberMessage }}
      </p>
    </SectionCard>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import { listAdminFamilyOptions } from '../../api/admin-families';
import { promptAction } from '../../utils/dialog';
import {
  listFamilyApplications,
  approveFamilyApplication,
  rejectFamilyApplication,
  adminCreateMember
} from '../../api/admin-applications';

const applications = ref([]);
const familyOptions = ref([]);
const statusFilter = ref('');
const addingMember = ref(false);
const addMemberMessage = ref('');
const addMemberError = ref(false);

const memberForm = reactive({
  familyId: null,
  name: '',
  mobile: '',
  roleTemplate: 'member'
});

function statusLabel(status) {
  const map = { pending: '待审批', approved: '已通过', rejected: '已拒绝' };
  return map[status] || status;
}

function statusTone(status) {
  if (status === 'pending') return 'tone-amber';
  if (status === 'approved') return 'tone-green';
  if (status === 'rejected') return 'tone-red';
  return 'tone-slate';
}

function merchantSummary(item) {
  return item.merchantMode === 'CREATE'
    ? `新建私厨：${item.proposedMerchantName || '-'}`
    : '通过私密商户邀请码加入';
}

async function loadApplications() {
  applications.value = await listFamilyApplications(statusFilter.value || undefined);
}

async function handleApprove(item) {
  const remark = await promptAction({ title: '通过家庭申请', label: '审批备注', defaultValue: '审核通过', required: false });
  if (remark === null) return;
  await approveFamilyApplication(item.id, { remark });
  await loadApplications();
}

async function handleReject(item) {
  const remark = await promptAction({ title: '拒绝家庭申请', label: '拒绝原因', defaultValue: '信息不完整，请补充', danger: true });
  if (!remark) return;
  await rejectFamilyApplication(item.id, { remark });
  await loadApplications();
}

async function handleAddMember() {
  addMemberMessage.value = '';
  addMemberError.value = false;

  if (!memberForm.familyId || !memberForm.name) {
    addMemberMessage.value = '请选择家庭并填写成员姓名';
    addMemberError.value = true;
    return;
  }

  addingMember.value = true;
  try {
    await adminCreateMember({ ...memberForm });
    addMemberMessage.value = '成员添加成功';
    addMemberError.value = false;
    memberForm.familyId = null;
    memberForm.name = '';
    memberForm.mobile = '';
    memberForm.roleTemplate = 'member';
  } catch (error) {
    addMemberMessage.value = error?.message || '添加失败';
    addMemberError.value = true;
  } finally {
    addingMember.value = false;
  }
}

onMounted(async () => {
  [applications.value, familyOptions.value] = await Promise.all([
    listFamilyApplications(statusFilter.value || undefined),
    listAdminFamilyOptions()
  ]);
});
</script>
