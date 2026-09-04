const STATUS_META = {
  PENDING: { label: '待审核', tone: 'pending' },
  APPROVED: { label: '已通过', tone: 'approved' },
  REJECTED: { label: '已驳回', tone: 'rejected' },
  WITHDRAWN: { label: '已撤回', tone: 'withdrawn' }
};

function trim(value) {
  return String(value === undefined || value === null ? '' : value).trim();
}

function normalizeTags(value) {
  const values = Array.isArray(value) ? value : String(value || '').split(/[，,]/);
  return Array.from(new Set(values.map(trim).filter(Boolean)));
}

function nullableText(value) {
  const valueText = trim(value);
  return valueText || null;
}

function nullableNumber(value) {
  if (value === null || value === undefined || trim(value) === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : NaN;
}

function ingredientItemId(item, index) {
  return trim(item.itemId || item.sourceLineKey)
    || (item.id ? `template-ingredient:${item.id}` : `client-ingredient:${index + 1}`);
}

function stepItemId(item, index) {
  return trim(item.itemId || item.itemKey)
    || (item.id ? `template-step:${item.id}` : `client-step:${index + 1}`);
}

function buildTemplateSnapshot(source = {}) {
  return {
    schemaVersion: 2,
    categoryId: Number(source.categoryId),
    name: trim(source.name),
    description: nullableText(source.description),
    referencePrice: nullableNumber(source.referencePrice),
    tasteTags: normalizeTags(source.tasteTags),
    mealTags: normalizeTags(source.mealTags).filter((item) => ['BREAKFAST', 'LUNCH', 'DINNER'].includes(item)),
    sortOrder: Number(source.sortOrder || 0),
    enabled: source.enabled !== false,
    ingredients: (source.ingredients || []).map((item, index) => {
      const rawCalcType = item.calcType || item.calculationType || null;
      const quantityStatus = item.quantityStatus
        || (rawCalcType === 'NO_PURCHASE' ? 'NOT_APPLICABLE'
          : (nullableNumber(item.quantity) === null ? 'MISSING' : 'VERIFIED'));
      const verified = quantityStatus === 'VERIFIED';
      return {
        itemId: ingredientItemId(item, index),
        ingredientName: trim(item.ingredientName || item.name),
        ingredientCategory: trim(item.ingredientCategory || item.category),
        quantityStatus,
        quantity: verified ? nullableNumber(item.quantity) : null,
        unit: verified ? nullableText(item.unit) : null,
        calcType: verified ? (rawCalcType || 'FIXED') : null,
        sourceText: nullableText(item.sourceText),
        sourceQuantityText: nullableText(item.sourceQuantityText),
        componentTemplateId: nullableNumber(item.componentTemplateId),
        componentMultiplier: nullableNumber(item.componentMultiplier),
        sortOrder: Number.isFinite(Number(item.sortOrder)) ? Number(item.sortOrder) : index + 1
      };
    }),
    cookingSteps: (source.cookingSteps || []).map((item, index) => ({
      itemId: stepItemId(item, index),
      stepNo: index + 1,
      title: nullableText(item.title),
      content: trim(item.content),
      durationSeconds: nullableNumber(item.durationSeconds),
      temperatureText: nullableText(item.temperatureText),
      heatLevel: nullableText(item.heatLevel),
      componentTemplateId: nullableNumber(item.componentTemplateId)
    }))
  };
}

function validateTemplateSnapshot(snapshot) {
  if (!snapshot || snapshot.schemaVersion !== 2) return '模板快照版本不正确';
  if (!Number.isInteger(snapshot.categoryId) || snapshot.categoryId < 1) return '请选择模板分类';
  if (!snapshot.name) return '请填写菜品名称';
  if (snapshot.referencePrice !== null
      && (!Number.isFinite(snapshot.referencePrice) || snapshot.referencePrice < 0)) {
    return '参考价格必须留空或填写大于等于 0 的数字';
  }
  if (!snapshot.ingredients.length) return '模板菜品至少需要 1 项食材';
  const ingredientIds = new Set();
  for (const item of snapshot.ingredients) {
    if (!item.itemId || ingredientIds.has(item.itemId)) return '请检查食材项目编号';
    ingredientIds.add(item.itemId);
    if (!item.ingredientName || !item.ingredientCategory) return '请补全食材名称和分类';
    if (!['VERIFIED', 'SOURCE_BATCH', 'MISSING', 'NOT_APPLICABLE'].includes(item.quantityStatus)) {
      return '请选择正确的食材数量状态';
    }
    if (item.quantityStatus === 'VERIFIED') {
      if (!Number.isFinite(item.quantity) || item.quantity <= 0 || !item.unit) return '请补全已核实食材的用量和单位';
      if (!['FIXED', 'PER_PERSON'].includes(item.calcType)) return '请选择正确的食材计算方式';
    } else if (item.quantity !== null || item.unit !== null || item.calcType !== null) {
      return '未核实食材不能填写采购用量';
    }
    if (item.componentTemplateId !== null
      && (!Number.isInteger(item.componentTemplateId) || item.componentTemplateId <= 0
        || !Number.isFinite(item.componentMultiplier) || item.componentMultiplier <= 0)) {
      return '请检查配料组件和展开倍数';
    }
  }
  const stepIds = new Set();
  for (const [index, item] of (snapshot.cookingSteps || []).entries()) {
    if (!item.itemId || stepIds.has(item.itemId)) return '请检查制作步骤项目编号';
    stepIds.add(item.itemId);
    if (item.stepNo !== index + 1 || !item.content) return '请按顺序补全制作步骤内容';
  }
  return '';
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '';
}

function decorateChangeRequest(item = {}) {
  const status = String(item.status || '').toUpperCase();
  const meta = STATUS_META[status] || { label: item.status || '未知状态', tone: 'unknown' };
  return {
    ...item,
    statusLabel: meta.label,
    statusTone: meta.tone,
    submittedText: formatTime(item.submittedAt),
    canWithdraw: status === 'PENDING',
    staleLabel: item.stale ? '模板已发生变化' : ''
  };
}

module.exports = {
  STATUS_META,
  buildTemplateSnapshot,
  decorateChangeRequest,
  normalizeTags,
  validateTemplateSnapshot
};
