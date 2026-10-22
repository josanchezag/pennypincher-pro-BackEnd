# syntax=docker/dockerfile:1

# Stage 1: build the Spring Boot fat jar with the pinned Gradle wrapper
FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon --quiet > /dev/null
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# Stage 2: minimal runtime image, running as a non-root user
FROM eclipse-temurin:21-jre-alpine
ARG APP_UID=10001
ARG KEY_DIR=/app/keys
RUN addgroup -S -g ${APP_UID} appgroup && adduser -S -u ${APP_UID} -G appgroup appuser \
    && mkdir -p ${KEY_DIR} && chown ${APP_UID}:${APP_UID} ${KEY_DIR}
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER ${APP_UID}:${APP_UID}
ENV SPRING_PROFILES_ACTIVE=prod \
    JWT_LOCAL_KEY_DIR=${KEY_DIR}
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
