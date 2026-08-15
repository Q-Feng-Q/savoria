function normalizeIds(ids) {
  return Array.from(new Set((ids || []).map(Number).filter(Number.isFinite))).slice(0, 100);
}

function createDishTemplateSelection(initialIds = []) {
  let selectedIds = normalizeIds(initialIds);
  function state(limitReached = false) {
    return { selectedIds: selectedIds.slice(), limitReached };
  }
  return {
    toggle(item) {
      const id = Number(item && item.templateId);
      if (!Number.isFinite(id) || item.imported) return state();
      if (selectedIds.includes(id)) selectedIds = selectedIds.filter(value => value !== id);
      else if (selectedIds.length < 100) selectedIds.push(id);
      return state(selectedIds.length >= 100);
    },
    selectPage(items) {
      let limitReached = false;
      for (const item of items || []) {
        const id = Number(item.templateId);
        if (!Number.isFinite(id) || item.imported || selectedIds.includes(id)) continue;
        if (selectedIds.length >= 100) { limitReached = true; break; }
        selectedIds.push(id);
      }
      return state(limitReached || selectedIds.length >= 100);
    },
    clear() { selectedIds = []; return state(); },
    snapshot() { return state(); }
  };
}

module.exports = { createDishTemplateSelection };
