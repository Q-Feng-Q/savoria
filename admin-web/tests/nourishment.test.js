const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = file => fs.readFileSync(path.join(__dirname, '..', file), 'utf8');
const load = async file => import(`data:text/javascript;base64,${Buffer.from(read(file)).toString('base64')}`);

test('snapshot comparison preserves multiline food descriptions', () => {
  assert.match(read('src/components/SnapshotComparison.vue'), /\.snapshot-grid\s*>\s*span\s*\{[^}]*white-space:\s*pre-wrap/s);
});

test('food type defaults only when absent and template forms retain component type', async () => {
  const { foodInformation, validateFoodInformation, createTemplateChangeForm } = await load('src/utils/dish-template-changes.js');
  assert.equal(foodInformation({ productType: '' }).productType, '');
  assert.ok(validateFoodInformation({ productType: '' }));
  assert.equal(validateFoodInformation({ productType: null }), '');
  assert.equal(createTemplateChangeForm({ templateType: 'COMPONENT' }).templateType, 'COMPONENT');
  assert.match(read('src/components/NourishmentFields.vue'), /v-if="form.templateType !== 'COMPONENT'"/);
  assert.match(read('src/components/ProductTypeBadge.vue'), /v-if="value === 'NOURISHMENT' && templateType !== 'COMPONENT'"/);
});

test('new snapshot fields preserve missing targets but distinguish historical omissions and clearing', () => {
  const source = read('src/components/SnapshotComparison.vue').split('<script setup>')[1].split('</script>')[0].replace(/^import .*;$/gm, '');
  const rowsFor = (baseSnapshot, targetSnapshot) => new Function('defineProps', 'computed', `${source}; return fieldRows;`)(() => ({ baseSnapshot, targetSnapshot }), fn => fn());
  for (const key of ['productType', 'nourishmentDescription', 'servingAdvice', 'precautions']) {
    for (const value of [undefined, null]) {
      const row = rowsFor({ [key]: 'old' }, { [key]: value }).find(row => row.key === key);
      assert.equal(row.after, '未修改，保留原值');
      assert.equal(row.changed, false);
    }
    assert.equal(rowsFor({}, {}) .find(row => row.key === key).before, '历史快照未记录');
    const cleared = rowsFor({ [key]: 'old' }, { [key]: '' }).find(row => row.key === key);
    assert.equal(cleared.after, '未填写');
    assert.equal(cleared.changed, true);
  }
});

test('pending reviews decode snapshots safely and merchant saving disables food fields', () => {
  const review = read('src/views/platform/DishReviewsView.vue');
  assert.match(review, /snapshotJson/);
  assert.match(review, /JSON.parse/);
  assert.match(review, /NOURISHMENT_FIELDS/);
  assert.doesNotMatch(review, /v-html/);
  const parseSource = review.match(/function parseSnapshot\(snapshotJson\) \{[\s\S]*?\n\}/)[0];
  const parse = new Function(`${parseSource}; return parseSnapshot;`)();
  assert.equal(parse('{broken'), null);
  assert.equal(parse('null'), null);
  assert.equal(parse('[]'), null);
  assert.deepEqual(parse('{"productType":"NOURISHMENT","precautions":"<script>alert(1)</script>"}'), { productType: 'NOURISHMENT', precautions: '<script>alert(1)</script>' });
  const editor = read('src/views/merchant/DishesView.vue');
  assert.match(editor, /<NourishmentFields :form="form" :disabled="saving"/);
  assert.match(editor, /if \(saving.value \|\| stepUploading.value\) return/);
});

test('nourishment labels follow the approved terminology across editors, filters and review', () => {
  for (const file of ['components/NourishmentFields.vue', 'components/ProductTypeBadge.vue', 'components/SnapshotComparison.vue', 'views/merchant/DishesView.vue', 'views/merchant/DishTemplatesView.vue', 'views/platform/DishTemplatesView.vue']) {
    assert.ok(read(`src/${file}`).includes('滋补食品'), file);
    assert.doesNotMatch(read(`src/${file}`), /食养产品|食养说明/);
  }
  assert.match(read('src/utils/dish-template-changes.js'), /滋补介绍/);
  assert.match(read('src/components/SnapshotComparison.vue'), /滋补介绍/);
});

