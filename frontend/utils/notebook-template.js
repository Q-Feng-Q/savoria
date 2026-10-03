const FIELD_TYPES = [
  'TEXT', 'LONG_TEXT', 'NUMBER', 'DATE', 'TIME', 'DATETIME',
  'SINGLE_SELECT', 'MULTI_SELECT', 'BOOLEAN', 'RATING', 'IMAGE'
];
const editorFor = (type) => ({ TEXT: 'input', LONG_TEXT: 'textarea', NUMBER: 'number',
  DATE: 'date', TIME: 'time', DATETIME: 'datetime', SINGLE_SELECT: 'picker',
  MULTI_SELECT: 'checks', BOOLEAN: 'switch', RATING: 'rating', IMAGE: 'image' })[type];

function validateFields(fields) {
  if (!Array.isArray(fields) || fields.length > 50) return { valid: false, message: '字段不能超过 50 项' };
  for (const field of fields) {
    if (!FIELD_TYPES.includes(field.type) || !String(field.label || '').trim()) {
      return { valid: false, message: '请填写字段名称和类型' };
    }
    if (['SINGLE_SELECT', 'MULTI_SELECT'].includes(field.type) &&
        (!Array.isArray(field.options) || !field.options.length || field.options.length > 30 ||
          field.options.some((option) => !String(option).trim()) ||
          new Set(field.options).size !== field.options.length)) {
      return { valid: false, message: '选项不能为空或重复' };
    }
  }
  return { valid: true };
}

function prepareFields(fields, previous = []) {
  return fields.map((field) => {
    const old = previous.find((candidate) => candidate.key === field.key);
    const next = { type: field.type, label: String(field.label || '').trim(),
      required: Boolean(field.required), options: field.options || [], unit: field.unit || null };
    if (old && old.type === field.type) next.key = old.key;
    return next;
  });
}

function present(value) {
  return !(value == null || value === '' || Array.isArray(value) && value.length === 0);
}

function validValue(field, value) {
  if (!present(value)) return !field.required;
  switch (field.type) {
    case 'TEXT': return typeof value === 'string' && value.length <= 2000;
    case 'LONG_TEXT': return typeof value === 'string' && value.length <= 20000;
    case 'NUMBER': return value !== '' && Number.isFinite(Number(value));
    case 'DATE': return /^\d{4}-\d{2}-\d{2}$/.test(value);
    case 'TIME': return /^\d{2}:\d{2}/.test(value);
    case 'DATETIME': return typeof value === 'string' &&
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/.test(value) &&
      !Number.isNaN(Date.parse(value));
    case 'SINGLE_SELECT': return (field.options || []).includes(value);
    case 'MULTI_SELECT': return Array.isArray(value) && value.every((item) =>
      (field.options || []).includes(item)) && new Set(value).size === value.length;
    case 'BOOLEAN': return typeof value === 'boolean';
    case 'RATING': return Number.isInteger(Number(value)) && Number(value) >= 1 && Number(value) <= 5;
    case 'IMAGE': return Array.isArray(value) && value.length <= 10 && value.every((item) =>
      typeof item === 'string' && /^[a-fA-F0-9-]{36}\.(png|jpg)$/.test(item));
    default: return false;
  }
}

function validateValues(fields, values) {
  for (const field of fields || []) {
    if (!validValue(field, (values || {})[field.key])) {
      return { valid: false, message: `请检查「${field.label || field.key}」` };
    }
  }
  return { valid: true };
}

function normalizeValues(fields, values) {
  const output = {};
  for (const field of fields || []) {
    const value = (values || {})[field.key];
    if (!present(value)) continue;
    output[field.key] = ['NUMBER', 'RATING'].includes(field.type) ? Number(value) : value;
  }
  return output;
}

module.exports = { FIELD_TYPES, editorFor, validateFields, prepareFields,
  validateValues, normalizeValues };
