# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
# Full JDK 21 (same version as the Gradle toolchain in build.gradle.kts).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Only what the build needs. Tests are not run here: CI has already run them.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src

# sed/chmod: on Windows the wrapper script can arrive with CRLF line endings or without the executable bit.
# The cache mount keeps the downloaded Gradle and dependencies between builds on the same machine,
# so only the first build downloads everything.
RUN --mount=type=cache,target=/root/.gradle \
    sed -i 's/\r$//' gradlew && chmod +x gradlew && \
    ./gradlew --no-daemon bootJar

# ---------- Stage 2: the image that runs ----------
# JRE only: no compiler, no Gradle, no source code.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Do not run as root inside the container.
RUN groupadd --system banking && useradd --system --gid banking --no-create-home banking

# build/libs holds exactly one jar (the plain jar is disabled in build.gradle.kts).
COPY --from=build /workspace/build/libs/*.jar app.jar

USER banking
EXPOSE 8080

# The heap may use up to 75% of the container's memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
