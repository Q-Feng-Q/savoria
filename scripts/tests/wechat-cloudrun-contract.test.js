const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.resolve(__dirname, '..', '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

test('cloudrun image exposes nginx on 8080 and runs Spring Boot on 8081', () => {
  const dockerfile = read('Dockerfile');
  assert.match(dockerfile, /FROM maven:3\.9[^\n]+ AS builder/i);
  assert.match(dockerfile, /COPY backend\/pom\.xml/);
  assert.match(dockerfile, /COPY backend\//);
  assert.match(dockerfile, /mvn -B -DskipTests clean package/);
  assert.match(dockerfile, /FROM eclipse-temurin:17-jre-alpine/i);
  assert.match(dockerfile, /apk add --no-cache[^\n]*curl[^\n]*nginx[^\n]*tini/);
  assert.match(dockerfile, /USER 10001:10001/);
  assert.match(dockerfile, /EXPOSE 8080/);
  assert.match(dockerfile, /HEALTHCHECK/);
  assert.doesNotMatch(dockerfile, /mysql-server|admin-web/);
  assert.match(dockerfile, /COPY --chown=10001:10001 cloudrun-entrypoint\.sh \/app\/cloudrun-entrypoint\.sh/);
  assert.match(dockerfile, /COPY --chown=10001:10001 cloudrun-nginx\.conf \/etc\/nginx\/nginx\.conf/);
  assert.match(dockerfile, /SERVER_ADDRESS=0\.0\.0\.0/);
  assert.match(dockerfile, /SERVER_PORT=8081/);
  assert.match(dockerfile, /ENTRYPOINT \["\/sbin\/tini", "--", "\/app\/cloudrun-entrypoint\.sh"\]/);

  assert.ok(fs.existsSync(path.join(root, 'cloudrun-entrypoint.sh')));
  const entrypoint = read('cloudrun-entrypoint.sh');
  for (const variable of [
    'MYSQL_ADDRESS',
    'MYSQL_DATABASE',
    'MYSQL_USERNAME',
    'MYSQL_PASSWORD',
    'FAMILY_KITCHEN_JWT_SECRET',
    'FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD',
    'FAMILY_KITCHEN_WECHAT_APP_ID',
    'FAMILY_KITCHEN_WECHAT_APP_SECRET'
  ]) {
    assert.match(entrypoint, new RegExp(variable));
  }
  assert.match(entrypoint, /java -jar \/app\/app\.jar &/);
  assert.match(entrypoint, /nginx -g 'daemon off;' &/);
  assert.match(entrypoint, /Java process exited unexpectedly/);
  assert.match(entrypoint, /Nginx process exited unexpectedly/);

  const nginx = read('cloudrun-nginx.conf');
  assert.match(nginx, /listen 0\.0\.0\.0:8080/);
  assert.match(nginx, /proxy_pass http:\/\/127\.0\.0\.1:8081/);
  assert.match(nginx, /location = \/healthz/);
  assert.match(nginx, /proxy_pass http:\/\/127\.0\.0\.1:8081\/public\/system-settings/);

  const attributes = read('.gitattributes');
  assert.match(attributes, /^\*\.sh text eol=lf$/m);
});

test('cloudrun config is safe for repository use and targets the Spring port', () => {
  const config = JSON.parse(read('container.config.json'));
  assert.equal(config.containerPort, 8080);
  assert.equal(config.customLogs, 'stdout');
  assert.deepEqual(config.envParams, {
    PORT: '8080',
    SERVER_ADDRESS: '0.0.0.0',
    SERVER_PORT: '8081',
    MYSQL_DATABASE: 'family_kitchen',
    FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_USERNAME: 'admin',
    FAMILY_KITCHEN_WECHAT_APP_ID: 'wx092b0184d87bf822'
  });
  assert.ok(config.initialDelaySeconds >= 300);
  assert.equal(JSON.stringify(config).includes('PASSWORD'), false);
  assert.equal(JSON.stringify(config).includes('SECRET'), false);
});

test('runtime configuration accepts cloud mysql variables and keeps secrets external', () => {
  const yaml = read('backend/src/main/resources/application.yml');
  assert.match(yaml, /port:\s*\$\{PORT:8080\}/);
  assert.match(yaml, /\$\{MYSQL_ADDRESS\}/);
  assert.match(yaml, /username:\s*\$\{MYSQL_USERNAME\}/);
  assert.match(yaml, /password:\s*\$\{MYSQL_PASSWORD\}/);
  assert.match(yaml, /secret:\s*\$\{FAMILY_KITCHEN_JWT_SECRET\}/);
  assert.match(yaml, /password:\s*\$\{FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD\}/);
  assert.match(yaml, /app-id:\s*"\$\{FAMILY_KITCHEN_WECHAT_APP_ID\}"/);
  assert.match(yaml, /app-secret:\s*"\$\{FAMILY_KITCHEN_WECHAT_APP_SECRET\}"/);
  assert.doesNotMatch(yaml, /192\.168\.|MYSQL_PASSWORD:[^}\r\n]+|FAMILY_KITCHEN_JWT_SECRET:[^}\r\n]+/);
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
