# syntax=docker/dockerfile:1.7

FROM node:20.19.4-alpine3.22 AS admin-builder
WORKDIR /workspace/admin-web
ARG RUN_TESTS=true
COPY admin-web/package.json admin-web/package-lock.json ./
RUN npm ci
COPY admin-web/ ./
RUN if [ "$RUN_TESTS" = "true" ]; then node --test tests; fi \
    && npm run build

FROM maven:3.9.11-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /workspace/backend
ARG RUN_TESTS=true
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY backend/ ./
RUN if [ "$RUN_TESTS" = "true" ]; then mvn test -B; fi
RUN mvn package -B -DskipTests

FROM ubuntu:24.04 AS runtime
ARG DEBIAN_FRONTEND=noninteractive
RUN printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d \
    && chmod 0755 /usr/sbin/policy-rc.d \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        gzip \
        mysql-client \
        mysql-server \
        nginx \
        openjdk-17-jre-headless \
        tini \
        util-linux \
    && rm -f /usr/sbin/policy-rc.d \
    && rm -rf /var/lib/apt/lists/* /var/lib/mysql/* /var/log/mysql/* \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin app \
    && mkdir -p \
        /app \
        /data \
        /run/mysqld \
        /usr/share/nginx/html \
        /tmp/nginx/client-body \
        /tmp/nginx/proxy \
        /tmp/nginx/fastcgi \
        /tmp/nginx/uwsgi \
        /tmp/nginx/scgi \
    && chown -R app:app /app /usr/share/nginx/html /tmp/nginx \
    && chown -R mysql:mysql /run/mysqld

COPY --from=admin-builder --chown=10001:10001 /workspace/admin-web/dist/ /usr/share/nginx/html/
COPY --from=backend-builder --chown=10001:10001 /workspace/backend/target/family-kitchen-backend-*.jar /app/app.jar
COPY --chown=app:app docker/single-image/nginx.conf /etc/nginx/nginx.conf
COPY docker/single-image/mysql.cnf /etc/mysql/mysql.conf.d/zz-family-kitchen.cnf
COPY docker/single-image/entrypoint.sh /app/entrypoint.sh
COPY docker/single-image/healthcheck.sh /app/healthcheck.sh
COPY docker/single-image/backup.sh /usr/local/bin/family-kitchen-backup

RUN chmod 0555 /app/entrypoint.sh /app/healthcheck.sh /usr/local/bin/family-kitchen-backup

ENV SERVER_ADDRESS=127.0.0.1 \
    SERVER_PORT=8081 \
    MYSQL_DATABASE=family_kitchen \
    MYSQL_APP_USERNAME=family_kitchen \
    MYSQL_DATADIR=/data/mysql \
    MYSQL_SOCKET=/run/mysqld/mysqld.sock \
    FAMILY_KITCHEN_FILE_STORAGE_LOCAL_ROOT=/data/uploads \
    FAMILY_KITCHEN_FEEDBACK_PRIVATE_ROOT=/data/feedback-private \
    FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PRIVATE_ROOT=/data/dish-template-assets/private \
    FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PUBLIC_ROOT=/data/uploads/dish-template-assets \
    JAVA_OPTS="-Xms128m -Xmx512m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

VOLUME ["/data"]
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=8s --start-period=180s --retries=5 \
  CMD ["/app/healthcheck.sh"]

ENTRYPOINT ["/usr/bin/tini", "--", "/app/entrypoint.sh"]
