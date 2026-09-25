export const TEMPLATE_CHANGE_STATUS = {
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', WITHDRAWN: '已撤回'
};

export const QUANTITY_STATUS_LABELS = {
  VERIFIED: '已核定', SOURCE_BATCH: '原配方批量', MISSING: '用量待补', NOT_APPLICABLE: '无需采购'
};

const text = (value) => String(value ?? '').trim();
const tags = (value) => Array.from(new Set((Array.isArray(value) ? value : String(value || '').split(/[，,]/)).map(text).filter(Boolean)));
const nullableNumber = (value) => value === null || value === undefined || text(value) === '' ? null : Number(value);
const stableId = (prefix, item, index) => text(item.itemId || (prefix === 'step' ? item.itemKey : item.sourceLineKey)) || `${prefix}-${item.id || item.templateIngredientId || item.templateCookingStepId || index + 1}`;

export const NOURISHMENT_FIELDS = [['nourishmentDescription', '滋补介绍'], ['servingAdvice', '食用建议'], ['precautions', '注意事项']];

export function foodInformation(value = {}) {
  return {
    productType: value.productType ?? 'NORMAL',
    ...Object.fromEntries(NOURISHMENT_FIELDS.map(([key]) => [key, text(value[key])]))
  };
}

export function validateFoodInformation(value) {
  if (!['NORMAL', 'NOURISHMENT'].includes(value.productType ?? 'NORMAL')) return '请选择菜品类型';
  for (const [key, label] of NOURISHMENT_FIELDS) {
    if (text(value[key]).length > 1000) return `${label}不能超过 1000 字`;
  }
  return '';
}

export function createTemplateChangeForm(detail = {}) {
  return {
    ...foodInformation(detail),
    templateType: detail.templateType,
    categoryId: detail.categoryId ?? '',
    name: detail.name || '',
    description: detail.description || '',
    imageUrl: detail.imageUrl || '', imagePreviewUrl: detail.imageUrl || '',
    imageAssetId: null, removeImage: false, imageRightsConfirmed: false,
    referencePrice: detail.referencePrice ?? '',
    tasteTagsText: tags(detail.tasteTags).join('，'),
    mealTags: tags(detail.mealTags),
    sortOrder: detail.sortOrder ?? 0,
    enabled: detail.enabled !== false,
    ingredients: (detail.ingredients || []).map((item, index) => ({
      itemId: stableId('ingredient', item, index),
      ingredientName: item.ingredientName || '',
      ingredientCategory: item.ingredientCategory || '',
      quantityStatus: item.quantityStatus || (item.quantity == null ? 'MISSING' : 'VERIFIED'),
      quantity: item.quantity ?? '', unit: item.unit || '', calcType: item.calcType || '',
      sourceText: item.sourceText || '', sourceQuantityText: item.sourceQuantityText || '',
      componentTemplateId: item.componentTemplateId ?? '',
      componentMultiplier: item.componentMultiplier ?? '', sortOrder: item.sortOrder ?? index + 1
    })),
    cookingSteps: (detail.cookingSteps || []).slice()
      .sort((left, right) => Number(left.stepNo || 0) - Number(right.stepNo || 0))
      .map((item, index) => ({
        itemId: stableId('step', item, index), stepNo: index + 1, title: item.title || '',
        imageUrls: [...(item.imageUrls || [])],
        content: item.content || '', durationSeconds: item.durationSeconds ?? '',
        temperatureText: item.temperatureText || '', heatLevel: item.heatLevel || '',
        componentTemplateId: item.componentTemplateId ?? ''
      }))
  };
}

export function buildTemplateSnapshot(form) {
  return {
    ...foodInformation(form),
    schemaVersion: 2,
    categoryId: Number(form.categoryId),
    name: text(form.name),
    description: text(form.description) || null,
    imageUrl: text(form.imageUrl) || null,
    imageAssetId: nullableNumber(form.imageAssetId),
    removeImage: Boolean(form.removeImage),
    imageRightsConfirmed: Boolean(form.imageRightsConfirmed),
    referencePrice: nullableNumber(form.referencePrice),
    tasteTags: tags(form.tasteTagsText),
    mealTags: tags(form.mealTags).filter((item) => ['BREAKFAST', 'LUNCH', 'DINNER'].includes(item)),
    sortOrder: Number(form.sortOrder || 0),
    enabled: Boolean(form.enabled),
    ingredients: (form.ingredients || []).map((item, index) => {
      const status = item.quantityStatus || 'MISSING';
      return {
        itemId: stableId('ingredient', item, index),
        ingredientName: text(item.ingredientName), ingredientCategory: text(item.ingredientCategory),
        quantityStatus: status,
        quantity: status === 'VERIFIED' ? nullableNumber(item.quantity) : null,
        unit: status === 'VERIFIED' ? text(item.unit) || null : null,
        calcType: status === 'VERIFIED' ? item.calcType || null : null,
        sourceText: text(item.sourceText) || null,
        sourceQuantityText: status === 'SOURCE_BATCH' ? text(item.sourceQuantityText) || null : null,
        componentTemplateId: nullableNumber(item.componentTemplateId),
        componentMultiplier: nullableNumber(item.componentMultiplier),
        sortOrder: index + 1
      };
    }),
    cookingSteps: (form.cookingSteps || []).map((item, index) => ({
      itemId: stableId('step', item, index), stepNo: index + 1, title: text(item.title) || null,
      imageUrls: [...(item.imageUrls || [])],
      content: text(item.content), durationSeconds: nullableNumber(item.durationSeconds),
      temperatureText: text(item.temperatureText) || null, heatLevel: text(item.heatLevel) || null,
      componentTemplateId: nullableNumber(item.componentTemplateId)
    }))
  };
}

export function validateTemplateSnapshot(value) {
  const foodError = validateFoodInformation(value);
  if (foodError) return foodError;
  if (!Number.isInteger(value.categoryId) || value.categoryId < 1) return '请选择模板分类';
  if (!value.name) return '请填写菜品名称';
  if (value.imageAssetId !== null && !value.imageRightsConfirmed) return '请确认拥有新图片的合法使用权';
  if (value.referencePrice !== null && (!Number.isFinite(value.referencePrice) || value.referencePrice < 0)) return '请填写正确的参考价格';
  for (const item of value.ingredients) {
    if (!item.ingredientName || !item.ingredientCategory) return '请补全食材名称和分类';
    if (!QUANTITY_STATUS_LABELS[item.quantityStatus]) return '请选择食材用量状态';
    if (item.quantityStatus === 'VERIFIED' && (!Number.isFinite(item.quantity) || item.quantity <= 0 || !item.unit || !['FIXED', 'PER_PERSON'].includes(item.calcType))) return '请补全已核定食材的用量、单位和计算方式';
    if (item.quantityStatus === 'SOURCE_BATCH' && !item.sourceQuantityText) return '请填写原配方批量用量';
  }
  if (value.cookingSteps.some((item) => !item.content)) return '请补全制作步骤内容';
  if (value.cookingSteps.some(item => item.imageUrls && (!Array.isArray(item.imageUrls) || item.imageUrls.length > 5))) return '每个步骤最多 5 张图片';
  return '';
}

export function formatTemplateChangeTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-';
}
