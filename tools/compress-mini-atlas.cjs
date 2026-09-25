// Size optimization only: keep the approved composition and full pixel dimensions.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require(process.env.KITCHEN_SHARP || 'C:/Users/Q/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = path.resolve(__dirname, '..');
async function main() {
  const source = path.join(root, 'frontend/assets/brand/approved-story-atlas.jpg');
  const backup = path.join(root, 'output/brand/source-backups/20260919-approved-story-atlas.jpg');
  fs.mkdirSync(path.dirname(backup), { recursive: true });
  if (!fs.existsSync(backup)) fs.copyFileSync(source, backup, fs.constants.COPYFILE_EXCL);
  // Always encode from the retained original: repeat runs do not accumulate JPEG loss.
  const original = await sharp(backup).metadata();
  const compressed = await sharp(backup).jpeg({ quality: 85, mozjpeg: true }).toBuffer();
  const result = await sharp(compressed).metadata();
  if (original.width !== result.width || original.height !== result.height) throw new Error('Image dimensions changed');
  if (compressed.length >= fs.statSync(backup).size) throw new Error('No size improvement');
  fs.writeFileSync(source, compressed);
  console.log(JSON.stringify({ originalBytes: fs.statSync(backup).size, optimizedBytes: compressed.length, width: result.width, height: result.height, backup }));
}
main().catch(error => { console.error(error); process.exitCode = 1; });
