function createDirtyForm(adapter, message = '内容尚未保存，确定离开吗？') {
  let dirty = false;

  return {
    markDirty() {
      if (dirty) return;
      dirty = true;
      if (adapter && typeof adapter.enableAlertBeforeUnload === 'function') {
        adapter.enableAlertBeforeUnload({ message });
      }
    },
    markClean() {
      if (!dirty) return;
      dirty = false;
      if (adapter && typeof adapter.disableAlertBeforeUnload === 'function') {
        adapter.disableAlertBeforeUnload();
      }
    },
    isDirty() {
      return dirty;
    },
    dispose() {
      if (dirty && adapter && typeof adapter.disableAlertBeforeUnload === 'function') {
        adapter.disableAlertBeforeUnload();
      }
      dirty = false;
    }
  };
}

module.exports = { createDirtyForm };
