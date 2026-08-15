const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.join(__dirname, '..');

function collectSource(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === '__tests__' || entry.name === 'assets') return [];
      return collectSource(full);
    }
    return /\.(vue|js|html)$/.test(entry.name) ? [full] : [];
  });
}

test('admin user-visible source uses 食光知味', () => {
  const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
  const source = collectSource(path.join(root, 'src'))
    .map((file) => fs.readFileSync(file, 'utf8'))
    .join('\n');

  assert.match(html, /<title>食光知味/);
  assert.doesNotMatch(`${html}\n${source}`, /灶间私厨|家庭厨房|Kitchen Admin/);
});
