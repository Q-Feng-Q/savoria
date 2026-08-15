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

FROM alpine:3.22.1 AS runtime
RUN apk add --no-cache nginx openjdk17-jre-headless tini \
    && addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app \
    && mkdir -p \
        /app \
        /data/uploads \
        /usr/share/nginx/html \
        /tmp/nginx/client-body \
        /tmp/nginx/proxy \
        /tmp/nginx/fastcgi \
        /tmp/nginx/uwsgi \
        /tmp/nginx/scgi \
    && chown -R 10001:10001 \
        /app \
        /data/uploads \
        /usr/share/nginx/html \
        /tmp/nginx

COPY --from=admin-builder --chown=10001:10001 /workspace/admin-web/dist/ /usr/share/nginx/html/
COPY --from=backend-builder --chown=10001:10001 /workspace/backend/target/family-kitchen-backend-*.jar /app/app.jar
COPY --chown=10001:10001 docker/single-image/nginx.conf /etc/nginx/nginx.conf
COPY --chown=10001:10001 docker/single-image/entrypoint.sh /app/entrypoint.sh

RUN chmod 0555 /app/entrypoint.sh

ENV SERVER_ADDRESS=127.0.0.1 \
    SERVER_PORT=8081 \
    FAMILY_KITCHEN_FILE_STORAGE_LOCAL_ROOT=/data/uploads

VOLUME ["/data/uploads"]
EXPOSE 8080
USER 10001:10001

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/api/public/system-settings || exit 1

ENTRYPOINT ["/sbin/tini", "--", "/app/entrypoint.sh"]
