<template>
  <div class="platform-family-workspace">
    <aside class="family-directory">
      <div class="family-directory__head">
        <span class="page-eyebrow">ALL FAMILIES</span>
        <h2>家庭目录</h2>
        <input v-model.trim="keyword" type="search" placeholder="搜索家庭或商户" />
      </div>
      <div class="family-directory__list">
        <button
          v-for="item in filteredFamilies"
          :key="item.familyId"
          class="family-directory__item"
          :class="{ 'is-active': item.familyId === selectedFamilyId }"
          type="button"
          @click="selectFamily(item.familyId)"
        >
          <span><strong>{{ item.familyName }}</strong><small>{{ item.merchantName }}</small></span>
          <em>{{ item.memberCount }} 人</em>
        </button>
        <AppEmpty v-if="!filteredFamilies.length" title="没有匹配家庭" description="换个家庭名或商户名试试。" />
      </div>
    </aside>

    <main class="family-inspector">
      <AppEmpty v-if="!detail" title="请选择家庭" description="从左侧目录选择一个家庭开始管理。" />
      <template v-else>
        <header class="family-inspector__hero">
          <div><span class="page-eyebrow">{{ detail.merchantName }}</span><h2>{{ detail.familyName }}</h2><p>{{ detail.members.length }} 位成员 · {{ detail.addresses.length }} 个地址</p></div>
          <span class="status-pill" :class="detail.status === 'active' ? 'tone-green' : 'tone-slate'">{{ detail.status === 'active' ? '正常服务' : '已停用' }}</span>
        </header>

        <section class="family-work-section">
          <div class="family-work-section__title"><h3>资料与配送</h3><button class="primary-button" type="button" @click="saveFamily">保存修改</button></div>
          <div class="form-grid two-columns">
            <label class="form-field"><span>家庭名称</span><input v-model.trim="familyForm.familyName" /></label>
            <label class="form-field"><span>状态</span><select v-model="familyForm.status"><option value="active">正常</option><option value="inactive">停用</option></select></label>
            <label class="form-field"><span>家庭备注</span><input v-model.trim="familyForm.note" placeholder="饮食偏好或服务备注" /></label>
            <label class="form-field"><span>默认配送费</span><input v-model.number="familyForm.deliveryFeeDefault" type="number" min="0" step="0.01" /></label>
            <label class="checkbox-line"><input v-model="familyForm.deliveryEnabled" type="checkbox" />支持配送</label>
            <label class="checkbox-line"><input v-model="familyForm.deliveryFree" type="checkbox" />免配送费</label>
          </div>
        </section>

        <section class="family-work-section">
          <div class="family-work-section__title"><h3>家庭成员</h3><span>{{ detail.members.length }} 人</span></div>
          <div class="member-editor-list">
            <article v-for="member in detail.members" :key="member.memberId" class="member-editor-row">
              <input v-model.trim="member.name" aria-label="成员姓名" />
              <input v-model.trim="member.mobile" aria-label="手机号" placeholder="手机号" />
              <select v-model="member.roleTemplate" aria-label="成员角色"><option value="member">普通成员</option><option value="admin">家庭管理员</option></select>
              <select v-model="member.activationStatus" aria-label="账号状态"><option value="active">正常</option><option value="pending">待激活</option><option value="disabled">禁用</option></select>
              <div class="inline-actions"><button class="text-button" type="button" @click="saveMember(member)">保存</button><button class="text-button danger-text" type="button" @click="removeMember(member)">移除</button></div>
              <div class="member-balance-line"><span>余额 ¥{{ member.availableBalance }}</span><button class="text-button" type="button" @click="adjustBalance(member)">调整余额</button></div>
            </article>
          </div>
        </section>

        <section class="family-work-section">
          <div class="family-work-section__title"><h3>家庭地址</h3><span>{{ detail.addresses.length }} 个</span></div>
          <div class="address-editor-list">
            <article v-for="address in detail.addresses" :key="address.addressId" class="address-editor-row">
              <div><strong>{{ address.contactName }} · {{ address.contactPhone }}</strong><p>{{ address.addressText }}</p></div>
              <div class="inline-actions"><span v-if="address.defaultAddress" class="dot-badge">默认</span><button v-else class="text-button" type="button" @click="makeDefault(address)">设为默认</button><button class="text-button" type="button" @click="editAddress(address)">编辑</button><button class="text-button danger-text" type="button" @click="removeAddress(address)">删除</button></div>
            </article>
          </div>
          <form class="address-create-row" @submit.prevent="saveAddress"><input v-model.trim="addressForm.contactName" placeholder="联系人" required /><input v-model.trim="addressForm.contactPhone" placeholder="联系电话" required /><input v-model.trim="addressForm.addressText" placeholder="详细地址" required /><button class="ghost-button" type="submit">{{ editingAddressId ? '保存地址' : '添加地址' }}</button></form>
        </section>

        <section class="family-work-section">
          <div class="family-work-section__title"><h3>家庭菜单</h3><button class="primary-button" type="button" @click="saveMenu">保存菜单</button></div>
          <div class="platform-menu-list">
            <article v-for="dish in menuRows" :key="dish.dishId" class="platform-menu-row">
              <label class="checkbox-line"><input v-model="dish.enabled" type="checkbox" />{{ dish.dishName }}</label>
              <span>{{ dish.categoryName || '未分类' }}</span>
              <label>家庭价格 <input v-model.number="dish.familyFinalPrice" type="number" min="0" step="0.01" /></label>
              <label>排序 <input v-model.number="dish.sortOrder" type="number" min="0" /></label>
            </article>
            <AppEmpty v-if="!menuRows.length" title="暂无可配置菜品" description="所属商户还没有创建菜品。" />
          </div>
        </section>

        <section class="family-danger-zone"><div><strong>停用家庭</strong><p>保留订单与账务历史，停止该家庭继续使用服务。</p></div><button class="ghost-button danger-inline" type="button" @click="disableFamily">停用家庭</button></section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import { notify } from '../../utils/feedback';
