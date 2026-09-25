export const feedbackStatuses = { OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' };
export const feedbackTypes = { BUG: 'BUG', SUGGESTION: '建议' };

export function validateFeedbackUpdate(status, reply, version) {
  if (!Object.hasOwn(feedbackStatuses, status)) throw new Error('反馈状态无效');
  const normalized = String(reply || '').trim();
  if (normalized.length > 2000) throw new Error('回复最多 2000 字符');
  if (['RESOLVED', 'CLOSED'].includes(status) && !normalized) throw new Error('已解决或已关闭需填写回复说明');
  return { status, reply: normalized, version };
}

// State is supplied by Vue in production; the same async lifecycle is exercised in Node tests.
export function createFeedbackManagement({ state, api, createUrl = blob => URL.createObjectURL(blob), revokeUrl = url => URL.revokeObjectURL(url) }) {
  const initial = () => ({ items: [], total: 0, page: 1, pageSize: 20, type: '', status: '', loading: false, listError: '', selectedId: null, detail: null, detailLoading: false, detailError: '', images: [], preview: '', editStatus: 'OPEN', reply: '', saving: false, saveError: '', saved: false, conflict: false });
  Object.assign(state, initial());
  let listVersion = 0, detailVersion = 0, disposed = false;
  function clearImages() {
    state.preview = '';
    state.images.forEach(image => { if (image.url) revokeUrl(image.url); });
    state.images = [];
  }
  function close() {
    detailVersion++;
    clearImages();
    Object.assign(state, { selectedId: null, detail: null, detailLoading: false, detailError: '', reply: '', editStatus: 'OPEN', saving: false, saveError: '', saved: false, conflict: false });
  }
  function reset() {
    listVersion++;
    close();
    Object.assign(state, initial());
  }
  async function loadList() {
    if (disposed) return;
    const version = ++listVersion;
    state.loading = true; state.listError = ''; state.items = [];
    try {
      const result = await api.list({ page: state.page, pageSize: 20, type: state.type, status: state.status });
      if (version !== listVersion || disposed) return;
      state.items = result.items || []; state.total = result.total || 0;
    } catch (error) {
      if (version === listVersion && !disposed) state.listError = error.message || '反馈加载失败';
    } finally {
      if (version === listVersion && !disposed) state.loading = false;
    }
  }
  async function loadImage(index, version = detailVersion) {
    const image = state.images[index];
    if (!image || image.loading || image.url || disposed) return;
    image.loading = true; image.error = '';
    try {
      const blob = await api.image(image.imageId);
      if (version !== detailVersion || disposed) return;
      image.url = createUrl(blob);
    } catch (error) {
      if (version === detailVersion && !disposed) image.error = error.message || '图片加载失败';
    } finally {
      if (version === detailVersion && !disposed) image.loading = false;
    }
  }
  async function open(id) {
    if (disposed) return;
    close(); state.selectedId = id;
    const version = detailVersion;
    state.detailLoading = true;
    try {
      const detail = await api.detail(id);
      if (version !== detailVersion || disposed) return;
      state.detail = detail; state.editStatus = detail.status; state.reply = detail.reply || '';
      state.images = (detail.images || []).map(image => ({ imageId: image.imageId, url: '', error: '', loading: false }));
      state.detailLoading = false;
      await Promise.all(state.images.map((_, index) => loadImage(index, version)));
    } catch (error) {
      if (version === detailVersion && !disposed) state.detailError = error.message || '详情加载失败';
    } finally {
      if (version === detailVersion && !disposed) state.detailLoading = false;
    }
  }
  async function save() {
    if (!state.detail || state.saving || state.conflict || disposed) return;
    state.saveError = ''; state.saved = false;
    let payload;
    try { payload = validateFeedbackUpdate(state.editStatus, state.reply, state.detail.version); }
    catch (error) { state.saveError = error.message; return; }
    const version = detailVersion, id = state.selectedId;
    state.saving = true;
    try {
      const detail = await api.update(id, payload);
      if (version !== detailVersion || disposed) return;
      state.detail = detail; state.reply = detail.reply || ''; state.editStatus = detail.status; state.saved = true;
      await loadList();
    } catch (error) {
      if (version !== detailVersion || disposed) return;
      state.conflict = error.status === 409 || error.code === 40901;
      state.saveError = state.conflict ? '反馈已被其他管理员修改。请先复制需要保留的草稿，再刷新详情后重新处理。' : error.message || '保存失败，请重试';
    } finally {
      if (version === detailVersion && !disposed) state.saving = false;
    }
  }
  return { loadList, open, close, reset, save, loadImage,
    filter() { state.page = 1; return loadList(); },
    refresh() { if (state.selectedId !== null && !state.saving) return open(state.selectedId); },
    turnPage(page) { if (page < 1 || page > Math.max(1, Math.ceil(state.total / 20))) return; state.page = page; return loadList(); },
    dispose() { reset(); disposed = true; }
  };
}
