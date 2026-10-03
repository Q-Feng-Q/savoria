const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');

function collectPackageFiles(directory = root) {
  const config = JSON.parse(fs.readFileSync(path.join(directory, 'project.config.json'), 'utf8'));
  const rules = config.packOptions?.ignore || [];
  function ignored(file) {
    return rules.some(rule => rule.type === 'folder'
      ? file === rule.value || file.startsWith(`${rule.value}/`)
      : rule.type === 'file' && file === rule.value);
  }
  const files = [];
  function walk(folder) {
    for (const entry of fs.readdirSync(folder, { withFileTypes: true })) {
      const absolute = path.join(folder, entry.name);
      const file = path.relative(directory, absolute).replace(/\\/g, '/');
      if (ignored(file)) continue;
      if (entry.isDirectory()) walk(absolute);
      else files.push({ file, bytes: fs.statSync(absolute).size });
    }
  }
  walk(directory);
  return files;
}

function reportPackageSize(directory = root) {
  const files = collectPackageFiles(directory);
  const app = JSON.parse(fs.readFileSync(path.join(directory, 'app.json'), 'utf8'));
  const subpackages = Object.fromEntries((app.subPackages || []).map(({ root: packageRoot }) => [packageRoot, 0]));
  const totalBytes = files.reduce((total, file) => total + file.bytes, 0);
  let mainBytes = 0;
  for (const item of files) {
    const packageRoot = Object.keys(subpackages).find((entry) => item.file.startsWith(`${entry}/`));
    if (packageRoot) subpackages[packageRoot] += item.bytes;
    else mainBytes += item.bytes;
  }
  const assetBytes = files.filter(item => item.file.startsWith('assets/')).reduce((total, file) => total + file.bytes, 0);
  return { totalBytes, mainBytes, subpackages, assetBytes, fileCount: files.length,
    largest: [...files].sort((a, b) => b.bytes - a.bytes).slice(0, 8) };
}

if (require.main === module) {
  const report = reportPackageSize();
  console.log(JSON.stringify(report, null, 2));
  console.log('Source-size estimate only; the WeChat developer tool compiled package remains authoritative.');
  if (report.mainBytes > 1800 * 1024
      || Object.values(report.subpackages).some((size) => size > 1800 * 1024)) process.exitCode = 1;
}

module.exports = { collectPackageFiles, reportPackageSize };
