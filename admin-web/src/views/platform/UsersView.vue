<template>
  <div class="user-admin-page">
    <header class="user-admin-toolbar">
      <div><span class="page-eyebrow">ACCOUNT DIRECTORY</span><h2>用户管理</h2><p>管理平台注册账户、登录状态与平台权限。</p></div>
      <button class="primary-button" type="button" @click="openCreateDialog">新增用户</button>
    </header>

    <section class="user-table-panel">
      <div class="user-table-tools">
        <label class="user-search"><span>搜索</span><input v-model.trim="keyword" type="search" placeholder="用户名、姓名或手机号" @keyup.enter="load" /></label>
        <button class="ghost-button" type="button" @click="load">查询</button>
        <span class="user-result-count">共 {{ users.length }} 位用户</span>
      </div>
      <div v-if="loading" class="table-empty">正在加载用户...</div>
      <AppEmpty v-else-if="!users.length" title="没有匹配用户" description="换个关键词重新搜索。" />
      <div v-else class="user-table-scroll">
        <table class="user-data-table">
          <thead><tr><th>用户</th><th>联系方式</th><th>业务归属</th><th>账号状态</th><th>平台角色</th><th>最近登录</th><th class="user-actions-column">操作</th></tr></thead>
          <tbody><tr v-for="user in users" :key="user.userId"><td><div class="user-identity"><span class="user-avatar">{{ avatarText(user) }}</span><span><strong>{{ user.nickname || user.username }}</strong><small>@{{ user.username }}</small></span></div></td><td><strong>{{ user.mobile || '未绑定手机' }}</strong><small>{{ user.email || '未绑定邮箱' }}</small></td><td><strong>{{ user.familyName || '无家庭' }}</strong><small>{{ user.merchantName || '无商户归属' }}</small></td><td><select v-model="user.status" class="table-select" aria-label="账号状态" @change="saveStatus(user)"><option value="ACTIVE">正常</option><option value="FROZEN">冻结</option><option value="CANCELLING">注销冷静期</option></select></td><td><select :value="isPlatformAdmin(user) ? 'platform_admin' : 'user'" class="table-select" aria-label="平台角色" @change="saveRole(user,$event.target.value)"><option value="user">普通用户</option><option value="platform_admin">平台管理员</option></select></td><td><span>{{ formatTime(user.lastLoginAt) }}</span><small>注册 {{ formatTime(user.createdAt) }}</small></td><td class="user-actions-column"><button class="user-delete-button" type="button" @click="deleteUser(user)">删除</button></td></tr></tbody>
        </table>
      </div>
    </section>

    <Transition name="user-modal">
      <div v-if="dialogOpen" class="user-modal-layer" @click.self="closeCreateDialog">
        <section class="user-create-modal" role="dialog" aria-modal="true" aria-labelledby="user-create-title">
          <header><div><span class="page-eyebrow">NEW ACCOUNT</span><h3 id="user-create-title">新增用户</h3><p>创建平台内部账户，稍后仍可调整账号状态和角色。</p></div><button class="modal-close" type="button" aria-label="关闭" @click="closeCreateDialog">×</button></header>
          <form @submit.prevent="createUser"><div class="modal-form-grid"><label class="form-field"><span>账户名</span><input v-model.trim="createForm.username" placeholder="3-50 位字母、数字或下划线" required /></label><label class="form-field"><span>初始密码</span><input v-model="createForm.password" type="password" placeholder="6-64 位" required /></label><label class="form-field"><span>姓名</span><input v-model.trim="createForm.name" placeholder="用户显示名称" required /></label><label class="form-field"><span>手机号</span><input v-model.trim="createForm.mobile" placeholder="选填" /></label><label class="form-field modal-form-grid__wide"><span>初始平台角色</span><select v-model="createForm.platformAdmin"><option :value="false">普通用户</option><option :value="true">平台管理员</option></select></label></div><footer><button class="ghost-button" type="button" @click="closeCreateDialog">取消</button><button class="primary-button" :disabled="creating">{{ creating ? '创建中...' : '确认创建' }}</button></footer></form>
        </section>
      </div>
    </Transition>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import { createAdminUser, deleteAdminUser, listAdminUsers, updateAdminUserPlatformRole, updateAdminUserStatus } from '../../api/admin-users';
import { notify } from '../../utils/feedback';
import { confirmAction } from '../../utils/dialog';
const users=ref([]),keyword=ref(''),loading=ref(false),creating=ref(false),dialogOpen=ref(false);
const emptyForm=()=>({username:'',password:'',name:'',mobile:'',platformAdmin:false});const createForm=reactive(emptyForm());
function isPlatformAdmin(user){return String(user.platformRoles||'').split(',').some((role)=>role.trim().toLowerCase()==='platform_admin');}
function avatarText(user){return String(user.nickname||user.username||'用').slice(0,1).toUpperCase();}
function formatTime(value){if(!value)return '从未';return String(value).replace('T',' ').slice(0,16);}
function openCreateDialog(){Object.assign(createForm,emptyForm());dialogOpen.value=true;}
function closeCreateDialog(){if(!creating.value)dialogOpen.value=false;}
async function load(){loading.value=true;try{users.value=await listAdminUsers(keyword.value);}finally{loading.value=false;}}
async function createUser(){if(!/^[A-Za-z0-9_]{3,50}$/.test(createForm.username))return notify('账户名需为 3-50 位字母、数字或下划线','warning');if(createForm.password.length<6||createForm.password.length>64)return notify('初始密码需为 6-64 位','warning');creating.value=true;try{await createAdminUser({...createForm});dialogOpen.value=false;notify('用户已创建','success');}finally{creating.value=false;}try{await load();}catch{return;}}
async function saveStatus(user){await updateAdminUserStatus(user.userId,user.status);notify('账号状态已更新','success');await load();}
async function saveRole(user,value){await updateAdminUserPlatformRole(user.userId,value==='platform_admin');notify('平台角色已更新，用户需要重新登录','success');await load();}
async function deleteUser(user){const confirmed=await confirmAction({title:`删除用户“${user.nickname||user.username}”`,message:'账户将立即失效并清除个人信息，订单与账务历史会继续保留。',danger:true,confirmText:'确认删除'});if(!confirmed)return;await deleteAdminUser(user.userId);notify('用户已安全删除','success');await load();}
onMounted(load);
</script>
