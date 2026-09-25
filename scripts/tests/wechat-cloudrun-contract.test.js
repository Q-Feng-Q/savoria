const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.resolve(__dirname, '..', '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('cloudrun image builds only the backend and listens on the injected port', () => {
  const dockerfile = read('Dockerfile');
  assert.match(dockerfile, /FROM maven:3\.9[^\n]+ AS builder/i);
  assert.match(dockerfile, /COPY backend\/pom\.xml/);
  assert.match(dockerfile, /COPY backend\//);
  assert.match(dockerfile, /mvn -B -DskipTests clean package/);
  assert.match(dockerfile, /FROM eclipse-temurin:17-jre-alpine/i);
  assert.match(dockerfile, /USER 10001:10001/);
  assert.match(dockerfile, /EXPOSE 8080/);
  assert.match(dockerfile, /HEALTHCHECK/);
  assert.doesNotMatch(dockerfile, /mysql-server|nginx|admin-web/);
});

test('cloudrun config is safe for repository use and targets the Spring port', () => {
  const config = JSON.parse(read('container.config.json'));
  assert.equal(config.containerPort, 8080);
  assert.equal(config.customLogs, 'stdout');
  assert.deepEqual(config.envParams, {});
  assert.equal(JSON.stringify(config).includes('PASSWORD'), false);
  assert.equal(JSON.stringify(config).includes('SECRET'), false);
});

test('runtime configuration accepts cloud mysql variables and keeps secrets external', () => {
  const yaml = read('backend/src/main/resources/application.yml');
  assert.match(yaml, /port:\s*\$\{PORT:8080\}/);
  assert.match(yaml, /\$\{MYSQL_ADDRESS:/);
  assert.match(yaml, /username:\s*\$\{MYSQL_USERNAME:/);
  assert.match(yaml, /password:\s*\$\{MYSQL_PASSWORD:/);
  assert.match(yaml, /app-id:\s*"\$\{FAMILY_KITCHEN_WECHAT_APP_ID:/);
  assert.match(yaml, /app-secret:\s*"\$\{FAMILY_KITCHEN_WECHAT_APP_SECRET:/);
  assert.doesNotMatch(yaml, /app-secret:\s*"[a-zA-Z0-9]{16,}"/);
  const dockerfile = read('Dockerfile');
  assert.match(dockerfile, /FAMILY_KITCHEN_FILE_STORAGE_LOCAL_ROOT=\/data\/uploads/);
});

test('docker context excludes user data and unrelated clients', () => {
  const ignore = read('.dockerignore');
  for (const entry of ['uploads/', 'output/', 'frontend/', 'admin-web/', '**/target/']) {
    assert.match(ignore, new RegExp(`^${entry.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`, 'm'));
  }
});
