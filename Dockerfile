# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.6-jdk-8 AS build
WORKDIR /app

# Cache dependencies first (leverages Docker layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -P prod -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM openjdk:8-jre-slim
WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser \
    && mkdir -p /app/logs \
    && chown -R appuser:appuser /app

COPY --from=build /app/target/community-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER appuser

EXPOSE 8887

# JVM tuning flags for container environments
ENTRYPOINT ["java", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:+UseCGroupMemoryLimitForHeap", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
