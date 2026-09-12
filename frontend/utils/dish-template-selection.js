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

const SOURCE_LABELS = {
  COOK_LIKE_HOC: 'CookLikeHOC',
  LOCAL_EXTENSION: '本地扩展'
};

function templatePriceText(value) {
  if (value === null || value === undefined || value === '') return '价格待完善';
  const price = Number(value);
  return Number.isFinite(price) ? `¥${price.toFixed(0)}` : '价格待完善';
}

function decorateTemplateRows(rows, selectedIds = [], imageResolver = (value) => value || '') {
  const selected = new Set((selectedIds || []).map(Number));
  return (rows || []).map((item) => {
    const rawImageUrl = item.imageUrl || '';
    return {
      ...item,
      imageUrl: imageResolver(rawImageUrl),
      hasImage: Boolean(rawImageUrl),
      selected: selected.has(Number(item.templateId)),
      tagsText: (item.tasteTags || []).join(' · '),
      priceText: templatePriceText(item.referencePrice),
      sourceLabel: SOURCE_LABELS[item.sourceType] || item.sourceCategory || '平台菜谱',
      stepsLabel: item.missingSteps ? '步骤待补充' : '含制作步骤'
    };
  });
}

function ingredientQuantityText(item = {}) {
  const status = item.quantityStatus || (item.quantity === null || item.quantity === undefined
    ? 'MISSING' : 'VERIFIED');
  if (status === 'NOT_APPLICABLE') return '无需采购';
  if (status === 'SOURCE_BATCH') return item.sourceQuantityText || '按原配方批量';
  if (status !== 'VERIFIED') return '用量待完善';
  return `${item.quantity}${item.unit ? ` ${item.unit}` : ''}`;
}

function decorateTemplateDetail(detail = {}, imageResolver = (value) => value || '') {
  const row = decorateTemplateRows([detail], [], imageResolver)[0];
  const cookingSteps = (detail.cookingSteps || []).slice()
    .sort((left, right) => Number(left.stepNo || 0) - Number(right.stepNo || 0))
    .map((item, index) => ({
      ...item,
      titleText: item.title || `步骤 ${item.stepNo || index + 1}`
    }));
  return {
    ...row,
    descriptionText: detail.description || '暂无菜谱简介',
    tasteText: (detail.tasteTags || []).join(' · '),
    mealText: (detail.mealTags || []).map((value) => ({
      BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐'
    }[value] || value)).join(' · '),
    ingredients: (detail.ingredients || []).map((item) => ({
      ...item,
      quantityText: ingredientQuantityText(item)
    })),
    cookingSteps,
    hasCookingSteps: cookingSteps.length > 0,
    imageCreditText: detail.imageAuthor && detail.imageLicense
      ? `${detail.imageAuthor} · ${detail.imageLicense}` : '暂无公开图片授权信息'
  };
}

module.exports = {
  createDishTemplateSelection,
  decorateTemplateDetail,
  decorateTemplateRows,
  ingredientQuantityText,
  templatePriceText
};
