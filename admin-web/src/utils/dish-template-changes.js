export const TEMPLATE_CHANGE_STATUS = {
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', WITHDRAWN: '已撤回'
};

const text = (value) => String(value ?? '').trim();
const tags = (value) => Array.from(new Set((Array.isArray(value) ? value : String(value || '').split(/[，,]/)).map(text).filter(Boolean)));

export function createTemplateChangeForm(detail = {}) {
  return {
    categoryId: detail.categoryId ?? '', name: detail.name || '', description: detail.description || '',
    imageUrl: detail.imageUrl || '', imageSourceUrl: detail.imageSourceUrl || '', imageAuthor: detail.imageAuthor || '',
    imageLicense: detail.imageLicense || '', referencePrice: detail.referencePrice ?? 0,
    tasteTagsText: tags(detail.tasteTags).join('，'), mealTags: tags(detail.mealTags),
    sortOrder: detail.sortOrder ?? 0, enabled: detail.enabled !== false,
    ingredients: (detail.ingredients || []).map((item, index) => ({
      ingredientName: item.ingredientName || '', ingredientCategory: item.ingredientCategory || '',
      quantity: item.quantity ?? 0, unit: item.unit || '', calcType: item.calcType || 'FIXED', sortOrder: item.sortOrder ?? index + 1
    }))
  };
}

export function buildTemplateSnapshot(form) {
  return {
    schemaVersion: 1, categoryId: Number(form.categoryId), name: text(form.name), description: text(form.description),
    imageUrl: text(form.imageUrl), imageSourceUrl: text(form.imageSourceUrl), imageAuthor: text(form.imageAuthor),
    imageLicense: text(form.imageLicense), referencePrice: Number(form.referencePrice), tasteTags: tags(form.tasteTagsText),
    mealTags: tags(form.mealTags).filter((item) => ['BREAKFAST', 'LUNCH', 'DINNER'].includes(item)),
    sortOrder: Number(form.sortOrder || 0), enabled: Boolean(form.enabled),
    ingredients: (form.ingredients || []).map((item, index) => ({
      ingredientName: text(item.ingredientName), ingredientCategory: text(item.ingredientCategory),
      quantity: Number(item.quantity), unit: text(item.unit), calcType: item.calcType || 'FIXED', sortOrder: index + 1
    }))
  };
}

export function validateTemplateSnapshot(value) {
  if (!Number.isInteger(value.categoryId) || value.categoryId < 1) return '请选择模板分类';
  if (!value.name || !value.description) return '请填写菜品名称和简介';
  if (!value.imageUrl || !value.imageSourceUrl || !value.imageAuthor || !value.imageLicense) return '请补全图片与版权信息';
  if (!Number.isFinite(value.referencePrice) || value.referencePrice < 0) return '请填写正确的参考价格';
  if (!value.ingredients.length) return '至少需要 1 项食材';
  if (value.ingredients.some((item) => !item.ingredientName || !item.ingredientCategory || !item.unit || !Number.isFinite(item.quantity) || item.quantity < 0)) return '请补全食材名称、分类、用量和单位';
  return '';
}

export function formatTemplateChangeTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-';
}
