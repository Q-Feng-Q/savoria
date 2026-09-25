<template>
  <div class="feedback-admin">
    <header class="feedback-toolbar">
      <div><span class="page-eyebrow">USER FEEDBACK</span><h2>用户反馈</h2><p>查看用户提交的问题与建议，记录处理进度和回复。</p></div>
      <button class="ghost-button" type="button" :disabled="state.loading" @click="controller.loadList">刷新列表</button>
    </header>
    <div class="feedback-workspace" :class="{ 'has-detail': state.selectedId !== null }">
      <section class="feedback-list" aria-label="反馈列表">
        <div class="feedback-filters">
          <label>类型<select v-model="state.type" @change="controller.filter"><option value="">全部类型</option><option v-for="(label, key) in feedbackTypes" :key="key" :value="key">{{ label }}</option></select></label>
          <label>状态<select v-model="state.status" @change="controller.filter"><option value="">全部状态</option><option v-for="(label, key) in feedbackStatuses" :key="key" :value="key">{{ label }}</option></select></label>
          <span class="muted">共 {{ state.total }} 条</span>
        </div>
        <div v-if="state.listError" class="feedback-error" role="alert">{{ state.listError }} <button type="button" @click="controller.loadList">重试</button></div>
        <p v-else-if="state.loading" class="feedback-empty" role="status">正在读取反馈…</p>
        <p v-else-if="!state.items.length" class="feedback-empty">暂无符合条件的反馈</p>
        <div v-else class="feedback-table-scroll">
          <table><thead><tr><th>反馈内容</th><th>提交人</th><th>状态 / 时间</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in state.items" :key="item.feedbackId" :class="{ selected: state.selectedId === item.feedbackId }">
              <td><span class="feedback-kind">{{ feedbackTypes[item.type] }}</span><p class="feedback-summary">{{ item.content }}</p></td>
              <td>{{ item.ownerName || '未设置昵称' }}<small>账号 #{{ item.ownerUserId }}</small></td>
              <td><span class="feedback-badge">{{ feedbackStatuses[item.status] }}</span><small>{{ time(item.createdAt) }}</small></td>
              <td><button class="ghost-button" type="button" @click="controller.open(item.feedbackId)">查看</button></td>
            </tr></tbody>
          </table>
        </div>
        <footer class="feedback-pagination"><button class="ghost-button" type="button" :disabled="state.loading || state.page <= 1" @click="controller.turnPage(state.page - 1)">上一页</button><span>{{ state.page }} / {{ Math.max(1, Math.ceil(state.total / 20)) }}</span><button class="ghost-button" type="button" :disabled="state.loading || state.page * 20 >= state.total" @click="controller.turnPage(state.page + 1)">下一页</button></footer>
      </section>
      <aside v-if="state.selectedId !== null" class="feedback-inspector" aria-label="反馈详情">
        <header><h3>反馈 #{{ state.selectedId }}</h3><button class="ghost-button" type="button" @click="controller.close">关闭详情</button></header>
        <p v-if="state.detailLoading" role="status">正在读取详情…</p>
        <div v-if="state.detailError" class="feedback-error" role="alert">{{ state.detailError }} <button type="button" @click="controller.refresh">重试</button></div>
        <template v-if="state.detail">
          <p class="muted">{{ state.detail.ownerName || '未设置昵称' }} · 账号 #{{ state.detail.ownerUserId }}<br>{{ time(state.detail.createdAt) }} · {{ feedbackTypes[state.detail.type] }}</p>
          <h4>原始描述</h4><p class="feedback-text">{{ state.detail.content }}</p>
          <div class="feedback-images">
            <div v-for="(image, index) in state.images" :key="image.imageId" class="feedback-image">
              <button v-if="image.url" type="button" class="feedback-thumbnail" @click="state.preview = image.url"><img :src="image.url" :alt="`反馈截图 ${index + 1}，点击放大`"></button>
              <span v-else-if="image.loading">图片读取中…</span>
              <button v-else type="button" @click="controller.loadImage(index)">{{ image.error || '读取图片' }} · 重试</button>
            </div>
          </div>
          <section v-if="state.preview" class="feedback-preview" aria-label="截图预览"><button class="ghost-button" type="button" @click="state.preview = ''">收起图片</button><img :src="state.preview" alt="反馈截图放大预览"></section>
          <form class="feedback-form" @submit.prevent="controller.save">
            <h4>处理反馈</h4>
            <label>状态<select v-model="state.editStatus" :disabled="state.saving"><option v-for="(label, key) in feedbackStatuses" :key="key" :value="key">{{ label }}</option></select></label>
            <label>回复用户<textarea v-model="state.reply" :disabled="state.saving" maxlength="2000" rows="6" placeholder="说明处理结果，用户将在反馈详情中看到回复" /></label>
            <span class="muted">{{ state.reply.length }} / 2000 · 已解决、已关闭必须填写回复</span>
            <p v-if="state.saveError" class="feedback-error" role="alert">{{ state.saveError }}</p>
            <button v-if="state.conflict" class="ghost-button" type="button" @click="controller.refresh">刷新详情（替换当前草稿）</button>
            <p v-if="state.saved" class="feedback-success" role="status">处理结果已保存</p>
            <button class="primary-button" type="submit" :disabled="state.saving || state.conflict">{{ state.saving ? '保存中…' : '保存处理结果' }}</button>
          </form>
          <section class="feedback-history"><h4>处理记录</h4><p v-if="!state.detail.history?.length" class="muted">暂无处理记录</p><div v-for="entry in state.detail.history" :key="entry.historyId" class="feedback-history-item"><strong>{{ feedbackStatuses[entry.fromStatus] }} → {{ feedbackStatuses[entry.toStatus] }}</strong><small>{{ time(entry.createdAt) }} · 管理员 #{{ entry.adminId }}</small><p class="feedback-text">{{ entry.reply || '未填写回复' }}</p></div></section>
        </template>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, watch } from 'vue';