import { confirmAction, promptAction } from '../../utils/dialog';
import { adjustAdminFamilyMemberBalance, createAdminFamilyAddress, deleteAdminFamilyAddress, disableAdminFamily, disableAdminFamilyMember, getAdminFamilyDetail, getAdminFamilyMenu, listAdminFamilyOptions, saveAdminFamilyMenu, setAdminFamilyDefaultAddress, updateAdminFamily, updateAdminFamilyAddress, updateAdminFamilyMember } from '../../api/admin-families';

const families = ref([]);
const detail = ref(null);
const selectedFamilyId = ref(null);
const keyword = ref('');
const menuRows = ref([]);
const editingAddressId = ref(null);
const familyForm = reactive({ familyName: '', note: '', status: 'active', deliveryEnabled: true, deliveryFeeDefault: 0, deliveryFree: false });
const addressForm = reactive({ contactName: '', contactPhone: '', addressText: '', defaultAddress: false });
const filteredFamilies = computed(() => { const q = keyword.value.toLowerCase(); return families.value.filter((item) => !q || `${item.familyName} ${item.merchantName}`.toLowerCase().includes(q)); });

function fillFamilyForm(value) { Object.assign(familyForm, { familyName: value.familyName || '', note: value.note || '', status: value.status || 'active', deliveryEnabled: Boolean(value.deliveryEnabled), deliveryFeeDefault: Number(value.deliveryFeeDefault || 0), deliveryFree: Boolean(value.deliveryFree) }); }
async function loadFamilies() { families.value = await listAdminFamilyOptions(); if (!selectedFamilyId.value && families.value[0]) await selectFamily(families.value[0].familyId); }
async function selectFamily(id) { selectedFamilyId.value = id; [detail.value, menuRows.value] = await Promise.all([getAdminFamilyDetail(id), getAdminFamilyMenu(id)]); fillFamilyForm(detail.value); editingAddressId.value = null; }
async function refresh() { await selectFamily(selectedFamilyId.value); families.value = await listAdminFamilyOptions(); }
async function saveFamily() { await updateAdminFamily(selectedFamilyId.value, { ...familyForm }); await refresh(); notify('家庭资料已保存', 'success'); }
async function saveMember(member) { await updateAdminFamilyMember(selectedFamilyId.value, member.memberId, { name: member.name, mobile: member.mobile || '', roleTemplate: member.roleTemplate, activationStatus: member.activationStatus }); await refresh(); notify('成员资料已保存', 'success'); }
async function removeMember(member) { const confirmed = await confirmAction({ title: '移除家庭成员', message: `确认移除“${member.name}”吗？历史账务仍会保留。`, danger: true, confirmText: '确认移除' }); if (!confirmed) return; await disableAdminFamilyMember(selectedFamilyId.value, member.memberId); await refresh(); }
async function adjustBalance(member) { const raw = await promptAction({ title: `调整“${member.name}”的余额`, message: '正数增加余额，负数扣减余额。', label: '调整金额', inputType: 'number', defaultValue: '0' }); if (raw === null || Number(raw) === 0) return; const amount = Math.abs(Number(raw)); await adjustAdminFamilyMemberBalance(selectedFamilyId.value, member.memberId, { type: Number(raw) > 0 ? 'MANUAL_CREDIT' : 'MANUAL_DEBIT', amount, remark: '平台管理员调整' }); await refresh(); notify('成员余额已更新', 'success'); }
function editAddress(address) { editingAddressId.value = address.addressId; Object.assign(addressForm, { contactName: address.contactName, contactPhone: address.contactPhone, addressText: address.addressText, defaultAddress: address.defaultAddress }); }
async function saveAddress() { if (editingAddressId.value) await updateAdminFamilyAddress(selectedFamilyId.value, editingAddressId.value, { ...addressForm }); else await createAdminFamilyAddress(selectedFamilyId.value, { ...addressForm }); Object.assign(addressForm, { contactName: '', contactPhone: '', addressText: '', defaultAddress: false }); editingAddressId.value = null; await refresh(); notify('地址已保存', 'success'); }
async function makeDefault(address) { await setAdminFamilyDefaultAddress(selectedFamilyId.value, address.addressId); await refresh(); }
async function removeAddress(address) { const confirmed = await confirmAction({ title: '删除家庭地址', message: address.addressText, danger: true, confirmText: '确认删除' }); if (!confirmed) return; await deleteAdminFamilyAddress(selectedFamilyId.value, address.addressId); await refresh(); }
async function saveMenu() { await saveAdminFamilyMenu(selectedFamilyId.value, menuRows.value.map((item) => ({ dishId: item.dishId, enabled: item.enabled, sortOrder: Number(item.sortOrder || 0), familyFinalPrice: Number(item.familyFinalPrice || 0) }))); await refresh(); notify('家庭菜单已保存', 'success'); }
async function disableFamily() { const confirmed = await confirmAction({ title: `停用“${detail.value.familyName}”`, message: '停用后将停止继续服务，但订单与账务历史会完整保留。', danger: true, confirmText: '确认停用' }); if (!confirmed) return; await disableAdminFamily(selectedFamilyId.value); await refresh(); notify('家庭已停用', 'warning'); }
onMounted(loadFamilies);
</script>
