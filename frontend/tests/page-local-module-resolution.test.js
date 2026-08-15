const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const frontendRoot = path.resolve(__dirname, '..');
const pagesRoot = path.join(frontendRoot, 'pages');

function collectJavaScriptFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = path.join(directory, entry.name);
    return entry.isDirectory()
      ? collectJavaScriptFiles(absolutePath)
      : entry.isFile() && entry.name.endsWith('.js')
        ? [absolutePath]
        : [];
  });
}

test('every relative module imported by a mini-program page resolves to a source file', () => {
  const unresolved = [];

  for (const sourceFile of collectJavaScriptFiles(pagesRoot)) {
    const source = fs.readFileSync(sourceFile, 'utf8');
    const imports = source.matchAll(/require\(['"](\.[^'"]+)['"]\)/g);

    for (const [, request] of imports) {
      const target = path.resolve(path.dirname(sourceFile), request);
      const exists = fs.existsSync(target) || fs.existsSync(`${target}.js`) || fs.existsSync(path.join(target, 'index.js'));
      if (!exists) {
        unresolved.push(`${path.relative(frontendRoot, sourceFile)} -> ${request}`);
      }
    }
  }

  assert.deepEqual(unresolved, []);
});