import { useAuthStore } from '../../stores/auth';
import { listAdminFeedback, getAdminFeedback, updateAdminFeedback, getFeedbackImage } from '../../api/feedback';
import { createFeedbackManagement, feedbackStatuses, feedbackTypes } from '../../utils/feedback-management';
const auth = useAuthStore();
const state = reactive({});
const controller = createFeedbackManagement({ state, api: { list: listAdminFeedback, detail: getAdminFeedback, update: updateAdminFeedback, image: getFeedbackImage } });
const time = value => String(value || '').replace('T', ' ').slice(0, 16);
watch(() => [JSON.stringify(auth.session), auth.apiBaseUrl], () => {
  controller.reset();
  if (auth.isPlatformAdmin) controller.loadList();
}, { immediate: true, flush: 'sync' });
onBeforeUnmount(() => controller.dispose());
</script>

<style scoped>
.feedback-admin { color:#513923; }
.feedback-toolbar,.feedback-inspector > header,.feedback-filters,.feedback-pagination { display:flex; align-items:center; justify-content:space-between; gap:16px; }
.feedback-toolbar { margin-bottom:24px; }
h2,h3,h4,p { margin-top:0; }
h2 { margin:6px 0 10px; } h4 { margin:24px 0 12px; }
.feedback-toolbar p,.muted,small { color:#82694e; font-size:13px; line-height:1.7; }
.feedback-workspace { display:grid; grid-template-columns:minmax(0,1fr); gap:24px; }
.feedback-workspace.has-detail { grid-template-columns:minmax(0,1.25fr) minmax(320px,1fr); }
.feedback-list,.feedback-inspector { min-width:0; border:1px solid #e6d3af; border-radius:20px; background:#fff4dc; padding:22px; }
.feedback-filters { flex-wrap:wrap; justify-content:flex-start; margin-bottom:20px; }
label { display:flex; gap:8px; flex-direction:column; font-size:14px; font-weight:600; }
select,textarea { padding:10px 12px; border:1px solid #ceb991; border-radius:10px; background:#fff8e8; color:#513923; font:inherit; min-width:0; }
.feedback-table-scroll { overflow-x:auto; }
table { width:100%; border-collapse:collapse; text-align:left; font-size:14px; }
th { color:#82694e; font-size:12px; font-weight:500; padding:12px 8px; }
td { border-top:1px solid #e8d8b9; padding:18px 8px; vertical-align:top; }
tr.selected { background:#f0e9cb; }
td:first-child { min-width:150px; } td small { display:block; margin-top:8px; white-space:nowrap; }
.feedback-summary { display:-webkit-box; -webkit-line-clamp:3; -webkit-box-orient:vertical; overflow:hidden; word-break:break-word; line-height:1.7; margin:8px 0 0; }
.feedback-kind { font-size:12px; color:#8c5a31; }
.feedback-badge { display:inline-block; padding:4px 9px; border-radius:16px; background:#e0e7c8; color:#4c6635; white-space:nowrap; font-size:12px; }
.feedback-pagination { margin-top:24px; font-size:13px; }
.feedback-inspector { align-self:start; }
.feedback-inspector > header { border-bottom:1px solid #e6d3af; padding-bottom:16px; margin-bottom:16px; }
.feedback-inspector h3 { margin:0; }
.feedback-text { white-space:pre-wrap; overflow-wrap:anywhere; line-height:1.85; font-size:14px; }
.feedback-images { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:10px; }
.feedback-image { min-width:0; font-size:12px; overflow-wrap:anywhere; }
.feedback-thumbnail { display:block; width:100%; padding:0; border:0; border-radius:10px; overflow:hidden; cursor:zoom-in; background:#ebddbc; }
.feedback-thumbnail img { width:100%; height:100px; object-fit:cover; display:block; }
.feedback-preview { margin-top:16px; }.feedback-preview img { display:block; width:100%; height:auto; margin-top:12px; }
.feedback-form { display:flex; flex-direction:column; gap:14px; margin-top:24px; border-top:1px solid #e6d3af; }
.feedback-form h4 { margin-bottom:0; }.feedback-form textarea { width:100%; box-sizing:border-box; resize:vertical; }
.feedback-error { padding:12px; color:#963f28; background:#f9e0c9; border-radius:10px; line-height:1.7; font-size:13px; }
.feedback-success { color:#4c6635; }.feedback-empty { padding:36px 0; text-align:center; color:#82694e; }
.feedback-history-item { border-top:1px solid #e6d3af; padding:16px 0; font-size:13px; }.feedback-history-item small { display:block; margin:6px 0; }
button { white-space:nowrap; }
button:disabled { opacity:.5; cursor:not-allowed; }
.feedback-list { align-self:start; }
@media(max-width:1100px) { .feedback-workspace.has-detail { grid-template-columns:minmax(0,1fr); } }
@media(max-width:540px) { .feedback-toolbar { align-items:flex-start; flex-direction:column; }.feedback-list,.feedback-inspector { padding:14px; }.feedback-pagination { gap:6px; } }
</style>
