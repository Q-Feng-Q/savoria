const PRODUCT_TYPES = [
  { value: 'NORMAL', label: '普通菜品' },
  { value: 'NOURISHMENT', label: '滋补食品' }
];
const TEXT_FIELDS = ['nourishmentDescription', 'servingAdvice', 'precautions'];

function nourishmentFields(source = {}) {
  const fields = { productType: source.productType == null ? 'NORMAL' : source.productType };
  TEXT_FIELDS.forEach((key) => { fields[key] = String(source[key] == null ? '' : source[key]).trim(); });
  return fields;
}

function validateNourishment(source = {}) {
  const fields = nourishmentFields(source);
  if (!PRODUCT_TYPES.some((item) => item.value === fields.productType)) return '请选择正确的菜品类型';
  if (TEXT_FIELDS.some((key) => fields[key].length > 1000)) return '滋补介绍、食用建议和注意事项每项最多 1000 字';
  return '';
}

function buildNourishmentSections(source = {}) {
  if (source.productType !== 'NOURISHMENT' || source.templateType === 'COMPONENT') return [];
  const fields = nourishmentFields(source);
  const labels = ['滋补介绍', '食用建议', '注意事项'];
  return TEXT_FIELDS.map((key, index) => ({ key, label: labels[index], text: fields[key] }))
    .filter((item) => item.text);
}

module.exports = { PRODUCT_TYPES, nourishmentFields, validateNourishment, buildNourishmentSections };
