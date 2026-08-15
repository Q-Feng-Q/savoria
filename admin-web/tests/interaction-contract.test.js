const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('admin views use the branded action dialog instead of browser prompts', () => {
  const views = path.resolve(__dirname, '..', 'src', 'views');
  const source = fs.readdirSync(views)
    .filter((name) => name.endsWith('.vue'))
    .map((name) => fs.readFileSync(path.join(views, name), 'utf8'))
    .join('\n');
  const app = fs.readFileSync(path.resolve(__dirname, '..', 'src', 'App.vue'), 'utf8');

  assert.doesNotMatch(source, /window\.(prompt|confirm)/);
  assert.match(app, /ActionDialog/);
});
