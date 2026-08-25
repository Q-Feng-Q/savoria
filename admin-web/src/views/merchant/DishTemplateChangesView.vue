<template>
  <div class="view-stack template-change-page">
    <SectionCard title="模板修改申请" subtitle="查看提交记录、双快照和审核结果，待审核申请可撤回">
      <template #actions><button class="ghost-button" type="button" :disabled="loading" @click="load(1)">刷新</button></template>
      <div class="template-change-toolbar">
        <label class="form-field"><span>状态</span><select v-model="query.status" @change="load(1)"><option value="">全部状态</option><option v-for="(label, value) in TEMPLATE_CHANGE_STATUS" :key="value" :value="value">{{ label }}</option></select></label>
        <label class="form-field"><span>模板菜名</span><input v-model.trim="query.keyword" type="search" placeholder="输入菜名" @keyup.enter="load(1)" /></label>
        <button class="primary-button" type="button" @click="load(1)">查询</button>
      </div>
      <div v-if="loading" class="table-empty">正在加载申请记录...</div>
      <AppEmpty v-else-if="!page.items.length" title="暂无模板修改申请" description="可以从模板菜市场发起第一条申请。" />
      <div v-else class="user-table-scroll">
        <table class="user-data-table change-table"><thead><tr><th>模板菜品</th><th>提交商户</th><th>状态</th><th>提交说明</th><th>提交时间</th><th>操作</th></tr></thead><tbody>
          <tr v-for="item in page.items" :key="item.requestId"><td><strong>{{ item.templateName }}</strong><small>申请 #{{ item.requestId }} · 模板 #{{ item.templateId }}</small></td><td>{{ item.merchantName || `商户 #${item.merchantId}` }}</td><td><StatusPill :status="item.status" :label="TEMPLATE_CHANGE_STATUS[item.status]" /></td><td class="note-cell">{{ item.submitNote || '未填写' }}</td><td>{{ formatTemplateChangeTime(item.submittedAt) }}</td><td><button class="text-button" type="button" @click="openDetail(item.requestId)">查看对比</button></td></tr>
        </tbody></table>
      </div>
      <div class="pager"><button class="ghost-button" type="button" :disabled="page.page <= 1" @click="load(page.page - 1)">上一页</button><span>第 {{ page.page }} 页 · 共 {{ page.total }} 条</span><button class="ghost-button" type="button" :disabled="page.page * page.pageSize >= page.total" @click="load(page.page + 1)">下一页</button></div>
    </SectionCard>

    <div v-if="detail" class="change-modal-layer" @click.self="closeDetail">
      <section class="change-detail-modal" role="dialog" aria-modal="true">
        <header><div><span class="page-eyebrow">REQUEST #{{ detail.requestId }}</span><h3>{{ detail.templateName }}</h3><p>{{ formatTemplateChangeTime(detail.submittedAt) }} · 版本 {{ detail.baseTemplateVersion }} → {{ detail.currentTemplateVersion }}</p></div><button class="modal-close" type="button" aria-label="关闭" @click="closeDetail">×</button></header>
        <div class="change-detail-body">
          <div class="detail-actions"><StatusPill :status="detail.status" :label="TEMPLATE_CHANGE_STATUS[detail.status]" /><span v-if="detail.stale" class="stale-badge">模板已变化，需撤回后重提</span></div>
          <div class="change-note"><strong>提交说明</strong><p>{{ detail.submitNote || '未填写' }}</p></div>
          <div v-if="detail.reviewReason" class="change-note change-note--review"><strong>审核意见</strong><p>{{ detail.reviewReason }}</p></div>
          <SnapshotComparison :base-snapshot="detail.baseSnapshot" :target-snapshot="detail.targetSnapshot" />
        </div>
        <footer><button class="ghost-button" type="button" @click="closeDetail">关闭</button><button v-if="detail.status === 'PENDING'" class="ghost-button danger-text" type="button" :disabled="busy" @click="withdraw">{{ busy ? '撤回中...' : '撤回申请' }}</button></footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import AppEmpty from '../../components/AppEmpty.vue';
import SectionCard from '../../components/SectionCard.vue';
import SnapshotComparison from '../../components/SnapshotComparison.vue';
import StatusPill from '../../components/StatusPill.vue';
import { getMerchantDishTemplateChange, listMerchantDishTemplateChanges, withdrawDishTemplateChange } from '../../api/dish-template-changes';
import { confirmAction } from '../../utils/dialog';
import { formatTemplateChangeTime, TEMPLATE_CHANGE_STATUS } from '../../utils/dish-template-changes';

const loading = ref(false); const busy = ref(false); const detail = ref(null);
const route = useRoute();
const query = reactive({ status: '', keyword: '' });
const page = reactive({ items: [], total: 0, page: 1, pageSize: 20 });
async function load(targetPage = 1) { loading.value = true; try { const result = await listMerchantDishTemplateChanges({ ...query, page: targetPage, pageSize: page.pageSize }); Object.assign(page, { items: result.items || [], total: result.total || 0, page: result.page || targetPage, pageSize: result.pageSize || 20 }); } finally { loading.value = false; } }
async function openDetail(requestId) { detail.value = await getMerchantDishTemplateChange(requestId); }
function closeDetail() { detail.value = null; }
async function withdraw() { const accepted = await confirmAction({ title: '撤回模板修改申请', message: '撤回后平台将无法继续审核，本次修改不会生效。', confirmText: '确认撤回', danger: true }); if (!accepted) return; busy.value = true; try { await withdrawDishTemplateChange(detail.value.requestId); await load(page.page); detail.value = await getMerchantDishTemplateChange(detail.value.requestId); } finally { busy.value = false; } }
onMounted(async () => {
  await load(1);
  if (route.query.requestId) await openDetail(route.query.requestId);
});
</script>

<style scoped>
.template-change-toolbar{display:grid;grid-template-columns:180px minmax(240px,1fr) auto;gap:12px;align-items:end;margin-bottom:16px}.change-table{min-width:980px}.note-cell{max-width:280px;white-space:normal}.pager{display:flex;align-items:center;justify-content:flex-end;gap:12px;margin-top:18px;color:var(--text-soft);font-size:13px}.change-modal-layer{position:fixed;inset:0;z-index:90;display:grid;place-items:center;padding:24px;background:rgba(26,34,29,.55);backdrop-filter:blur(4px)}.change-detail-modal{display:flex;flex-direction:column;width:min(1120px,100%);max-height:calc(100vh - 48px);overflow:hidden;border:1px solid var(--line);border-radius:18px;background:#fffdf8;box-shadow:0 28px 90px rgba(22,32,26,.28)}.change-detail-modal>header,.change-detail-modal>footer{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:20px 24px;border-bottom:1px solid var(--line)}.change-detail-modal>header h3{margin:4px 0;font-size:24px}.change-detail-modal>header p,.change-note p{margin:0;color:var(--text-soft)}.change-detail-body{display:grid;gap:16px;overflow:auto;padding:20px 24px}.change-detail-modal>footer{justify-content:flex-end;border-top:1px solid var(--line);border-bottom:0}.stale-badge{padding:7px 10px;border-radius:8px;background:#fff0d2;color:#92610a;font-size:12px;font-weight:700}.change-note{padding:14px;border-left:3px solid var(--brand);background:#f4f8f3}.change-note--review{border-left-color:#e3a13a;background:#fff7e8}@media(max-width:760px){.template-change-toolbar{grid-template-columns:1fr}.change-modal-layer{padding:10px}.change-detail-modal{max-height:calc(100vh - 20px)}}
</style>
