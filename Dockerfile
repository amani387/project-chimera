# Multi-stage Dockerfile for Project Chimera

# Stage 1: Build the app using Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy Maven wrapper and pom first to take advantage of caching
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Copy source and build
COPY src src
RUN ./mvnw -B -DskipTests package

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create a non-root user
RUN addgroup -S chimera && adduser -S chimera -G chimera

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /workspace/target/chimera-0.0.1-SNAPSHOT.jar /app/app.jar

# Ensure the runtime user owns the app directory
RUN chown -R chimera:chimera /app

USER chimera

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
