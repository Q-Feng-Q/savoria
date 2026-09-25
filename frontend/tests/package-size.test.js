const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { collectPackageFiles, reportPackageSize } = require('../scripts/check-package-size.cjs');
const root = path.resolve(__dirname, '..');

test('upload source package leaves headroom below the 2 MiB compiled limit', () => {
  const report = reportPackageSize();
  assert.ok(report.totalBytes <= 1800 * 1024, `upload sources ${(report.totalBytes / 1024).toFixed(1)} KiB exceed 1800 KiB budget`);
});

test('unreferenced high-resolution welcome source is not uploaded', () => {
  assert.equal(collectPackageFiles().some(item => item.file === 'assets/brand/scenes/auth-welcome.png'), false);
  assert.ok(fs.existsSync(path.join(root, 'assets/brand/scenes/auth-welcome.png')), 'keep editable source locally');
});

test('every statically referenced local image remains in the upload package', () => {
  const files = collectPackageFiles();
  const packaged = new Set(files.map(item => item.file));
  for (const item of files.filter(item => item.file !== 'project.config.json' && /\.(?:js|json|wxml|wxss)$/.test(item.file))) {
    const content = fs.readFileSync(path.join(root, item.file), 'utf8');
    for (const match of content.matchAll(/(?:\/)?(assets\/[A-Za-z0-9_./-]+\.(?:png|jpg|jpeg|webp|gif|svg))/g)) {
      assert.ok(packaged.has(match[1]), `${item.file} references an excluded/missing image: ${match[1]}`);
    }
  }
  // Brand logos are selected with a template string, rather than static paths.
  for (const size of [64, 128, 256]) assert.ok(packaged.has(`assets/brand/logo/logo-${size}.png`));
});
