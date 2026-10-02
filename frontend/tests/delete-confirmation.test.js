const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

function withPage(name, run) {
  const pagePath = path.join(root, 'pages', 'merchant', name, 'index.js');
  const oldPage = global.Page;
  const oldWx = global.wx;
  let definition;
  global.Page = (value) => { definition = value; };
  global.wx = { showModal: async () => ({ confirm: false }), showToast() {} };
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
    global.Page = oldPage;
    global.wx = oldWx;
  }
}

test('dish editor keeps ingredient and step when deletion is cancelled', async () => withPage('dish-edit', async (page) => {
  page.setData({ dish: { ingredients: [{ id: 'rice', name: '大米' }], cookingSteps: [{ title: '煮', content: '煮熟' }] } });
  await page.removeIngredient({ currentTarget: { dataset: { index: 0 } } });
  await page.removeCookingStep({ currentTarget: { dataset: { index: 0 } } });
  assert.equal(page.data.dish.ingredients.length, 1);
  assert.equal(page.data.dish.cookingSteps.length, 1);
  assert.equal(page._formDirty, undefined);
}));

test('template change editor keeps image, ingredient and step when deletion is cancelled', async () => withPage('dish-template-change-edit', async (page) => {
  page.setData({ form: { imagePreviewUrl: '/image.jpg', ingredients: [{ itemId: 1, ingredientName: '大米' }, { itemId: 2, ingredientName: '盐' }], cookingSteps: [{ itemId: 3, title: '煮', content: '煮熟' }] } });
  await page.removeTemplateImage();
  await page.removeIngredient({ currentTarget: { dataset: { index: 0 } } });
  await page.removeCookingStep({ currentTarget: { dataset: { index: 0 } } });
  assert.equal(page.data.form.imagePreviewUrl, '/image.jpg');
  assert.equal(page.data.form.ingredients.length, 2);
  assert.equal(page.data.form.cookingSteps.length, 1);
  assert.equal(page._formDirty, undefined);
}));

test('feedback image removal asks before clearing the draft image', () => {
  const source = read('pages/account/feedback-create/index.js');
  assert.match(source, /async removeImage\s*\(/);
  assert.match(source, /await confirmDelete\(/);
  assert.match(read('utils/confirm-delete.js'), /await wx\.showModal\(/);
});
