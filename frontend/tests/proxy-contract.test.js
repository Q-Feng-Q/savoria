const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const frontendRoot = path.resolve(__dirname, '..');

test('mini program production config targets the approved cloud hosting service', () => {
  const appSource = fs.readFileSync(path.join(frontendRoot, 'app.js'), 'utf8');
  assert.match(appSource, /const CLOUD_ENV = 'prod-d5g0vleyp9ed264bf'/);
  assert.match(appSource, /const CLOUD_SERVICE = 'springboot-6dl0'/);
  assert.match(appSource, /wx\.cloud\.init\(/);
  assert.match(appSource, /enabled:\s*true/);
});

test('proxy has a start script and development files stay outside the upload package', () => {
  const packageJson = JSON.parse(fs.readFileSync(path.join(frontendRoot, 'package.json'), 'utf8'));
  const projectConfig = JSON.parse(fs.readFileSync(path.join(frontendRoot, 'project.config.json'), 'utf8'));
  const ignoredValues = projectConfig.packOptions.ignore.map((item) => item.value);

  assert.equal(packageJson.scripts.proxy, 'node dev-proxy.js');
  assert.ok(ignoredValues.includes('dev-proxy.js'));
  assert.ok(ignoredValues.includes('package.json'));
});
