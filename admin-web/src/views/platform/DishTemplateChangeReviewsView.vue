<template>
  <div class="view-stack template-review-page">
    <SectionCard title="模板修改审核" subtitle="审核通过将原子覆盖平台模板主信息和全部食材，已导入的商户菜品不会跟随变化">
      <template #actions><button class="ghost-button" type="button" :disabled="loading" @click="load(1)">刷新</button></template>
      <div class="review-toolbar">
        <label class="form-field"><span>审核状态</span><select v-model="query.status" @change="load(1)"><option value="">全部状态</option><option v-for="(label, value) in TEMPLATE_CHANGE_STATUS" :key="value" :value="value">{{ label }}</option></select></label>
        <label class="form-field"><span>菜名关键词</span><input v-model.trim="query.keyword" type="search" @keyup.enter="load(1)" /></label>
        <label class="form-field"><span>商户</span><select v-model="query.merchantId" :disabled="merchantLoading" @change="load(1)"><option value="">全部商户</option><option v-for="merchant in merchants" :key="merchant.merchantId" :value="merchant.merchantId">{{ merchant.name }}</option></select><button v-if="merchantError" class="text-button" type="button" @click="loadMerchants">加载失败，重试</button></label>
        <button class="primary-button" type="button" @click="load(1)">查询</button>
      </div>
      <div v-if="loading" class="table-empty">正在加载模板修改申请...</div>
      <AppEmpty v-else-if="!page.items.length" title="暂无待处理申请" description="当前筛选范围内没有模板修改申请。" />
      <div v-else class="user-table-scroll"><table class="user-data-table review-table"><thead><tr><th>模板菜品</th><th>商户</th><th>状态</th><th>提交说明</th><th>版本</th><th>提交时间</th><th>操作</th></tr></thead><tbody>
        <tr v-for="item in page.items" :key="item.requestId"><td><strong>{{ item.templateName }}</strong><small>申请 #{{ item.requestId }} · 模板 #{{ item.templateId }}</small></td><td><strong>{{ item.merchantName || `商户 #${item.merchantId}` }}</strong><small>#{{ item.merchantId }}</small></td><td><StatusPill :status="item.status" :label="TEMPLATE_CHANGE_STATUS[item.status]" /></td><td class="reason-cell">{{ item.submitNote || '未填写' }}</td><td>v{{ item.baseTemplateVersion }}</td><td>{{ formatTemplateChangeTime(item.submittedAt) }}</td><td><button class="text-button" type="button" @click="openReview(item.requestId)">审核详情</button></td></tr>
      </tbody></table></div>
      <div class="pager"><button class="ghost-button" type="button" :disabled="page.page <= 1" @click="load(page.page - 1)">上一页</button><span>第 {{ page.page }} 页 · 共 {{ page.total }} 条</span><button class="ghost-button" type="button" :disabled="page.page * page.pageSize >= page.total" @click="load(page.page + 1)">下一页</button></div>
    </SectionCard>

    <div v-if="detail" class="review-modal-layer" @click.self="closeReview">
      <section class="review-modal" role="dialog" aria-modal="true">
        <header><div><span class="page-eyebrow">TEMPLATE CHANGE REVIEW</span><h3>{{ detail.templateName }}</h3><p>{{ detail.merchantName || `商户 #${detail.merchantId}` }} · 申请 #{{ detail.requestId }}</p></div><button class="modal-close" type="button" aria-label="关闭" @click="closeReview">×</button></header>
        <div class="review-body">
          <div class="review-status-line"><StatusPill :status="detail.status" :label="TEMPLATE_CHANGE_STATUS[detail.status]" /><span>提交版本 v{{ detail.baseTemplateVersion }} · 当前版本 v{{ detail.currentTemplateVersion }}</span></div>
          <div v-if="detail.stale" class="stale-warning"><strong>该申请已过期</strong><span>模板在提交后已经变化，不能通过；可驳回并提示商户按最新版本重提。</span></div>
          <div class="review-note"><strong>商户说明</strong><p>{{ detail.submitNote || '未填写' }}</p></div>
          <div v-if="detail.reviewReason" class="review-note review-note--result"><strong>审核意见</strong><p>{{ detail.reviewReason }}</p></div>
          <SnapshotComparison :base-snapshot="detail.baseSnapshot" :target-snapshot="detail.targetSnapshot" />
        </div>
        <footer><button class="ghost-button" type="button" @click="closeReview">关闭</button><template v-if="detail.status === 'PENDING'"><button class="ghost-button danger-text" type="button" :disabled="busy" @click="reject">驳回</button><button class="primary-button" type="button" :disabled="busy || detail.stale" :title="detail.stale ? '模板版本已变化，不能通过' : ''" @click="approve">{{ busy ? '处理中...' : '通过并覆盖模板' }}</button></template></footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import SnapshotComparison from '../../components/SnapshotComparison.vue';
