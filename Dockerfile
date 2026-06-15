# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.8-eclipse-temurin-8 AS build
WORKDIR /build
COPY . .
RUN mvn -B -P prod clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod
EXPOSE 8887
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
