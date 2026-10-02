const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const pagePath = path.join(root, 'pages', 'merchant', 'dish-edit', 'index.js');

function withDishEditor(run) {
  const previousPage = global.Page;
  const previousWx = global.wx;
  let definition;
  global.Page = (value) => { definition = value; };
  global.wx = { showToast() {} };
  try {
    delete require.cache[require.resolve(pagePath)];
    require(pagePath);
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update) {
        for (const [key, value] of Object.entries(update)) {
          if (key.includes('.')) {
            const [parent, child] = key.split('.');
            this.data[parent][child] = value;
          } else this.data[key] = value;
        }
      }
    });
    return run(page);
  } finally {
    delete require.cache[require.resolve(pagePath)];
    global.Page = previousPage;
    global.wx = previousWx;
  }
}

test('dish ingredient search filters by name or category and keeps original selection index', () => withDishEditor((page) => {
  page.setData({
    ingredients: [
      { ingredientId: 1, name: '大米', category: '主食', unit: '克' },
      { ingredientId: 2, name: '食盐', category: '调味', unit: '克' },
      { ingredientId: 3, name: '豆腐', category: '豆制品', unit: '克' }
    ],
    dish: { ingredients: [] },
    ingredientIndex: 0,
    selectedIngredientName: '大米'
  });

  page.bindIngredientSearch({ detail: { value: '调味' } });
  assert.deepEqual(page.data.filteredIngredients.map((item) => item.name), ['食盐']);

  page.bindIngredientSearch({ detail: { value: ' 豆 ' } });
  assert.deepEqual(page.data.filteredIngredients.map((item) => item.name), ['豆腐']);
  assert.equal(page.data.filteredIngredients[0].sourceIndex, 2);
  assert.equal(page.data.ingredientIndex, -1);
  assert.equal(page.data.ingredientResultsOpen, true);

  page.bindIngredient({ currentTarget: { dataset: { index: 2 } } });
  assert.equal(page.data.ingredientIndex, 2);
  assert.equal(page.data.selectedIngredientName, '豆腐');
  assert.equal(page.data.ingredientResultsOpen, false);
  page.setData({ newIngredientQuantity: '200' });
  page.addIngredient();
  assert.equal(page.data.dish.ingredients[0].ingredientId, 3);
}));

test('dish ingredient search shows empty state without accidentally adding the old selection', () => withDishEditor((page) => {
  const toasts = [];
  global.wx.showToast = (value) => toasts.push(value);
  page.setData({ ingredients: [{ ingredientId: 1, name: '大米', unit: '克' }], dish: { ingredients: [] }, ingredientIndex: 0, selectedIngredientName: '大米', newIngredientQuantity: '100' });
  page.bindIngredientSearch({ detail: { value: '不存在' } });
  assert.deepEqual(page.data.filteredIngredients, []);
  page.addIngredient();
  assert.deepEqual(page.data.dish.ingredients, []);
  assert.match(toasts[0].title, /选择原材料/);
}));

test('dish edit markup uses an inline searchable ingredient list instead of a native picker', () => {
  const markup = fs.readFileSync(path.join(root, 'pages', 'merchant', 'dish-edit', 'index.wxml'), 'utf8');
  assert.match(markup, /bindinput="bindIngredientSearch"/);
  assert.match(markup, /scroll-view[^>]*scroll-y/);
  assert.match(markup, /data-index="\{\{item\.sourceIndex\}\}"[^>]*bindtap="bindIngredient"/);
  assert.doesNotMatch(markup, /<picker[^>]*range="\{\{ingredients\}\}"/);
});
