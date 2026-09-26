# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace/backend

COPY backend/pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY backend/ ./
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl nginx tini tzdata \
    && addgroup -g 10001 -S app \
    && adduser -u 10001 -S -D -H -G app app \
    && mkdir -p /app /data/uploads/dish-template-assets /data/feedback-private /data/dish-template-assets/private \
        /tmp/nginx/client-body /tmp/nginx/proxy /tmp/nginx/fastcgi /tmp/nginx/uwsgi /tmp/nginx/scgi \
    && chown -R app:app /app /data /tmp/nginx

COPY --from=builder --chown=10001:10001 /workspace/backend/target/family-kitchen-backend-*.jar /app/app.jar
COPY --chown=10001:10001 docker/cloud-hosting-full/cloudrun-entrypoint.sh /app/cloudrun-entrypoint.sh
COPY --chown=10001:10001 docker/cloud-hosting-full/cloudrun-nginx.conf /etc/nginx/nginx.conf

RUN chmod 0555 /app/cloudrun-entrypoint.sh \
    && nginx -t

ENV TZ=Asia/Shanghai \
    PORT=8080 \
    SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=8081 \
    FAMILY_KITCHEN_FILE_STORAGE_LOCAL_ROOT=/data/uploads \
    FAMILY_KITCHEN_FEEDBACK_PRIVATE_ROOT=/data/feedback-private \
    FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PRIVATE_ROOT=/data/dish-template-assets/private \
    FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PUBLIC_ROOT=/data/uploads/dish-template-assets \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=300s --retries=5 \
  CMD curl --fail --silent --show-error --max-time 3 "http://127.0.0.1:${PORT}/healthz" >/dev/null || exit 1

ENTRYPOINT ["/sbin/tini", "--", "/app/cloudrun-entrypoint.sh"]
