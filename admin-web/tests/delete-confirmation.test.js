const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

function functionSource(source, name) {
  const start = source.search(new RegExp(`(?:async )?function ${name}\\(`));
  assert.notEqual(start, -1, `${name} exists`);
  const next = source.slice(start + 1).search(/\n(?:async )?function \w+\(/);
  return source.slice(start, next < 0 ? undefined : start + 1 + next);
}

test('merchant purchase deletion asks before calling the backend', () => {
  const body = functionSource(read('src/views/merchant/PurchasesView.vue'), 'deleteTempItem');
  assert.match(body, /await confirmAction\(/);
  assert.ok(body.indexOf('await confirmAction(') < body.indexOf('deleteTempPurchaseItem('));
});

test('admin dish and template editors confirm destructive local changes', () => {
  for (const [file, handlers] of [
    ['src/views/merchant/DishesView.vue', ['removeIngredientRow', 'removeStepRow']],
    ['src/views/merchant/DishTemplatesView.vue', ['removeTemplateIngredient', 'removeCookingStep', 'removeTemplateImage']],
    ['src/views/platform/DishTemplateDetailView.vue', ['removeIngredient', 'removeStep']]
  ]) {
    const source = read(file);
    for (const name of handlers) {
      const body = functionSource(source, name);
      assert.match(body, /await confirmAction\(/, `${file}: ${name} requires confirmation`);
      assert.match(body, /if \(!.*\) return;/, `${file}: ${name} must respect cancellation`);
    }
  }
});

test('shared admin step image control confirms before removing a photo', () => {
  const body = functionSource(read('src/components/StepImages.vue'), 'remove');
  assert.match(body, /await confirmAction\(/);
  assert.ok(body.indexOf('await confirmAction(') < body.indexOf("emit('update:modelValue'"));
});
