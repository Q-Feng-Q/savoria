import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/(?:[A-Za-z]:)/, (value) => value.slice(1))), '..');

function read(relativePath) {
  const absolutePath = path.join(root, relativePath);
  assert.ok(fs.existsSync(absolutePath), `缺少文件：${relativePath}`);
  return fs.readFileSync(absolutePath, 'utf8');
}

const dockerfile = read('docker/single-image/Dockerfile');
const entrypoint = read('docker/single-image/entrypoint.sh');
const mysqlConfig = read('docker/single-image/mysql.cnf');
const healthcheck = read('docker/single-image/healthcheck.sh');
const backup = read('docker/single-image/backup.sh');
const envExample = read('docker/single-image/family-kitchen.env.example');
const deployment = read('docs/deployment.md');
const powershellRunner = read('scripts/run-single-container.ps1');
const shellRunner = read('scripts/run-single-container.sh');

assert.match(dockerfile, /FROM ubuntu:24\.04 AS runtime/);
assert.match(dockerfile, /mysql-server/);
assert.match(dockerfile, /zz-family-kitchen\.cnf/);
assert.match(dockerfile, /VOLUME \["\/data"\]/);
assert.match(dockerfile, /HEALTHCHECK[\s\S]*healthcheck\.sh/);

for (const directory of ['mysql', 'uploads', 'feedback-private', 'dish-template-assets', 'backups']) {
  assert.match(entrypoint, new RegExp(`/data/${directory}`));
}

assert.match(entrypoint, /mysqld .*--initialize-insecure/);
assert.match(entrypoint, /CREATE DATABASE IF NOT EXISTS/);
assert.match(entrypoint, /\/data\/config\/mysql-root-password/);
assert.match(entrypoint, /ALTER USER 'root'@'localhost'/);
assert.match(entrypoint, /SPRING_DATASOURCE_URL=/);
assert.match(entrypoint, /FAMILY_KITCHEN_FEEDBACK_PRIVATE_ROOT=/);
assert.match(entrypoint, /FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PRIVATE_ROOT=/);

assert.match(mysqlConfig, /bind-address=127\.0\.0\.1/);
assert.match(mysqlConfig, /character-set-server=utf8mb4/);
assert.match(healthcheck, /mysqladmin/);
assert.match(healthcheck, /api\/public\/system-settings/);
assert.match(backup, /mysqldump/);
assert.match(backup, /\/data\/backups/);

for (const variable of [
  'MYSQL_APP_PASSWORD',
  'FAMILY_KITCHEN_JWT_SECRET',
  'FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD',
]) {
  assert.match(envExample, new RegExp(`^${variable}=`, 'm'));
}

assert.match(deployment, /MySQL、Spring Boot 和 Nginx/);
assert.match(deployment, /family-kitchen-data:\/data/);
assert.match(powershellRunner, /family-kitchen-data/);
assert.match(shellRunner, /family-kitchen-data/);

process.stdout.write('单容器部署包静态验证通过。\n');
