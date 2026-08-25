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

function buildTemplateSnapshot(source = {}) {
  return {
    schemaVersion: 1,
    categoryId: Number(source.categoryId),
    name: trim(source.name),
    description: trim(source.description),
    imageUrl: trim(source.imageUrl),
    imageSourceUrl: trim(source.imageSourceUrl),
    imageAuthor: trim(source.imageAuthor),
    imageLicense: trim(source.imageLicense),
    referencePrice: Number(source.referencePrice),
    tasteTags: normalizeTags(source.tasteTags),
    mealTags: normalizeTags(source.mealTags).filter((item) => ['BREAKFAST', 'LUNCH', 'DINNER'].includes(item)),
    sortOrder: Number(source.sortOrder || 0),
    enabled: source.enabled !== false,
    ingredients: (source.ingredients || []).map((item, index) => ({
      ingredientName: trim(item.ingredientName || item.name),
      ingredientCategory: trim(item.ingredientCategory || item.category),
      quantity: Number(item.quantity),
      unit: trim(item.unit),
      calcType: item.calcType || item.calculationType || 'FIXED',
      sortOrder: Number.isFinite(Number(item.sortOrder)) ? Number(item.sortOrder) : index + 1
    }))
  };
}

function validateTemplateSnapshot(snapshot) {
  if (!Number.isInteger(snapshot.categoryId) || snapshot.categoryId < 1) return '请选择模板分类';
  if (!snapshot.name) return '请填写菜品名称';
  if (!snapshot.description) return '请填写菜品简介';
  if (!snapshot.imageUrl) return '请上传菜品图片';
  if (!snapshot.imageSourceUrl) return '请填写图片来源地址';
  if (!snapshot.imageAuthor) return '请填写图片作者或来源平台';
  if (!snapshot.imageLicense) return '请填写图片授权说明';
  if (!Number.isFinite(snapshot.referencePrice) || snapshot.referencePrice < 0) return '参考价格必须是大于等于 0 的数字';
  if (!snapshot.ingredients.length) return '模板菜品至少需要 1 项食材';
  for (const item of snapshot.ingredients) {
    if (!item.ingredientName || !item.ingredientCategory || !item.unit) return '请补全食材名称、分类和单位';
    if (!Number.isFinite(item.quantity) || item.quantity < 0) return '食材用量必须是大于等于 0 的数字';
    if (!['FIXED', 'PER_PERSON', 'NO_PURCHASE'].includes(item.calcType)) return '请选择正确的食材计算方式';
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
