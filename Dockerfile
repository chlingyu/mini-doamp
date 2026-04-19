# syntax=docker/dockerfile:1.7
# Multi-stage build for Mini DOAMP — Spring Boot 3.4 on Java 21
#
#   docker build -t mini-doamp:local .
#   docker run --rm -p 9999:9999 \
#     -e DB_URL=jdbc:mysql://host.docker.internal:3306/mini_doamp?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true \
#     -e JWT_SECRET=... mini-doamp:local
#
# Images: https://hub.docker.com/_/eclipse-temurin

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# First copy only the Gradle metadata to reuse the dependency cache layer
COPY gradle ./gradle
COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
COPY mini-doamp-api/build.gradle       ./mini-doamp-api/build.gradle
COPY mini-doamp-core/build.gradle      ./mini-doamp-core/build.gradle
COPY mini-doamp-doamp/build.gradle     ./mini-doamp-doamp/build.gradle
COPY mini-doamp-event/build.gradle     ./mini-doamp-event/build.gradle
COPY mini-doamp-gateway/build.gradle   ./mini-doamp-gateway/build.gradle
COPY mini-doamp-server/build.gradle    ./mini-doamp-server/build.gradle
COPY mini-doamp-sop/build.gradle       ./mini-doamp-sop/build.gradle

RUN chmod +x gradlew \
    # Prime the dependency cache; failure here shouldn't abort the build, the
    # real compile stage below will surface genuine errors.
    && ./gradlew --no-daemon :mini-doamp-server:dependencies > /dev/null 2>&1 || true

# Now pull the source and produce the fat jar
COPY mini-doamp-api       ./mini-doamp-api
COPY mini-doamp-core      ./mini-doamp-core
COPY mini-doamp-doamp     ./mini-doamp-doamp
COPY mini-doamp-event     ./mini-doamp-event
COPY mini-doamp-gateway   ./mini-doamp-gateway
COPY mini-doamp-server    ./mini-doamp-server
COPY mini-doamp-sop       ./mini-doamp-sop

RUN ./gradlew --no-daemon :mini-doamp-server:bootJar -x test

# Split Spring Boot's layered jar so each layer can be cached independently
RUN mkdir -p extracted && \
    java -Djarmode=layertools -jar mini-doamp-server/build/libs/mini-doamp.jar extract --destination extracted

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

# curl is used by HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"

# Non-root for security (Kubernetes runAsNonRoot-friendly)
RUN groupadd --system --gid 1000 app \
    && useradd --system --uid 1000 --gid app --home /app app

WORKDIR /app
COPY --from=builder --chown=app:app /workspace/extracted/dependencies/          ./
COPY --from=builder --chown=app:app /workspace/extracted/spring-boot-loader/    ./
COPY --from=builder --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/application/           ./

USER app

EXPOSE 9999
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:9999/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher"]
