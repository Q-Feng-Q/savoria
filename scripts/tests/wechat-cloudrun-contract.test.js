const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.resolve(__dirname, '..', '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const normalize = (value) => value.replace(/\r\n/g, '\n');

test('default and archived backend-only packages run Spring Boot directly on 8081', () => {
  const dockerfile = read('Dockerfile');
  const archived = read('docker/cloud-hosting-backend/Dockerfile');

  assert.equal(normalize(archived), normalize(dockerfile));
  assert.match(dockerfile, /FROM maven:3\.9[^\n]+ AS builder/i);
  assert.match(dockerfile, /COPY backend\/pom\.xml/);
  assert.match(dockerfile, /COPY backend\//);
  assert.match(dockerfile, /mvn -B -DskipTests clean package/);
  assert.match(dockerfile, /FROM eclipse-temurin:17-jre-alpine/i);
  assert.match(dockerfile, /apk add --no-cache[^\n]*curl[^\n]*tzdata/);
  assert.doesNotMatch(dockerfile, /nginx|SERVER_PORT|cloudrun-entrypoint/);
  assert.match(dockerfile, /USER 10001:10001/);
  assert.match(dockerfile, /PORT=8081/);
  assert.match(dockerfile, /EXPOSE 8081/);
  assert.match(dockerfile, /HEALTHCHECK/);
  assert.match(dockerfile, /ENTRYPOINT \["java", "-jar", "\/app\/app\.jar"\]/);
  assert.doesNotMatch(dockerfile, /mysql-server|admin-web/);

  const config = JSON.parse(read('container.config.json'));
  const archivedConfig = JSON.parse(read(
    'docker/cloud-hosting-backend/container.config.json'));
  assert.deepEqual(archivedConfig, config);
  assert.equal(config.containerPort, 8081);
  assert.equal(config.envParams.PORT, '8081');
  assert.equal(Object.hasOwn(config.envParams, 'SERVER_PORT'), false);
  assert.equal(JSON.stringify(config).includes('PASSWORD'), false);
  assert.equal(JSON.stringify(config).includes('SECRET'), false);
});

test('complete archived package preserves the nginx and Spring Boot topology', () => {
  const packageRoot = 'docker/cloud-hosting-full';
  const dockerfile = read(`${packageRoot}/Dockerfile`);
  assert.match(dockerfile, /apk add --no-cache[^\n]*curl[^\n]*nginx[^\n]*tini/);
  assert.match(dockerfile, /EXPOSE 8080/);
  assert.match(dockerfile, /SERVER_ADDRESS=0\.0\.0\.0/);
  assert.match(dockerfile, /SERVER_PORT=8081/);
  assert.match(dockerfile,
    /COPY --chown=10001:10001 docker\/cloud-hosting-full\/cloudrun-entrypoint\.sh \/app\/cloudrun-entrypoint\.sh/);
  assert.match(dockerfile,
    /COPY --chown=10001:10001 docker\/cloud-hosting-full\/cloudrun-nginx\.conf \/etc\/nginx\/nginx\.conf/);
  assert.match(dockerfile,
    /ENTRYPOINT \["\/sbin\/tini", "--", "\/app\/cloudrun-entrypoint\.sh"\]/);

  const entrypoint = read(`${packageRoot}/cloudrun-entrypoint.sh`);
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

  const nginx = read(`${packageRoot}/cloudrun-nginx.conf`);
  assert.match(nginx, /listen 0\.0\.0\.0:8080/);
  assert.match(nginx, /proxy_pass http:\/\/127\.0\.0\.1:8081/);
  assert.match(nginx, /location = \/healthz/);

  const config = JSON.parse(read(`${packageRoot}/container.config.json`));
  assert.equal(config.containerPort, 8080);
  assert.equal(config.envParams.PORT, '8080');
  assert.equal(config.envParams.SERVER_PORT, '8081');
});

test('runtime configuration accepts cloud mysql variables and keeps secrets external', () => {
  const yaml = read('backend/src/main/resources/application.yml');
  assert.match(yaml, /port:\s*\$\{PORT:8081\}/);
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
