# syntax=docker/dockerfile:1
#
# Image of notification-service. Built from the repository root, because Gradle needs the wrapper, the settings and
# the root build file (which brings the Spring Boot plugin):
#   docker build -f services/notification-service/Dockerfile -t notification-service:local .
# docker-compose.yaml does the same (service "notification-service").

# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Only what the build needs; the main app's src/ isn't copied. Tests are not run here: CI has already run them.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY services/notification-service services/notification-service

# The cache mount keeps Gradle and the dependencies between builds. It has its own id: Docker Compose builds this
# image at the same time as the main app's, and two Gradle builds in separate containers can't share one Gradle home
# (each waits for the other's lock until it times out).
# sed/chmod: on Windows the wrapper script can arrive with CRLF line endings or without the executable bit.
RUN --mount=type=cache,id=gradle-notification-service,target=/root/.gradle \
    sed -i 's/\r$//' gradlew && chmod +x gradlew && \
    ./gradlew --no-daemon :notification-service:bootJar

# ---------- Stage 2: the image that runs ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Do not run as root inside the container.
RUN groupadd --system notifications && useradd --system --gid notifications --no-create-home notifications

# build/libs holds exactly one jar (the plain jar is disabled in the service's build.gradle.kts).
COPY --from=build /workspace/services/notification-service/build/libs/*.jar app.jar

USER notifications
EXPOSE 8082

# The heap may use up to 75% of the container's memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