test('snapshot round trips food type and optional text including explicit clearing', async () => {
  const { createTemplateChangeForm, buildTemplateSnapshot, validateTemplateSnapshot } = await load('src/utils/dish-template-changes.js');
  const form = createTemplateChangeForm({ categoryId: 1, name: '汤', productType: 'NOURISHMENT', nourishmentDescription: ' 清淡 ', servingAdvice: ' 温热食用 ', precautions: ' 留意过敏 ' });
  const snapshot = buildTemplateSnapshot(form);
  assert.equal(snapshot.productType, 'NOURISHMENT');
  assert.equal(snapshot.nourishmentDescription, '清淡');
  assert.equal(snapshot.servingAdvice, '温热食用');
  assert.equal(snapshot.precautions, '留意过敏');
  assert.equal(validateTemplateSnapshot(snapshot), '');
  form.productType = 'NORMAL';
  assert.equal(buildTemplateSnapshot(form).nourishmentDescription, '清淡');
  form.nourishmentDescription = '  ';
  assert.equal(buildTemplateSnapshot(form).nourishmentDescription, '');
  assert.equal(createTemplateChangeForm().productType, 'NORMAL');
  for (const field of ['nourishmentDescription', 'servingAdvice', 'precautions']) {
    assert.ok(validateTemplateSnapshot({ ...snapshot, [field]: 'a'.repeat(1001) }));
    assert.equal(validateTemplateSnapshot({ ...snapshot, [field]: 'a'.repeat(1000) }), '');
  }
});

test('all existing editors share optional food information and lists expose independent type filters', () => {
  for (const file of ['merchant/DishesView.vue', 'merchant/DishTemplatesView.vue', 'platform/DishTemplateDetailView.vue']) {
    assert.match(read(`src/views/${file}`), /<NourishmentFields/);
  }
  for (const file of ['merchant/DishesView.vue', 'merchant/DishTemplatesView.vue', 'platform/DishTemplatesView.vue']) {
    assert.match(read(`src/views/${file}`), /productType/);
    assert.match(read(`src/views/${file}`), /<ProductTypeBadge/);
  }
  const editor = read('src/components/NourishmentFields.vue');
  assert.match(editor, /v-if=.*NOURISHMENT/);
  assert.match(editor, /maxlength="1000"/);
  assert.doesNotMatch(editor.split('<script setup>')[0], /required|watch\(/);
  const detail = read('src/components/NourishmentDetails.vue');
  assert.match(detail, /COMPONENT/);
  assert.match(detail, /NOURISHMENT/);
  assert.doesNotMatch(detail, /v-html/);
  for (const field of ['productType', 'nourishmentDescription', 'servingAdvice', 'precautions']) {
    assert.ok(read('src/components/SnapshotComparison.vue').includes(field));
  }
});

test('merchant dish API forwards optional type before requesting results', async () => {
  const source = read('src/api/dishes.js').replace("import { normalizeArray, request } from './http';", 'const normalizeArray = value => value; const request = async url => url;');
  const api = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
  assert.equal(await api.listMerchantDishes({ productType: 'NOURISHMENT' }), '/api/merchant/dishes?productType=NOURISHMENT');
  assert.equal(await api.listMerchantDishes(), '/api/merchant/dishes');
  assert.equal(await api.listDishTemplates({ productType: 'NOURISHMENT', page: 2 }), '/api/merchant/dish-templates?productType=NOURISHMENT&page=2');
});

test('list filters ignore responses from earlier requests', async () => {
  const cases = [
    ['merchant/DishesView.vue', /async function loadData\(\) \{[\s\S]*?\n\}/, 'loadData', 'listMerchantDishes'],
    ['merchant/DishTemplatesView.vue', /async function loadTemplates\(targetPage = 1\) \{[\s\S]*?\n\}/, 'loadTemplates', 'listDishTemplates'],
    ['platform/DishTemplatesView.vue', /async function load\(targetPage = 1\) \{[^\n]+/, 'load', 'listAdminDishTemplates']
  ];
  for (const [file, pattern, name, apiName] of cases) {
    const pending = [];
    const request = () => new Promise(resolve => pending.push(resolve));
    const loading = { value: false }, dishes = { value: [] }, page = { pageSize: 20 };
    const source = read(`src/views/${file}`).match(pattern)[0];
    const loadList = new Function(apiName, 'loading', 'dishes', 'page', `let loadGeneration = 0; const categories = {}, ingredientOptions = {}, query = {}, productTypeFilter = {}; const listDishCategories = async () => []; const listMerchantIngredients = async () => []; ${source}; return ${name};`)(request, loading, dishes, page);
    const first = loadList();
    const second = loadList();
    pending[1](name === 'loadData' ? ['new'] : { items: ['new'], total: 1 });
    await second;
    pending[0](name === 'loadData' ? ['old'] : { items: ['old'], total: 1 });
    await first;
    assert.deepEqual(name === 'loadData' ? dishes.value : page.items, ['new'], file);
    assert.equal(loading.value, false);
    const third = loadList();
    const fourth = loadList();
    pending[2](name === 'loadData' ? ['stale'] : { items: ['stale'], total: 1 });
    await third;
    assert.equal(loading.value, true, `${file}: older completion must not hide newer loading`);
    pending[3](name === 'loadData' ? ['latest'] : { items: ['latest'], total: 1 });
    await fourth;
    assert.equal(loading.value, false);
  }
});
