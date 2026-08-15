function getPurchaseItemKey(item) {
  return `${item.ingredientId || item.id}__${item.unit || ''}`;
}

function decoratePurchaseItems(items = [], checks = {}) {
  return items.map((item) => {
    const key = getPurchaseItemKey(item);
    return {
      ...item,
      purchaseKey: key,
      isChecked: Boolean(checks[key])
    };
  });
}

function togglePurchaseCheck(checks = {}, key) {
  const next = { ...checks };
  next[key] = !next[key];
  return next;
}

function addTemporaryPurchaseItem(items = [], item) {
  if (!item.name || !item.quantity || !item.unit) return items;
  return items.concat({
    id: `temp-${Date.now()}`,
    name: item.name,
    quantity: item.quantity,
    unit: item.unit,
    category: item.category || '临时',
    isTemp: true
  });
}

module.exports = {
  getPurchaseItemKey,
  decoratePurchaseItems,
  togglePurchaseCheck,
  addTemporaryPurchaseItem
};
