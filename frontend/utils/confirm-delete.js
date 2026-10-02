async function confirmDelete(owner, title, content) {
  if (owner._confirmingDelete) return false;
  owner._confirmingDelete = true;
  try {
    const result = await wx.showModal({ title, content, confirmText: '确认删除', confirmColor: '#b7473d' });
    return Boolean(result && result.confirm);
  } catch (_) {
    return false;
  } finally {
    owner._confirmingDelete = false;
  }
}

module.exports = { confirmDelete };
