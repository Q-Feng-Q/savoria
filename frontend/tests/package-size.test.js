const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { collectPackageFiles, reportPackageSize } = require('../scripts/check-package-size.cjs');
const root = path.resolve(__dirname, '..');

test('each upload source package leaves headroom below its 2 MiB compiled limit', () => {
  const report = reportPackageSize();
  assert.ok(report.mainBytes <= 1800 * 1024,
    `main package ${(report.mainBytes / 1024).toFixed(1)} KiB exceeds 1800 KiB budget`);
  for (const [root, bytes] of Object.entries(report.subpackages)) {
    assert.ok(bytes <= 1800 * 1024,
      `${root} ${(bytes / 1024).toFixed(1)} KiB exceeds 1800 KiB budget`);
  }
  assert.ok(report.subpackages['pages/notebook/detail'] > 0, 'notebook detail pages use an isolated subpackage');
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
