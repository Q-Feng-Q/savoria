# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace/backend

COPY backend/pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY backend/ ./
# The repository test suite is verified outside the image build. Keeping the
# production image build deterministic also avoids requiring Docker-in-Docker
# for Testcontainers-based tests in WeChat Cloud Hosting.
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl tzdata \
    && addgroup -g 10001 -S app \
    && adduser -u 10001 -S -D -H -G app app \
    && mkdir -p /app /data/uploads/dish-template-assets /data/feedback-private /data/dish-template-assets/private \
    && chown -R app:app /app /data

COPY --from=builder --chown=10001:10001 /workspace/backend/target/family-kitchen-backend-*.jar /app/app.jar

ENV TZ=Asia/Shanghai \
    PORT=8080 \
    FAMILY_KITCHEN_FILE_STORAGE_LOCAL_ROOT=/data/uploads \
    FAMILY_KITCHEN_FEEDBACK_PRIVATE_ROOT=/data/feedback-private \
    FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PRIVATE_ROOT=/data/dish-template-assets/private \
    FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PUBLIC_ROOT=/data/uploads/dish-template-assets \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=5 \
  CMD curl --fail --silent --show-error --max-time 3 "http://127.0.0.1:${PORT}/public/system-settings" >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
