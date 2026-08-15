<template>
  <div class="view-stack">
    <SectionCard title="家庭列表" subtitle="查看资料、配送策略和成员余额">
      <div class="content-grid order-layout">
        <div class="table-card">
          <table class="table">
            <thead>
              <tr>
                <th>家庭</th>
                <th>联系人摘要</th>
                <th>配送</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in families" :key="item.familyId">
                <td>
                  <strong>{{ item.familyName }}</strong>
                  <div class="table-sub">{{ item.note || '暂无备注' }}</div>
                </td>
                <td>{{ item.contactNames || item.contactSummary || '-' }}</td>
                <td>{{ item.deliveryEnabled ? (item.deliveryFree ? '免配送费' : `¥${item.deliveryFeeDefault}`) : '仅自取' }}</td>
                <td><button class="text-button" type="button" @click="selectFamily(item.familyId)">查看</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <SectionCard title="家庭详情" subtitle="基础资料、配送与成员余额">
          <AppEmpty
            v-if="!familyDetail"
            title="请选择家庭"
            description="从左侧列表中选中家庭后，这里会出现详情、地址和钱包信息。"
          />

          <div v-else class="detail-stack">
            <label class="form-field">
              <span>家庭名称</span>
              <input v-model.trim="profileForm.familyName" type="text" />
            </label>
            <label class="form-field">
              <span>备注</span>
              <textarea v-model.trim="profileForm.note" />
            </label>
            <label class="form-field">
              <span>联系人摘要</span>
              <input v-model.trim="profileForm.contactNames" type="text" placeholder="例如：陈梅 / 陈浩 / 陈月" />
            </label>
            <div class="detail-actions">
              <button class="primary-button" type="button" @click="saveProfile">保存资料</button>
            </div>

            <div class="section-divider"></div>

            <div class="form-grid two-columns">
              <label class="form-field">
                <span>配送开关</span>
                <select v-model="deliveryForm.deliveryEnabled">
                  <option :value="true">支持配送</option>
                  <option :value="false">仅自取</option>
                </select>
              </label>
              <label class="form-field">
                <span>配送费</span>
                <input v-model.number="deliveryForm.deliveryFeeDefault" type="number" min="0" step="0.01" />
              </label>
              <label class="form-field">
                <span>免配送费</span>
                <select v-model="deliveryForm.deliveryFree">
                  <option :value="false">否</option>
                  <option :value="true">是</option>
                </select>
              </label>
            </div>
            <div class="detail-actions">
              <button class="primary-button" type="button" @click="saveDelivery">保存配送策略</button>
            </div>

            <div class="section-divider"></div>

            <div class="info-block">
              <strong>地址簿</strong>
              <div class="item-stack">
                <article v-for="item in familyDetail.addresses || []" :key="item.addressId" class="list-row-card static">
                  <div>
                    <strong>{{ item.contactName }} · {{ item.contactPhone }}</strong>
                    <p>{{ item.addressText }}</p>
                  </div>
                  <span class="dot-badge" v-if="item.defaultAddress">默认</span>
                </article>
              </div>
            </div>

            <div class="info-block">
              <strong>成员余额</strong>
              <div class="item-stack">
                <article v-for="item in familyDetail.members || []" :key="item.memberId" class="list-row-card static">
                  <div>
                    <strong>{{ item.name }}</strong>
                    <p>可用 ¥{{ item.availableBalance }} · 冻结 ¥{{ item.frozenBalance }}</p>
                  </div>
                  <div class="inline-actions">
                    <button class="text-button" type="button" @click="viewMemberLedgers(item)">流水</button>
                    <button class="text-button" type="button" @click="adjustMember(item)">调余额</button>
                  </div>
                </article>
              </div>
            </div>

            <div v-if="memberLedgers.length" class="info-block">
              <strong>成员流水</strong>
              <div class="table-card">
                <table class="table">
                  <thead>
                    <tr>
                      <th>时间</th>
                      <th>类型</th>
                      <th>金额</th>
                      <th>备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in memberLedgers" :key="item.ledgerId">
                      <td>{{ item.createdAt }}</td>
                      <td>{{ item.type }}</td>
                      <td>{{ item.amount }}</td>
                      <td>{{ item.remark || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </SectionCard>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import { promptAction } from '../../utils/dialog';
import {
  adjustMemberWallet,
  getMemberWalletLedgers,
  getMerchantFamilyDetail,
  listMerchantFamilies,
  updateMerchantFamilyDeliveryPolicy,
  updateMerchantFamilyProfile
} from '../../api/families';

const families = ref([]);
const selectedFamilyId = ref(null);
const familyDetail = ref(null);
const memberLedgers = ref([]);
const profileForm = reactive({
  familyName: '',
  note: '',
  contactNames: ''
});
const deliveryForm = reactive({
  deliveryEnabled: true,
  deliveryFeeDefault: 0,
  deliveryFree: false
});

async function loadFamilies() {
  families.value = await listMerchantFamilies();
  if (!selectedFamilyId.value && families.value[0]) {
    await selectFamily(families.value[0].familyId);
  }
}

async function selectFamily(familyId) {
  selectedFamilyId.value = familyId;
  familyDetail.value = await getMerchantFamilyDetail(familyId);
  memberLedgers.value = [];
  profileForm.familyName = familyDetail.value.familyName || '';
  profileForm.note = familyDetail.value.note || '';
  profileForm.contactNames = familyDetail.value.contactNames || '';
  deliveryForm.deliveryEnabled = Boolean(familyDetail.value.deliveryEnabled);
  deliveryForm.deliveryFeeDefault = Number(familyDetail.value.deliveryFeeDefault || 0);
  deliveryForm.deliveryFree = Boolean(familyDetail.value.deliveryFree);
}

async function saveProfile() {
  if (!selectedFamilyId.value) return;
  await updateMerchantFamilyProfile(selectedFamilyId.value, { ...profileForm });
  await loadFamilies();
  await selectFamily(selectedFamilyId.value);
}

async function saveDelivery() {
  if (!selectedFamilyId.value) return;
  await updateMerchantFamilyDeliveryPolicy(selectedFamilyId.value, { ...deliveryForm });
  await loadFamilies();
  await selectFamily(selectedFamilyId.value);
}

async function viewMemberLedgers(member) {
  memberLedgers.value = await getMemberWalletLedgers(member.memberId);
}

async function adjustMember(member) {
  const amount = await promptAction({ title: `调整 ${member.name} 的余额`, message: '正数增加余额，负数扣减余额。', label: '调整金额', inputType: 'number', defaultValue: '20' });
  if (amount === null) return;
  const remark = await promptAction({ title: '填写调整备注', label: '备注', defaultValue: '后台人工调整' });
  if (!remark) return;
  await adjustMemberWallet(member.memberId, {
    type: Number(amount) >= 0 ? 'MANUAL_CREDIT' : 'MANUAL_DEBIT',
    amount: Number(amount),
    remark
  });
  await selectFamily(selectedFamilyId.value);
}

onMounted(loadFamilies);
</script>
