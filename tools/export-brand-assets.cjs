// Mechanical resize only: preserve the approved drawing and transparent background.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require(process.env.KITCHEN_SHARP || 'C:/Users/Q/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = path.resolve(__dirname, '..');
async function main() {
  const source = path.join(root, 'output/brand/shi-guang-zhi-wei-20260916/logo-original.png');
  for (const folder of ['frontend/assets/brand/logo', 'admin-web/public/brand']) {
    fs.mkdirSync(path.join(root, folder), { recursive: true });
    for (const size of [64, 128, 256]) {
      const target = path.join(root, folder, `logo-${size}.png`);
      if (fs.existsSync(target)) throw new Error(`Refusing to overwrite ${target}`);
      await sharp(source).resize(size, size).png().toFile(target);
      console.log(folder, size, fs.statSync(target).size);
    }
  }
  fs.copyFileSync(path.join(root, 'output/brand/shi-guang-zhi-wei-20260916/favicon.ico'), path.join(root, 'admin-web/public/favicon.ico'), fs.constants.COPYFILE_EXCL);
}
main().catch(error => { console.error(error); process.exitCode = 1; });