import StatusPill from '../../components/StatusPill.vue';
import { approveDishTemplateChange, getAdminDishTemplateChange, listAdminDishTemplateChanges, rejectDishTemplateChange } from '../../api/dish-template-changes';
import { promptAction } from '../../utils/dialog';
import { listAdminMerchants } from '../../api/admin-merchants';
import { formatTemplateChangeTime, TEMPLATE_CHANGE_STATUS } from '../../utils/dish-template-changes';
import { notify } from '../../utils/feedback';

const loading = ref(false); const busy = ref(false); const detail = ref(null);
const merchants = ref([]); const merchantLoading = ref(false); const merchantError = ref(false);
async function loadMerchants() {
  merchantLoading.value = true; merchantError.value = false;
  try { merchants.value = await listAdminMerchants(); }
  catch { merchantError.value = true; }
  finally { merchantLoading.value = false; }
}
const query = reactive({ status: 'PENDING', keyword: '', merchantId: '' });
const page = reactive({ items: [], total: 0, page: 1, pageSize: 20 });
async function load(targetPage = 1) { loading.value = true; try { const result = await listAdminDishTemplateChanges({ ...query, page: targetPage, pageSize: page.pageSize }); Object.assign(page, { items: result.items || [], total: result.total || 0, page: result.page || targetPage, pageSize: result.pageSize || 20 }); } finally { loading.value = false; } }
async function openReview(requestId) { detail.value = await getAdminDishTemplateChange(requestId); }
function closeReview() { detail.value = null; }
async function approve() { if (detail.value.stale) return; const reason = await promptAction({ title: '通过模板修改申请', label: '审核意见（可选）', placeholder: '可留空', required: false, confirmText: '确认通过' }); if (reason === null) return; busy.value = true; try { await approveDishTemplateChange(detail.value.requestId, reason || ''); notify('已通过，平台模板已更新', 'success'); await load(page.page); detail.value = await getAdminDishTemplateChange(detail.value.requestId); } finally { busy.value = false; } }
async function reject() { const reason = await promptAction({ title: '驳回模板修改申请', label: '驳回原因', placeholder: '请明确说明需要调整的内容', danger: true, required: true, confirmText: '确认驳回' }); if (!reason) return; busy.value = true; try { await rejectDishTemplateChange(detail.value.requestId, reason); notify('申请已驳回并通知商户', 'success'); await load(page.page); detail.value = await getAdminDishTemplateChange(detail.value.requestId); } finally { busy.value = false; } }
onMounted(() => { loadMerchants(); load(1); });
</script>

<style scoped>
.review-toolbar{display:grid;grid-template-columns:170px minmax(240px,1fr) 150px auto;gap:12px;align-items:end;margin-bottom:16px}.review-table{min-width:1080px}.reason-cell{max-width:260px;white-space:normal}.pager{display:flex;align-items:center;justify-content:flex-end;gap:12px;margin-top:18px;color:var(--text-soft);font-size:13px}.review-modal-layer{position:fixed;inset:0;z-index:90;display:grid;place-items:center;padding:24px;background:rgba(26,34,29,.58);backdrop-filter:blur(4px)}.review-modal{display:flex;flex-direction:column;width:min(1160px,100%);max-height:calc(100vh - 48px);overflow:hidden;border:1px solid var(--line);border-radius:18px;background:#fffdf8;box-shadow:0 28px 90px rgba(22,32,26,.3)}.review-modal>header,.review-modal>footer{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:20px 24px;border-bottom:1px solid var(--line)}.review-modal>header h3{margin:4px 0;font-size:24px}.review-modal>header p,.review-note p{margin:0;color:var(--text-soft)}.review-body{display:grid;gap:16px;overflow:auto;padding:20px 24px}.review-modal>footer{justify-content:flex-end;align-items:center;border-top:1px solid var(--line);border-bottom:0}.review-status-line{display:flex;align-items:center;gap:12px;color:var(--text-soft);font-size:13px}.stale-warning{display:flex;flex-direction:column;gap:4px;padding:14px;border-left:3px solid #d9912d;background:#fff1d5;color:#81580e}.review-note{padding:14px;border-left:3px solid var(--brand);background:#f3f8f2}.review-note--result{border-left-color:#4d78b9;background:#eef4ff}@media(max-width:880px){.review-toolbar{grid-template-columns:1fr 1fr}}@media(max-width:620px){.review-toolbar{grid-template-columns:1fr}.review-modal-layer{padding:10px}.review-modal{max-height:calc(100vh - 20px)}}
</style>
