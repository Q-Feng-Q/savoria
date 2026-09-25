const STATUS = { OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' };
function validateDraft({ type, content, images = [] }) {
  if (!['BUG', 'SUGGESTION'].includes(type)) return '请选择反馈类型';
  if (!String(content || '').trim()) return '请填写问题或建议描述';
  if (String(content).trim().length > 2000) return '描述最多 2000 字';
  if (images.length > 6) return '最多选择 6 张图片';
  if (images.some((image) => image.state !== 'ready' || !image.imageId)) return '请等待图片上传完成，或重试失败图片';
  return '';
}
function presentFeedback(row) {
  return { ...row, typeLabel: row.type === 'SUGGESTION' ? '功能建议' : 'BUG 反馈',
    statusLabel: STATUS[row.status] || '处理中', timeLabel: String(row.createdAt || '').replace('T', ' ').slice(0, 16) };
}
function createSubmission(makeId = () => `${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`) {
  let payload = null;
  return {
    prepare(draft) {
      if (!payload) payload = { requestId: makeId(), type: draft.type, content: draft.content.trim(), imageIds: draft.images.map((image) => image.imageId) };
      return { ...payload, imageIds: [...payload.imageIds] };
    },
    pending: () => Boolean(payload),
    clear() { payload = null; }
  };
}
module.exports = { validateDraft, presentFeedback, createSubmission };
